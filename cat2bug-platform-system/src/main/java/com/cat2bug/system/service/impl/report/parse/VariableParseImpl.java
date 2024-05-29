package com.cat2bug.system.service.impl.report.parse;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.service.IReportParseService;
import com.cat2bug.system.service.ISysProjectService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-05-21 23:51
 * @Version: 1.0.0
 */
@Service
public class VariableParseImpl implements IReportParseService {
    private final static Logger logger = LogManager.getLogger(VariableParseImpl.class);

    private static final String  RG = "\\$(t|text){0,1}\\{(.*)\\}";

    @Autowired
    private ISysProjectService sysProjectService;

    @Override
    public boolean isHandle(String content) {
        return Pattern.matches(RG,content);
    }

    @Override
    public String parse(Long projectId, String content) {
        Matcher matcher = Pattern.compile(RG).matcher(content);
        while(matcher.find()){
            try {
                String api = matcher.group(1);
                if (StringUtils.isBlank(api)) continue;

                Map<String, String> params = new LinkedHashMap<>();
                String strParams = matcher.group(4);
                if (StringUtils.isNotBlank(strParams)) {
                    String[] strs = strParams.split(",");
                    for (String s : strs) {
                        if (StringUtils.isBlank(s)) continue;
                        String[] ss = s.split(":");
                        if (ss.length > 1) {
                            params.put(ss[0], ss[1]);
                        } else {
                            params.put(s, s);
                        }
                    }
                }
                Object values = null;
                switch (api) {
                    case "api.project":
                        values = this.getProject(projectId);
//                    params = (params==null || params.size()==0?defaultCaseTitleNames:params);
                        break;
                }
                if (values != null) {
                    String strTable = this.toContent(values, null);
                    return strTable;
                }
            }catch (Exception e) {
                return " ";
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
