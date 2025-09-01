package com.cat2bug.web.controller.common;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.framework.config.RoutePropertiesConfig;
import com.cat2bug.framework.web.domain.RouteInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.servlet.ConditionalOnMissingFilterBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-12-16 20:02
 * @Version: 1.0.0
 */
@Slf4j
@RestController
@ConditionalOnProperty(prefix = "cat2bug.proxy.routes.ai", name = "enabled", havingValue = "true")
public class Cat2AiProxyController {
    @Resource
    private RoutePropertiesConfig routeProperties;

//    @Resource
//    private RestTemplate restTemplate;

    @Resource
    WebClient webClient;

    /**
     * 请求转发
     * 支持 文件转发
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/cat2ai/**")
    public ResponseEntity<Object> proxy(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String prefix = "cat2ai";
        RouteInfo route = routeProperties.getRouteByPrefix(prefix);
        if (route == null) {
            return new ResponseEntity("No route found!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        String url = rebuildUlr(request, "", prefix);
        // 读取数据
        BufferedReader bufferedReader = request.getReader();
        String line =null;
        StringBuffer sb = new StringBuffer();
        while ((line =bufferedReader.readLine())!=null){
            // 一次读一行，不等于null就继续读
            sb.append(line);
        }
        Mono<String> mono = webClient.post()
                // 请求路径
                .uri(url)
                // 携带参数
                .bodyValue(sb.toString())
                // 获取响应体
                .retrieve()
                // 响应数据类型转换
                .bodyToMono(String.class)
                .doOnError(throwable -> log.info(throwable.getMessage()))
                .onErrorResume(e -> Mono.just("Error " + e.getMessage()));
        return new ResponseEntity(mono.blockOptional().get(), HttpStatus.OK);
    }

//    private RequestEntity buildRequestEntity(String url, HttpServletRequest request) throws IOException {
//        // 获取 所有heads
//        HttpHeaders headers = parseHeader(request);
//        // 单独处理文件上传
//        if (isMultipart(request)) {
//            RequestEntity formData = getFormDataEntity(url, request, headers);
//            if (formData != null) {
//                return formData;
//            }
//            throw new RuntimeException("参数异常");
//        }
//        //这里获取不到 form-data 中数据，只能获取,requestBody, form-urlencoded-www参数
//        byte[] body = parseBody(request);
//        return new RequestEntity(body, headers, HttpMethod.resolve(request.getMethod()), URI.create(url));
//    }
//
//
//    private RequestEntity getFormDataEntity(String url, HttpServletRequest request, HttpHeaders headers) throws IOException {
//        MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
//        MultiValueMap<String, MultipartFile> multiFileMap = multipartHttpServletRequest.getMultiFileMap();
//        //处理文件
//        MultiValueMap formData = new LinkedMultiValueMap();
//        for (Map.Entry<String, List<MultipartFile>> entry : multiFileMap.entrySet()) {
//            String key = entry.getKey();
//            for (MultipartFile multipartFile : entry.getValue()) {
//                ByteArrayResource fileResource = new ByteArrayResource(multipartFile.getBytes()) {
//                    @Override
//                    public long contentLength() {
//                        return multipartFile.getSize();
//                    }
//
//                    @Override
//                    public String getFilename() {
//                        return multipartFile.getOriginalFilename();
//                    }
//                };
//                formData.add(key, fileResource);
//            }
//        }
//        //处理 form-data 中非 文件类型参数
//        Map<String, String[]> parameterMap = multipartHttpServletRequest.getParameterMap();
//        for (Map.Entry<String, String[]> stringEntry : parameterMap.entrySet()) {
//            for (String s : stringEntry.getValue()) {
//                formData.add(stringEntry.getKey(), s);
//            }
//        }
//        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//        return new RequestEntity(formData, headers, HttpMethod.resolve(request.getMethod()), URI.create(url));
//    }
//
//    private boolean isMultipart(HttpServletRequest request) {
//        return request instanceof MultipartHttpServletRequest;
//    }
//
//    private HttpHeaders parseHeader(HttpServletRequest request) {
//        HttpHeaders headers = new HttpHeaders();
//        Enumeration<String> iterator = request.getHeaderNames();
//        while (iterator.hasMoreElements()) {
//            String next = iterator.nextElement();
//            List<String> list = Collections.list(request.getHeaders(next));
//            for (String s : list) {
//                headers.add(next, s);
//            }
//        }
//        return headers;
//    }
//
//    private byte[] parseBody(HttpServletRequest request) throws IOException {
//        return StreamUtils.copyToByteArray(request.getInputStream());
//    }

    private String rebuildUlr(HttpServletRequest request, String host, String prefix) {
        String query = request.getQueryString();
        char trimChar = '/';
        // 清空左边的指定字符
        String trimmedLeft = prefix.replaceAll("^" + trimChar + "+", "");
        // 清空右边的指定字符
        String trimmedRight = trimmedLeft.replaceAll(trimChar + "+$", "");
        return host.concat(request.getRequestURI().replace("/"+trimmedRight, "")).concat(query != null ? "?".concat(query) : "");
    }

//    private String rebuildUlr(HttpServletRequest request, String host, String prefix) {
//        String query = request.getQueryString();
//        char trimChar = '/';
//        // 清空左边的指定字符
//        String trimmedLeft = prefix.replaceAll("^" + trimChar + "+", "");
//        // 清空右边的指定字符
//        String trimmedRight = trimmedLeft.replaceAll(trimChar + "+$", "");
//        return host.concat(request.getRequestURI().replace("/"+trimmedRight, "")).concat(query != null ? "?".concat(query) : "");
//    }
}
