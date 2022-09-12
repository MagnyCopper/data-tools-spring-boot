package io.github.magnycopper.datatools.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.magnycopper.datatools.framework.plugin.BasicDataToolsPlugin;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plugin", name = "active-plugin-name", havingValue = "test-data-plugin")
public class TestDataPlugin extends BasicDataToolsPlugin {

    /**
     * 日期格式化器
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 默认OkHttpClient
     */
    private static final OkHttpClient DEFAULT_OK_HTTP_CLIENT = new OkHttpClient();

    /**
     * 全局默认ObjectMapper
     */
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = JsonMapper.builder()
            .build();

    @Override
    public void run() throws Exception {
        log.info("test!!!");
    }
}
