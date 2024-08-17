import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

import nova.*;
class ConcurrentClearingMapTest {

    private ConcurrentClearingMap<String, Integer> map;

    @BeforeEach
    void setUp() {
        map = new ConcurrentClearingMap<>();
    }

    @AfterEach
    void tearDown() {
        map.shutdown();
    }

    @Test
    void testPutAndGet() {
        map.put("key1", 1);
        assertEquals(1, map.get("key1"));
    }

    @Test
    void testContains() {
        map.put("key1", 1);
        assertTrue(map.contain("key1"));
        assertFalse(map.contain("key2"));
    }

    @Test
    void testClear() throws InterruptedException {
        map.put("key1", 1);
        assertTrue(map.contain("key1"));

        // Wait for the map to clear
        Thread.sleep(NovaConstant.CACHE_CLEAR_TIME_IN_SECOND * 1000 + 100);

        assertFalse(map.contain("key1"));
    }

    @Test
    @Timeout(10)
    void testConcurrentAccess() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(100);

        for (int i = 0; i < 100; i++) {
            final int value = i;
            executorService.submit(() -> {
                map.put("key" + value, value);
                Integer retrieved = map.get("key" + value);
                if (retrieved != null) {
                    assertEquals(value, retrieved);
                }
                latch.countDown();
            });
        }

        latch.await();
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(20)
    void pressureTest() throws InterruptedException {
        int numThreads = 100;
        int operationsPerThread = 10000;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            executorService.submit(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String key = "key" + ThreadLocalRandom.current().nextInt(1000);
                    int value = ThreadLocalRandom.current().nextInt(1000);

                    switch (ThreadLocalRandom.current().nextInt(3)) {
                        case 0:
                            map.put(key, value);
                            break;
                        case 1:
                            map.get(key);
                            break;
                        case 2:
                            map.contain(key);
                            break;
                    }
                }
                latch.countDown();
            });
        }

        latch.await();
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Pressure test completed in " + duration + " ms");
        System.out.println("Operations per second: " + (numThreads * operationsPerThread * 1000L / duration));
        //Pressure test completed in 322 ms
        //Operations per second: 3105590
        //LOL
    }
}