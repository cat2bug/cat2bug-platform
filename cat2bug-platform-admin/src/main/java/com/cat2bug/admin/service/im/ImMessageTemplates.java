package com.cat2bug.admin.service.im;

import com.cat2bug.common.constant.DefectConstants;
import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.vo.EnumVo;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.common.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 缺陷、报告、通知三种事件的正文（对齐 Spring {@code DefectMessageOfNoticeTemplateImpl}、
 * {@code ReportMessageOfNoticeTemplateImpl}、{@code DefaultMessageTemplateImpl}）。
 * <p>
 * Markdown 正文同时用作站内通知内容，改动会直接影响通知页展示。
 */
public final class ImMessageTemplates {

    private ImMessageTemplates() {
    }

    private record Line(String label, String value) {
    }

    public static ImMessageBody defect(SysDefect defect, String link) {
        String number = "#" + (defect.getProjectNum() != null ? defect.getProjectNum() : 0);
        String name = blankToEmpty(defect.getDefectName());
        String state = defectStateLabel(defect.getDefectState());
        String handlers = formatHandleByNames(defect);

        List<Line> lines = new ArrayList<>();
        lines.add(new Line("缺陷", number + " " + name));
        if (StringUtils.isNotEmpty(state)) {
            lines.add(new Line("状态", state));
        }
        if (StringUtils.isNotEmpty(handlers)) {
            lines.add(new Line("处理人", handlers));
        }
        if (StringUtils.isNotEmpty(defect.getDefectDescribe())) {
            lines.add(new Line("描述", defect.getDefectDescribe()));
        }

        StringBuilder md = new StringBuilder();
        md.append(String.format("## %s %s \n\n", number, name));
        if (StringUtils.isNotEmpty(link)) {
            md.append(String.format("[查看缺陷详情](%s) \n\n", link));
        }
        if (StringUtils.isNotEmpty(defect.getDefectDescribe())) {
            md.append("**描述:** ").append(defect.getDefectDescribe()).append(" \n\n");
        }
        if (StringUtils.isNotEmpty(handlers)) {
            md.append("**处理人:** ").append(handlers).append(" \n\n");
        }

        return new ImMessageBody(text("缺陷通知", link, lines), md.toString(), html("缺陷通知", link, lines));
    }

    /** 用户手写的通知：Markdown 与站内内容即原文，文本前加标题，HTML 转义后保留换行。 */
    public static ImMessageBody notice(String title, String content) {
        String safeContent = blankToEmpty(content);
        String safeTitle = blankToEmpty(title);
        String text = StringUtils.isNotEmpty(safeTitle) ? safeTitle + "\n" + safeContent : safeContent;
        String html = (StringUtils.isNotEmpty(safeTitle) ? "<h3>" + escapeHtml(safeTitle) + "</h3>" : "")
                + "<p>" + escapeHtml(safeContent).replace("\n", "<br />") + "</p>";
        return new ImMessageBody(text, safeContent, html);
    }

    public static String defectStateLabel(Integer defectState) {
        if (defectState == null) {
            return "";
        }
        for (EnumVo state : DefectConstants.getDefectStateList()) {
            if (state.getKey() != null && state.getKey().intValue() == defectState) {
                return state.getValue() != null ? MessageUtils.message(state.getValue()) : String.valueOf(defectState);
            }
        }
        if (defectState >= 0 && defectState < DefectConstants.DEFECT_STATES.length) {
            return MessageUtils.message(DefectConstants.DEFECT_STATES[defectState]);
        }
        return String.valueOf(defectState);
    }

    public static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String text(String header, String link, List<Line> lines) {
        StringBuilder sb = new StringBuilder(header).append('\n');
        if (StringUtils.isNotEmpty(link)) {
            sb.append("点击查看 ").append(link).append(" \n");
        }
        for (Line line : lines) {
            sb.append(line.label()).append(": ").append(line.value()).append(" \n");
        }
        return sb.toString();
    }

    private static String html(String header, String link, List<Line> lines) {
        StringBuilder sb = new StringBuilder(escapeHtml(header)).append(" <br />");
        if (StringUtils.isNotEmpty(link)) {
            String safeLink = escapeHtml(link);
            sb.append("点击查看 <a style=\"color: #409eff;\" href=\"").append(safeLink).append("\">")
                    .append(safeLink).append("</a> <br />");
        }
        for (Line line : lines) {
            sb.append(escapeHtml(line.label())).append(": ")
                    .append(escapeHtml(line.value()).replace("\n", "<br />")).append(" <br />");
        }
        return sb.toString();
    }

    private static String formatHandleByNames(SysDefect defect) {
        if (defect.getHandleByList() == null || defect.getHandleByList().isEmpty()) {
            return "";
        }
        return defect.getHandleByList().stream()
                .map(user -> {
                    if (user == null) {
                        return "";
                    }
                    if (StringUtils.isNotEmpty(user.getNickName())) {
                        return user.getNickName();
                    }
                    return blankToEmpty(user.getUserName());
                })
                .filter(StringUtils::isNotEmpty)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private static String blankToEmpty(String value) {
        return value != null ? value : "";
    }
}
