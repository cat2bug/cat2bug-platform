package com.cat2bug.system.service;

import java.util.Map;

public interface IReportParseService {
    boolean isHandle(String content);
    String parse(Long projectId, String content, Map<String, Object> param);
}
