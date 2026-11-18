package ua.blackout.bot;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import ua.blackout.service.UserStore;
import ua.blackout.source.YasnoSource;

import java.time.Clock;

public class Main {

    public static void main(String[] args) throws Exception {
        String token = System.getenv("BOT_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("BOT_TOKEN is not set");
        }
        String dbPath = envOrDefault("DB_PATH", "blackout.db");
        int healthPort = Integer.parseInt(envOrDefault("HEALTH_PORT", "8080"));

        YasnoSource source = new YasnoSource();
        UserStore store = new UserStore("jdbc:sqlite:" + dbPath);
        // /health asks the source to refresh, so the schedule stays fresh
        // even when no user sends /next for a long time.
        try (HealthServer health = new HealthServer(healthPort, source::refreshIfDue, Clock.systemUTC());
             TelegramBotsLongPollingApplication app = new TelegramBotsLongPollingApplication()) {
            health.start();
            app.registerBot(token, new BlackoutBot(token, store, source));
            Thread.currentThread().join();
        }
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
