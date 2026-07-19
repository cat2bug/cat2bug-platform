package com.cat2bug.system.service.impl.report.parse;

import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.service.IReportParseService;
import com.cat2bug.system.service.ISysProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Materialize {@code $image}/{@code $img} tokens to static {@code <img>} tags.
 */
@Service
public class SysProjectImageParseImpl implements IReportParseService {
    /**
     * Aligns with frontend IMAGE_TOKEN_RE:
     * {@code $image["style":"..."]{api.project.projectIcon}}
     */
    private static final Pattern TOKEN = Pattern.compile(
            "\\$(?:img|image)((?:\\[[^\\]]*\\])*)?\\s*\\{(api\\.project(?:\\.\\w+)*)\\}");

    @Autowired
    @Lazy
    private ISysProjectService sysProjectService;

    @Override
    public boolean isHandle(String content) {
        return StringUtils.isNotBlank(content) && TOKEN.matcher(content).find();
    }

    @Override
    public String parse(Long projectId, String content, Map<String, Object> param) {
        SysProject sysProject = sysProjectService.selectSysProjectByProjectId(projectId);
        if (sysProject == null || StringUtils.isBlank(content)) {
            return content;
        }

        Map<String, Object> safeParam = ReportParseUtils.safeParams(param);
        Matcher matcher = TOKEN.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String attrChunk = matcher.group(1);
            String path = matcher.group(2);
            String field = "";
            if (path != null && path.length() > "api.project".length()) {
                field = path.substring("api.project.".length());
            }
            String raw = ReportParseUtils.readBeanField(sysProject, field).trim();
            if (StringUtils.isBlank(raw) || " ".equals(raw)) {
                // Never wipe the token to empty — fall back to default project icon for projectIcon.
                if ("projectIcon".equals(field)) {
                    raw = "1";
                } else {
                    matcher.appendReplacement(sb, ReportParseUtils.quoteReplacement(matcher.group(0)));
                    continue;
                }
            }
            String src = ReportParseUtils.resolveImageSrc(raw, field, safeParam);
            if (StringUtils.isBlank(src)) {
                matcher.appendReplacement(sb, ReportParseUtils.quoteReplacement(matcher.group(0)));
                continue;
            }
            String style = ReportParseUtils.extractStyleAttribute(attrChunk);
            StringBuilder html = new StringBuilder();
            html.append("<img src=\"")
                    .append(ReportParseUtils.escapeHtmlAttr(src))
                    .append("\" alt=\"")
                    .append(ReportParseUtils.escapeHtmlAttr(StringUtils.isBlank(field) ? "image" : field))
                    .append("\"");
            if (StringUtils.isNotBlank(style)) {
                html.append(" style=\"").append(ReportParseUtils.escapeHtmlAttr(style)).append("\"");
            }
            html.append(" />");
            matcher.appendReplacement(sb, ReportParseUtils.quoteReplacement(html.toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
