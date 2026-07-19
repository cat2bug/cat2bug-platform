package com.cat2bug.system.service.impl.report.parse;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.utils.DictUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.service.IReportParseService;
import com.cat2bug.system.service.ISysCaseService;
import com.cat2bug.system.service.ISysDefectService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-05-18 14:27
 * @Version: 1.0.0
 */
@Service
@SuppressWarnings("unchecked")
public class TableParseImpl implements IReportParseService {
    private final static Logger logger = LogManager.getLogger(TableParseImpl.class);

    private static final Pattern TOKEN = Pattern.compile(
            "\\$table\\{((?:\\w|\\.)+)(?:\\[([^\\]]*)\\])?\\}");

    private static final Map<String,String> defaultCaseTitleNames = new LinkedHashMap();

    private static final Map<String,String> defaultDefectTitleNames = new LinkedHashMap();

    @Lazy
    @Autowired
    private ISysCaseService sysCaseService;
    @Lazy
    @Autowired
    private ISysDefectService sysDefectService;

    public TableParseImpl() {
        Arrays.stream(new String[] {"projectNum","defectTypeName","defectLevel","defectName","defectStateName","moduleName","moduleVersion","defectDescribe","createBy","createTime",
                "updateBy","updateTime","imgList","annexList"}).forEach(s->defaultDefectTitleNames.put(s,s));
    }

    @Override
    public boolean isHandle(String content) {
        return StringUtils.isNotBlank(content) && TOKEN.matcher(content).find();
    }

    @Override
    public String parse(Long projectId, String content, Map<String, Object> para) {
        if (StringUtils.isBlank(content)) {
            return content;
        }
        Matcher matcher = TOKEN.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String api = matcher.group(1);
            if (StringUtils.isBlank(api)) {
                matcher.appendReplacement(sb, ReportParseUtils.quoteReplacement(matcher.group(0)));
                continue;
            }

            Map<String, String> params = new LinkedHashMap<>();
            String strParams = matcher.group(2);
            if (StringUtils.isNotBlank(strParams)) {
                String[] strs = strParams.split(",");
                for (String s : strs) {
                    if (StringUtils.isBlank(s)) {
                        continue;
                    }
                    String[] ss = s.split(":");
                    if (ss.length > 1) {
                        params.put(ss[0].trim(), ss[1].trim());
                    } else {
                        params.put(s.trim(), s.trim());
                    }
                }
            }
            List<Object> values = null;
            switch (api) {
                case "api.case.list":
                    values = this.getCaseList(projectId);
                    params = (params == null || params.size() == 0 ? defaultCaseTitleNames : params);
                    break;
                case "api.defect.list":
                    values = this.getDefectList(projectId, ReportParseUtils.safeParams(para));
                    params = (params == null || params.size() == 0 ? defaultDefectTitleNames : params);
                    break;
                default:
                    break;
            }
            if (values != null) {
                String strTable = this.toTable(values, params).toString();
                matcher.appendReplacement(sb, ReportParseUtils.quoteReplacement(strTable));
            } else {
                matcher.appendReplacement(sb, ReportParseUtils.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private List<Object> getCaseList(Long projectId) {
        SysCase sysCase = new SysCase();
        sysCase.setProjectId(projectId);
        return sysCaseService.selectSysCaseList(sysCase).stream().collect(Collectors.toList());
    }

    private List<Object> getDefectList(Long projectId, Map<String, Object> para) {
        SysDefect sysDefect = new SysDefect();
        sysDefect.setProjectId(projectId);
        String vueAppBaseApi = (String) para.get("vueAppBaseApi");
        return sysDefectService.selectSysDefectList(sysDefect).stream().map(d->{
            ObjectMapper mapper = new ObjectMapper();
            try {
                Map<String,Object> map = mapper.readValue(JSON.toJSONString(d), new TypeReference<Map<String, Object>>() {});
                if(map.containsKey("defectLevel")) {
                    map.put("defectLevel", MessageUtils.message(String.valueOf(map.get("defectLevel"))));
                }
                if(map.containsKey("defectType")) {
                    map.put("defectType", MessageUtils.message(String.valueOf(map.get("defectType"))));
                }
                if(map.containsKey("imgUrls")) {
                    String strImageList = (String) map.get("imgUrls");
                    String[] imgs = strImageList.split(",");

                    map.put("imgList", Arrays.stream(imgs).filter(img->StringUtils.isNotBlank(img)).map(img->{
                        return String.format("<img src=\"%s%s\" style=\"width:max-width:200px;max-height:200px;\" />",vueAppBaseApi,img);
                    }).collect(Collectors.joining()));
                }
                return map;
            } catch (JsonProcessingException e) {
                return d;
            }
        }).collect(Collectors.toList());
    }

    private StringBuffer toTable(List<Object> list, Map<String,String> params) {
        StringBuffer sb = new StringBuffer();
        if(params==null || params.size()==0){
            params = new LinkedHashMap<>();
            Field[] fields = list.get(0).getClass().getDeclaredFields();
            for(Field f:fields) {
                params.put(f.getName(),f.getName());
            }
        }
        sb.append("|"+params.entrySet().stream().map(h-> MessageUtils.message(h.getValue())).collect(Collectors.joining("|"))+"|\n");
        sb.append("|"+params.entrySet().stream().map(h->"-").collect(Collectors.joining("|"))+"|\n");
        for(Object o:list) {
            String json = JSON.toJSONString(o);
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String,Object> map = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                sb.append("|"+params.entrySet().stream().map(h->{

                    if(map.containsKey(h.getKey())) {
                        Object obj = map.get(h.getKey());
                        String val = null;
                        val = String.valueOf(obj);
                        return val;
                    }
                    return " ";
                }).collect(Collectors.joining("|"))+"|\n");
            }catch (Exception e) {
                logger.error(e);
            }
        }
        return sb;
    }
}
