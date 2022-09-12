package io.github.magnycopper.datatools.framework.plugin;

/**
 * @program: Wind.BDG.PEVC.DataTools
 * @description: 基础数据处理插件
 * @author: cmhan.han@wind.com.cn
 * @create: 2021-04-29 09:42
 */
public abstract class BasicDataToolsPlugin extends BasicPlugin {

    /**
     * 数据处理插件主方法
     *
     * @throws Exception 主方法运行过程中发生异常
     */
    public abstract void run() throws Exception;

    @Override
    public final PluginTypeEnum getPluginType() {
        return PluginTypeEnum.DATA_TOOLS;
    }
}
