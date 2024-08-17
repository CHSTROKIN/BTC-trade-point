package org.example;// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws Exception {
        String[] endpoint = {"wss://ws-feed.pro.coinbase.com"};
        String[] productId = {"BTC-USD", "ETH-EUR"};
        String tag = "trade@coinbase";

        BigAtomicCounter counter = new BigAtomicCounter();
        // The number of the thread should be divisible by 2, since for every producer you should have a consumer
        ExecutorService executors = Executors.newFixedThreadPool(NovaConstant.THREAD_NUMBER);
        assert (NovaConstant.THREAD_NUMBER % 2 == 0);
        for(int i = 0; i < Math.floorDiv(NovaConstant.THREAD_NUMBER, 2); i++) {
            String threadEndPoint = endpoint[i % endpoint.length];
            String threadProduct = productId[i % productId.length];
            Thread taskProducer = new RawProducer(threadEndPoint, threadProduct, tag, counter);
            Thread taskConsumer = new Normalizer();
            executors.execute(taskProducer);
            executors.execute(taskConsumer);
        }
        executors.shutdown();

    }
}