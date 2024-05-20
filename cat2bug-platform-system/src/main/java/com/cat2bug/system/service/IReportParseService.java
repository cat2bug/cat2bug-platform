package com.cat2bug.system.service;

public interface IReportParseService {
    boolean isHandle(String content);
    String parse(Long projectId, String content);
}
