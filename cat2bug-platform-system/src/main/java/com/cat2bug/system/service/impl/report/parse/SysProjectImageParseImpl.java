package com.cat2bug.system.service.impl.report.parse;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.service.IReportParseService;
import com.cat2bug.system.service.ISysProjectService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-05-21 23:51
 * @Version: 1.0.0
 */
@Service
public class SysProjectImageParseImpl implements IReportParseService {
    private final static Logger logger = LogManager.getLogger(SysProjectImageParseImpl.class);

    private final static String VUE_APP_BASE_API_KEY = "vueAppBaseApi";

    private static final String  RG = "\\$(img|image)(\\[([\\\"'a-zA-Z0-9]+:.+)*\\])*\\{(api\\.project)\\.(.+)?(\\})";

    @Autowired
    private ISysProjectService sysProjectService;

    @Override
    public boolean isHandle(String content) {
        return Pattern.matches(RG,content);
    }

    @Override
    public String parse(Long projectId, String content, Map<String, Object> param) {
        SysProject sysProject = sysProjectService.selectSysProjectByProjectId(projectId);
        if(sysProject==null) return content;

        Matcher matcher = Pattern.compile(RG).matcher(content);
        while(matcher.find()){
            for(int i=0;i<matcher.groupCount();i++) {
                logger.info(matcher.group(i));
            }
            String properName = matcher.group(5);
            if(StringUtils.isBlank(properName)) return content;

            try {
                String src = toContent(sysProject, properName);
                if(StringUtils.isBlank(src)) continue;
//                if(param!=null && param.containsKey(VUE_APP_BASE_API_KEY) && StringUtils.isNotBlank((String)param.get(VUE_APP_BASE_API_KEY))) {
//                    src = param.get(VUE_APP_BASE_API_KEY) + "/" + (src.indexOf("/")==0?src.substring(1):src);
//                }

                String params = matcher.group(3);
                String property = "";
                if(StringUtils.isNotBlank(params)) {
                    String[] ps = params.split(",");
                    property = Arrays.stream(ps).map(s->{
                        int startIndex = s.indexOf(":");
                        if(startIndex>0 && startIndex< s.length()-1) {
                            return s.substring(0,startIndex-1)+"="+s.substring(startIndex+1);
                        }
                        return "";
                    }).collect(Collectors.joining(" "));
                }
                String html = String.format("<img src=\"%s\" alt=\"%s\" $s />",src, properName, property);
                content = matcher.replaceAll(html);
            } catch (Exception e) {
                return content;
            }
        }
        return content;
    }

    protected Object getProject(Long projectId) {
        return sysProjectService.selectSysProjectByProjectId(projectId);
    }

    protected String toContent(Object value,String celName) throws JsonProcessingException {
        String json = JSON.toJSONString(value);
        if(StringUtils.isBlank(celName)) return json;
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> map = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        if(map.containsKey(celName)) {
            return map.get(celName)==null?" ":String.valueOf(map.get(celName));
        }
        return " ";
    }
}
