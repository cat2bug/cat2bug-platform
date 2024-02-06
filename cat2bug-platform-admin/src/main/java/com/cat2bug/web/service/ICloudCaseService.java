package com.cat2bug.web.service;

import com.cat2bug.system.domain.SysCase;

import java.util.List;

/**
 * 云用例服务
 */
public interface ICloudCaseService {
    /**
     * 搜索人工智能提供的用例列表
     * @param content
     * @return
     */
    public List<SysCase> searchCaseListOfAI(String content);
}
