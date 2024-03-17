package com.cat2bug.common.report;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.core.domain.entity.SysDefect;

import java.util.List;

/**
 * 缺陷报告
 * @Author: yuzhantao
 * @CreateTime: 2024-03-13 15:46
 * @Version: 1.0.0
 */
public class DefectReportCoder implements  IReportCoder<List<SysDefect>> {

    @Override
    public String encode(List<SysDefect> sysDefects) {
        return JSON.toJSONString(sysDefects);
    }

    @Override
    public List<SysDefect> decode(String code) {
        return JSON.parseArray(code,SysDefect.class);
    }
}
