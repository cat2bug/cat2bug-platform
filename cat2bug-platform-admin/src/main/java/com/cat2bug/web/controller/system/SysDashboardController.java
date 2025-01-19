package com.cat2bug.web.controller.system;

import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.system.domain.SysDefectLine;
import com.cat2bug.system.service.ISysDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2025-01-19 04:49
 * @Version: 1.0.0
 */
@RestController
@RequestMapping("/system/dashboard")
public class SysDashboardController {
    private final static String MONTH_TYPE = "month";
    @Autowired
    ISysDashboardService sysDashboardService;

    @PreAuthorize("@ss.hasPermi('system:dashboard:query')")
    @GetMapping("/{projectId}/defectLine")
    public AjaxResult defectLine(@PathVariable("projectId") Long projectId, @RequestParam("timeType") String timeType) {
        List<SysDefectLine> defectLineList = sysDashboardService.defectLine(projectId, timeType);
        Set<String> timeSet = new LinkedHashSet<>();
        switch (timeType) {
            case MONTH_TYPE:
                setCountOfMonth(timeSet, defectLineList);
                break;
            default:
                setCountOfDay(timeSet, defectLineList);
                break;
        }
        Map<SysDefectStateEnum, List<SysDefectLine>> defectStateGroup = defectLineList.stream().collect(Collectors.groupingBy(SysDefectLine::getDefectState));
        Map<String, List<Long>> defectStateLines = new HashMap<>();
        defectStateGroup.forEach((k,v)->{
            defectStateLines.put(k.name(), new ArrayList<>());
            for (String s : timeSet) {
                Optional<SysDefectLine> defectLine = v.stream().filter(d->d.getDefectTime().equals(s)).findFirst();
                if(defectLine.isPresent()) {
                    defectStateLines.get(k.name()).add(defectLine.get().getDefectCount());
                } else {
                    defectStateLines.get(k.name()).add(0L);
                }
            }
        });
        Map<String, Object> ret = new HashMap<>();
        ret.put("data", defectStateLines);              // 按状态类型分组的日完成数量
        ret.put("types", SysDefectStateEnum.values());  // 状态分类
        ret.put("times", timeSet);                      // 时间数组
        return AjaxResult.success(ret);
    }

    private void setCountOfDay(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        LongSummaryStatistics summaryStatistics = defectLineList.stream().map(x-> {
            try {
                return format.parse(x.getDefectTime()).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.summarizingLong(x -> x));
        final long DAY_SIZE = 1000*60*60*24;    // 一天的毫秒时长
        final long DEFAULT_DAY = 30;            // 默认最大天数
        long dayCount = (summaryStatistics.getMax() - summaryStatistics.getMin())/DAY_SIZE;

        // 如果缺陷数据小于30天，就显示30天的，没有的天数补齐
        if(dayCount<DEFAULT_DAY) {
            for(long i=0;i<=DEFAULT_DAY;i++) {
                timeSet.add(format.format(new Date(summaryStatistics.getMin()+i*DAY_SIZE)));
            }
        } else {
            // 如果缺陷数据大于30天，就显示30的的
            for(long i=DEFAULT_DAY;i>=0;i--) {
                timeSet.add(format.format(new Date(summaryStatistics.getMax()-i*DAY_SIZE)));
            }
        }
    }

    private void setCountOfMonth(Set<String> timeSet, List<SysDefectLine> defectLineList) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
        LongSummaryStatistics summaryStatistics = defectLineList.stream().map(x-> {
            try {
                return format.parse(x.getDefectTime()).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.summarizingLong(x -> x));
        final int DEFAULT_MONTH = 12;            // 默认最大天数
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(new Date(summaryStatistics.getMin()));
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(new Date(summaryStatistics.getMax()));
        int monthCount = endCalendar.get(Calendar.MONTH)-startCalendar.get(Calendar.MONTH);

        // 如果缺陷数据小于12个月，就显示12个月的，没有的天数补齐
        if(monthCount<DEFAULT_MONTH) {
            timeSet.add(format.format(startCalendar.getTime()));
            for(int i=1;i<DEFAULT_MONTH;i++) {
                startCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(startCalendar.getTime()));
            }
        } else {
            // 如果缺陷数据大于12个月，就显示30的的
            endCalendar.add(Calendar.MONTH, -DEFAULT_MONTH);
            timeSet.add(format.format(endCalendar.getTime()));
            for(int i=1;i<=DEFAULT_MONTH;i++) {
                endCalendar.add(Calendar.MONTH, 1);
                timeSet.add(format.format(endCalendar.getTime()));
            }
        }
    }
}
