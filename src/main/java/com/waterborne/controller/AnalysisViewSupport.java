package com.waterborne.controller;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

final class AnalysisViewSupport {
    private AnalysisViewSupport() {
    }

    static VBox page(String geographicNote) {
        VBox page = new VBox(16);
        page.setPadding(new Insets(10));
        Label note = new Label(geographicNote);
        note.setWrapText(true);
        note.getStyleClass().add("metric-subtext");
        page.getChildren().add(note);
        return page;
    }

    static GridPane cardGrid(List<Metric> metrics) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        for (int column = 0; column < 3; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(100.0 / 3.0);
            constraints.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(constraints);
        }
        for (int index = 0; index < metrics.size(); index++) {
            Metric metric = metrics.get(index);
            VBox card = new VBox(8);
            card.getStyleClass().add("metric-card");
            Label title = new Label(metric.title());
            title.setWrapText(true);
            title.getStyleClass().add("metric-title");
            Label value = new Label("—");
            value.setWrapText(true);
            value.getStyleClass().add("metric-value");
            card.getChildren().addAll(title, value);
            metric.setValue(value);
            GridPane.setHgrow(card, Priority.ALWAYS);
            grid.add(card, index % 3, index / 3);
        }
        return grid;
    }

    static VBox panel(String title, Node content) {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("content-panel");
        Label heading = new Label(title);
        heading.getStyleClass().add("panel-heading");
        panel.getChildren().addAll(heading, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        return panel;
    }

    static GridPane chartGrid(List<ChartPanel> panels) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        for (int column = 0; column < 2; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(50);
            constraints.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(constraints);
        }
        for (int index = 0; index < panels.size(); index++) {
            ChartPanel chartPanel = panels.get(index);
            Node chart = chartPanel.chart();
            chart.setId(chartPanel.id());
            chart.setStyle("-fx-pref-height: 300px;");
            GridPane.setHgrow(chart, Priority.ALWAYS);
            grid.add(panel(chartPanel.title(), chart), index % 2, index / 2);
        }
        return grid;
    }

    static BarChart<String, Number> barChart(String xLabel, String yLabel) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(xLabel);
        xAxis.setTickLabelRotation(-25);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);
        yAxis.setForceZeroInRange(true);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCategoryGap(8);
        chart.setPrefHeight(300);
        return chart;
    }

    static LineChart<String, Number> lineChart(String xLabel, String yLabel) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(xLabel);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);
        yAxis.setForceZeroInRange(true);
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setPrefHeight(300);
        return chart;
    }

    static ScatterChart<Number, Number> scatterChart(String xLabel, String yLabel) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel(xLabel);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);
        ScatterChart<Number, Number> chart = new ScatterChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setPrefHeight(300);
        return chart;
    }

    static PieChart pieChart() {
        PieChart chart = new PieChart();
        chart.setAnimated(false);
        chart.setPrefHeight(300);
        return chart;
    }

    static void setBars(BarChart<String, Number> chart, String seriesName,
                        List<XYChart.Data<String, Number>> values) {
        chart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(seriesName);
        series.getData().addAll(values);
        chart.getData().add(series);
    }

    static void setHistogram(BarChart<String, Number> chart, List<Double> values) {
        chart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Records");
        if (values.isEmpty()) {
            chart.getData().add(series);
            return;
        }
        double minimum = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maximum = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        int buckets = 10;
        if (minimum == maximum) {
            series.getData().add(new XYChart.Data<>(String.format("%.2f", minimum), values.size()));
        } else {
            int[] counts = new int[buckets];
            double step = (maximum - minimum) / buckets;
            for (double value : values) {
                int index = Math.min(buckets - 1, (int) ((value - minimum) / step));
                counts[index]++;
            }
            for (int index = 0; index < buckets; index++) {
                double lower = minimum + index * step;
                double upper = index == buckets - 1 ? maximum : lower + step;
                series.getData().add(new XYChart.Data<>(
                        String.format("%.1f–%.1f", lower, upper), counts[index]));
            }
        }
        chart.getData().add(series);
    }

    static String format(double value) {
        return Double.isFinite(value) ? String.format("%,.2f", value) : "—";
    }

    record ChartPanel(String id, String title, Node chart) {
    }

    static final class Metric {
        private final String title;
        private Label value;

        Metric(String title) {
            this.title = title;
        }

        String title() {
            return title;
        }

        void setValue(Label value) {
            this.value = value;
        }

        void update(String text) {
            value.setText(text);
        }

        void setTooltip(String text) {
            value.setTooltip(new javafx.scene.control.Tooltip(text));
        }
    }
}
