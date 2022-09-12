package io.github.magnycopper.datatools.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Base64;

/**
 * @program: Wind.BDG.PEVC.DataTools
 * @description: 序列化工具
 * @author: cmhan.han@wind.com.cn
 * @create: 2022-08-18 15:45
 */
@Slf4j
public class SerializeUtils {

    /**
     * 反序列化
     *
     * @param serializeText 序列化文本
     * @param <T>           序列化的类型
     * @return 反序列化结果
     */
    public static <T> T deserialize(String serializeText) throws Exception {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Base64.getDecoder().decode(serializeText));
        try (ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return (T) objectInputStream.readObject();
        }
    }

    /**
     * 序列化函数
     *
     * @param serializable 需要被序列化的对象
     * @return 序列化结果
     */
    public static String serialize(Serializable serializable) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(serializable);
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        }
    }
}
