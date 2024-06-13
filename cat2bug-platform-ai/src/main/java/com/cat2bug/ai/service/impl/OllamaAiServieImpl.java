package com.cat2bug.ai.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.cat2bug.ai.service.IAiService;
import com.cat2bug.ai.vo.*;
import com.cat2bug.common.constant.HttpStatus;
import lombok.Data;
import okhttp3.*;
import okio.Buffer;
import okio.BufferedSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-11 11:56
 * @Version: 1.0.0
 * Ollama服务
 */
@Service
@ConfigurationProperties(prefix = "cat2bug.ai")
@Data
public class OllamaAiServieImpl implements IAiService {
    private final static Logger log = LogManager.getLogger(OllamaAiServieImpl.class);
    /**
     * 接口内容类型
     */
    private static final MediaType FORM_CONTENT_TYPE = MediaType.parse("application/json;charset=utf-8");
    /**
     * 聊天接口
     */
    private static final String GENERATE_URL = "/api/generate";
    /**
     * 下载模型接口
     */
    private static final String PULL_MODULE_URL = "/api/pull";
    /**
     * 下载模型接口
     */
    private static final String REMOVE_MODULE_URL = "/api/delete";
    /**
     * 显示模型信息接口
     */
    private static final String SHOW_MODULE_URL = "/api/show";
    /**
     * 显示本地模型列表接口
     */
    private static final String MODULE_LIST_URL = "/api/tags";
    /**
     * 问答数据格式类型
     */
    private static final String PROMPT_FORMAT_TYPE = "json";

    /**
     * 配置项：服务主机和端口
     */
    private String host;

    /**
     * 配置项：http超时时间
     */
    private long timeout;

    /**
     * 配置项：使用的模型名
     */
    private List<String> modules;

    @PostConstruct
    public void init() {
        // 对没有的模型进行下载
        if(this.modules!=null) {
            Set<String> storeModules = this.getModuleList(OllamaModuleListItem.class).stream().map(item->item.getName()).collect(Collectors.toSet());
            modules.forEach(m->{
                if(storeModules.contains(m)==false) {
                    log.info("开始下载模型：{}",m);
                    pullModule(m,true);
                }
            });
        }
    }

    @Override
    public <T> T generate(String moduleName, String prompt, boolean stream, Class<T> cls) {
        try {
            OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(this.timeout, TimeUnit.SECONDS).build();
            String requestPrompt = null;
            String format = null;
            if(cls == String.class){
                requestPrompt = String.format("请用中文回复,%s",prompt);
            } else if(cls instanceof Object) {
                requestPrompt = String.format("请用中文回复,%s,格式:%s",prompt,JSON.toJSONString(cls.newInstance(), SerializerFeature.WriteMapNullValue));
                format = PROMPT_FORMAT_TYPE;
            } else {
                requestPrompt = prompt;
            }
            Prompt promptVo = new Prompt(moduleName,requestPrompt,format,false);
            String body = JSON.toJSONString(promptVo);
            RequestBody formBody = RequestBody.create(FORM_CONTENT_TYPE, body);
            Request request = new Request.Builder()
                    .url(getApiUrl(this.host, GENERATE_URL))
                    .post(formBody).build();

            Response response = client.newCall(request).execute();
            if(response.isSuccessful()) {
               String json = response.body().string();
               OllamaGenerateResponse responseBody =  JSON.parseObject(json, OllamaGenerateResponse.class);
                if(cls == String.class){
                    return (T)responseBody.getResponse();
                } else if(cls instanceof Object) {
                    return JSON.parseObject(responseBody.getResponse(),cls);
                } else {
                    return (T)responseBody.getResponse();
                }

            } else {
                throw new RuntimeException(response.message());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 显示模型信息
     * @param moduleName    模型名称
     * @param cls           返回类型
     * @return
     * @param <T>
     */
    @Override
    public <T> T getModuleInfo(String moduleName, Class<T> cls) {
        try {
            OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(this.timeout, TimeUnit.SECONDS).build();
            OllamaModuleRequest moduleRequest = new OllamaModuleRequest(moduleName);
            String body = JSON.toJSONString(moduleRequest);
            RequestBody formBody = RequestBody.create(FORM_CONTENT_TYPE, body);
            Request request = new Request.Builder()
                    .url(getApiUrl(this.host, SHOW_MODULE_URL))
                    .post(formBody).build();

            Response response = client.newCall(request).execute();
            if(response.isSuccessful()){
                String json = response.body().string();

                if(cls == String.class){
                    return (T)json;
                } else if(cls instanceof Object) {
                    return JSON.parseObject(json, cls);
                } else {
                    return (T)json;
                }
            } else {
                throw new RuntimeException(response.message());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取本地模型列表
     * @param cls           返回类型
     * @return
     * @param <T>
     */
    @Override
    public <T> List<T> getModuleList(Class<T> cls) {
        try {
            OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(this.timeout, TimeUnit.SECONDS).build();
            Request request = new Request.Builder()
                    .url(getApiUrl(this.host, MODULE_LIST_URL))
                    .get().build();

            Response response = client.newCall(request).execute();
            if(response.isSuccessful()){
                String json = response.body().string();
                OllamaModuleList moduleListObj = JSON.parseObject(json,OllamaModuleList.class);
                return moduleListObj.getModels().stream().map(m->(T)m).collect(Collectors.toList());
            } else {
                throw new RuntimeException(response.message());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    /**
     * 下拉模型
     * @param moduleName    模型名称
     * @param stream        是否通过流数据返回
     * @return              返回信息
     */
    @Override
    public String pullModule(String moduleName, boolean stream) {
        try {
            OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(this.timeout, TimeUnit.SECONDS).build();
            OllamaModuleRequest pullModuleRequest = new OllamaModuleRequest(moduleName,stream);
            String body = JSON.toJSONString(pullModuleRequest);
            RequestBody formBody = RequestBody.create(FORM_CONTENT_TYPE, body);
            Request request = new Request.Builder()
                    .url(getApiUrl(this.host, PULL_MODULE_URL))
                    .post(formBody).build();

            if(stream) {
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        log.error(e);
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        if(response.isSuccessful()) {
                            BufferedSource source = response.body().source();
                            source.request(Long.MAX_VALUE); // Prepare to read the entire source into the buffer
                            Buffer buffer = source.buffer();

                            while (!buffer.exhausted()) {
                                String line = buffer.readUtf8Line();
                                log.info(String.format("下载模型【%s】,返回信息:%s", moduleName, line));
                            }
                        }
                    }
                });
            } else {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    OllamaModuleResponse responseBody = JSON.parseObject(json, OllamaModuleResponse.class);
                    return responseBody.getStatus();
                } else {
                    throw new RuntimeException(response.message());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean removeModule(String moduleName) {
        try {
            OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(this.timeout, TimeUnit.SECONDS).build();
            OllamaModuleRequest pullModuleRequest = new OllamaModuleRequest(moduleName);
            String body = JSON.toJSONString(pullModuleRequest);
            RequestBody formBody = RequestBody.create(FORM_CONTENT_TYPE, body);
            Request request = new Request.Builder()
                    .url(getApiUrl(this.host, REMOVE_MODULE_URL))
                    .delete(formBody).build();

            Response response = client.newCall(request).execute();
            return response.isSuccessful();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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
