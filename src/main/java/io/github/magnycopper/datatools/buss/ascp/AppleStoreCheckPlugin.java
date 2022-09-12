package io.github.magnycopper.datatools.buss.ascp;

import io.github.magnycopper.datatools.buss.ascp.common.AppleProductEnums;
import io.github.magnycopper.datatools.buss.ascp.common.AppleStoreEnums;
import io.github.magnycopper.datatools.buss.ascp.service.AppleStoreCheckerService;
import io.github.magnycopper.datatools.framework.plugin.BasicDataToolsPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plugin", name = "active-plugin-name", havingValue = "apple-store-check-plugin")
public class AppleStoreCheckPlugin extends BasicDataToolsPlugin {

    private final AppleStoreCheckerService appleStoreCheckerService;

    public AppleStoreCheckPlugin(AppleStoreCheckerService appleStoreCheckerService) {
        this.appleStoreCheckerService = appleStoreCheckerService;
    }

    @Override
    public void run() throws Exception {
        // Apple Store库存检查
        appleStoreCheckerService.checkAppleStore(AppleStoreEnums.R534, AppleProductEnums.MQ1C3CH_A);
        appleStoreCheckerService.checkAppleStore(AppleStoreEnums.R534, AppleProductEnums.MQ873CH_A);
    }
}
