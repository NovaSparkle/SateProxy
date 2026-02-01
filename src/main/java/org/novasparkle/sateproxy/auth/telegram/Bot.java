package org.novasparkle.sateproxy.auth.telegram;

import lombok.Getter;
import lombok.SneakyThrows;
import org.novasparkle.sateproxy.SateProxy;
import org.novasparkle.sateproxy.configuration.TelegramManager;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Collection;

public class Bot implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient client;
    @Getter
    private final String token;

    public Bot() {
        this.token = TelegramManager.getString("bot.token");
        this.client = new OkHttpTelegramClient(token);

        SateProxy.instance.getLogger().info(TelegramManager.getString("initialized"));
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                String text = message.getText();
                this.sendMessage(message.getChatId(), text);
            }
        }
    }


    @SneakyThrows
    public void sendMessage(long chatId, String text, ReplyKeyboard markup) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(markup)
                .build();
        this.client.execute(sendMessage);
    }
    @SneakyThrows
    public Message sendMessage(long chatId, String text) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .build();
        return this.client.execute(sendMessage);
    }

    @SneakyThrows
    public void deleteMessage(long chatId, int msgId) {
        DeleteMessage deleteMessage = DeleteMessage.builder()
                .chatId(chatId)
                .messageId(msgId)
                .build();
        this.client.execute(deleteMessage);
    }

    @SneakyThrows
    public void editMessage(long chatId, int msgId, Collection<? extends InlineKeyboardRow> keyboard, String newMessage) {
        EditMessageText editMessageText = EditMessageText.builder().parseMode("Markdown").chatId(chatId).messageId(msgId).text(newMessage).build();
        this.client.execute(editMessageText);
        if (keyboard != null) {
            EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder()
                    .chatId(chatId)
                    .messageId(msgId)
                    .replyMarkup(
                            InlineKeyboardMarkup.builder()
                                    .keyboard(keyboard)
                                    .build()
                    ).build();
            this.client.execute(editMessageReplyMarkup);
        }
    }
}
