package ts.andrey.eventcollector.service;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ts.andrey.eventcollector.service.component.SimpleCache;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleCacheTest {

    SimpleCache simpleCache;

    @BeforeEach
    void setUp() {
        simpleCache = new SimpleCache(1, 10);
    }

    @Test
    void shouldAddKeyToCache() {
        // WHEN
        simpleCache.put("testKey");

        final var actual = simpleCache.getCache();

        // THEN
        assertEquals(1, simpleCache.size());
        assertTrue(actual.containsKey("testKey"));
    }

    @Test
    void shouldNotAddNullKey() {
        // WHEN
        simpleCache.put(null);

        // THEN
        System.out.println(simpleCache.getCache());
        assertEquals(0, simpleCache.size());
    }

    @Test
    void shouldReturnTrueForExistingKey() {
        // GIVEN
        simpleCache.put("testKey");

        // WHEN & THEN
        assertTrue(simpleCache.contains("testKey"));
    }

    @SneakyThrows
    @Test
    void shouldUpdateExpiredTimeForExistingKey() {
        // GIVEN
        simpleCache.put("testKey");
        final var map = simpleCache.getCache();

        // WHEN & THEN
        assertTrue(map.containsKey("testKey"));
        Thread.sleep(1000);
        assertTrue(simpleCache.contains("testKey"));
        assertNotEquals(map.get("testKey"), simpleCache.getCache().get("testKey"));
    }

    @Test
    void shouldReturnFalseForNonExistingKey() {
        assertFalse(simpleCache.contains("nonExistingKey"));
    }

    @Test
    void shouldReturnFalseForEmptyKey() {
        assertFalse(simpleCache.contains(""));
    }

    @Test
    void shouldDeleteKeyFromCache() {
        // GIVEN
        simpleCache.put("testKey");

        // WHEN
        simpleCache.remove("testKey");

        // THEN
        assertEquals(0, simpleCache.size());
    }

    @Test
    void shouldNotThrowForRemoveNullKey() {
        assertDoesNotThrow(() -> simpleCache.remove(null));
    }

    @Test
    void shouldRemoveExpiredEntries() throws InterruptedException {
        // GIVEN
        simpleCache.put("expiredKey");
        simpleCache.put("one");
        simpleCache.put("two");
        simpleCache.put("three");
        simpleCache.put("four");
        simpleCache.put("five");
        simpleCache.put("six");
        simpleCache.put("seven");
        simpleCache.put("eight");
        simpleCache.put("nine");
        simpleCache.put("ten");

        Thread.sleep(1010);

        simpleCache.put("validKey");

        // WHEN
        simpleCache.contains("validKey");

        // THEN
        assertEquals(1, simpleCache.size());
        assertTrue(simpleCache.contains("validKey"));
        assertFalse(simpleCache.contains("expiredKey"));
    }

    @Test
    void shouldRemoveNullKeys() {
        // GIVEN
        simpleCache.put(null);
        simpleCache.put("validKey");

        // WHEN
        simpleCache.cleanUp();

        // THEN
        assertEquals(1, simpleCache.size());
        assertTrue(simpleCache.contains("validKey"));
    }

    @Test
    void shouldReturnCorrectCacheSize() {
        // GIVEN
        simpleCache.put("key1");
        simpleCache.put("key2");

        // WHEN & THEN
        assertEquals(2, simpleCache.size());
    }

}
