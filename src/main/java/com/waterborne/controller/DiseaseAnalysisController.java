package com.waterborne.controller;

import com.waterborne.model.DiseaseRecord;
import com.waterborne.service.DiseaseService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
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

public class DiseaseAnalysisController implements Initializable {
    @FXML private VBox contentBox;

    private final ComboBox<String> countryFilter = new ComboBox<>();
    private final ComboBox<String> regionFilter = new ComboBox<>();
    private final ComboBox<String> yearFilter = new ComboBox<>();
    private final ComboBox<String> sourceFilter = new ComboBox<>();
    private final TableView<DiseaseRecord> recordsTable = new TableView<>();
    private final BarChart<String, Number> comparisonChart =
            AnalysisViewSupport.barChart("Disease indicator", "Average cases per 100,000 people");
    private final BarChart<String, Number> diarrhealCountryChart =
            AnalysisViewSupport.barChart("Country", "Diarrheal cases per 100,000");
    private final BarChart<String, Number> choleraCountryChart =
            AnalysisViewSupport.barChart("Country", "Cholera cases per 100,000");
    private final BarChart<String, Number> typhoidCountryChart =
            AnalysisViewSupport.barChart("Country", "Typhoid cases per 100,000");
    private final ScatterChart<Number, Number> cleanWaterChart =
            AnalysisViewSupport.scatterChart("Access to clean water (%)", "Diarrheal cases per 100,000");
    private final ScatterChart<Number, Number> sanitationChart =
            AnalysisViewSupport.scatterChart("Sanitation coverage (%)", "Disease cases per 100,000");
    private final BarChart<String, Number> contaminantChart =
            AnalysisViewSupport.barChart("Contaminant level (ppm)", "Records");
    private final LineChart<String, Number> trendsChart =
            AnalysisViewSupport.lineChart("Year", "Average cases per 100,000 people");
    private final AnalysisViewSupport.Metric recordsMetric =
            new AnalysisViewSupport.Metric("Records");
    private final AnalysisViewSupport.Metric countriesMetric =
            new AnalysisViewSupport.Metric("Countries represented");
    private final AnalysisViewSupport.Metric yearsMetric =
            new AnalysisViewSupport.Metric("Years represented");
    private final AnalysisViewSupport.Metric diarrhealMetric =
            new AnalysisViewSupport.Metric("Average diarrheal cases / 100,000");
    private final AnalysisViewSupport.Metric choleraMetric =
            new AnalysisViewSupport.Metric("Average cholera cases / 100,000");
    private final AnalysisViewSupport.Metric typhoidMetric =
            new AnalysisViewSupport.Metric("Average typhoid cases / 100,000");

    private DiseaseService service;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            service = new DiseaseService();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load disease and environmental data.", e);
        }

        contentBox.getChildren().setAll(
                AnalysisViewSupport.page("Country/region-level environmental and disease records. "
                        + "Geography is not district-level, is not asserted to represent India, and is "
                        + "not merged into or used to validate the Indian district risk score."),
                buildFilters(),
                AnalysisViewSupport.cardGrid(List.of(recordsMetric, countriesMetric, yearsMetric,
                        diarrhealMetric, choleraMetric, typhoidMetric)),
                AnalysisViewSupport.chartGrid(List.of(
                        new AnalysisViewSupport.ChartPanel("disease-comparison-chart",
                                "Disease cases comparison · filtered averages", comparisonChart),
                        new AnalysisViewSupport.ChartPanel("diarrheal-country-chart",
                                "Diarrheal cases by country · top 10", diarrhealCountryChart),
                        new AnalysisViewSupport.ChartPanel("cholera-country-chart",
                                "Cholera cases by country · top 10", choleraCountryChart),
                        new AnalysisViewSupport.ChartPanel("typhoid-country-chart",
                                "Typhoid cases by country · top 10", typhoidCountryChart),
                        new AnalysisViewSupport.ChartPanel("clean-water-disease-chart",
                                "Clean water access versus diarrheal cases", cleanWaterChart),
                        new AnalysisViewSupport.ChartPanel("sanitation-disease-chart",
                                "Sanitation coverage versus disease cases", sanitationChart),
                        new AnalysisViewSupport.ChartPanel("contaminant-distribution-chart",
                                "Contaminant level distribution", contaminantChart),
                        new AnalysisViewSupport.ChartPanel("disease-trends-chart",
                                "Disease trends over years · filtered averages", trendsChart))),
                recordsTable);

        configureTable();
        setFilterOptions(countryFilter, service.getAllRecords().stream()
                .map(DiseaseRecord::country).distinct().sorted().toList());
        setFilterOptions(regionFilter, service.getAllRecords().stream()
                .map(DiseaseRecord::region).distinct().sorted().toList());
        setFilterOptions(yearFilter, service.getAllRecords().stream()
                .map(DiseaseRecord::year).filter(value -> value != null)
                .map(String::valueOf).distinct().sorted().toList());
        setFilterOptions(sourceFilter, service.getAllRecords().stream()
                .map(DiseaseRecord::waterSourceType).distinct().sorted().toList());
        countryFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh());
        regionFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh());
        yearFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh());
        sourceFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh());
        refresh();
    }

    private HBox buildFilters() {
        countryFilter.setPromptText("All countries");
        regionFilter.setPromptText("All regions");
        yearFilter.setPromptText("All years");
        sourceFilter.setPromptText("All water sources");
        for (ComboBox<String> filter : List.of(countryFilter, regionFilter, yearFilter, sourceFilter)) {
            filter.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(filter, Priority.ALWAYS);
        }
        return new HBox(10, countryFilter, regionFilter, yearFilter, sourceFilter);
    }

    private void configureTable() {
        recordsTable.setId("disease-environment-table");
        recordsTable.setPlaceholder(new Label("No records match the selected filters."));
        recordsTable.setPrefHeight(420);
        recordsTable.getStyleClass().add("content-panel");
        recordsTable.getColumns().addAll(
                textColumn("Country", DiseaseRecord::country, 130),
                textColumn("Region", DiseaseRecord::region, 105),
                textColumn("Year", record -> record.year() == null ? "—" : record.year().toString(), 80),
                textColumn("Water source type", DiseaseRecord::waterSourceType, 130),
                numberColumn("Contaminant level (ppm)", DiseaseRecord::contaminantLevel, 155),
                numberColumn("Clean water access (%)", DiseaseRecord::cleanWaterAccess, 145),
                numberColumn("Sanitation coverage (%)", DiseaseRecord::sanitationCoverage, 145),
                numberColumn("Diarrheal cases / 100k", DiseaseRecord::diarrhealCases, 145),
                numberColumn("Cholera cases / 100k", DiseaseRecord::choleraCases, 135),
                numberColumn("Typhoid cases / 100k", DiseaseRecord::typhoidCases, 135));
    }

    private TableColumn<DiseaseRecord, String> textColumn(
            String title, Function<DiseaseRecord, String> getter, double width) {
        TableColumn<DiseaseRecord, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(getter.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private TableColumn<DiseaseRecord, Double> numberColumn(
            String title, Function<DiseaseRecord, Double> getter, double width) {
        TableColumn<DiseaseRecord, Double> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(getter.apply(cell.getValue())));
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value == null ? "—" : AnalysisViewSupport.format(value));
            }
        });
        column.setPrefWidth(width);
        return column;
    }

    private void refresh() {
        List<DiseaseRecord> filtered = service.filter(countryFilter.getValue(), regionFilter.getValue(),
                yearFilter.getValue(), sourceFilter.getValue());
        recordsMetric.update(String.valueOf(filtered.size()));
        countriesMetric.update(String.valueOf(filtered.stream().map(DiseaseRecord::country).distinct().count()));
        yearsMetric.update(String.valueOf(filtered.stream().map(DiseaseRecord::year)
                .filter(year -> year != null).distinct().count()));
        diarrhealMetric.update(AnalysisViewSupport.format(
                DiseaseService.average(filtered, DiseaseRecord::diarrhealCases)));
        choleraMetric.update(AnalysisViewSupport.format(
                DiseaseService.average(filtered, DiseaseRecord::choleraCases)));
        typhoidMetric.update(AnalysisViewSupport.format(
                DiseaseService.average(filtered, DiseaseRecord::typhoidCases)));

        updateComparisonChart(filtered);
        updateCountryChart(diarrhealCountryChart, filtered, DiseaseRecord::diarrhealCases);
        updateCountryChart(choleraCountryChart, filtered, DiseaseRecord::choleraCases);
        updateCountryChart(typhoidCountryChart, filtered, DiseaseRecord::typhoidCases);
        updateCleanWaterChart(filtered);
        updateSanitationChart(filtered);
        AnalysisViewSupport.setHistogram(contaminantChart, filtered.stream()
                .map(DiseaseRecord::contaminantLevel).filter(value -> value != null).toList());
        updateTrendsChart(filtered);
        recordsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void updateComparisonChart(List<DiseaseRecord> records) {
        List<XYChart.Data<String, Number>> values = new ArrayList<>();
        addAverageBar(values, "Diarrheal", records, DiseaseRecord::diarrhealCases);
        addAverageBar(values, "Cholera", records, DiseaseRecord::choleraCases);
        addAverageBar(values, "Typhoid", records, DiseaseRecord::typhoidCases);
        AnalysisViewSupport.setBars(comparisonChart, "Average cases per 100,000", values);
    }

    private void addAverageBar(List<XYChart.Data<String, Number>> values, String name,
                               List<DiseaseRecord> records, Function<DiseaseRecord, Double> measure) {
        double average = DiseaseService.average(records, measure);
        if (Double.isFinite(average)) {
            values.add(new XYChart.Data<>(name, average));
        }
    }

    private void updateCountryChart(BarChart<String, Number> chart, List<DiseaseRecord> records,
                                   Function<DiseaseRecord, Double> measure) {
        Map<String, Double> averages = records.stream()
                .filter(record -> measure.apply(record) != null)
                .collect(Collectors.groupingBy(DiseaseRecord::country,
                        Collectors.averagingDouble(record -> measure.apply(record))));
        List<XYChart.Data<String, Number>> values = averages.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(entry -> new XYChart.Data<String, Number>(entry.getKey(), entry.getValue()))
                .toList();
        AnalysisViewSupport.setBars(chart, "Average cases per 100,000", values);
    }

    private void updateCleanWaterChart(List<DiseaseRecord> records) {
        cleanWaterChart.getData().clear();
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Records");
        records.stream().filter(record -> record.cleanWaterAccess() != null
                        && record.diarrhealCases() != null)
                .forEach(record -> series.getData().add(
                        new XYChart.Data<>(record.cleanWaterAccess(), record.diarrhealCases())));
        cleanWaterChart.getData().add(series);
    }

    private void updateSanitationChart(List<DiseaseRecord> records) {
        sanitationChart.getData().clear();
        addSanitationSeries(records, "Diarrheal", DiseaseRecord::diarrhealCases);
        addSanitationSeries(records, "Cholera", DiseaseRecord::choleraCases);
        addSanitationSeries(records, "Typhoid", DiseaseRecord::typhoidCases);
    }

    private void addSanitationSeries(List<DiseaseRecord> records, String name,
                                     Function<DiseaseRecord, Double> measure) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(name);
        records.stream().filter(record -> record.sanitationCoverage() != null
                        && measure.apply(record) != null)
                .forEach(record -> series.getData().add(
                        new XYChart.Data<>(record.sanitationCoverage(), measure.apply(record))));
        sanitationChart.getData().add(series);
    }

    private void updateTrendsChart(List<DiseaseRecord> records) {
        trendsChart.getData().clear();
        addTrendSeries(records, "Diarrheal", DiseaseRecord::diarrhealCases);
        addTrendSeries(records, "Cholera", DiseaseRecord::choleraCases);
        addTrendSeries(records, "Typhoid", DiseaseRecord::typhoidCases);
    }

    private void addTrendSeries(List<DiseaseRecord> records, String name,
                                Function<DiseaseRecord, Double> measure) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(name);
        Map<Integer, Double> averages = records.stream()
                .filter(record -> record.year() != null && measure.apply(record) != null)
                .collect(Collectors.groupingBy(DiseaseRecord::year,
                        Collectors.averagingDouble(record -> measure.apply(record))));
        averages.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> series.getData().add(
                        new XYChart.Data<>(entry.getKey().toString(), entry.getValue())));
        trendsChart.getData().add(series);
    }

    private void setFilterOptions(ComboBox<String> filter, List<String> options) {
        List<String> values = new ArrayList<>();
        values.add("All");
        values.addAll(options);
        filter.setItems(FXCollections.observableArrayList(values));
        filter.setValue("All");
    }
}
