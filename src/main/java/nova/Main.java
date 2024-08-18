package nova;// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws Exception {
        String[] endpoint = {"wss://ws-feed.pro.coinbase.com"};
        String[] productId = {"BTC-USD", "ETH-EUR"};
        String tag = "trade@coinbase";

        BigAtomicCounter counter = new BigAtomicCounter();
        // The number of the thread should be divisible by 2, since for every producer you should have a consumer
        ExecutorService executors = Executors.newFixedThreadPool(NovaConstant.NUMBER_OF_PRODUCER);
        ConcurrentClearingMap<String, String> cache = new ConcurrentClearingMap<>();
        for(int i = 0; i < NovaConstant.NUMBER_OF_PRODUCER; i++) {
            String threadEndPoint = endpoint[i % endpoint.length];
            String threadProduct = productId[i % productId.length];
            Thread taskProducer = new RawProducer(threadEndPoint, threadProduct, tag, counter);
            executors.execute(taskProducer);
        }
        for(int i = 0; i < NovaConstant.NUMBER_OF_CONSUMER; i++) {
            Thread taskConsumer = new Normalizer(cache);
            executors.execute(taskConsumer);
        }
        executors.shutdown();

    }
}