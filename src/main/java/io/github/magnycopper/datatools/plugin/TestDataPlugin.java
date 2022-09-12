package io.github.magnycopper.datatools.plugin;

import io.github.magnycopper.datatools.framework.plugin.BasicDataToolsPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plugin", name = "active-plugin-name", havingValue = "test-data-plugin")
public class TestDataPlugin extends BasicDataToolsPlugin {

    @Override
    public void run() throws Exception {

    }
}
