package io.github.magnycopper.datatools.buss.ascp;

import io.github.magnycopper.datatools.buss.ascp.common.AppleProductEnums;
import io.github.magnycopper.datatools.buss.ascp.common.AppleStoreEnums;
import io.github.magnycopper.datatools.buss.ascp.entity.InStockStateEntity;
import io.github.magnycopper.datatools.buss.ascp.service.AppleStoreCheckerService;
import io.github.magnycopper.datatools.common.utils.TelegramBotApiUtils;
import io.github.magnycopper.datatools.framework.plugin.BasicDataToolsPlugin;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plugin", name = "active-plugin-name", havingValue = "apple-store-check-plugin")
public class AppleStoreCheckPlugin extends BasicDataToolsPlugin {

    private final AppleStoreCheckerService appleStoreCheckerService;
    private final TelegramBotApiUtils telegramBotApiUtils;

    public AppleStoreCheckPlugin(AppleStoreCheckerService appleStoreCheckerService, TelegramBotApiUtils telegramBotApiUtils) {
        this.appleStoreCheckerService = appleStoreCheckerService;
        this.telegramBotApiUtils = telegramBotApiUtils;
    }

    @Override
    public void run() throws Exception {
        // Apple Store库存检查
        Map<String, String> configMaps = getConfigMaps();
        String[] appleProductCodes = configMaps.getOrDefault("products", "").split(",");
        List<AppleProductEnums> appleProductEnums = Arrays.stream(appleProductCodes)
                .map(productCode -> AppleProductEnums.valueOf(productCode.replace("/", "_")))
                .collect(Collectors.toList());
        String[] cities = configMaps.getOrDefault("cities", "").split(",");
        String telegramChatId = configMaps.get("telegram-chat-id");
        List<InStockStateEntity> results = new ArrayList<>();
        for (AppleStoreEnums appleStoreEnums : AppleStoreEnums.values()) {
            if (StringUtils.equalsAny(appleStoreEnums.getCity(), cities)) {
                List<InStockStateEntity> inStockStateEntities = appleStoreCheckerService.checkFulfillmentMessages(appleStoreEnums, appleProductEnums.toArray(new AppleProductEnums[]{}));
                results.addAll(inStockStateEntities);
            }
        }
        // 按产品分组
        Map<String, List<InStockStateEntity>> productInStockGroup = results.stream()
                .collect(Collectors.groupingBy(inStockStateEntity -> inStockStateEntity.getAppleProductEnums().getName()));
        // 遍历输出结果
        productInStockGroup.forEach((k, v) -> {
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(String.format("%s->发货时间:\n", k));
                // 处理自提时间
                Map<String, List<InStockStateEntity>> pickupDateGroup = v.stream()
                        .filter(data -> StringUtils.isNotBlank(data.getPickupDate()))
                        .collect(Collectors.groupingBy(InStockStateEntity::getPickupDate));
                if (pickupDateGroup.size() > 0) {
                    stringBuilder.append("自提:\n");
                    pickupDateGroup.forEach((date, stores) -> stringBuilder.append(String.format("%s=>%s\n", date, stores.stream()
                            .map(store -> String.format("%s[%s]", store.getAppleStoreEnums().getName(), store.getAppleStoreEnums().getCity()))
                            .collect(Collectors.joining(",")))));
                }
                // 处理邮寄时间
                Map<String, List<InStockStateEntity>> deliveryDateGroup = v.stream()
                        .filter(data -> StringUtils.isNotBlank(data.getDeliveryDate()))
                        .collect(Collectors.groupingBy(InStockStateEntity::getDeliveryDate));
                if (deliveryDateGroup.size() > 0) {
                    stringBuilder.append("邮寄:\n");
                    deliveryDateGroup.forEach((date, stores) -> stringBuilder.append(String.format("%s=>%s\n", date, stores.stream()
                            .map(store -> String.format("%s[%s]", store.getAppleStoreEnums().getName(), store.getAppleStoreEnums().getCity()))
                            .collect(Collectors.joining(",")))));
                }
                // 同城邮寄时间
                Map<String, List<InStockStateEntity>> fastDeliveryDateGroup = v.stream()
                        .filter(data -> StringUtils.isNotBlank(data.getFastDeliveryDate()))
                        .collect(Collectors.groupingBy(InStockStateEntity::getFastDeliveryDate));
                if (fastDeliveryDateGroup.size() > 0) {
                    stringBuilder.append("同城快递:\n");
                    fastDeliveryDateGroup.forEach((date, stores) -> stringBuilder.append(String.format("%s=>%s\n", date, stores.stream()
                            .map(store -> String.format("%s[%s]", store.getAppleStoreEnums().getName(), store.getAppleStoreEnums().getCity()))
                            .collect(Collectors.joining(",")))));
                }
                log.info("\n{}", stringBuilder);
                telegramBotApiUtils.sendMessage(telegramChatId, stringBuilder.toString());
            } catch (Exception e) {
                log.error("处理产品发货时间发生异常", e);
            }
        });
    }
}
