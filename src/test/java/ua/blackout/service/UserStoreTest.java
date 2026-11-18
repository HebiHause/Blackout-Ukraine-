package ua.blackout.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserStoreTest {

    @TempDir
    Path dir;

    private String url() {
        return "jdbc:sqlite:" + dir.resolve("test.db");
    }

    @Test
    void unknownChatHasNoQueue() throws Exception {
        UserStore store = new UserStore(url());

        assertEquals(Optional.empty(), store.findQueue(42));
    }

    @Test
    void queueSurvivesRestart() throws Exception {
        new UserStore(url()).saveQueue(42, "5.1");

        // A new store on the same file is what the bot sees after a restart.
        assertEquals(Optional.of("5.1"), new UserStore(url()).findQueue(42));
    }

    @Test
    void savingAgainReplacesQueue() throws Exception {
        UserStore store = new UserStore(url());
        store.saveQueue(42, "5.1");
        store.saveQueue(42, "17.1");

        assertEquals(Optional.of("17.1"), store.findQueue(42));
    }
}
