package nova;

import com.rabbitmq.client.*;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.sql.DriverManager;
import java.util.concurrent.TimeoutException;

public class Normalizer extends Thread implements Consumer {
  private static final String QUEUE_NAME = NovaConstant.QUEUE_NAME;
  private static final ConnectionFactory factory = new ConnectionFactory();
  private static final String url = NovaConstant.dbUrl;
  private static final String EXCHANGE_NAME = NovaConstant.EXCHANGE_NAME;
  private final Cache<String, String> cache;

  static {
    factory.setHost(NovaConstant.HOST);
    factory.setPort(NovaConstant.PORT);
    factory.setRequestedHeartbeat(NovaConstant.HEADER_BEAT);
    factory.setAutomaticRecoveryEnabled(true);
    factory.setNetworkRecoveryInterval(NovaConstant.RECOVER);
  }

  public Normalizer(Cache cache) {
    this.cache = cache;
  }

  private static Connection createConnection() throws IOException, TimeoutException {
    factory.setHost("localhost");
    return factory.newConnection();
  }

  @Override
  public void dataPersistance(String data) {
    String[] result = this.normalizeData(data);
    String productId = result[0];
    double price = Double.parseDouble(result[1]);
    String time = result[2];
    double size = Double.parseDouble(result[3]);
    String source = result[4];
    String totalStringKey = result[0] + result[1] + result[2] + result[3] + result[4];
    if (this.cache.contain(totalStringKey)) {
      //            System.out.println("contained in the cache " + totalStringKey);
      return;
    }
    this.cache.put(totalStringKey, "True");
    String insertSQL =
        "INSERT INTO crypto(product_id, price, time, size, source) VALUES(?,?,?,?,?);";

    try (java.sql.Connection connection = DriverManager.getConnection(url);
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

  @Override
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

      DeliverCallback deliverCallback =
          (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            try {
              System.out.println(" [x] Received '" + message + "', start to persistence");
              this.dataPersistance(message);
              channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
              System.err.println("Error processing message: " + e.getMessage());
              channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
            }
          };

      channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});

      // Keep the consumer running
      while (!Thread.currentThread().isInterrupted()) {
        Thread.sleep(NovaConstant.CONSUMER_WAIT_TIME);
      }
    } catch (IOException | TimeoutException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void run() {
    receiveFromProducer();
  }

  @Override
  public String[] normalizeData(String data) {
    String[] parts = data.split(",");
    String source = parts[0].split("@")[1];
    String time = parts[1];
    String product_id = parts[2];
    String price = parts[3];
    String size = parts[4];
    System.out.println();
    return new String[] {product_id, price, time, size, source};
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
    ConcurrentClearingMap<String, String> cache = new ConcurrentClearingMap<>();
    Thread consumerThread = new Normalizer(cache);
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
