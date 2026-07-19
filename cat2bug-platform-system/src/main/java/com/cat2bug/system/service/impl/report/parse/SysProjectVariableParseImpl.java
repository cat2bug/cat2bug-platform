package com.cat2bug.system.service.impl.report.parse;

import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.service.IReportParseService;
import com.cat2bug.system.service.ISysProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Materialize {@code ${api.project.*}} (and legacy {@code $t{}/{@code $text{}}) to plain text.
 */
@Service
public class SysProjectVariableParseImpl implements IReportParseService {
    private static final Pattern TOKEN = Pattern.compile(
            "\\$(?:t|text)?\\{(api\\.project(?:\\.\\w+)*)\\}");

    @Autowired
    private ISysProjectService sysProjectService;

    @Override
    public boolean isHandle(String content) {
        return StringUtils.isNotBlank(content) && TOKEN.matcher(content).find();
    }

    @Override
    public String parse(Long projectId, String content, Map<String, Object> para) {
        SysProject sysProject = sysProjectService.selectSysProjectByProjectId(projectId);
        if (sysProject == null || StringUtils.isBlank(content)) {
            return content;
        }

        Matcher matcher = TOKEN.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String path = matcher.group(1);
            String field = "";
            if (path != null && path.length() > "api.project".length()) {
                field = path.substring("api.project.".length());
            }
            String replacement = ReportParseUtils.readBeanField(sysProject, field);
            matcher.appendReplacement(sb, ReportParseUtils.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
