package dev.rabauer.racing_telemetry;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import software.xdev.chartjs.model.charts.LineChart;
import software.xdev.chartjs.model.data.LineData;
import software.xdev.chartjs.model.dataset.LineDataset;
import software.xdev.chartjs.model.options.LineOptions;
import software.xdev.vaadin.chartjs.ChartContainer;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Route("")
public class TelemetryView extends VerticalLayout {

    private static final int MAX_POINTS = 50;
    private static final long UPDATE_INTERVAL_MS = 300L;

    private final TelemetryService telemetryService;
    private final Deque<TelemetryPoint> buffer = new ArrayDeque<>(MAX_POINTS + 1);
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "telemetry-updater"));

    private final ChartContainer chartContainer;
    private ScheduledFuture<?> scheduledTask;

    public TelemetryView(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H1("Racing Telemetry Viewer"));

        chartContainer = new ChartContainer();
        chartContainer.setWidthFull();
        chartContainer.setHeight("500px");
        add(chartContainer);

        // Pre-fill buffer and render initial chart once the component is in the DOM
        addAttachListener(e -> {
            for (int i = 0; i < MAX_POINTS; i++) {
                buffer.add(telemetryService.next());
            }
            refreshChart();
            startUpdates(e.getUI());
        });

        addDetachListener(e -> stopUpdates());
    }

    private void startUpdates(UI ui) {
        scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            TelemetryPoint point = telemetryService.next();
            // UI.access ensures thread-safe Vaadin state mutation when @Push is active
            ui.access(() -> {
                buffer.add(point);
                if (buffer.size() > MAX_POINTS) {
                    buffer.poll();
                }
                refreshChart();
            });
        }, UPDATE_INTERVAL_MS, UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopUpdates() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        scheduler.shutdown();
    }

    private void refreshChart() {
        chartContainer.showChart(buildChartConfig());
    }

    private String buildChartConfig() {
        TelemetryPoint[] points = buffer.toArray(TelemetryPoint[]::new);

        String[] labels = IntStream.range(0, points.length)
            .mapToObj(String::valueOf)
            .toArray(String[]::new);

        LineDataset speedDataset = dataset("Speed (km/h)", "rgba(54, 162, 235, 1)", "rgba(54, 162, 235, 0.05)");
        LineDataset throttleDataset = dataset("Throttle (%)", "rgba(75, 192, 92, 1)", "rgba(75, 192, 92, 0.05)");
        LineDataset brakeDataset = dataset("Brake (%)", "rgba(255, 99, 132, 1)", "rgba(255, 99, 132, 0.05)");

        for (TelemetryPoint p : points) {
            speedDataset.addData(p.speed());
            throttleDataset.addData(p.throttle());
            brakeDataset.addData(p.brake());
        }

        return new LineChart(
            new LineData()
                .addLabels(labels)
                .addDataset(speedDataset)
                .addDataset(throttleDataset)
                .addDataset(brakeDataset),
            new LineOptions()
                .setAnimation(false)
                .setResponsive(true)
                .setMaintainAspectRatio(false)
        ).toJson();
    }

    private static LineDataset dataset(String label, String borderColor, String backgroundColor) {
        return new LineDataset()
            .setLabel(label)
            .setBorderColor(borderColor)
            .setBackgroundColor(backgroundColor)
            .setBorderWidth(2)
            .setTension(0.4)
            .setPointRadius(List.of(0))
            .setFill(false);
    }
}
