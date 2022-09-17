package io.github.magnycopper.datatools.buss.ascp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.magnycopper.datatools.buss.ascp.common.AppleProductEnums;
import io.github.magnycopper.datatools.buss.ascp.common.AppleStoreEnums;
import io.github.magnycopper.datatools.buss.ascp.entity.InStockStateEntity;
import io.github.magnycopper.datatools.common.utils.RequestUtils;
import io.github.magnycopper.datatools.common.utils.TelegramBotApiUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

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
    private JsonNode requestFulfillmentMessagesAPI(AppleStoreEnums appleStoreEnums, AppleProductEnums... appleProductEnums) throws IOException {
        Map<String, String> requestParms = new HashMap<>();
        if (appleStoreEnums != null) {
            requestParms.put("store", appleStoreEnums.getId());
        }
        for (int i = 0; i < appleProductEnums.length; i++) {
            requestParms.put("parts." + i, appleProductEnums[i].getCode());
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
    public List<InStockStateEntity> checkFulfillmentMessages(AppleStoreEnums appleStoreEnums, AppleProductEnums... appleProductEnums) throws IOException {
        JsonNode rootNode = requestFulfillmentMessagesAPI(appleStoreEnums, appleProductEnums);
        // 按产品区分存货状态
        Map<String, InStockStateEntity> inStockStateMap = new HashMap<>();
        // 自提信息节点
        JsonNode pickupMessageNode = rootNode.at("/pickupMessage");
        // 邮寄信息节点
        JsonNode deliveryMessageNode = rootNode.at("/deliveryMessage");
        // 依次检查各个产品
        List<InStockStateEntity> inStockStateEntities = new ArrayList<>();
        for (AppleProductEnums product : appleProductEnums) {
            InStockStateEntity inStockStateEntity = new InStockStateEntity();
            inStockStateEntity.setAppleStoreEnums(appleStoreEnums);
            inStockStateEntity.setAppleProductEnums(product);
            // 处理自提时间
            pickupMessageNode.at("/stores").forEach(stores -> {
                String storeNumber = stores.at("/storeNumber").asText();
                if (appleStoreEnums.getId().equals(storeNumber)) {
                    JsonNode partsAvailabilityNode = stores.at("/partsAvailability");
                    String pickupSearchQuote = partsAvailabilityNode.get(product.getCode()).at("/pickupSearchQuote").asText();
                    inStockStateEntity.setPickupDate(pickupSearchQuote);
                }
            });
            // 处理邮寄时间
            Map<String, Set<String>> deliveryDateMap = new HashMap<>();
            deliveryMessageNode.get(product.getCode()).at("/regular/deliveryOptions").forEach(deliveryOptions -> {
                String shippingCost = deliveryOptions.at("/shippingCost").asText();
                String date = deliveryOptions.at("/date").asText();
                Set<String> dataSet = new HashSet<>();
                dataSet.add(date);
                deliveryDateMap.merge(shippingCost, dataSet, (oldValue, newValue) -> {
                    oldValue.addAll(dataSet);
                    return oldValue;
                });
            });
            if (deliveryDateMap.size() > 0) {
                if (deliveryDateMap.containsKey("免费")) {
                    inStockStateEntity.setDeliveryDate(String.join("/", deliveryDateMap.get("免费")));
                }
                if (deliveryDateMap.containsKey("RMB 45")) {
                    inStockStateEntity.setFastDeliveryDate(String.join("/", deliveryDateMap.get("RMB 45")));
                }
            }
            if (!StringUtils.isAllBlank(inStockStateEntity.getPickupDate(), inStockStateEntity.getDeliveryDate(), inStockStateEntity.getFastDeliveryDate())) {
                inStockStateEntities.add(inStockStateEntity);
            }
        }
        return inStockStateEntities;
    }
}
