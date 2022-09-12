package io.github.magnycopper.datatools.common.utils;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @program: Wind.BDG.PEVC.DataTools
 * @description: SQL小工具
 * @author: cmhan.han@wind.com.cn
 * @create: 2021-04-29 14:21
 */
public class SQLUtils {

    /**
     * 获取需要操作的keys
     *
     * @param data       原始数据
     * @param keys       需要操作的key
     * @param ignoreKeys 需要过滤的key
     * @return 真实需要操作的key
     */
    public static List<String> getKeys(Map<String, Object> data, Collection<String> keys, String... ignoreKeys) {
        // 将data中的key经过2此过两次过滤,首先要在keys中,其次要不出现在ignoreKeys,设下的是合法key
        return data.keySet()
                .stream()
                .filter(key -> StringUtils.equalsAny(key, keys.toArray(new String[0])))
                .filter(key -> !StringUtils.equalsAny(key, ignoreKeys))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 获取需要操作的keys
     *
     * @param data       原始数据
     * @param ignoreKeys 需要过滤的key
     * @return 真实需要操作的key
     */
    public static List<String> getKeys(Map<String, Object> data, String... ignoreKeys) {
        return getKeys(data, data.keySet(), ignoreKeys);
    }

    /**
     * 获取需要处理的values
     *
     * @param data       原始数据
     * @param keys       需要操作的key
     * @param ignoreKeys 需要过滤的key
     * @return 需要处理的values
     */
    public static Object[] getValues(Map<String, Object> data, Collection<String> keys, String... ignoreKeys) {
        // 获取需要处理的keys
        List<String> keySet = getKeys(data, keys, ignoreKeys);
        // 按照key从data中取值
        return keySet.stream().map(key -> data.getOrDefault(key, null)).toArray(Object[]::new);
    }

    /**
     * 获取需要处理的values
     *
     * @param data       原始数据
     * @param ignoreKeys 需要过滤的key
     * @return 需要处理的values
     */
    public static Object[] getValues(Map<String, Object> data, String... ignoreKeys) {
        // 获取需要处理的keys
        List<String> keySet = getKeys(data, data.keySet(), ignoreKeys);
        // 按照key从data中取值
        return keySet.stream().map(key -> data.getOrDefault(key, null)).toArray(Object[]::new);
    }

    /**
     * 生成插入语句模板
     *
     * @param dbName     数据库名称
     * @param data       需要插入的数据
     * @param keys       需要写入的key
     * @param hasUpdate  是否添加UPDATE语句
     * @param ignoreKeys 忽略的key
     * @return 插入语句模板
     */
    private static String getInsertSQL(String dbName, Map<String, Object> data, Collection<String> keys, boolean hasUpdate, String... ignoreKeys) {
        // 获取需要处理的key
        List<String> keySet = getKeys(data, keys, ignoreKeys);
        if (hasUpdate) {
            // 生成update部分的语句
            List<String> updatePart = keySet.stream().map(key -> String.format("%s = VALUES(%s)", key, key)).collect(Collectors.toList());
            // 拼接SQL语句
            return String.format("INSERT INTO %s (%s) VALUES (%s) ON DUPLICATE KEY UPDATE %s", dbName, String.join(",", keySet), Stream.generate(() -> "?").limit(keySet.size()).collect(Collectors.joining(",")), String.join(",", updatePart));
        }
        // 拼接SQL语句
        return String.format("INSERT INTO %s (%s) VALUES (%s)", dbName, String.join(",", keySet), Stream.generate(() -> "?").limit(keySet.size()).collect(Collectors.joining(",")));
    }

    /**
     * 生成插入语句模板
     *
     * @param dbName     数据库名称
     * @param data       需要插入的数据
     * @param ignoreKeys 忽略的key
     * @return 插入语句模板
     */
    public static String getInsertSQL(String dbName, Map<String, Object> data, String... ignoreKeys) {
        return getInsertSQL(dbName, data, data.keySet(), false, ignoreKeys);
    }

    /**
     * 生成插入语句模板
     *
     * @param dbName     数据库名称
     * @param data       需要插入的数据
     * @param ignoreKeys 忽略的key
     * @return 插入语句模板
     */
    public static String getInsertSQLWithUpdate(String dbName, Map<String, Object> data, String... ignoreKeys) {
        return getInsertSQL(dbName, data, data.keySet(), true, ignoreKeys);
    }
}

@Data
class SQLParms {

    /**
     * SQL语句
     */
    private String sql;

    /**
     * SQL参数
     */
    private Object[] parms;
}
