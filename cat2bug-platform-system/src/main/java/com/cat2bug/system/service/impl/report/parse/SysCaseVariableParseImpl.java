package com.cat2bug.system.service.impl.report.parse;

import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.service.IReportParseService;
import com.cat2bug.system.service.ISysCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Materialize {@code ${api.case.total}} (and legacy {@code $t{}/{@code $text{}}) to plain text.
 */
@Service
public class SysCaseVariableParseImpl implements IReportParseService {
    private static final Pattern TOKEN = Pattern.compile(
            "\\$(?:t|text)?\\{(api\\.case(?:\\.\\w+)*)\\}");

    @Autowired
    private ISysCaseService sysCaseService;

    @Override
    public boolean isHandle(String content) {
        return StringUtils.isNotBlank(content) && TOKEN.matcher(content).find();
    }

    @Override
    public String parse(Long projectId, String content, Map<String, Object> para) {
        if (StringUtils.isBlank(content)) {
            return content;
        }
        String total;
        try {
            total = String.valueOf(sysCaseService.totalByProjectId(projectId));
        } catch (Exception e) {
            total = " ";
        }

        Matcher matcher = TOKEN.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String path = matcher.group(1);
            String field = "";
            if (path != null && path.length() > "api.case".length()) {
                field = path.substring("api.case.".length());
            }
            String replacement = "total".equals(field) || StringUtils.isBlank(field) ? total : " ";
            matcher.appendReplacement(sb, ReportParseUtils.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
