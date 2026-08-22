package com.waterborne.controller;

import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;
import com.waterborne.service.DistrictService;
import com.waterborne.util.FormatUtil;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

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
    @FXML private TableColumn<District, String> waterScarcityColumn;
    @FXML private TableColumn<District, String> sanitationColumn;
    @FXML private TableColumn<District, String> populationColumn;
    @FXML private TableColumn<District, String> riskScoreColumn;
    @FXML private TableColumn<District, RiskLevel> riskLevelColumn;

    private DistrictService districtService;
    private final ObservableList<District> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupFilterOptions();
        setupListeners();
    }

    @Override
    public void initService(DistrictService service) {
        this.districtService = service;
        loadTableData();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(cellData -> new SimpleLongProperty(cellData.getValue().getId()));
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        waterScarcityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(FormatUtil.formatPercentage(cellData.getValue().getWaterScarcity())));
        sanitationColumn.setCellValueFactory(cellData -> new SimpleStringProperty(FormatUtil.formatPercentage(cellData.getValue().getSanitationCoverage())));
        populationColumn.setCellValueFactory(cellData -> new SimpleStringProperty(FormatUtil.formatPopulation(cellData.getValue().getPopulation())));
        riskScoreColumn.setCellValueFactory(cellData -> new SimpleStringProperty(FormatUtil.formatScore(cellData.getValue().getRiskScore())));

        // Custom Cell Renderer for Risk Level Badge
        riskLevelColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getRiskLevel()));
        riskLevelColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(RiskLevel level, boolean empty) {
                super.updateItem(level, empty);
                if (empty || level == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(level.getLabel());
                    badge.getStyleClass().add("badge");
                    if (level == RiskLevel.HIGH) {
                        badge.getStyleClass().add("badge-high");
                    } else if (level == RiskLevel.MODERATE) {
                        badge.getStyleClass().add("badge-moderate");
                    } else {
                        badge.getStyleClass().add("badge-low");
                    }

                    HBox container = new HBox(badge);
                    container.setAlignment(Pos.CENTER);
                    setGraphic(container);
                    setText(null);
                }
            }
        });

        districtTableView.setPlaceholder(new Label("No district records found matching your filters."));
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

    private void setupListeners() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        riskFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadTableData() {
        if (districtService == null) return;
        try {
            List<District> list = districtService.getAllDistricts();
            masterData.setAll(list);
            applyFilters();
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to load table data: " + e.getMessage());
        }
    }

    private void applyFilters() {
        if (districtService == null) return;

        String query = searchField.getText();
        String selectedRisk = riskFilterComboBox.getValue();
        String selectedSort = sortComboBox.getValue();

        RiskLevel filterLevel = null;
        if ("HIGH RISK".equals(selectedRisk)) filterLevel = RiskLevel.HIGH;
        else if ("MODERATE RISK".equals(selectedRisk)) filterLevel = RiskLevel.MODERATE;
        else if ("LOW RISK".equals(selectedRisk)) filterLevel = RiskLevel.LOW;

        try {
            List<District> filtered = districtService.filterDistricts(query, filterLevel, selectedSort);
            districtTableView.setItems(FXCollections.observableArrayList(filtered));
            resultCountLabel.setText("Showing " + filtered.size() + " of " + masterData.size() + " districts");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleResetFilters() {
        searchField.clear();
        riskFilterComboBox.getSelectionModel().selectFirst();
        sortComboBox.getSelectionModel().selectFirst();
        applyFilters();
    }
}
