package com.cat2bug.common.utils;

import com.alibaba.fastjson.JSON;
import okhttp3.*;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
@Author: yuzhantao
@CreateTime: 2024-12-26 13:25
@Version: 1.0.0
*/
public class CompressUtil {

    private ZipOutputStream outputStream;

    private static final int minCONNECT_TIMEOUT = 3000;
    /**
     * Http 写入超时时间
     */
    private static final int minWRITE_TIMEOUT = 3000;
    /**
     * Http Read超时时间
     */
    private static final int minREAD_TIMEOUT = 3000;
    /**
     * Http Async Call Timeout
     */
    private static final int minCall_TIMEOUT = 3000;
    /**
     * Http连接池
     */
    private static final int connectionPoolSize = 1000;
//    /**
//     * 静态Http请求池
//     */
//    private static OkHttpClient client=null;
    /**
     * 静态连接池对象
     */
    private static ConnectionPool mConnectionPool=new ConnectionPool(connectionPoolSize, 30, TimeUnit.MINUTES);
    /**
     * ContentType
     */
    private static final String ContentType="application/json;charset=utf-8";
    /**
     * AcceptType
     */
    private static final String AcceptType="application/json;charset=utf-8";
    /**
     * Content-Type
     */
    private static final MediaType MediaType_ContentType = MediaType.parse(ContentType);


    public static CompressUtil createStream(String fileName) throws FileNotFoundException {
        return new CompressUtil(fileName);
    }

    private CompressUtil(String fileName) throws FileNotFoundException {
        this.outputStream = new ZipOutputStream(new FileOutputStream(fileName));
    }

    public void build() throws IOException {
        this.outputStream.close();
    }

    public CompressUtil addJsonFile(String fileName, Object obj) throws IOException {
        ZipEntry entry = new ZipEntry(fileName);
        this.outputStream.putNextEntry(entry);
        byte[] buffer = JSON.toJSONString(obj).getBytes(StandardCharsets.UTF_8);
        this.outputStream.write(buffer, 0, buffer.length);
        this.outputStream.closeEntry();
        return this;
    }

    public CompressUtil addUrlFile(String fileName, String url) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
//                .addInterceptor(new RetryIntercepter(3))//重试3次
//                .addInterceptor(new GzipRequestInterceptor())//gzip压缩
                .connectTimeout(minCONNECT_TIMEOUT, TimeUnit.MILLISECONDS) //连接超时
                .readTimeout(minREAD_TIMEOUT, TimeUnit.MILLISECONDS) //读取超时
                .writeTimeout(minWRITE_TIMEOUT, TimeUnit.MILLISECONDS) //写超时
                .callTimeout(minCall_TIMEOUT, TimeUnit.MILLISECONDS)
                // okhttp默认使用的RealConnectionPool初始化线程数==2147483647，在服务端会导致大量线程TIMED_WAITING
                //ThreadPoolExecutor(0, 2147483647, 60L, TimeUnit.SECONDS, new SynchronousQueue(), Util.threadFactory("OkHttp ConnectionPool", true));
                .connectionPool(mConnectionPool)
                .build();

        Request request = new Request.Builder()
                //访问路径
                .url(url)
                .build();
        Response response = null;
        response = client.newCall(request).execute();
        //转化成byte数组
        byte[] bytes = response.body().bytes();
//        ClassPathResource resource = new ClassPathResource(url);
//        InputStream inputStream = resource.getInputStream();
//
//        byte[] bytes = IOUtils.toByteArray(inputStream);
        ZipEntry entry = new ZipEntry(fileName);
        this.outputStream.putNextEntry(entry);
        this.outputStream.write(bytes, 0, bytes.length);
        this.outputStream.closeEntry();

        response.close();
        return this;
    }
}
