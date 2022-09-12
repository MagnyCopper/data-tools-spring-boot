package io.github.magnycopper.datatools.framework.plugin;

import io.github.magnycopper.datatools.framework.config.PluginProperties;

import java.util.Map;

/**
 * @program: Wind.BDG.PEVC.DataTools
 * @description: 基础插件类
 * @author: cmhan.han@wind.com.cn
 * @create: 2021-04-29 09:37
 */
public abstract class BasicPlugin {

    /**
     * 测试操作标记
     */
    private boolean isDryRun;

    /**
     * 插件的配置对象
     */
    private PluginProperties.Config config;

    /**
     * 获取当前插件的插件类型
     *
     * @return 插件类型
     */
    public abstract PluginTypeEnum getPluginType();

    /**
     * 插件初始化方法
     *
     * @throws Exception 主方法运行过程中发生异常
     */
    public void init() throws Exception {
    }

    /**
     * 获取测试操作状态标记
     *
     * @return 测试操作状态标记
     */
    protected final Boolean isDryRun() {
        return config.getIsDryRun();
    }

    /**
     * 获取插件个性化配置
     *
     * @return 插件个性化配置
     */
    protected final Map<String, String> getConfigMaps() {
        return config.getConfigMaps();
    }

    /**
     * 绑定插件配置
     *
     * @param config 插件配置
     */
    public final void bindConfig(PluginProperties.Config config) {
        this.config = config;
    }

    /**
     * 获取插件配置
     *
     * @return 插件配置
     */
    public final PluginProperties.Config getConfig() {
        return this.config;
    }
}
