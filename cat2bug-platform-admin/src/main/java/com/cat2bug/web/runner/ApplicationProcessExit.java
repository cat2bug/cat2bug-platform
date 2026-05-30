package com.cat2bug.web.runner;

/**
 * 进程退出抽象，便于测试拦截 {@link System#exit(int)}。
 */
public interface ApplicationProcessExit
{
    void exit(int status);
}
