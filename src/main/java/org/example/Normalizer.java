package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

public class Normalizer extends Thread{
    private final BlockingQueue<String> dataQueue;
    private final String outputFile;
    private final static String QUEUE_NAME = "CoinBaseQueue";
    private static final ConnectionFactory factory = new ConnectionFactory();

    static {
        factory.setHost("localhost");
        factory.setPort(5672);
        // Add more configuration as needed
    }

    public Normalizer(String outputFile) {
        this.dataQueue = new LinkedBlockingQueue<>();
        this.outputFile = outputFile;
    }

    private static Connection createConnection() throws IOException, TimeoutException {
        factory.setHost("localhost");
        return factory.newConnection();
    }

    public void subscribe(String data) {
        try {
            dataQueue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void perSistance() {
        try (FileWriter writer = new FileWriter(outputFile, true)) {
            while (true) {
                String data = dataQueue.take();
                String normalizedData = normalizeData(data);
                writer.write(normalizedData + "\n");
                writer.flush();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void receiveFromProducer() {
        System.out.println("start to listen");
        try (Connection connection = createConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println(" [x] Received '" + message + "'");
            };
            channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });

            // Keep the consumer running
            while (true) {
                Thread.sleep(1000);
            }
        } catch (IOException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void run() {
        receiveFromProducer();
    }

    private String normalizeData(String data) {
        // Assuming the data format is: tag: timestamp,product_id,price,size
        String[] parts = data.split(": ", 2);
        String[] fields = parts[1].split(",");

        // Normalize the data (e.g., convert timestamp, standardize decimal places)
        String normalizedTimestamp = convertTimestamp(fields[0]);
        String normalizedPrice = String.format("%.2f", Double.parseDouble(fields[2]));
        String normalizedSize = String.format("%.8f", Double.parseDouble(fields[3]));

        return String.format("%s,%s,%s,%s,%s",
                parts[0], normalizedTimestamp, fields[1], normalizedPrice, normalizedSize);
    }

    private String convertTimestamp(String isoTimestamp) {
        // Convert ISO 8601 timestamp to Unix timestamp (milliseconds)
        return String.valueOf(java.time.Instant.parse(isoTimestamp).toEpochMilli());
    }

    public static void main(String[] args) {
        String endpoint = "wss://ws-feed.pro.coinbase.com";
        String productId = "BTC-USD";
        String tag = "trade@coinbase";
        BigAtomicCounter counter = new BigAtomicCounter();

//        RawProducer producer = new RawProducer(endpoint, productId, tag);
        System.out.println("begin to run");
        Thread consumerThread = new Normalizer("");
        Thread producerThread = new RawProducer(endpoint, productId, tag, counter);
        System.out.println("thread created");
//        consumerThread.start();
        producerThread.start();
        try {
//            consumerThread.join();
            producerThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}