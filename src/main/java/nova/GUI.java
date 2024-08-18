package nova;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

public class GUI extends JFrame {
    private final ScheduledExecutorService guiUpdater = Executors.newSingleThreadScheduledExecutor();
    private ExecutorService executors = Executors.newFixedThreadPool(NovaConstant.NUMBER_OF_PRODUCER + NovaConstant.NUMBER_OF_CONSUMER);
    private BigAtomicCounter counter;
    private ConcurrentClearingMap<String, String> cache;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final JLabel counterLabel;
    private final JLabel cacheLabel;
    private final TimeSeries counterSeries;
    private final TimeSeries cacheSeries;

    public GUI() {
        setTitle("Coinbase Trading Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(NovaConstant.GUI_WIDTH, NovaConstant.GUI_HEIGHT);
        setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel();
        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        add(controlPanel, BorderLayout.NORTH);

        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        counterLabel = new JLabel("Counter: 0");
        cacheLabel = new JLabel("Cache size: 0");
        infoPanel.add(counterLabel);
        infoPanel.add(cacheLabel);
        add(infoPanel, BorderLayout.CENTER);

        JLabel counterLabel = new JLabel("Counter: 0");
        JLabel cacheLabel = new JLabel("Cache size: 0");
        infoPanel.add(counterLabel);
        infoPanel.add(cacheLabel);
        add(infoPanel, BorderLayout.SOUTH);

        // Create the chart
        counterSeries = new TimeSeries("Counter");
        cacheSeries = new TimeSeries("Cache Size");
        var dataset = new TimeSeriesCollection();
        dataset.addSeries(counterSeries);
        dataset.addSeries(cacheSeries);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Counter and Cache Size Over Time",
                "Time",
                "Value",
                dataset,
                true,
                true,
                false
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(750, 450));
        add(chartPanel, BorderLayout.CENTER);

        startButton.addActionListener(e -> startApplication());
        stopButton.addActionListener(e -> stopApplication());

        guiUpdater.scheduleAtFixedRate(this::updateGUI, 0, NovaConstant.UPDATE_GUI_TIME_IN_SECOND, TimeUnit.SECONDS);
    }


    private void startApplication() {
        if (isRunning.compareAndSet(false, true)) {
            String[] endpoint = new String[]{"wss://ws-feed.pro.coinbase.com"};
            String[] productId = new String[]{"BTC-USD", "ETH-EUR"};
            String tag = "trade@coinbase";

            counter = new BigAtomicCounter();
            cache = new ConcurrentClearingMap<>();

            for(int i = 0; i < NovaConstant.NUMBER_OF_PRODUCER; i++) {
                String threadEndPoint = endpoint[i % endpoint.length];
                String threadProduct = productId[i % productId.length];
                Thread taskProducer = new RawProducer(threadEndPoint, threadProduct, tag, counter);
                executors.execute(taskProducer);
            }
            for(int i = 0; i < NovaConstant.NUMBER_OF_CONSUMER; i++) {
                Thread taskConsumer = new Normalizer(cache);
                executors.execute(taskConsumer);
            }
        }
    }

    private void stopApplication() {
        if (isRunning.compareAndSet(true, false)) {
            executors.shutdownNow();
            executors = Executors.newFixedThreadPool(NovaConstant.NUMBER_OF_PRODUCER);
        }
    }

    private void updateGUI() {
        if (isRunning.get()) {
            SwingUtilities.invokeLater(() -> {
                long counterValue = counter.get().longValue();
                int cacheSize = cache.size();
                counterLabel.setText("Counter: " + counter.get());
                cacheLabel.setText("Cache size: " + cache.size());
                Second now = new Second();
                this.counterSeries.add(now, counterValue);
                cacheSeries.add(now, cacheSize);

                // Limit the number of data points to prevent memory issues
                if (counterSeries.getItemCount() > 300) {
                    counterSeries.delete(0, 0);
                    cacheSeries.delete(0, 0);
                }

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