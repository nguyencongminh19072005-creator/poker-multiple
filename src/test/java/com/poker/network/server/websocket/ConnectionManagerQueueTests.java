package com.poker.network.server.websocket;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionManagerQueueTests {
    @Test
    void commandsFromOneClientStayOrderedOnWorkerPool() throws Exception {
        ConnectionManager.Session session = new ConnectionManager.Session();
        List<Integer> order = new CopyOnWriteArrayList<>();
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            assertThat(session.enqueue(workers, () -> {
                firstStarted.countDown();
                try {
                    if (!releaseFirst.await(2, TimeUnit.SECONDS)) throw new AssertionError("first timed out");
                    order.add(1);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            })).isTrue();
            assertThat(firstStarted.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(session.enqueue(workers, () -> {
                order.add(2);
                done.countDown();
            })).isTrue();
            assertThat(order).isEmpty();
            releaseFirst.countDown();
            assertThat(done.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(order).containsExactly(1, 2);
        } finally {
            releaseFirst.countDown();
            workers.shutdownNow();
        }
    }
}
