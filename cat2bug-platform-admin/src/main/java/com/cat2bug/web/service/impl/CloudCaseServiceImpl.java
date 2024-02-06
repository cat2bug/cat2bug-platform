package com.cat2bug.web.service.impl;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSON;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.web.service.ICloudCaseService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-04 17:17
 * @Version: 1.0.0
 */
@Log4j2
@Service
public class CloudCaseServiceImpl implements ICloudCaseService {
    private static String baseUrl = "http://127.0.0.1:8080";
    @Override
    public List<SysCase> searchCaseListOfAI(String content) {
        String requestUrl = baseUrl+"/ai/case/list";
        int socketTimeout = 60000;
        if (StringUtils.isNotBlank(content)) {
            requestUrl +=  "?content=" + content;
        }
        String respData = null;
        HttpRequest httpRequest = HttpRequest.get(requestUrl).timeout(socketTimeout).header("token",
                "application/json");
        respData = httpRequest.execute().body();
        log.info(String.format("http get | 请求信息：%s | 响应信息: %s", httpRequest.getUrl(), respData));
        return JSON.parseArray(respData, SysCase.class);
    }
}
