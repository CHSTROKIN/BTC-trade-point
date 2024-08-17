package org.example;




import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;


import java.sql.DriverManager;
import java.sql.Statement;


public class Normalizer extends Thread{
    private final BlockingQueue<String> dataQueue;
    private final String outputFile;
    private final static String QUEUE_NAME = "CoinBaseQueue";
    private static final ConnectionFactory factory = new ConnectionFactory();
    private static final String url = "jdbc:sqlite:DataBase/crypto2.db";

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

    private void perSistance(String data) {
        String[] result = this.normalizeData(data);
        String productId = result[0];
        double price = 0.0;
        double size = 0.0;
        String time = "";
        String source = "";
        String insertSQL = "INSERT INTO crypto(product_id, price, time, size, source) VALUES(?,?,?,?,?);";
        try(java.sql.Connection connection = DriverManager.getConnection(url);
            PreparedStatement statement = connection.prepareStatement(insertSQL)) {
            // Set the values for the placeholders
            statement.setString(1, productId);
            statement.setDouble(2, price);
            statement.setString(3, time);
            statement.setDouble(4, size);
            // Execute the insert statement
            int rowsAffected = statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void receiveFromProducer() {
//        System.out.println("start to listen");
        try (Connection connection = createConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
//            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println(" [x] Received '" + message + ", start to persistance");
                this.perSistance(message);
            };
            channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });

            // Keep the consumer running
            while (true) {
                Thread.sleep(10);
            }
        } catch (IOException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void run() {
        receiveFromProducer();
    }

    private String[] normalizeData(String data) {
        // Assuming the data format is: tag: timestamp,product_id,price,size
        System.out.println("********************************");
        System.out.println("Data receive is:"+data);
        String[] parts = data.split(": ", 2);
        String[] fields = parts[1].split(",");

        // Normalize the data (e.g., convert timestamp, standardize decimal places)
        String normalizedTimestamp = convertTimestamp(fields[0]);
        String normalizedPrice = String.format("%.10f", Double.parseDouble(fields[2]));
        String normalizedSize = String.format("%.10f", Double.parseDouble(fields[3]));
        String[] result = { parts[0], normalizedTimestamp, fields[1], normalizedPrice, normalizedSize};
        System.out.println("ProductId is " + fields[1]);
        System.out.println("Source is " + parts[0]);
        System.out.println("Time is " + normalizedTimestamp);
        System.out.println("Price is " + normalizedPrice);
        System.out.println("Size is " + normalizedSize);
        return result;
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
        consumerThread.start();
        producerThread.start();
        try {
            consumerThread.join();
            producerThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}