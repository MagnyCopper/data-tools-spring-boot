package io.github.magnycopper.datatools.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: datatools-spring-boot-project
 * @description: 批处理工具类
 * @author: cmhan.han@wind.com.cn
 * @create: 2020-11-18 11:26
 */
@Slf4j
public class BatchUtils {

    /**
     * 批处理方法
     *
     * @param tList     待处理的list
     * @param batchSize 没批数量
     * @param batchOps  分批操作
     * @param <T>       元素类型
     */
    public static <T> void batchProcess(List<T> tList, int batchSize, BatchOps<T> batchOps) {
        List<List<T>> batchLists = ListUtils.partition(tList, batchSize);
        for (int i = 1; i <= batchLists.size(); i++) {
            try {
                log.info("开始单线程处理第{}批数据，总计：{}批", i, batchLists.size());
                batchOps.batchOp(batchLists.get(i - 1));
            } catch (Exception e) {
                log.error("batchOp发生异常", e);
            }
        }
    }

    /**
     * 多线程批处理方法
     *
     * @param tList       待处理的list
     * @param batchSize   没批数量
     * @param parallelism 线程数
     * @param batchOps    分批操作
     * @param <T>         元素类型
     */
    public static <T> void batchProcess(List<T> tList, int batchSize, int parallelism, BatchOps<T> batchOps) {
        if (parallelism <= 1) {
            batchProcess(tList, batchSize, batchOps);
        } else {
            ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
            try {
                List<List<T>> batchLists = ListUtils.partition(tList, batchSize);
                AtomicInteger atomicInteger = new AtomicInteger(0);
                CountDownLatch countDownLatch = new CountDownLatch(batchLists.size());
                for (List<T> datas : batchLists) {
                    executorService.submit(() -> {
                        try {
                            log.info("多线程处理第{}批数据，总计：{}批,线程数:{}", atomicInteger.incrementAndGet(), batchLists.size(), parallelism);
                            batchOps.batchOp(datas);
                        } catch (Exception e) {
                            log.error("batchOp发生异常", e);
                        } finally {
                            countDownLatch.countDown();
                        }
                    });
                }
                countDownLatch.await();
            } catch (Exception e) {
                log.error("多线程分批异常", e);
            } finally {
                executorService.shutdown();
            }
        }
    }

    @FunctionalInterface
    public interface BatchOps<T> {

        /**
         * 批处理函数
         *
         * @param tList 待处理的list
         */
        void batchOp(List<T> tList) throws Exception;
    }
}
