package com.cat2bug.system.service.impl.report.parse;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.domain.SysCaseStep;
import com.cat2bug.system.service.IReportParseService;
import com.cat2bug.system.service.ISysCaseService;
import com.cat2bug.system.service.ISysDefectService;
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
public class SysCaseCardParseImpl implements IReportParseService {
    private final static Logger logger = LogManager.getLogger(SysCaseCardParseImpl.class);

    private static final String  RG = "\\s*\\$card\\{(api\\.case\\.list)(\\})";

    @Lazy
    @Autowired
    private ISysCaseService sysCaseService;

    public SysCaseCardParseImpl() {
    }

    @Override
    public boolean isHandle(String content) {
        return Pattern.matches(RG,content);
    }

    @Override
    public String parse(Long projectId, String content, Map<String, Object> para) {
        List<SysCase> list = getCaseList(projectId);

        Matcher matcher = Pattern.compile(RG).matcher(content);
        while(matcher.find()){
            try {
                content = matcher.replaceAll(this.toCard(list, null).toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return content;
    }

    private StringBuffer toCard(List<SysCase> list, Map<String,String> params) {
        StringBuffer sb = new StringBuffer();
        list.stream().forEach(c->{
            sb.append("<table style=\"display:table;\">");
            sb.append("<tbody>");
            // 第一行
            sb.append("<tr>");
            sb.append(String.format("<td>%S</td>",MessageUtils.message("mk.case.id")));
            sb.append(String.format("<td>%S</td>",c.getCaseNum()));
            sb.append(String.format("<td>%S</td>",MessageUtils.message("mk.case.name")));
            sb.append(String.format("<td>%S</td>",c.getCaseName()));
            sb.append("</tr>");

            // 第二行
            sb.append("<tr>");
            sb.append(String.format("<td>%S</td>",MessageUtils.message("mk.case.module")));
            sb.append(String.format("<td colspan=3>%S</td>",c.getModuleName()));
            sb.append("</tr>");

            // 第三行
            sb.append("<tr>");
            sb.append(String.format("<td>%S</td>",MessageUtils.message("mk.case.expect")));
            sb.append(String.format("<td colspan=3>%S</td>",c.getCaseExpect()));
            sb.append("</tr>");

            // 第四行
            sb.append("<tr>");
            sb.append(String.format("<td>%S</td>",MessageUtils.message("mk.case.precondition")));
            sb.append(String.format("<td colspan=3>%S</td>",c.getCasePreconditions()));
            sb.append("</tr>");

            if(c.getCaseStep()!=null && c.getCaseStep().size()>0) {
                // 第五行
                sb.append("<tr>");
                sb.append(String.format("<td colspan=4 style=\"text-align: center;vertical-align: middle;font-weight:500;border-bottom:none;\">%S</td>",MessageUtils.message("mk.case.step")));
                sb.append("</tr>");

                // 第六行
                sb.append("<tr style=\"border-top:none;\">");
                sb.append("<td colspan=4 style=\"padding:0px;border:none;\">");

                // 测试步骤表
                sb.append("<table style=\"width:100%;margin: 0px;display: table;\">");
                sb.append("<thead>");
                sb.append("<tr>");
                sb.append(String.format("<th>%s</th>",MessageUtils.message("mk.case.number")));
                sb.append(String.format("<th>%s</th>",MessageUtils.message("mk.case.precondition")));
                sb.append(String.format("<th>%s</th>",MessageUtils.message("mk.case.step-describe")));
                sb.append("</tr>");
                sb.append("</thead>");
                sb.append("<tbody>");
                for(int i=0;i<c.getCaseStep().size();i++) {
                    SysCaseStep s = c.getCaseStep().get(i);
                    sb.append("<tr>");
                    sb.append(String.format("<th>%s</th>",i+1));
                    sb.append(String.format("<th>%s</th>",StringUtils.isBlank(s.getStepExpect())?"":s.getStepExpect()));
                    sb.append(String.format("<th>%s</th>",StringUtils.isBlank(s.getStepDescribe())?"":s.getStepDescribe()));
                    sb.append("</tr>");
                }
                sb.append("</tbody>");
                sb.append("</table>");

                sb.append("</td>");
                sb.append("</tr>");
            }
            sb.append("</tbody>");
            sb.append("</table>");
            sb.append("<br />");
        });
        return sb;
    }

    private List<SysCase> getCaseList(Long projectId) {
        SysCase sysCase = new SysCase();
        sysCase.setProjectId(projectId);
        return sysCaseService.selectSysCaseList(sysCase).stream().collect(Collectors.toList());
    }
}
