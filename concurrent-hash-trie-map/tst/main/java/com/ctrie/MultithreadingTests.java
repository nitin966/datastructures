package com.ctrie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MultithreadingTests {

    private ConcurrentTrie<String, Integer> trie;

    @BeforeEach
    void setUp() {
        trie = new ConcurrentTrie<>();
    }

    @Test
    void testConcurrentInsertions() throws InterruptedException, ExecutionException {
        int numThreads = 10;
        int numInsertionsPerThread = 1000;

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            futures.add(executorService.submit(() -> {
                for (int j = 0; j < numInsertionsPerThread; j++) {
                    trie.put("key-" + threadId + "-" + j, threadId * 1000 + j);
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();

        for (int i = 0; i < numThreads; i++) {
            for (int j = 0; j < numInsertionsPerThread; j++) {
                assertEquals(Integer.valueOf(i * 1000 + j), trie.get("key-" + i + "-" + j));
            }
        }
    }

    @Test
    void testConcurrentInsertionsAndLookups() throws InterruptedException, ExecutionException {
        int numThreads = 10;
        int numOperationsPerThread = 1000;

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            futures.add(executorService.submit(() -> {
                for (int j = 0; j < numOperationsPerThread; j++) {
                    trie.put("key-" + threadId + "-" + j, threadId * 1000 + j);
                }
            }));
        }

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            futures.add(executorService.submit(() -> {
                for (int j = 0; j < numOperationsPerThread; j++) {
                    trie.get("key-" + threadId + "-" + j);
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();
    }

    @Test
    void testConcurrentInsertionsAndRemovals() throws InterruptedException, ExecutionException {
        int numThreads = 10;
        int numOperationsPerThread = 1000;

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            futures.add(executorService.submit(() -> {
                for (int j = 0; j < numOperationsPerThread; j++) {
                    trie.put("key-" + threadId + "-" + j, threadId * 1000 + j);
                }
            }));
        }

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            futures.add(executorService.submit(() -> {
                for (int j = 0; j < numOperationsPerThread; j++) {
                    trie.remove("key-" + threadId + "-" + j);
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();

        for (int i = 0; i < numThreads; i++) {
            for (int j = 0; j < numOperationsPerThread; j++) {
                try {
                    trie.get("key-" + i + "-" + j);
                } catch (NoSuchElementException ignored) {}
            }
        }
    }

    @Test
    void testConcurrentUpdates() throws InterruptedException, ExecutionException {
        int numThreads = 10;
        int numUpdatesPerThread = 1000;

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            futures.add(executorService.submit(() -> {
                for (int j = 0; j < numUpdatesPerThread; j++) {
                    trie.put("key-" + threadId + "-" + j, threadId);
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        futures.clear();

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            futures.add(executorService.submit(() -> {
                for (int j = 0; j < numUpdatesPerThread; j++) {
                    trie.put("key-" + threadId + "-" + j, threadId * 1000 + j);
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();

        for (int i = 0; i < numThreads; i++) {
            for (int j = 0; j < numUpdatesPerThread; j++) {
                assertEquals(Integer.valueOf(i * 1000 + j), trie.get("key-" + i + "-" + j));
            }
        }
    }

    @Test
    void testConcurrentContainsKey() throws InterruptedException, ExecutionException {
        int numThreads = 10;
        int numOperationsPerThread = 1000;

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            futures.add(executorService.submit(() -> {
                for (int j = 0; j < numOperationsPerThread; j++) {
                    trie.put("key-" + threadId + "-" + j, threadId * 1000 + j);
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        futures.clear();

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            futures.add(executorService.submit(() -> {
                for (int j = 0; j < numOperationsPerThread; j++) {
                    assertTrue(trie.containsKey("key-" + threadId + "-" + j));
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();
    }
}
