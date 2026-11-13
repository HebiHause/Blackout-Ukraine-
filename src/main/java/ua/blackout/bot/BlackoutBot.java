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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlackoutBot implements LongPollingSingleThreadUpdateConsumer {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(KYIV);

    private final TelegramClient client;
    private final YasnoSource source = new YasnoSource();
    private final OutageCalculator calculator = new OutageCalculator();

    private final Map<Long, String> queues = new HashMap<>();

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
        String[] parts = text.split("\\s+");
        String command = parts[0];

        String reply;
        if (command.equals("/start")) {
            reply = "Вітаю в Blackouts Ukraine!\nВкажіть, будь ласка, спочатку свою чергу: /queue 1.1";
        } else if (command.equals("/queue")) {
            reply = setQueue(chatId, parts);
        } else if (command.equals("/next")) {
            reply = nextOutageText(chatId);
        } else {
            reply = "Невідома команда. Спробуйте /start, /queue або /next.";
        }
        send(chatId, reply);

    }


    private String setQueue(long chatId, String[] parts) {
        if (parts.length < 2) {
            return "Вкажіть чергу після команди, наприклад: /queue 1.1";
        }
        String queue = parts[1];
        if (!isValidQueue(queue)) {
            return "Невірна черга. Допустимі значення: від 1.1 до 60.1.";
        }
        queues.put(chatId, queue);
        return "Чергу збережено: " + queue + ". Тепер надішліть /next.";
    }

    private boolean isValidQueue(String queue) {
        if (!queue.matches("\\d{1,2}\\.1")) {
            return false;
        }
        int number = Integer.parseInt(queue.substring(0, queue.indexOf('.')));
        return number >= 1 && number <= 60;
    }

    private String nextOutageText(long chatId) {
        String queue = queues.get(chatId);
        if (queue == null) {
            return "Спершу вкажіть чергу: /queue 1.1";
        }
        List<DaySchedule> days;
        try {
            days = source.getSchedules(queue);
        } catch (IllegalStateException e) {
            return "Дані YASNO поки недоступні. Спробуйте пізніше.";
        }
        ZonedDateTime now = ZonedDateTime.now(KYIV);
        int nowMinute = now.toLocalTime().toSecondOfDay() / 60;
        int best = -1;
        for (DaySchedule day : days) {
            if (!day.date().equals(now.toLocalDate())) {
                continue;
            }
            int left = calculator.minutesUntilNextOutage(day.slots(), nowMinute);
            if (left >= 0 && (best < 0 || left < best)) {
                best = left;
            }
        }
        String asOf = "Черга " + queue + ". Джерело: YASNO, станом на " + TIME_FORMAT.format(source.lastUpdated()) + " (Київ).";
        if (best < 0) {
            return "Більше початків відключень на сьогодні за графіком немає.\n" + asOf;
        }
        return "До наступного відключення: " + formatMinutes(best) + " .\n" + asOf;
    }

    private String formatMinutes(int total){
        int hours = total / 60;
        int minutes = total % 60;
        if (hours == 0){
            return minutes + " хв";
        }
        if (minutes == 0){
            return hours + " год";
        }
        return hours + " год " + minutes + " хв";
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