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
    private static final int threadNumber = 100;
    public static void main(String[] args) throws Exception {
        String endpoint = "wss://ws-feed.pro.coinbase.com";
        String productId = "BTC-USD";
        String tag = "trade@coinbase";
        String outputFile = "";
        BigAtomicCounter counter = new BigAtomicCounter();
        ExecutorService executors = Executors.newFixedThreadPool(threadNumber);
        System.out.println(Math.floorDiv(threadNumber, 2));
        for(int i = 0; i < Math.floorDiv(threadNumber, 2); i++) {
            Thread taskProducer = new RawProducer(endpoint, productId, tag, counter);
            Thread taskConsumer = new Normalizer(outputFile);
            executors.execute(taskProducer);
            executors.execute(taskConsumer);
        }
//        executors.shutdown();

    }
}