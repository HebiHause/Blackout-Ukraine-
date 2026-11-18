package ua.blackout.bot;

import ua.blackout.model.DaySchedule;
import ua.blackout.model.Slot;
import ua.blackout.service.OutageCalculator;
import ua.blackout.service.UserStore;
import ua.blackout.source.YasnoSource;

import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BlackoutBot implements LongPollingSingleThreadUpdateConsumer {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(KYIV);
    private static final String DISCLAIMER =
            "Це плановий графік: аварійні відключення в нього не входять і можуть статися будь-коли.";

    private final TelegramClient client;
    private final YasnoSource source;
    private final OutageCalculator calculator = new OutageCalculator();

    private final UserStore store;

    public BlackoutBot(String token, UserStore store, YasnoSource source) {
        this.client = new OkHttpTelegramClient(token);
        this.store = store;
        this.source = source;
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
        try {
            store.saveQueue(chatId, queue);
        } catch (SQLException e) {
            System.err.println("save failed: " + e.getMessage());
            return "Не вдалося зберегти чергу. Спробуйте ще раз.";
        }
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
        String queue;
        try {
            queue = store.findQueue(chatId).orElse(null);
        } catch (SQLException e) {
            System.err.println("read failed: " + e.getMessage());
            return "Не вдалося прочитати чергу. Спробуйте ще раз.";
        }
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
        List<Slot> today = slotsFor(days, now.toLocalDate());
        List<Slot> tomorrow = slotsFor(days, now.toLocalDate().plusDays(1));
        OutageCalculator.Countdown countdown = calculator.countdown(today, tomorrow, nowMinute);

        String head;
        if (countdown.outageNow()) {
            head = "Зараз відключення. Світло має повернутися через " + formatMinutes(countdown.minutes()) + ".";
        } else if (countdown.minutes() >= 0) {
            head = "До наступного відключення: " + formatMinutes(countdown.minutes()) + ".";
        } else {
            head = "На сьогодні й завтра відключень за графіком немає.";
        }
        String asOf = "Черга " + queue + ". Джерело: YASNO, станом на " + TIME_FORMAT.format(source.lastUpdated()) + " (Київ).";
        return head + "\n" + asOf + "\n" + DISCLAIMER;
    }

    private List<Slot> slotsFor(List<DaySchedule> days, LocalDate date) {
        for (DaySchedule day : days) {
            if (day.date().equals(date)) {
                return day.slots();
            }
        }
        return List.of();
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