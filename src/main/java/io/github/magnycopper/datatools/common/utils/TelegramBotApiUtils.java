package io.github.magnycopper.datatools.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class TelegramBotApiUtils {

    /**
     * TelegramBotApiToken
     */
    @Value(value = "${basic-service.telegram-bot-api.token:${TELEGRAM_BOT_API_TOKEN}}")
    private String token;

    /**
     * TelegramBotApiUrl
     */
    @Value(value = "${basic-service.telegram-bot-api.url:https://api.telegram.org/bot%s}")
    private String apiUrl;

    /**
     * 获取TelegramBot接口地址
     *
     * @return TelegramBot接口地址
     */
    private String getBotApiUrl() {
        return String.format(apiUrl + "/sendMessage", token);
    }

    /**
     * 发送机器人消息
     *
     * @param chatId  聊天ID
     * @param message 消息内容
     * @throws IOException 消息发送异常
     */
    public void sendMessage(String chatId, String message) throws IOException {
        Map<String, String> parms = new HashMap<>();
        parms.put("chat_id", chatId);
        parms.put("text", message);
        String s = RequestUtils.get(getBotApiUrl(), parms);
        log.info(s);
    }
}
