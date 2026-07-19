package com.cat2bug.system.service.impl.report.parse;

import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysPlan;
import com.cat2bug.system.service.IReportParseService;
import com.cat2bug.system.service.ISysPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Materialize {@code ${api.plan.field[n]}} where {@code n} matches {@link SysPlan#getPlanNumber()}.
 */
@Service
public class SysPlanVariableParseImpl implements IReportParseService {
    private static final Pattern TOKEN = Pattern.compile(
            "\\$(?:t|text)?\\{(api\\.plan\\.(\\w+))\\[\\s*(\\d+)\\s*\\]\\}");

    @Autowired
    @Lazy
    private ISysPlanService sysPlanService;

    @Override
    public boolean isHandle(String content) {
        return StringUtils.isNotBlank(content) && TOKEN.matcher(content).find();
    }

    @Override
    public String parse(Long projectId, String content, Map<String, Object> para) {
        if (StringUtils.isBlank(content) || projectId == null) {
            return content;
        }
        SysPlan query = new SysPlan();
        query.setProjectId(projectId);
        List<SysPlan> planList = sysPlanService.selectSysPlanList(query);

        Matcher matcher = TOKEN.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String field = matcher.group(2);
            long planNumber;
            try {
                planNumber = Long.parseLong(matcher.group(3));
            } catch (NumberFormatException e) {
                matcher.appendReplacement(sb, ReportParseUtils.quoteReplacement(" "));
                continue;
            }
            SysPlan matched = null;
            if (planList != null) {
                for (SysPlan plan : planList) {
                    if (plan != null && plan.getPlanNumber() != null
                            && plan.getPlanNumber().longValue() == planNumber) {
                        matched = plan;
                        break;
                    }
                }
            }
            String replacement = matched == null
                    ? " "
                    : ReportParseUtils.readBeanField(matched, field);
            matcher.appendReplacement(sb, ReportParseUtils.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
