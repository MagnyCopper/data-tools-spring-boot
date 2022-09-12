package io.github.magnycopper.datatools.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @program: Wind.BDG.PEVC.DataTools
 * @description: 插件相关配置文件映射
 * @author: cmhan.han@wind.com.cn
 * @create: 2021-04-29 09:31
 */
@Data
@Component
@ConfigurationProperties(prefix = "plugin")
public class PluginProperties {

    /**
     * 启用的插件名称
     */
    private String activePluginName;

    /**
     * 插件配置列
     */
    private List<Config> configs;

    /**
     * 插件配置项
     */
    @Data
    public static class Config {
        /**
         * 插件名称
         */
        private String name;

        /**
         * 测试操作标识
         */
        private Boolean isDryRun = false;

        /**
         * 插件的定时表达式
         */
        private String cron;

        /**
         * 插件个性化配置信息
         */
        private Map<String, String> configMaps;
    }
}
