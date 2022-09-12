package io.github.magnycopper.datatools.buss.ascp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.magnycopper.datatools.buss.ascp.common.AppleProductEnums;
import io.github.magnycopper.datatools.buss.ascp.common.AppleStoreEnums;
import io.github.magnycopper.datatools.common.utils.RequestUtils;
import io.github.magnycopper.datatools.common.utils.TelegramBotApiUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AppleStoreCheckerService {

    /**
     * 全局默认ObjectMapper
     */
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = JsonMapper.builder()
            .build();

    /***
     * AppleStore存货检查API地址
     */
    private static final String APPLE_STORE_CHECK_URL = "https://www.apple.com.cn/shop/fulfillment-messages";

    private final TelegramBotApiUtils telegramBotApiUtils;

    public AppleStoreCheckerService(TelegramBotApiUtils telegramBotApiUtils) {
        this.telegramBotApiUtils = telegramBotApiUtils;
    }

    /**
     * 请求线上接口查询对应库存数量
     *
     * @param appleStoreEnums   applestore代码
     * @param appleProductEnums 产品代码
     * @return 查询结果
     * @throws IOException 接口调用异常
     */
    private JsonNode request(AppleStoreEnums appleStoreEnums, AppleProductEnums... appleProductEnums) throws IOException {
        Map<String, String> requestParms = new HashMap<>();
        if (appleStoreEnums != null) {
            requestParms.put("store", appleStoreEnums.name());
        }
        for (int i = 0; i < appleProductEnums.length; i++) {
            requestParms.put("parts." + i, appleProductEnums[i].getProductCode());
        }
        String resultJsonString = RequestUtils.get(APPLE_STORE_CHECK_URL, requestParms);
        JsonNode resultNode = DEFAULT_OBJECT_MAPPER.readTree(resultJsonString);
        String resultCode = resultNode.at("/head/status").asText();
        if (!"200".equals(resultCode)) {
            throw new IOException(String.format("接口调用失败:%s", APPLE_STORE_CHECK_URL));
        }
        return resultNode.at("/body/content");
    }

    /**
     * 检查Applestore官网产品存货
     *
     * @param appleStoreEnums   AppleStore店代码
     * @param appleProductEnums 产品代码
     * @throws IOException 查询时发生异常
     */
    public void checkAppleStore(AppleStoreEnums appleStoreEnums, AppleProductEnums appleProductEnums) throws IOException {
        JsonNode rootNode = request(appleStoreEnums, appleProductEnums);
        // 邮寄预计时间
        JsonNode deliveryMessageName = rootNode.at("/deliveryMessage");
        // 依次检查各个产品
        JsonNode deliveryProductNode = deliveryMessageName.get(appleProductEnums.getProductCode());
        String deliveryDate = deliveryProductNode.at("/regular/deliveryOptions/0/date").asText();
        // 线下自提检查
        Map<String, String> pickupStoreState = new HashMap<>();
        for (JsonNode storesNode : rootNode.at("/pickupMessage/stores")) {
            String storeName = storesNode.at("/storeName").asText();
            JsonNode partsAvailabilityNode = storesNode.at("/partsAvailability");
            // 依次检查各个产品
            JsonNode pickupProductNode = partsAvailabilityNode.get(appleProductEnums.getProductCode());
            String pickupSearchQuote = pickupProductNode.at("/pickupSearchQuote").asText();
            pickupStoreState.put(storeName, pickupSearchQuote);
        }
        // 汇总输出结果
        String pickupString = pickupStoreState.entrySet().stream()
                .map(entry -> String.format("%s:%s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n"));
        String result = String.format("[%s]的存货情况\n邮寄预计时间:\n%s\n线下自提时间:\n%s", appleProductEnums.getProductName(), deliveryDate, pickupString);
        log.info(result);
        telegramBotApiUtils.sendMessage("1383302470", result);
    }
}
