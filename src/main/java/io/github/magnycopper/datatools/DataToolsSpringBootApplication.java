package io.github.magnycopper.datatools;

import io.github.magnycopper.datatools.framework.config.PluginProperties;
import io.github.magnycopper.datatools.framework.config.PluginState;
import io.github.magnycopper.datatools.framework.plugin.BasicDataToolsPlugin;
import io.github.magnycopper.datatools.framework.plugin.BasicPlugin;
import io.github.magnycopper.datatools.framework.plugin.PluginTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@EnableCaching
@EnableRetry
@EnableScheduling
@SpringBootApplication
public class DataToolsSpringBootApplication implements ApplicationRunner {

    /**
     * 插件配置文件
     */
    @Autowired
    private PluginProperties pluginProperties;

    /**
     * 注入启动的插件对象
     */
    @Autowired
    private BasicPlugin basicPlugin;

    /**
     * 框架初始化函数
     *
     * @throws Exception 框架初始化异常
     */
    @PostConstruct
    public void init() throws Exception {
        log.info("加载的插件名称：{}", pluginProperties.getActivePluginName());
        // 获取插件配置
        PluginProperties.Config config = getPluginConfig(pluginProperties.getActivePluginName());
        // 绑定插件对象和配置
        basicPlugin.bindConfig(config);
        // 更新插件状态
        PluginState.update(basicPlugin);
        // 数据插件状态的方法
        log.info("当前插件:{}的状态：\n {}", pluginProperties.getActivePluginName(), PluginState.print());
        log.info("开始执行插件:{}的init方法...", pluginProperties.getActivePluginName());
        basicPlugin.init();
    }

    /**
     * 从插件配置中获取启用插件的配置
     *
     * @param activePluginName 启用插件的名称
     * @return 插件的配置
     */
    private PluginProperties.Config getPluginConfig(String activePluginName) {
        Map<String, List<PluginProperties.Config>> configMap = pluginProperties.getConfigs().stream().collect(Collectors.groupingBy(PluginProperties.Config::getName));
        if (configMap.getOrDefault(activePluginName, new ArrayList<>()).size() != 1) {
            throw new RuntimeException(String.format("无法确定%s插件的唯一配置,请检查application-plugins.yaml配置文件", activePluginName));
        }
        return configMap.get(activePluginName).stream().findAny().orElse(new PluginProperties.Config());
    }

    public static void main(String[] args) {
        SpringApplication.run(DataToolsSpringBootApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 添加计时器
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            log.info("执行插件:{}", pluginProperties.getActivePluginName());
            // 调用对应插件的run方法
            if (basicPlugin.getPluginType() == PluginTypeEnum.DATA_TOOLS) {
                ((BasicDataToolsPlugin) basicPlugin).run();
            }
        } catch (Exception e) {
            log.error("运行插件:" + pluginProperties.getActivePluginName() + "发生异常!", e);
        } finally {
            stopWatch.stop();
            log.info("运行插件:{}结束,耗时:{}", pluginProperties.getActivePluginName(), stopWatch.formatTime());
            stopWatch.reset();
        }
    }

    /**
     * 定时任务处理函数
     */
    @Scheduled(cron = "#{@pluginState.scheduledCron}", zone = "Asia/Shanghai")
    public void scheduledHandler() {
        // 添加计时器
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            log.info("定时执行插件:{},cron表达式:{}", pluginProperties.getActivePluginName(), PluginState.scheduledCron);
            // 调用对应插件的run方法
            if (basicPlugin.getPluginType() == PluginTypeEnum.DATA_TOOLS) {
                ((BasicDataToolsPlugin) basicPlugin).run();
            }
        } catch (Exception e) {
            log.error("运行插件:" + pluginProperties.getActivePluginName() + "发生异常!", e);
        } finally {
            stopWatch.stop();
            log.info("运行插件:{}结束,耗时:{}", pluginProperties.getActivePluginName(), stopWatch.formatTime());
            stopWatch.reset();
        }
    }
}
