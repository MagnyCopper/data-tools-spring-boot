package io.github.magnycopper.datatools.buss.ascp.entity;

import io.github.magnycopper.datatools.buss.ascp.common.AppleProductEnums;
import io.github.magnycopper.datatools.buss.ascp.common.AppleStoreEnums;
import lombok.Data;

import java.io.Serializable;

@Data
public class InStockStateEntity implements Serializable {

    /**
     * AppleStore
     */
    private AppleStoreEnums appleStoreEnums;

    /**
     * 产品
     */
    private AppleProductEnums appleProductEnums;

    /**
     * 自提日期
     */
    private String pickupDate;

    /**
     * 邮寄日期
     */
    private String deliveryDate;

    /**
     * 同城快递日期
     */
    private String fastDeliveryDate;
}
