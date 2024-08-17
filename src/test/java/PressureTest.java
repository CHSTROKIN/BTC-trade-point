package nova;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class PressureTest {

    private static final int TEST_DURATION_SECONDS = 30; // 5 minutes
    private static final int NUM_PRODUCERS = 100;
    private static final int NUM_CONSUMERS = 100;

    private ExecutorService executorService;
    private AtomicLong totalMessagesProduced;
    private AtomicLong totalMessagesConsumed;
    private AtomicLong cacheSize;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(NUM_PRODUCERS + NUM_CONSUMERS);
        totalMessagesProduced = new AtomicLong(0);
        totalMessagesConsumed = new AtomicLong(0);
        cacheSize = new AtomicLong(0);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executorService.shutdownNow();
        executorService.awaitTermination(30, TimeUnit.SECONDS);
    }

    @Test
    void testSystemPerformanceUnderLoad() throws InterruptedException {
        String endpoint = "wss://ws-feed.pro.coinbase.com";
        String[] productIds = {"BTC-USD", "ETH-USD", "LTC-USD"};
        String tag = "trade@coinbase";

        BigAtomicCounter counter = new BigAtomicCounter();
        ConcurrentClearingMap<String, String> cache = new ConcurrentClearingMap<>();
        List consumerList = new LinkedList<Consumer>();
        List producerList = new LinkedList<Producer>();

        // Start producers
        for (int i = 0; i < NUM_PRODUCERS; i++) {
            final int index = i;
            executorService.submit(() -> {
                RawProducer producer = new RawProducer(endpoint, productIds[index % productIds.length], tag, counter) {
                    @Override
                    public void publishToConsumers(String data) {
                        super.publishToConsumers(data);
                        totalMessagesProduced.incrementAndGet();
                    }
                };
                producer.start();
            });
        }

        // Start consumers
        for (int i = 0; i < NUM_CONSUMERS; i++) {
            executorService.submit(() -> {
                Normalizer consumer = new Normalizer(cache) {
                    @Override
                    public void dataPersistance(String data) {
                        super.dataPersistance(data);
                        totalMessagesConsumed.incrementAndGet();
                    }
                };
                consumer.start();
            });
        }

        // Monitor and assert
        long startTime = System.currentTimeMillis();
        long lastReportTime = startTime;
        long lastProducedMessages = 0;
        long lastConsumedMessages = 0;

        while (System.currentTimeMillis() - startTime < TEST_DURATION_SECONDS * 1000) {
            Thread.sleep(5000); // Check every 5 seconds

            long currentTime = System.currentTimeMillis();
            long elapsedSeconds = (currentTime - lastReportTime) / 1000;
            long currentProducedMessages = totalMessagesProduced.get();
            long currentConsumedMessages = totalMessagesConsumed.get();

            long producedMessagesInInterval = currentProducedMessages - lastProducedMessages;
            long consumedMessagesInInterval = currentConsumedMessages - lastConsumedMessages;

            double productionRate = (double) producedMessagesInInterval / elapsedSeconds;
            double consumptionRate = (double) consumedMessagesInInterval / elapsedSeconds;

            System.out.println("--- Performance Report ---");
            System.out.println("Elapsed time: " + (currentTime - startTime) / 1000 + " seconds");
            System.out.println("Total messages produced: " + currentProducedMessages);
            System.out.println("Total messages consumed: " + currentConsumedMessages);
            System.out.println("Production rate: " + productionRate + " msgs/sec");
            System.out.println("Consumption rate: " + consumptionRate + " msgs/sec");
            System.out.println("Cache size: " + cache.size());
            System.out.println("---------------------------");

            assertTrue(productionRate > 0, "Production rate should be positive");
            assertTrue(consumptionRate > 0, "Consumption rate should be positive");
            assertTrue(cache.size() < 1000000, "Cache size should not grow unbounded");

            lastReportTime = currentTime;
            lastProducedMessages = currentProducedMessages;
            lastConsumedMessages = currentConsumedMessages;
        }

        assertTrue(totalMessagesProduced.get() > 0, "Should have produced messages");
        assertTrue(totalMessagesConsumed.get() > 0, "Should have consumed messages");

    }
}