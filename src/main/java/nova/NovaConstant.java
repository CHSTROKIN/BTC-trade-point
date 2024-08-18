package nova;

public class NovaConstant {
  public static final String QUEUE_NAME = "CoinBaseQueue";
  public static final String EXCHANGE_NAME = "CryptoStreamExchange";
  public static final String dbUrl = "jdbc:sqlite:DataBase/file.db";
  public static final int NUMBER_OF_PRODUCER = 200;
  public static final int PRODUCER_WAIT_TIME = 10;
  public static final int CONSUMER_WAIT_TIME = 10;
  public static final int CACHE_CLEAR_TIME_IN_SECOND = 10;
  public static final int CACHE_INITAL_DELAY = CACHE_CLEAR_TIME_IN_SECOND;
  public static final int UPDATE_GUI_TIME_IN_SECOND = 1;
  public static final int GUI_HEIGHT = 600;
  public static final int GUI_WIDTH = 600;
  public static final int NUMBER_OF_CONSUMER = 1;
  public static final int SHUTDOWN_SECOND = 60;
  public static final String HOST = "localhost";
  public static final int PORT = 5672;
  // Heartbeat every 30 seconds
  public static final int HEADER_BEAT = 30;
  // Attempt recovery every 10 seconds
  public static final int RECOVER = 10000;
}
