package io.github.magnycopper.datatools.common.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.retry.support.RetryTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @program: Wind.BDG.PEVC.DataTools
 * @description: 通用请求工具类
 * @author: cmhan.han@wind.com.cn
 * @create: 2022-08-19 10:32
 */
@Slf4j
public class RequestUtils {

    /**
     * 重试器
     */
    private static final RetryTemplate RETRY_TEMPLATE = RetryTemplate.builder()
            .maxAttempts(3)
            .fixedBackoff(1000)
            .build();

    /**
     * 默认OkHttpClient
     */
    private static final OkHttpClient DEFAULT_OK_HTTP_CLIENT = new OkHttpClient();

    /**
     * 使用get发送请求
     *
     * @param url   接口地址
     * @param parms 接口参数
     * @return 接口返回值
     * @throws IOException 请求接口发生异常
     */
    public static String get(String url, Map<String, String> parms) throws IOException {
        // 计时器
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 解析url
        HttpUrl.Builder httpUrlBuilder = HttpUrl
                .parse(url)
                .newBuilder();
        if (parms != null && parms.size() > 0) {
            for (Map.Entry<String, String> entry : parms.entrySet()) {
                httpUrlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }
        HttpUrl httpUrl = httpUrlBuilder.build();
        Request request = new Request.Builder()
                .url(httpUrl)
                .build();
        String result = RETRY_TEMPLATE.execute(retryContext -> {
            try (Response response = DEFAULT_OK_HTTP_CLIENT.newCall(request)
                    .execute()) {
                ResponseBody body = response.body();
                return body == null ? null : body.string();
            }
        });
        stopWatch.stop();
        log.info("请求GET接口:{},耗时:{}s", httpUrl.uri(), stopWatch.getTime(TimeUnit.SECONDS));
        return result;
    }

    /**
     * 使用post发送请求
     *
     * @param url      请求地址
     * @param bodyJson 请求体内容
     * @return 接口返回结果
     * @throws IOException 请求接口发生异常
     */
    public static String post(String url, String bodyJson) throws IOException {
        // 计时器
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 解析url
        HttpUrl httpUrl = HttpUrl.parse(url);
        // 构建requestBody
        RequestBody requestBody = RequestBody.create(MediaType.get("application/json;charset=utf-8"), bodyJson);
        Request request = new Request.Builder()
                .url(httpUrl)
                .post(requestBody)
                .build();
        String result = RETRY_TEMPLATE.execute(retryContext -> {
            try (Response response = DEFAULT_OK_HTTP_CLIENT.newCall(request)
                    .execute()) {
                ResponseBody body = response.body();
                return body == null ? null : body.string();
            }
        });
        stopWatch.stop();
        log.info("请求POST接口:{},耗时:{}s", httpUrl.uri(), stopWatch.getTime(TimeUnit.SECONDS));
        return result;
    }
}
