package org.example;




import com.rabbitmq.client.*;
import com.rabbitmq.client.Connection;

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
    private final static String QUEUE_NAME = NovaConstant.QUEUE_NAME;
    private static final ConnectionFactory factory = new ConnectionFactory();
    private static final String url = NovaConstant.dbUrl;
    private static final String EXCHANGE_NAME = NovaConstant.EXCHANGE_NAME;

    static {
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setRequestedHeartbeat(30);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(10000);
    }


    public Normalizer() {

    }

    private static Connection createConnection() throws IOException, TimeoutException {
        factory.setHost("localhost");
        return factory.newConnection();
    }


    private void perSistance(String data) {
        String[] result = this.normalizeData(data);
        String productId = result[0];
        double price = Double.valueOf(result[1]);
        String time = result[2];
        double size = Double.valueOf(result[3]);
        String source = result[4];
        String insertSQL = "INSERT INTO crypto(product_id, price, time, size, source) VALUES(?,?,?,?,?);";
        try(java.sql.Connection connection = DriverManager.getConnection(url);
            PreparedStatement statement = connection.prepareStatement(insertSQL)) {
            // Set the values for the placeholders
            statement.setString(1, productId);
            statement.setDouble(2, price);
            statement.setString(3, time);
            statement.setDouble(4, size);
            statement.setString(5, source);
            // Execute the insert statement
            int rowsAffected = statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void receiveFromProducer() {
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            // Declare a durable exchange
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT, true);

            // Declare a durable queue
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);

            // Bind the queue to the exchange
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "");

            // Set QoS to 1 to ensure sequential processing
            channel.basicQos(1);

            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                try {
                    System.out.println(" [x] Received '" + message + "', start to persistence");
                    this.perSistance(message);
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (Exception e) {
                    System.err.println("Error processing message: " + e.getMessage());
                    channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                }
            };

            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });

            // Keep the consumer running
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(1000);
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
        String[] parts = data.split(": ", 2);
        String[] fields = parts[1].split(",");
        // Normalize the data (e.g., convert timestamp, standardize decimal places)
        String normalizedTimestamp = convertTimestamp(fields[0]);
        String normalizedPrice = String.format("%.10f", Double.parseDouble(fields[2]));
        String normalizedSize = String.format("%.10f", Double.parseDouble(fields[3]));
        String[] result = { fields[1], normalizedPrice, normalizedTimestamp, normalizedSize, parts[0]};
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
        System.out.println("begin to run");
        Thread consumerThread = new Normalizer();
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