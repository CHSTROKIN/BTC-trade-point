package nova;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GUI extends JFrame {
    private final ScheduledExecutorService guiUpdater = Executors.newSingleThreadScheduledExecutor();
    private ExecutorService executors = Executors.newFixedThreadPool(NovaConstant.THREAD_NUMBER);
    private BigAtomicCounter counter;
    private ConcurrentClearingMap<String, String> cache;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final JLabel counterLabel;
    private final JLabel cacheLabel;

    public GUI() {
        setTitle("Coinbase Trading Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 200);
        setLayout(new BorderLayout());

        var controlPanel = new JPanel();
        var startButton = new JButton("Start");
        var stopButton = new JButton("Stop");
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        add(controlPanel, BorderLayout.NORTH);

        var infoPanel = new JPanel(new GridLayout(2, 1));
        counterLabel = new JLabel("Counter: 0");
        cacheLabel = new JLabel("Cache size: 0");
        infoPanel.add(counterLabel);
        infoPanel.add(cacheLabel);
        add(infoPanel, BorderLayout.CENTER);

        startButton.addActionListener(e -> startApplication());
        stopButton.addActionListener(e -> stopApplication());

        guiUpdater.scheduleAtFixedRate(this::updateGUI, 0, NovaConstant.UPDATE_GUI_TIME_IN_SECOND, TimeUnit.SECONDS);
    }


    private void startApplication() {
        if (isRunning.compareAndSet(false, true)) {
            var endpoint = new String[]{"wss://ws-feed.pro.coinbase.com"};
            var productId = new String[]{"BTC-USD", "ETH-EUR"};
            var tag = "trade@coinbase";

            counter = new BigAtomicCounter();
            cache = new ConcurrentClearingMap<>();

            for (int i = 0; i < NovaConstant.THREAD_NUMBER; i++) {
                var threadEndPoint = endpoint[i % endpoint.length];
                var threadProduct = productId[i % productId.length];
                var taskProducer = new RawProducer(threadEndPoint, threadProduct, tag, counter);
                executors.execute(taskProducer);
            }
            var taskConsumer = new Normalizer(cache);
            executors.execute(taskConsumer);
        }
    }

    private void stopApplication() {
        if (isRunning.compareAndSet(true, false)) {
            executors.shutdownNow();
            executors = Executors.newFixedThreadPool(NovaConstant.THREAD_NUMBER);
        }
        while (executors.isTerminated()){
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateGUI() {
        if (isRunning.get()) {
            SwingUtilities.invokeLater(() -> {
                counterLabel.setText("Counter: " + counter.get());
                cacheLabel.setText("Cache size: " + cache.size());
            });
        }
    }

    @Override
    public void dispose() {
        guiUpdater.shutdownNow();
        executors.shutdownNow();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            var gui = new GUI();
            gui.setVisible(true);
        });
    }
}