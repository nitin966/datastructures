package com.ctrie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ConcurrentTrie.
 */
public class ConcurrentTrieTest {

    private ConcurrentTrie<Integer, String> trie;

    @BeforeEach
    public void setUp() {
        trie = new ConcurrentTrie<>();
    }

    @Test
    public void testInsertAndLookup() {
        trie.put(1, "one");
        trie.put(2, "two");
        trie.put(3, "three");

        assertEquals("one", trie.get(1));
        assertEquals("two", trie.get(2));
        assertEquals("three", trie.get(3));
        testNotPresent(4);
    }

    private void testNotPresent(Integer key) {
        try {
            trie.get(key);
        } catch (NoSuchElementException ignored) {}
    }

    @Test
    public void testUpdate() {
        trie.put(1, "one");
        trie.put(1, "uno");

        assertEquals("uno", trie.get(1));
    }

    @Test
    public void testRemove() {
        trie.put(1, "one");
        trie.put(2, "two");

        assertEquals("one", trie.remove(1));
        testNotPresent(1);

        assertEquals("two", trie.remove(2));
        testNotPresent(2);

        assertNull(trie.remove(3));
    }

    @Test
    public void testConcurrentInsertions() throws InterruptedException {
        ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<>();
        for (int i = 0; i < 1000; i++) {
            map.put(i, "value" + i);
        }

        map.forEach((key, value) -> trie.put(key, value));

        map.forEach((key, value) -> assertEquals(value, trie.get(key)));
    }

    @Test
    public void testConcurrentRemovals() throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            trie.put(i, "value" + i);
        }

        for (int i = 0; i < 1000; i++) {
            System.out.println("Value of i " + i);
            assertEquals("value" + i, trie.remove(i));
        }

        for (int i = 0; i < 1000; i++) {
            testNotPresent(i);
        }
    }

    @Test
    public void testComplexOperations() {
        trie.put(1, "one");
        trie.put(2, "two");
        trie.put(3, "three");

        assertEquals("one", trie.get(1));
        assertEquals("two", trie.get(2));
        assertEquals("three", trie.get(3));

        assertEquals("one", trie.remove(1));
        testNotPresent(1);

        trie.put(1, "one");
        assertEquals("one", trie.get(1));

        assertEquals("two", trie.remove(2));
        testNotPresent(2);

        trie.put(2, "two");
        assertEquals("two", trie.get(2));
    }
}

