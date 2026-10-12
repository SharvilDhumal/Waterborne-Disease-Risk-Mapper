package com.waterborne.controller;

import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;
import com.waterborne.service.DistrictService;
import com.waterborne.util.AlertUtil;
import com.waterborne.util.FormatUtil;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Function;

/**
 * Controller for DataTableView.fxml.
 * Provides sortable, filterable TableView with custom RiskLevel badge cell renderers.
 */
public class DataTableController implements Initializable, BaseController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> riskFilterComboBox;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private Label resultCountLabel;

    @FXML private TableView<District> districtTableView;
    @FXML private TableColumn<District, Number> idColumn;
    @FXML private TableColumn<District, String> nameColumn;
    @FXML private TableColumn<District, Number> waterScarcityColumn;
    @FXML private TableColumn<District, Number> censusWaterScarcityColumn;
    @FXML private TableColumn<District, Number> nfhsWaterCoverageColumn;
    @FXML private TableColumn<District, Number> sanitationColumn;
    @FXML private TableColumn<District, Number> censusSanitationColumn;
    @FXML private TableColumn<District, Number> nfhsSanitationCoverageColumn;
    @FXML private TableColumn<District, Number> populationColumn;
    @FXML private TableColumn<District, Number> riskScoreColumn;
    @FXML private TableColumn<District, RiskLevel> riskLevelColumn;

    private DistrictService districtService;
    private final ObservableList<District> masterData = FXCollections.observableArrayList();
    private FilteredList<District> filteredData;
    private SortedList<District> sortedData;
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(150));

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupFilterOptions();
        setupObservableData();
        setupListeners();
        applySelectedSort();
    }

    @Override
    public void initService(DistrictService service) {
        this.districtService = service;
        loadTableData();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(cell -> new SimpleLongProperty(cell.getValue().getId()));
        idColumn.setComparator((first, second) -> Long.compare(first.longValue(), second.longValue()));
        nameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        waterScarcityColumn.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getWaterScarcity()));
        waterScarcityColumn.setCellFactory(column ->
                formattedNumberCell(value -> FormatUtil.formatPercentage(value.doubleValue())));
        censusWaterScarcityColumn.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getCensusWaterScarcity()));
        censusWaterScarcityColumn.setCellFactory(column ->
                formattedNumberCell(value -> formatOptionalPercentage(value.doubleValue())));
        nfhsWaterCoverageColumn.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getNfhsImprovedWaterCoverage()));
        nfhsWaterCoverageColumn.setCellFactory(column ->
                formattedNumberCell(value -> formatOptionalPercentage(value.doubleValue())));
        sanitationColumn.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getSanitationCoverage()));
        sanitationColumn.setCellFactory(column ->
                formattedNumberCell(value -> FormatUtil.formatPercentage(value.doubleValue())));
        censusSanitationColumn.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getCensusSanitationCoverage()));
        censusSanitationColumn.setCellFactory(column ->
                formattedNumberCell(value -> formatOptionalPercentage(value.doubleValue())));
        nfhsSanitationCoverageColumn.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getNfhsImprovedSanitationCoverage()));
        nfhsSanitationCoverageColumn.setCellFactory(column ->
                formattedNumberCell(value -> formatOptionalPercentage(value.doubleValue())));
        populationColumn.setCellValueFactory(cell -> new SimpleLongProperty(cell.getValue().getPopulation()));
        populationColumn.setCellFactory(column -> formattedNumberCell(value -> FormatUtil.formatPopulation(value.longValue())));
        riskScoreColumn.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getRiskScore()));
        riskScoreColumn.setCellFactory(column ->
                formattedNumberCell(value -> FormatUtil.formatScore(value.doubleValue())));

        riskLevelColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getRiskLevel()));
        riskLevelColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(RiskLevel level, boolean empty) {
                super.updateItem(level, empty);
                if (empty || level == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label badge = new Label(level.getLabel());
                badge.getStyleClass().add("badge");
                if (level == RiskLevel.HIGH) badge.getStyleClass().add("badge-high");
                else if (level == RiskLevel.MODERATE) badge.getStyleClass().add("badge-moderate");
                else badge.getStyleClass().add("badge-low");

                HBox container = new HBox(badge);
                container.setAlignment(Pos.CENTER);
                setGraphic(container);
                setText(null);
            }
        });

        districtTableView.setPlaceholder(new Label("No district records found matching your filters."));
    }

    private TableCell<District, Number> formattedNumberCell(Function<Number, String> formatter) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null
                        : Double.isFinite(value.doubleValue()) ? formatter.apply(value) : "—");
            }
        };
    }

    private String formatOptionalPercentage(double value) {
        return Double.isFinite(value) ? FormatUtil.formatPercentage(value) : "—";
    }

    private void setupFilterOptions() {
        riskFilterComboBox.setItems(FXCollections.observableArrayList(
                "All Risk Levels",
                "HIGH RISK",
                "MODERATE RISK",
                "LOW RISK"
        ));
        riskFilterComboBox.getSelectionModel().selectFirst();

        sortComboBox.setItems(FXCollections.observableArrayList(
                "Risk Score — High to Low",
                "Risk Score — Low to High",
                "District Name — A-Z",
                "Water Scarcity — High to Low",
                "Sanitation — Low to High",
                "Population — High to Low"
        ));
        sortComboBox.getSelectionModel().selectFirst();
    }

    private void setupObservableData() {
        filteredData = new FilteredList<>(masterData, district -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(districtTableView.comparatorProperty());
        districtTableView.setItems(sortedData);
    }

    private void setupListeners() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> searchDelay.playFromStart());
        searchDelay.setOnFinished(event -> applyFilters());
        riskFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        sortComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applySelectedSort());
    }

    private void loadTableData() {
        if (districtService == null) return;
        resultCountLabel.setText("Loading districts...");

        Task<List<District>> task = new Task<>() {
            @Override
            protected List<District> call() throws Exception {
                return districtService.getAllDistricts();
            }
        };
        task.setOnSucceeded(event -> {
            masterData.setAll(task.getValue());
            applyFilters();
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            resultCountLabel.setText("Could not load district data");
            System.err.println("[ERROR] Failed to load table data: " + error.getMessage());
            AlertUtil.showError("Data Load Error", "Could Not Load Districts",
                    "Unable to load district data: " + error.getMessage());
        });

        Thread loader = new Thread(task, "district-table-data-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void applyFilters() {
        String query = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedRisk = riskFilterComboBox.getValue();

        RiskLevel filterLevel = null;
        if ("HIGH RISK".equals(selectedRisk)) filterLevel = RiskLevel.HIGH;
        else if ("MODERATE RISK".equals(selectedRisk)) filterLevel = RiskLevel.MODERATE;
        else if ("LOW RISK".equals(selectedRisk)) filterLevel = RiskLevel.LOW;

        RiskLevel selectedLevel = filterLevel;
        filteredData.setPredicate(district ->
                (query.isEmpty() || district.getName().toLowerCase(Locale.ROOT).contains(query))
                        && (selectedLevel == null || district.getRiskLevel() == selectedLevel));
        resultCountLabel.setText("Showing " + filteredData.size() + " of " + masterData.size() + " districts");
    }

    private void applySelectedSort() {
        String selectedSort = sortComboBox.getValue();
        if (selectedSort == null) return;

        TableColumn<District, ?> column;
        TableColumn.SortType sortType;
        switch (selectedSort) {
            case "Risk Score — Low to High" -> {
                column = riskScoreColumn;
                sortType = TableColumn.SortType.ASCENDING;
            }
            case "District Name — A-Z" -> {
                column = nameColumn;
                sortType = TableColumn.SortType.ASCENDING;
            }
            case "Water Scarcity — High to Low" -> {
                column = waterScarcityColumn;
                sortType = TableColumn.SortType.DESCENDING;
            }
            case "Sanitation — Low to High" -> {
                column = sanitationColumn;
                sortType = TableColumn.SortType.ASCENDING;
            }
            case "Population — High to Low" -> {
                column = populationColumn;
                sortType = TableColumn.SortType.DESCENDING;
            }
            default -> {
                column = riskScoreColumn;
                sortType = TableColumn.SortType.DESCENDING;
            }
        }

        column.setSortType(sortType);
        districtTableView.getSortOrder().setAll(column);
        districtTableView.sort();
    }

    @FXML
    public void handleResetFilters() {
        searchField.clear();
        riskFilterComboBox.getSelectionModel().selectFirst();
        sortComboBox.getSelectionModel().selectFirst();
        applySelectedSort();
        applyFilters();
    }
}
