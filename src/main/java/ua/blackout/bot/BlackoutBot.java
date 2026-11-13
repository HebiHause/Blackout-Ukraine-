package ua.blackout.bot;

import ua.blackout.model.DaySchedule;
import ua.blackout.service.OutageCalculator;
import ua.blackout.source.YasnoSource;

import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class BlackoutBot implements LongPollingSingleThreadUpdateConsumer {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    private final TelegramClient client;
    private final YasnoSource source = new YasnoSource();
    private final OutageCalculator calculator = new OutageCalculator();

    public BlackoutBot(String token) {
        this.client = new OkHttpTelegramClient(token);
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        String reply;
        if (text.equals("/start")) {
            reply = "Привіт! Надішліть /next, щоб дізнатися, коли наступне відключення.";
        } else if (text.equals("/next")) {
            reply = nextOutageText();
        } else {
            reply = "Невідома команда. Спробуйте /start або /next.";
        }
        send(chatId, reply);
    }

    private String nextOutageText() {
        List<DaySchedule> days = source.getSchedules("1.1");
        int now = ZonedDateTime.now(KYIV).toLocalTime().toSecondOfDay() / 60;
        int best = -1;
        for (DaySchedule day : days) {
            // Only today's schedule is used for the countdown.
            if (!day.date().equals(ZonedDateTime.now(KYIV).toLocalDate())) {
                continue;
            }
            int left = calculator.minutesUntilNextOutage(day.slots(), now);
            if (left >= 0 && (best < 0 || left < best)) {
                best = left;
            }
        }
        if (best < 0) {
            return "На сьогодні відключень за графіком не залишилось.";
        }
        return "До наступного відключення: " + best + " хв.\n"
                + "Джерело: YASNO, станом на " + source.lastUpdated();
    }

    private void send(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            client.execute(message);
        } catch (TelegramApiException e) {
            System.err.println("send failed: " + e.getMessage());
        }
    }
}