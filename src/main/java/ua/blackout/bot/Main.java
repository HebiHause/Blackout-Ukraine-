package ua.blackout.bot;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import ua.blackout.service.UserStore;

public class Main {

    public static void main(String[] args) throws Exception {
        String token = System.getenv("BOT_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("BOT_TOKEN is not set");
        }
        try (TelegramBotsLongPollingApplication app = new TelegramBotsLongPollingApplication()) {
            UserStore store = new UserStore("jdbc:sqlite:blackout.db");
            app.registerBot(token, new BlackoutBot(token, store));
            Thread.currentThread().join();
        }
    }
}