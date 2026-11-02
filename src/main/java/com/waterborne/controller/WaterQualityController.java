package com.waterborne.controller;

import com.waterborne.model.WaterQualityRecord;
import com.waterborne.service.WaterQualityService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WaterQualityController implements Initializable {
    @FXML private VBox contentBox;

    private final ComboBox<String> stateFilter = new ComboBox<>();
    private final ComboBox<String> bodyFilter = new ComboBox<>();
    private final TextField searchField = new TextField();
    private final TableView<WaterQualityRecord> stationTable = new TableView<>();
    private final VBox stationDetails = new VBox(8);
    private final BarChart<String, Number> bodChart =
            AnalysisViewSupport.barChart("Monitoring location", "BOD (mg/L)");
    private final BarChart<String, Number> fecalChart =
            AnalysisViewSupport.barChart("Monitoring location", "Fecal coliform (MPN/100ml)");
    private final BarChart<String, Number> totalChart =
            AnalysisViewSupport.barChart("Monitoring location", "Total coliform (MPN/100ml)");
    private final BarChart<String, Number> oxygenChart =
            AnalysisViewSupport.barChart("Dissolved oxygen (mg/L)", "Monitoring stations");
    private final BarChart<String, Number> phChart =
            AnalysisViewSupport.barChart("pH", "Monitoring stations");
    private final PieChart bodyChart = AnalysisViewSupport.pieChart();
    private final AnalysisViewSupport.Metric stationsMetric =
            new AnalysisViewSupport.Metric("Monitoring stations");
    private final AnalysisViewSupport.Metric statesMetric =
            new AnalysisViewSupport.Metric("States represented");
    private final AnalysisViewSupport.Metric phMetric =
            new AnalysisViewSupport.Metric("Average pH");
    private final AnalysisViewSupport.Metric bodMetric =
            new AnalysisViewSupport.Metric("Average BOD (mg/L)");
    private final AnalysisViewSupport.Metric oxygenMetric =
            new AnalysisViewSupport.Metric("Average dissolved oxygen (mg/L)");
    private final AnalysisViewSupport.Metric coliformMetric =
            new AnalysisViewSupport.Metric("High-coliform locations*");

    private WaterQualityService service;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            service = new WaterQualityService();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load water-quality monitoring data.", e);
        }

        contentBox.getChildren().setAll(
                AnalysisViewSupport.page("Indian water-quality monitoring stations (station/state level). "
                        + "This dataset has no district identifiers and is analyzed independently of district risk scores."),
                buildFilters(),
                AnalysisViewSupport.cardGrid(List.of(stationsMetric, statesMetric, phMetric, bodMetric,
                        oxygenMetric, coliformMetric)),
                AnalysisViewSupport.chartGrid(List.of(
                        new AnalysisViewSupport.ChartPanel("water-bod-chart",
                                "BOD by monitoring location · highest 12", bodChart),
                        new AnalysisViewSupport.ChartPanel("water-fecal-chart",
                                "Fecal coliform comparison · highest 12", fecalChart),
                        new AnalysisViewSupport.ChartPanel("water-total-chart",
                                "Total coliform comparison · highest 12", totalChart),
                        new AnalysisViewSupport.ChartPanel("water-oxygen-chart",
                                "Dissolved oxygen distribution", oxygenChart),
                        new AnalysisViewSupport.ChartPanel("water-ph-chart",
                                "pH distribution", phChart),
                        new AnalysisViewSupport.ChartPanel("water-body-chart",
                                "Water-body type distribution", bodyChart))),
                stationTable,
                stationDetails);
        coliformMetric.setTooltip(
                "Locations at or above the dataset-wide 75th percentile for either fecal or total coliform.");

        configureTable();
        setFilterOptions(stateFilter, service.getAllRecords().stream()
                .map(WaterQualityRecord::state).distinct().sorted().toList());
        setFilterOptions(bodyFilter, service.getAllRecords().stream()
                .map(WaterQualityRecord::waterBodyType).distinct().sorted().toList());
        stateFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh());
        bodyFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refresh());
        stationTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, selected) -> showStationDetails(selected));
        refresh();
    }

    private HBox buildFilters() {
        stateFilter.setPromptText("All states");
        bodyFilter.setPromptText("All water-body types");
        searchField.setPromptText("Search station code or location");
        searchField.getStyleClass().add("search-field");
        stateFilter.setMaxWidth(Double.MAX_VALUE);
        bodyFilter.setMaxWidth(Double.MAX_VALUE);
        searchField.setMaxWidth(Double.MAX_VALUE);
        HBox filters = new HBox(10, stateFilter, bodyFilter, searchField);
        HBox.setHgrow(stateFilter, Priority.ALWAYS);
        HBox.setHgrow(bodyFilter, Priority.ALWAYS);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        return filters;
    }

    private void configureTable() {
        stationTable.setId("water-station-table");
        stationTable.setPlaceholder(new Label("No monitoring stations match the selected filters."));
        stationTable.setPrefHeight(360);
        stationTable.getStyleClass().add("content-panel");
        stationTable.getColumns().addAll(
                textColumn("Station code", WaterQualityRecord::stationCode),
                textColumn("Monitoring location", WaterQualityRecord::location),
                textColumn("State", WaterQualityRecord::state),
                textColumn("Water-body type", WaterQualityRecord::waterBodyType),
                numberColumn("pH", WaterQualityRecord::averagePh),
                numberColumn("BOD", WaterQualityRecord::averageBod),
                numberColumn("Dissolved oxygen", WaterQualityRecord::averageDissolvedOxygen),
                numberColumn("Fecal coliform", WaterQualityRecord::averageFecalColiform),
                numberColumn("Total coliform", WaterQualityRecord::averageTotalColiform));
    }

    private TableColumn<WaterQualityRecord, String> textColumn(
            String title, Function<WaterQualityRecord, String> getter) {
        TableColumn<WaterQualityRecord, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(getter.apply(cell.getValue())));
        column.setPrefWidth(title.equals("Monitoring location") ? 260 : 135);
        return column;
    }

    private TableColumn<WaterQualityRecord, Double> numberColumn(
            String title, Function<WaterQualityRecord, Double> getter) {
        TableColumn<WaterQualityRecord, Double> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(getter.apply(cell.getValue())));
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : format(value));
            }
        });
        column.setPrefWidth(140);
        return column;
    }

    private void refresh() {
        List<WaterQualityRecord> filtered = service.filter(stateFilter.getValue(), bodyFilter.getValue(),
                searchField.getText());
        stationsMetric.update(String.valueOf(filtered.size()));
        statesMetric.update(String.valueOf(filtered.stream()
                .map(WaterQualityRecord::state).distinct().count()));
        phMetric.update(AnalysisViewSupport.format(
                WaterQualityService.average(filtered, WaterQualityRecord::averagePh)));
        bodMetric.update(AnalysisViewSupport.format(
                WaterQualityService.average(filtered, WaterQualityRecord::averageBod)));
        oxygenMetric.update(AnalysisViewSupport.format(
                WaterQualityService.average(filtered, WaterQualityRecord::averageDissolvedOxygen)));
        coliformMetric.update(String.valueOf(service.highColiformCount(filtered)));

        updateComparisonChart(bodChart, filtered, WaterQualityRecord::averageBod);
        updateComparisonChart(fecalChart, filtered, WaterQualityRecord::averageFecalColiform);
        updateComparisonChart(totalChart, filtered, WaterQualityRecord::averageTotalColiform);
        AnalysisViewSupport.setHistogram(oxygenChart, filtered.stream()
                .map(WaterQualityRecord::averageDissolvedOxygen).filter(value -> value != null).toList());
        AnalysisViewSupport.setHistogram(phChart, filtered.stream()
                .map(WaterQualityRecord::averagePh).filter(value -> value != null).toList());
        updateBodyChart(filtered);

        stationTable.setItems(FXCollections.observableArrayList(filtered));
        if (!filtered.isEmpty()) {
            stationTable.getSelectionModel().selectFirst();
        } else {
            showStationDetails(null);
        }
    }

    private void updateComparisonChart(BarChart<String, Number> chart, List<WaterQualityRecord> records,
                                       Function<WaterQualityRecord, Double> measure) {
        List<XYChart.Data<String, Number>> values = records.stream()
                .filter(record -> measure.apply(record) != null)
                .sorted(Comparator.comparing(measure).reversed())
                .limit(12)
                .map(record -> new XYChart.Data<String, Number>(
                        shortLocation(record.location()) + " [" + record.stationCode() + "]",
                        measure.apply(record)))
                .toList();
        AnalysisViewSupport.setBars(chart, "Average measurement", values);
    }

    private void updateBodyChart(List<WaterQualityRecord> records) {
        Map<String, Long> counts = records.stream()
                .collect(Collectors.groupingBy(WaterQualityRecord::waterBodyType, Collectors.counting()));
        bodyChart.getData().clear();
        counts.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> bodyChart.getData().add(
                        new PieChart.Data(entry.getKey(), entry.getValue())));
    }

    private void showStationDetails(WaterQualityRecord record) {
        stationDetails.getChildren().clear();
        stationDetails.getStyleClass().setAll("content-panel");
        Label heading = new Label(record == null
                ? "Select a monitoring station to view its measurements"
                : "Station measurements · " + record.location() + " (" + record.stationCode() + ")");
        heading.getStyleClass().add("panel-heading");
        stationDetails.getChildren().add(heading);
        if (record == null) {
            return;
        }

        GridPane measurements = new GridPane();
        measurements.setHgap(20);
        measurements.setVgap(8);
        addMeasurement(measurements, 0, "Temperature (°C)", record.temperatureMin(), record.temperatureMax());
        addMeasurement(measurements, 1, "Dissolved oxygen (mg/L)",
                record.dissolvedOxygenMin(), record.dissolvedOxygenMax());
        addMeasurement(measurements, 2, "pH", record.phMin(), record.phMax());
        addMeasurement(measurements, 3, "Conductivity (µmhos/cm)",
                record.conductivityMin(), record.conductivityMax());
        addMeasurement(measurements, 4, "BOD (mg/L)", record.bodMin(), record.bodMax());
        addMeasurement(measurements, 5, "Nitrate + nitrite (mg/L)",
                record.nitrateMin(), record.nitrateMax());
        addMeasurement(measurements, 6, "Fecal coliform (MPN/100ml)",
                record.fecalColiformMin(), record.fecalColiformMax());
        addMeasurement(measurements, 7, "Total coliform (MPN/100ml)",
                record.totalColiformMin(), record.totalColiformMax());
        stationDetails.getChildren().add(measurements);
    }

    private void addMeasurement(GridPane grid, int row, String name, Double minimum, Double maximum) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("metric-title");
        grid.add(nameLabel, 0, row);
        grid.add(new Label("Min: " + format(minimum)), 1, row);
        grid.add(new Label("Max: " + format(maximum)), 2, row);
    }

    private void setFilterOptions(ComboBox<String> filter, List<String> options) {
        List<String> values = new ArrayList<>();
        values.add("All");
        values.addAll(options);
        filter.setItems(FXCollections.observableArrayList(values));
        filter.setValue("All");
    }

    private String shortLocation(String location) {
        return location.length() <= 24 ? location : location.substring(0, 21) + "...";
    }

    private String format(Double value) {
        return value == null ? "—" : AnalysisViewSupport.format(value);
    }

}
