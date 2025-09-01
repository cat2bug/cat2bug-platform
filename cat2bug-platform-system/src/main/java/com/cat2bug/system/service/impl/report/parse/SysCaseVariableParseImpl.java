package com.cat2bug.system.service.impl.report.parse;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.service.IReportParseService;
import com.cat2bug.system.service.ISysCaseService;
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
public class SysCaseVariableParseImpl implements IReportParseService {
    private final static Logger logger = LogManager.getLogger(SysCaseVariableParseImpl.class);

    private static final String  RG = "\\$(t|text){0,1}\\{(api\\.case)(\\.(.+))?(\\})";

    @Autowired
    private ISysCaseService sysCaseService;

    @Override
    public boolean isHandle(String content) {
        return Pattern.matches(RG,content);
    }

    @Override
    public String parse(Long projectId, String content, Map<String, Object> para) {
        String total = "";
        try {
            total = sysCaseService.totalByProjectId(projectId) + "";
        }catch (Exception e) {
            total = e.getLocalizedMessage();
        }

        Matcher matcher = Pattern.compile(RG).matcher(content);
        while(matcher.find()){
            String properName = matcher.group(4);
            try {
                switch (properName) {
                    case "total":
                        content = matcher.replaceAll(total);
                }
            } catch (Exception e) {}
        }
        return content;
    }
}
