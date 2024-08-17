import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import nova.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.jmock.integration.junit4.JUnitRuleMockery;

public class BehaviorTest {
    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    public Producer fakeProducer;
    public Consumer fakeConsumer;
    public static String endpoint = "wss://ws-feed.pro.coinbase.com";
    public static String productId = "BTC-USD";
    public static String tag = "trade@coinbase";
    public static BigAtomicCounter counter = new BigAtomicCounter();
    public Producer trueProducer = new RawProducer(endpoint, productId, tag, counter);

    @Before
    public void setup() {
        fakeProducer = context.mock(Producer.class);
        fakeConsumer = context.mock(Consumer.class);
    }
    @Test
    public void test() {

    }



}
