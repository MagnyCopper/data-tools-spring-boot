package io.github.magnycopper.datatools.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: Wind.BDG.PEVC.DataTools
 * @description: 日志小工具
 * @author: cmhan.han@wind.com.cn
 * @create: 2021-04-29 14:38
 */
@Slf4j
@Component
public class LogUtils {

    /**
     * 默认日志保存路径
     */
    public static String LOG_DIR_PATH;

    /**
     * 日志写入方法
     *
     * @param logFilePath 日志路径
     * @param logs        写入内容
     */
    private static void writeLogs(Path logFilePath, String... logs) {
        // 对文件地址加锁
        synchronized (logFilePath.toString().intern()) {
            try {
                // 创建保存路径
                Files.createDirectories(logFilePath.getParent());
                // 写入日志数据(替换换行符)
                Files.write(logFilePath, Arrays.stream(logs)
                        .map(text -> text.replaceAll("[\\r\\n]", ""))
                        .collect(Collectors.toList()), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                log.info("成功写入{}条日志,到{}", logs.length, logFilePath);
            } catch (Exception e) {
                log.error("日志" + logFilePath + "记录失败", e);
            }
        }
    }

    /**
     * 写入日志方法
     *
     * @param logFileName 日志名称
     * @param logs        需要写入的日志
     */
    public static void writeLogs(String logFileName, String... logs) {
        writeLogs(Paths.get(LOG_DIR_PATH + File.separator + logFileName + ".logx"), logs);
    }

    /**
     * 写入日志方法
     *
     * @param logFileName 日志名称
     * @param logs        需要写入的日志
     */
    public static void writeLogs(String logFileName, List<String> logs) {
        writeLogs(logFileName, logs.toArray(new String[]{}));
    }

    /**
     * 读取日志文件
     *
     * @param logFilePath 日志路径
     * @return 日志内容
     */
    private static List<String> readLogs(Path logFilePath) {
        // 对文件地址加锁
        synchronized (logFilePath.toString().intern()) {
            try {
                if (!"logx".equals(FilenameUtils.getExtension(logFilePath.toString()))) {
                    throw new RuntimeException(String.format("错误的日志文件格式:%s,请检查文件拓展名.", FilenameUtils.getExtension(logFilePath.toString())));
                }
                log.info("开始从日志文件{},读取日志", logFilePath);
                return Files.lines(logFilePath, StandardCharsets.UTF_8).parallel().collect(Collectors.toList());
            } catch (Exception e) {
                log.error("日志" + logFilePath + "读取失败", e);
                return new ArrayList<>();
            }
        }
    }

    /**
     * 读取日志内容
     *
     * @param logFilePath 日志文件路径
     * @return 日志内容
     */
    public static List<String> readLogs(String logFilePath) {
        return readLogs(Paths.get(logFilePath));
    }

    @Value(value = "${basic-service.log-dir-path:./logs}")
    public void setLogDirPath(String logDirPath) {
        LOG_DIR_PATH = logDirPath;
    }
}
