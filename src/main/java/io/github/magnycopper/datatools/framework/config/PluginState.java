package io.github.magnycopper.datatools.framework.config;

import io.github.magnycopper.datatools.framework.plugin.BasicPlugin;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

/**
 * @program: Wind.BDG.PEVC.DataTools
 * @description: 插件状态类
 * @author: cmhan.han@wind.com.cn
 * @create: 2021-04-29 10:00
 */
@Slf4j
@Component
public class PluginState {

    /**
     * 定时插件cron表达式
     */
    public static String scheduledCron = ScheduledTaskRegistrar.CRON_DISABLED;

    /**
     * 插件状态更新函数
     *
     * @param basicPlugin 插件类
     */
    public static void update(BasicPlugin basicPlugin) {
        PluginProperties.Config config = basicPlugin.getConfig();
        // 更新定时表达式
        scheduledCron = StringUtils.isNotBlank(config.getCron()) ? config.getCron() : ScheduledTaskRegistrar.CRON_DISABLED;
    }

    /**
     * 输出打印信息
     *
     * @return 状态数据
     */
    public static String print() {
        return String.format("scheduledCron = %s \n ", scheduledCron);
    }
}
