package com.cat2bug.web.runner;

import org.springframework.stereotype.Component;

@Component
class SystemApplicationProcessExit implements ApplicationProcessExit
{
    @Override
    public void exit(int status)
    {
        System.exit(status);
    }
}
