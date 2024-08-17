package org.example;

import com.rabbitmq.client.*;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

public class RawProducer extends Thread{
    private final String endpoint;
    private final String productId;
    private final String tag;
    private final static String QUEUE_NAME = NovaConstant.QUEUE_NAME;
    private static final String EXCHANGE_NAME = NovaConstant.EXCHANGE_NAME;

    private final BigAtomicCounter counter;

    private static final ConnectionFactory factory = new ConnectionFactory();

    static {
        factory.setHost("localhost");
        factory.setPort(5672);
        // Heartbeat every 30 seconds
        factory.setRequestedHeartbeat(30);
        // Enable automatic recovery
        factory.setAutomaticRecoveryEnabled(true);
        // Attempt recovery every 10 seconds
        factory.setNetworkRecoveryInterval(10000);
    }
    public RawProducer(String endpoint, String productId, String tag, BigAtomicCounter counter) {
        this.endpoint = endpoint;
        this.productId = productId;
        this.tag = tag;
        this.counter = counter;
    }

    private static Connection createConnection() throws IOException, TimeoutException {
        factory.setHost("localhost");
        return factory.newConnection();
    }
    public void run() {
        HttpClient client = HttpClient.newHttpClient();
        WebSocket.Builder wsBuilder = client.newWebSocketBuilder();
        WebSocket webSocket = wsBuilder.buildAsync(URI.create(endpoint), new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                handleMessage(data.toString());
                return WebSocket.Listener.super.onText(webSocket, data, last);
            }
        }).join();

        // Subscribe to the product feed
        String subscribeMessage = String.format(
                "{\"type\":\"subscribe\",\"product_ids\":[\"%s\"],\"channels\":[\"matches\"]}",
                productId
        );
        webSocket.sendText(subscribeMessage, true);

        // Keep the connection alive
        while (true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private void handleMessage(String message) {
        JSONObject json = new JSONObject(message);
        if (json.getString("type").equals("match")) {
            String data = String.format("%s,%s,%s,%s",
                    json.getString("time"),
                    json.getString("product_id"),
                    json.getString("price"),
                    json.getString("size")
            );
            publishToConsumers(tag + ": " + data);
        }
    }

    private void publishToConsumers(String data) {
        try (Connection connection = createConnection();
             Channel channel = connection.createChannel()) {

            // Declare a durable exchange
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT, true);

            // Declare a durable queue
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);

            // Bind the queue to the exchange
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "");

            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .deliveryMode(2) // Make message persistent
                    .build();
            channel.basicPublish(EXCHANGE_NAME, "", properties, data.getBytes(StandardCharsets.UTF_8));
            System.out.println("Produced: " + data);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //Testing script for development
        String endpoint = "wss://ws-feed.pro.coinbase.com";
        String productId = "BTC-USD";
        String tag = "trade@coinbase";
        BigAtomicCounter counter = new BigAtomicCounter();
        Thread thread1 = new RawProducer(endpoint, productId, tag, counter);
        thread1.start();
        try {
            thread1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}