package io.github.magnycopper.datatools.common.utils;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @program: Wind.BDG.PEVC.DataTools
 * @description: 差异分析工具
 * @author: cmhan.han@wind.com.cn
 * @create: 2021-03-26 10:47
 */
@Slf4j
@Component
public class DiffUtils {

    /**
     * 空值占位符
     */
    private static final String NULL_MARK = "@NULL@";
    /**
     * 内存桶
     */
    private static final Map<String, Map<String, List<Node>>> STORE = new HashMap<>();
    /**
     * 分桶数量
     */
    @Value(value = "${basic-service.diff.diff-bucket-num:1}")
    private int BUCKETS;
    /**
     * 差分比较并发度
     */
    @Value(value = "${basic-service.diff.parallelism:1}")
    private int PARALLELISM;

    /**
     * 将数据写入存储桶
     *
     * @param taskId          任务ID
     * @param dataType        数据类型
     * @param keys            数据唯一键
     * @param collections     原始数据
     * @param writeToBucketOp 将数据入桶的函数
     */
    public synchronized void addToBucket(String taskId, DataType dataType, Collection<String> keys, Collection<Map<String, Object>> collections, WriteToBucketOp writeToBucketOp) {
        // 将原始数据依次生成Node对象并按照hash值分组
        Collection<Map<String, Object>> datas = collections == null ? new ArrayList<>() : collections;
        Map<String, List<Node>> buckets = datas
                .parallelStream()
                .map(data -> new Node(taskId, dataType, keys, data))
                .collect(Collectors.groupingByConcurrent(node -> Math.abs(node.hash() % BUCKETS) + ""));
        // 使用流写入桶
        writeToBucketOp.writeToBucket(buckets);
    }

    /**
     * 将数据写入内存桶
     *
     * @param taskId   任务ID
     * @param dataType 数据类型
     * @param keys     数据唯一键
     * @param datas    原始数据
     */
    public void addToMemBucket(String taskId, DataType dataType, Collection<String> keys, Collection<Map<String, Object>> datas) {
        addToBucket(taskId, dataType, keys, datas, buckets -> {
            // 将数据写入内存桶
            // 1. 从STRORE中根据taskID加载分桶数据,若不存在则初始化
            Map<String, List<Node>> bucket = STORE.get(taskId);
            if (bucket == null) {
                bucket = new ConcurrentHashMap<>();
                STORE.put(taskId, bucket);
            }
            log.info("桶内数据:{},待写入数据:{}",
                    bucket.entrySet()
                            .stream()
                            .map(entry -> String.format("%s号桶=%s条", entry.getKey(), entry.getValue().size()))
                            .collect(Collectors.joining("|")),
                    buckets.entrySet()
                            .stream()
                            .map(entry -> String.format("%s号桶=%s条", entry.getKey(), entry.getValue().size()))
                            .collect(Collectors.joining("|")));
            // 2. 依次将计算结果与原有数据进行合并
            for (Map.Entry<String, List<Node>> entry : buckets.entrySet()) {
                bucket.merge(entry.getKey(), entry.getValue(), (oldData, newData) -> {
                    oldData.addAll(newData);
                    return oldData;
                });
            }
            log.info("写入后桶内数据:{}",
                    bucket.entrySet()
                            .stream()
                            .map(entry -> String.format("%s号桶=%s条", entry.getKey(), entry.getValue().size()))
                            .collect(Collectors.joining("|")));
        });
    }

    /**
     * target变成source差分
     *
     * @param taskId           差分任务ID
     * @param moreOp           差分后的多余数据操作
     * @param diffOp           差分后的差异数据操作
     * @param lessOp           差分后的缺少操作
     * @param readFromBucketOp 从差分桶中读取数据
     * @param cleanBucketOp    清空差分桶
     * @param ignoreFields     差分比较时忽略的字段
     */
    public void diff(String taskId, MoreOp moreOp, DiffOp diffOp, LessOp lessOp, ReadFromBucketOp readFromBucketOp, CleanBucketOp cleanBucketOp, String... ignoreFields) {
        ExecutorService executorService = Executors.newFixedThreadPool(PARALLELISM);
        CountDownLatch countDownLatch = new CountDownLatch(BUCKETS);
        try {
            for (int i = 0; i < BUCKETS; i++) {
                int bucketNum = i;
                executorService.submit(() -> {
                    try {
                        // 查询需要比较的数据
                        // 分别读取桶中数据
                        List<Node> sourceDatas = readFromBucketOp.readFromBucket(taskId, DataType.SOURCE, bucketNum);
                        List<Node> targetDatas = readFromBucketOp.readFromBucket(taskId, DataType.TARGET, bucketNum);
                        // 差分比较
                        Map<String, List<Map<String, Object>>> diffResult = compareBucket(sourceDatas, targetDatas, ignoreFields);
                        List<Map<String, Object>> moreDatas = diffResult.get("MORE");
                        List<Map<String, Object>> diffDatas = diffResult.get("DIFF");
                        List<Map<String, Object>> lessDatas = diffResult.get("LESS");
                        log.info("任务Id:{},{}号桶,source较target比较结果,多出:{},差异:{},缺失:{}", taskId, bucketNum, moreDatas.size(), diffDatas.size(), lessDatas.size());
                        // 多余数据处理操作
                        moreOp.moreDataHandler(moreDatas);
                        // 差异数据处理操作
                        diffOp.diffDataHandler(diffDatas);
                        // 缺少数据处理操作
                        lessOp.lessDataHandler(lessDatas);
                    } catch (Exception e) {
                        log.error("任务ID:" + taskId + ",桶编号:" + bucketNum + "差异比较时发生异常", e);
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            }
            countDownLatch.await();
        } catch (Exception e) {
            log.error(taskId + "多线程差异比较发生异常", e);
        } finally {
            // 释放线程池
            executorService.shutdown();
            // 删除差分数据
            cleanBucketOp.cleanBucket(taskId);
        }
    }

    /**
     * target变成source差分
     *
     * @param taskId       差分任务ID
     * @param moreOp       差分后的多余数据操作
     * @param diffOp       差分后的差异数据操作
     * @param lessOp       差分后的缺少操作
     * @param ignoreFields 差分比较时忽略的字段
     */
    public void memDiff(String taskId, MoreOp moreOp, DiffOp diffOp, LessOp lessOp, String... ignoreFields) {
        diff(taskId,
                moreOp,
                diffOp,
                lessOp,
                (id, type, num) -> {
                    // 从STORE中加载数据
                    Map<String, List<Node>> buckets = STORE.get(id);
                    List<Node> bucket = buckets.containsKey(num + "") ? buckets.get(num + "") : new ArrayList<>();
                    return bucket
                            .stream()
                            .filter(node -> type.equals(node.getDataType()))
                            .collect(Collectors.toList());
                },
                STORE::remove,
                ignoreFields);
    }

    /**
     * 比较桶中数据source->target
     *
     * @param sourceDatas  来源数据
     * @param targetDatas  目标数据
     * @param ignoreFields 比较时忽略的字段
     * @return 差分比较结果
     */
    private Map<String, List<Map<String, Object>>> compareBucket(@NonNull List<Node> sourceDatas, @NonNull List<Node> targetDatas, String... ignoreFields) {
        // 待处理数据
        List<Map<String, Object>> moreList = new ArrayList<>();
        List<Map<String, Object>> diffList = new ArrayList<>();
        List<Map<String, Object>> lessList = new ArrayList<>();
        if (sourceDatas.size() == 0 && targetDatas.size() == 0) {
            // 首先判断是否来源和目标同时为空
        } else {
            if (sourceDatas.size() == 0) {
                // 特殊情况A:source文件不存在,全部target均为新增数据;
                lessList.addAll(targetDatas.stream()
                        .map(Node::getData)
                        .collect(Collectors.toList()));
            } else if (targetDatas.size() == 0) {
                // 特殊情况B:target文件不存在,全部source均为删除数据;
                moreList.addAll(sourceDatas.stream()
                        .map(Node::getData)
                        .collect(Collectors.toList()));
            } else {
                // 正常比较差异,首先加载目标数据,并按照hash分组
                Map<Integer, List<Node>> targetDatasGroupByHash = targetDatas.stream()
                        .collect(Collectors.groupingBy(Node::hash));
                // 依次遍历来源数据,逐个比较结果
                for (Node sourceData : sourceDatas) {
                    if (sourceData != null) {
                        // 若目标桶中包含该数据的hash
                        if (targetDatasGroupByHash.containsKey(sourceData.hash())) {
                            // 包含hash则依次比较唯一键的值是否一致
                            String sourceKeyValues = String.join("", sourceData.keyValues());
                            String sourceValues = String.join("", sourceData.values(ignoreFields));
                            // 根据hash值从目标Map中查询数据
                            List<Node> targetNodes = targetDatasGroupByHash.get(sourceData.hash());
                            // 迭代器循环比较
                            Iterator<Node> targetNodeIterator = targetNodes.iterator();
                            while (targetNodeIterator.hasNext()) {
                                Node targetNode = targetNodeIterator.next();
                                String targetKeyValues = String.join("", targetNode.keyValues());
                                if (sourceKeyValues.equals(targetKeyValues)) {
                                    // 找到相同数据,判断是否需要更新
                                    String targetValues = String.join("", targetNode.values(ignoreFields));
                                    if (!sourceValues.equals(targetValues)) {
                                        diffList.add(sourceData.getData());
                                    }
                                    targetNodeIterator.remove();
                                }
                            }
                            // 若移除后链表的长度为0,则移除该key
                            if (targetNodes.size() == 0) {
                                targetDatasGroupByHash.remove(sourceData.hash());
                            }
                        } else {
                            // 目标桶中不存在,需要插入
                            moreList.add(sourceData.getData());
                        }
                    }
                }
                // target中剩余的数据为删除数据
                lessList.addAll(targetDatasGroupByHash.values().stream()
                        .flatMap(Collection::stream)
                        .map(Node::getData)
                        .collect(Collectors.toList()));
            }
        }
        // 封装对象返回
        Map<String, List<Map<String, Object>>> compareResult = new HashMap<>(3);
        compareResult.put("MORE", moreList);
        compareResult.put("DIFF", diffList);
        compareResult.put("LESS", lessList);
        return compareResult;
    }

    /**
     * 数据类型枚举
     */
    public enum DataType {
        // 原始数据
        SOURCE,
        // 目标数据
        TARGET
    }

    /**
     * source->target多出数据处理函数
     */
    @FunctionalInterface
    public interface MoreOp {

        /**
         * source->target多出数据处理函数
         *
         * @param moreDataList source->target多出数据
         */
        void moreDataHandler(List<Map<String, Object>> moreDataList);
    }

    /**
     * source->target多出数据处理函数
     */
    @FunctionalInterface
    public interface DiffOp {

        /**
         * source->target差异数据处理函数
         *
         * @param diffDataList source->target差异数据
         */
        void diffDataHandler(List<Map<String, Object>> diffDataList);
    }

    /**
     * source->target少数据处理函数
     */
    @FunctionalInterface
    public interface LessOp {

        /**
         * source->target少数据处理函数
         *
         * @param lessDataList source->target缺少数据
         */
        void lessDataHandler(List<Map<String, Object>> lessDataList);
    }

    /**
     * 数据入桶接口
     */
    @FunctionalInterface
    public interface WriteToBucketOp {

        /**
         * 向差分桶写入数据
         *
         * @param buckets 预差分数据
         */
        void writeToBucket(Map<String, List<Node>> buckets);
    }

    /**
     * 从桶中读取数据接口
     */
    @FunctionalInterface
    public interface ReadFromBucketOp {

        /**
         * 从差分桶中读取数据
         *
         * @param taskId    差分任务ID
         * @param dataType  差分数据类型
         * @param bucketNum 差分桶编号
         */
        List<Node> readFromBucket(String taskId, DataType dataType, int bucketNum);
    }

    /**
     * 从桶中读取数据接口
     */
    @FunctionalInterface
    public interface CleanBucketOp {

        /**
         * 清空差分数据
         *
         * @param taskId 差分任务ID
         */
        void cleanBucket(String taskId);
    }

    /**
     * 数据节点对象
     */
    @Data
    @NoArgsConstructor
    private static class Node implements Serializable {

        /**
         * 差分任务ID
         */
        private String taskId;

        /**
         * 数据类型
         */
        private DataType dataType;

        /**
         * 唯一键
         */
        private Collection<String> keys;

        /**
         * 原始数据
         */
        private Map<String, Object> data;

        /**
         * 构造方法
         *
         * @param taskId   差分任务ID
         * @param dataType 差分数据类型
         * @param keys     唯一键
         * @param data     原始数据
         */
        public Node(String taskId, DataType dataType, Collection<String> keys, Map<String, Object> data) {
            this.taskId = taskId;
            this.dataType = dataType;
            // 仅使用存在于数据中的keys
            this.keys = keys.stream()
                    .distinct()
                    .filter(data::containsKey)
                    .collect(Collectors.toList());
            this.data = data;
        }

        /**
         * 查询全部可用key对应的values,NULL值用'@NULL@'代替
         *
         * @param ignoreFields 需要忽略处理的字段
         * @return values值
         */
        public List<String> values(String... ignoreFields) {
            // 按照统一的方式对key排序以保证value的顺序
            return data.keySet().stream()
                    .sorted()
                    .filter(key -> !StringUtils.equalsAny(key, ignoreFields))
                    .map(key -> Objects.toString(data.get(key), NULL_MARK))
                    .collect(Collectors.toList());
        }

        /**
         * 查询全部唯一键的对应value,NULL值用'@NULL@'代替
         *
         * @return 多个唯一键的value组合
         */
        public List<String> keyValues() {
            return keys.stream()
                    .sorted()
                    .map(key -> Objects.toString(data.get(key), NULL_MARK))
                    .collect(Collectors.toList());
        }

        /**
         * 根据多个唯一键的值计算hash值
         *
         * @return hash值
         */
        public int hash() {
            return Objects.hash(keyValues().toArray());
        }
    }
}
