import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import nova.*;
import org.jmock.Expectations;
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
    public Cache<String,String> fakeCache;
    public static BigAtomicCounter counter = new BigAtomicCounter();
    public Producer trueProducer = new RawProducer(endpoint, productId, tag, counter);

    @Before
    public void setup() {
        fakeProducer = context.mock(Producer.class);
        fakeConsumer = context.mock(Consumer.class);
        fakeCache = context.mock(Cache.class);
    }

    @Test
    public void testCacheUsed() {
        Consumer trueConsumer = new Normalizer(fakeCache);
        String data = "trade@test,2023-08-17T12:00:00Z,BTC-USD,30000,0.1";
        String dataKey = "BTC-USD300002023-08-17T12:00:00Z0.1test";
        context.checking(new Expectations(){{
            exactly(1).of(fakeCache).contain(dataKey);
            exactly(1).of(fakeCache).put(dataKey, "True");
        }});
        trueConsumer.dataPersistance(data);
    }




}
