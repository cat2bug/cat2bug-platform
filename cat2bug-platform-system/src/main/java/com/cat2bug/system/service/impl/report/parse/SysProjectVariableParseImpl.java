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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-05-21 23:51
 * @Version: 1.0.0
 */
@Service
public class SysProjectVariableParseImpl implements IReportParseService {
    private final static Logger logger = LogManager.getLogger(SysProjectVariableParseImpl.class);

    private static final String  RG = "\\$(t|text){0,1}\\{(api\\.project)(\\.(.+))?(\\})";

    @Autowired
    private ISysProjectService sysProjectService;

    @Override
    public boolean isHandle(String content) {
        return Pattern.matches(RG,content);
    }

    @Override
    public String parse(Long projectId, String content, Map<String, Object> para) {
        SysProject sysProject = sysProjectService.selectSysProjectByProjectId(projectId);
        if(sysProject==null) return content;

        Matcher matcher = Pattern.compile(RG).matcher(content);
        while(matcher.find()){
            String properName = matcher.group(4);
            try {
                if(StringUtils.isBlank(properName)) {
                    return matcher.replaceAll(JSON.toJSONString(sysProject));
                } else {
                    String str = toContent(sysProject, properName);
                    return matcher.replaceAll(str);
                }
            } catch (Exception e) {
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
