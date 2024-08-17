package nova;

import java.lang.annotation.Inherited;
import java.util.concurrent.*;
import java.util.Map;

public class ConcurrentClearingMap<K, V> implements Cache<K,V> {

    private final Map<K, V> map = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ConcurrentClearingMap() {
        // Schedule the map to be cleared every second
        scheduler.scheduleAtFixedRate(this::clear, NovaConstant.CACHE_INITAL_DELAY, NovaConstant.CACHE_CLEAR_TIME_IN_SECOND, TimeUnit.SECONDS);
    }
    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }
    @Override
    public V get(K key) {
        return map.get(key);
    }
    @Override
    public boolean contain(K key) {
        return map.containsKey(key);
    }
    @Override
    public void clear() {
        map.clear();
//        System.out.println("Map cleared at: " + System.currentTimeMillis());
    }
    @Override
    public int size() {
        return map.size();
    }
    public void shutdown() {
        scheduler.shutdown();
    }

    public static void main(String[] args) {
        //Baseline Experiment
        ConcurrentClearingMap<String, Integer> clearingMap = new ConcurrentClearingMap<>();

        // Create a thread pool for concurrent access
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        // Simulate concurrent reads and writes
        for (int i = 0; i < 100; i++) {
            final int value = i;
            executorService.submit(() -> {
                clearingMap.put("key" + value, value);
                System.out.println("Put: key" + value + " = " + value);

                // Simulate some work
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(100, 500));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                Integer retrieved = clearingMap.get("key" + value);
                System.out.println("Get: key" + value + " = " + retrieved);
            });
        }

        // Shutdown the executor service
        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Shutdown the clearing scheduler
        clearingMap.shutdown();
    }
}