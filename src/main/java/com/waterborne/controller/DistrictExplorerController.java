package com.waterborne.controller;

import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;
import com.waterborne.service.DistrictService;
import com.waterborne.util.FormatUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for DistrictExplorerView.fxml.
 * Handles district search list, custom cell rendering, progress bars, and automated risk interpretation.
 */
public class DistrictExplorerController implements Initializable, BaseController {

    @FXML private TextField searchField;
    @FXML private ListView<District> districtListView;

    @FXML private Label districtNameLabel;
    @FXML private Label riskScoreLabel;
    @FXML private Label riskBadgeLabel;

    @FXML private Label scarcityValueLabel;
    @FXML private ProgressBar scarcityProgressBar;

    @FXML private Label sanitationValueLabel;
    @FXML private ProgressBar sanitationProgressBar;

    @FXML private Label densityValueLabel;
    @FXML private ProgressBar densityProgressBar;

    @FXML private Label populationValueLabel;
    @FXML private Label interpretationLabel;

    private DistrictService districtService;
    private final ObservableList<District> masterDistricts = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupListView();
        setupSearchListener();
    }

    @Override
    public void initService(DistrictService service) {
        this.districtService = service;
        loadDistricts();
    }

    private void setupListView() {
        districtListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(District item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label name = new Label(item.getName());
                    name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label badge = new Label(item.getRiskLevel().name());
                    badge.getStyleClass().add("badge");
                    if (item.getRiskLevel() == RiskLevel.HIGH) badge.getStyleClass().add("badge-high");
                    else if (item.getRiskLevel() == RiskLevel.MODERATE) badge.getStyleClass().add("badge-moderate");
                    else badge.getStyleClass().add("badge-low");

                    HBox container = new HBox(name, spacer, badge);
                    container.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(container);
                }
            }
        });

        districtListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                displayDistrictDetails(newSelection);
            }
        });
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                districtListView.setItems(masterDistricts);
            } else {
                String query = newVal.trim().toLowerCase();
                ObservableList<District> filtered = masterDistricts.filtered(
                        d -> d.getName().toLowerCase().contains(query)
                );
                districtListView.setItems(filtered);
            }
            if (!districtListView.getItems().isEmpty()) {
                districtListView.getSelectionModel().selectFirst();
            }
        });
    }

    private void loadDistricts() {
        if (districtService == null) return;
        try {
            List<District> list = districtService.getAllDistricts();
            masterDistricts.setAll(list);
            districtListView.setItems(masterDistricts);

            if (!masterDistricts.isEmpty()) {
                districtListView.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayDistrictDetails(District district) {
        if (district == null) return;

        districtNameLabel.setText(district.getName());
        riskScoreLabel.setText("Score: " + FormatUtil.formatScore(district.getRiskScore()) + " / 100");

        RiskLevel level = district.getRiskLevel();
        riskBadgeLabel.setText(level.getLabel());
        riskBadgeLabel.getStyleClass().removeAll("badge-low", "badge-moderate", "badge-high");
        if (level == RiskLevel.HIGH) riskBadgeLabel.getStyleClass().add("badge-high");
        else if (level == RiskLevel.MODERATE) riskBadgeLabel.getStyleClass().add("badge-moderate");
        else riskBadgeLabel.getStyleClass().add("badge-low");

        // Progress Bars & Labels
        scarcityValueLabel.setText(FormatUtil.formatPercentage(district.getWaterScarcity()));
        scarcityProgressBar.setProgress(district.getWaterScarcity() / 100.0);

        sanitationValueLabel.setText(FormatUtil.formatPercentage(district.getSanitationCoverage()));
        sanitationProgressBar.setProgress(district.getSanitationCoverage() / 100.0);

        densityValueLabel.setText(FormatUtil.formatDensity(district.getPopulationDensity()));
        double densityProgress = Math.min(1.0, district.getPopulationDensity() / 2000.0);
        densityProgressBar.setProgress(densityProgress);

        populationValueLabel.setText(FormatUtil.formatPopulation(district.getPopulation()) + " (" + String.format("%,d", district.getPopulation()) + " residents)");

        // Automated Risk Interpretation Summary Generation
        interpretationLabel.setText(generateInterpretationText(district));
    }

    private String generateInterpretationText(District d) {
        StringBuilder sb = new StringBuilder();
        sb.append("District ").append(d.getName()).append(" presents an empirical waterborne disease risk score of ")
          .append(FormatUtil.formatScore(d.getRiskScore())).append("/100, placing it in the ").append(d.getRiskLevel().getLabel()).append(" tier. ");

        if (d.getRiskLevel() == RiskLevel.HIGH) {
            sb.append("Primary risk drivers include acute water scarcity (").append(FormatUtil.formatPercentage(d.getWaterScarcity()))
              .append(") and poor sanitation infrastructure (").append(FormatUtil.formatPercentage(d.getSanitationCoverage()))
              .append(" coverage), compounded by high population density (").append(FormatUtil.formatDensity(d.getPopulationDensity()))
              .append("). Immediate resource deployment for water purification and infrastructure intervention is strongly advised.");
        } else if (d.getRiskLevel() == RiskLevel.MODERATE) {
            sb.append("The district exhibits moderate environmental exposure. Water scarcity stands at ")
              .append(FormatUtil.formatPercentage(d.getWaterScarcity())).append(" with sanitation coverage at ")
              .append(FormatUtil.formatPercentage(d.getSanitationCoverage())).append(". Targeted sanitation upgrades and water conservation planning are recommended to prevent escalation to high risk.");
        } else {
            sb.append("The district maintains favorable environmental public health indicators, with relatively low water scarcity (")
              .append(FormatUtil.formatPercentage(d.getWaterScarcity())).append(") and robust sanitation coverage (")
              .append(FormatUtil.formatPercentage(d.getSanitationCoverage())).append("). Routine monitoring is advised.");
        }

        return sb.toString();
    }
}
