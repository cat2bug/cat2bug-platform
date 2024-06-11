package com.cat2bug.ai.service.impl;

import com.alibaba.fastjson.JSON;
import com.cat2bug.ai.service.IAiService;
import com.cat2bug.ai.vo.PromptVo;
import com.cat2bug.ai.vo.ResponseBody;
import com.cat2bug.common.constant.HttpStatus;
import okhttp3.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;


/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-11 11:56
 * @Version: 1.0.0
 * Ollama服务
 */
@Service
@ConfigurationProperties(prefix = "cat2bug.ai")
public class OllamaAiServieImpl implements IAiService {
    /**
     * 接口内容类型
     */
    private static final MediaType FORM_CONTENT_TYPE = MediaType.parse("application/json;charset=utf-8");
    /**
     * 聊天接口
     */
    private static final String GENERATE_URL = "/api/generate";
    /**
     * 问答数据格式类型
     */
    private static final String PROMPT_FORMAT_TYPE = "json";

    /**
     * 服务主机和端口
     */
    private String host;

    @PostConstruct
    public void init() {
        String ret = this.generate("llama3:8b","请用中文回复我，中国的首都是哪里");
        System.out.println("========================"+ret);
    }
    @Override
    public String generate(String moduleName, String prompt) {
        try {
            OkHttpClient client = new OkHttpClient();
            PromptVo promptVo = new PromptVo(moduleName,prompt,PROMPT_FORMAT_TYPE,false);
            String body = JSON.toJSONString(promptVo);
            RequestBody formBody = RequestBody.create(FORM_CONTENT_TYPE, body);
            Request request = new Request.Builder()
                    .url(getApiUrl(this.host, GENERATE_URL))
                    .post(formBody).build();

            Response response = client.newCall(request).execute();
            if(response.code()== HttpStatus.SUCCESS){
               ResponseBody responseBody =  JSON.parseObject(response.body().string(),ResponseBody.class);
               return responseBody.getContext();
            } else {
                throw new RuntimeException(response.message());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取接口地址
     * @param host  服务器地址
     * @param path  接口路径
     * @return  接口地址
     */
    private static String getApiUrl(String host, String path) {
        if(host==null || "".equals(host))
            throw new RuntimeException("The host parameter cannot be empty!");

        String h = host.endsWith("/")?host.substring(0,host.length()-1):host;
        return h+path;
    }
}
