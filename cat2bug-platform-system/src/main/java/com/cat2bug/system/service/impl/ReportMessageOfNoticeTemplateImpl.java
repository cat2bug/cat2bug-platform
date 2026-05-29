package com.cat2bug.system.service.impl;

import com.cat2bug.common.core.domain.entity.SysReport;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.im.service.IMessageTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-10 16:03
 * @Version: 1.0.0
 */
@Service
public class ReportMessageOfNoticeTemplateImpl implements IMessageTemplate<SysReport> {
    private final static String REPORT_CONFIG_KEY = "report";
    private final static String EVENT_CONFIG_KEY = "event";
    private final static String NEW_REPORT_CONFIG_KEY = "newReport";
    private final static String OPTION_CONFIG_KEY = "option";
    private final static String OPTION_REALTIME_CONFIG_KEY = "realtime";
    private final static String SWITCH_KEY = "switch";


    @Override
    public String toText(SysReport obj, Map<String, Object> params) {
        if(this.isValid(params)==false) return null;
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("%s\n",MessageUtils.message("report.notice")));
        sb.append(String.format("%s %s/#/project/report?projectId=%d&reportId=%d \n", MessageUtils.message("report.click-view"), obj.getSrcHost(), obj.getProjectId(), obj.getReportId()));
        return sb.toString();
    }

    @Override
    public String toHtml(SysReport obj, Map<String, Object> params) {
        if(this.isValid(params)==false) return null;
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("%s <br />",MessageUtils.message("report.notice")));
        sb.append(String.format("%s <a style=\"color: #409eff;\" href=\"%s/#/project/report?projectId=%d&reportId=%d\">%s/#/project/report?reportId=%d</a> <br />", MessageUtils.message("report.click-view"), obj.getSrcHost(), obj.getProjectId(), obj.getReportId(), obj.getSrcHost(), obj.getReportId()));
        return sb.toString();
    }

    @Override
    public String toMarkdown(SysReport obj, Map<String, Object> params) {
        if(this.isValid(params)==false) return null;
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("## %s \n\n", obj.getReportTitle()));
        sb.append(String.format("%s [%s/#/project/report?reportId=%d](%s/#/project/report?projectId=%d&reportId=%d) \n\n", MessageUtils.message("report.click-view"), obj.getSrcHost(), obj.getReportId(), obj.getSrcHost(), obj.getProjectId(), obj.getReportId()));
        return sb.toString();
    }

    private boolean isValid(Map<String, Object> params) {
        Map<String, Object> reportParams = getNestedMap(params, REPORT_CONFIG_KEY);
        if (reportParams == null) {
            return false;
        }
        Map<String, Object> optionParams = getNestedMap(reportParams, OPTION_CONFIG_KEY);
        if (optionParams == null) {
            return false;
        }
        Map<String, Object> intervalParams = getNestedMap(optionParams, OPTION_REALTIME_CONFIG_KEY);
        if (intervalParams == null || !intervalParams.containsKey(SWITCH_KEY)) {
            return false;
        }
        Object switchValue = intervalParams.get(SWITCH_KEY);
        return switchValue instanceof Boolean && (Boolean) switchValue;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getNestedMap(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value instanceof Map<?, ?> nested) {
            return (Map<String, Object>) nested;
        }
        return null;
    }
}
