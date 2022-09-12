package io.github.magnycopper.datatools.buss.ascp.common;

import lombok.Getter;

@Getter
public enum AppleProductEnums {

    MQ1C3CH_A("iPhone 14 Pro 256GB 暗紫色", "MQ1C3CH/A"),
    MQ873CH_A("iPhone 14 Pro Max 256GB 深空黑色", "MQ873CH/A"),
    MGN63CH_A("MacBook Air (M1 芯片机型) - 深空灰色", "MGN63CH/A");

    private final String productName;
    private final String productCode;

    AppleProductEnums(String productName, String productCode) {
        this.productName = productName;
        this.productCode = productCode;
    }
}
