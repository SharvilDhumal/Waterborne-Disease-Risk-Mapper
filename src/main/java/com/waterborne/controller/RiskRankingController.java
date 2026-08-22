package com.waterborne.controller;

import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;
import com.waterborne.service.DistrictService;
import com.waterborne.util.FormatUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for RiskRankingView.fxml.
 * Manages risk ranking leaderboards and top 3 podium spotlight cards.
 */
public class RiskRankingController implements Initializable, BaseController {

    @FXML private Label rank1NameLabel;
    @FXML private Label rank1ScoreLabel;
    @FXML private Label rank1BadgeLabel;
    @FXML private Label rank1MetricsLabel;

    @FXML private Label rank2NameLabel;
    @FXML private Label rank2ScoreLabel;
    @FXML private Label rank2BadgeLabel;
    @FXML private Label rank2MetricsLabel;

    @FXML private Label rank3NameLabel;
    @FXML private Label rank3ScoreLabel;
    @FXML private Label rank3BadgeLabel;
    @FXML private Label rank3MetricsLabel;

    @FXML private TableView<District> rankingTableView;
    @FXML private TableColumn<District, String> rankNumberColumn;
    @FXML private TableColumn<District, String> districtNameColumn;
    @FXML private TableColumn<District, String> riskScoreColumn;
    @FXML private TableColumn<District, RiskLevel> riskLevelColumn;
    @FXML private TableColumn<District, String> waterScarcityColumn;
    @FXML private TableColumn<District, String> sanitationColumn;
    @FXML private TableColumn<District, String> populationColumn;

    private DistrictService districtService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupRankingColumns();
    }

    @Override
    public void initService(DistrictService service) {
        this.districtService = service;
        loadRankingData();
    }

    private void setupRankingColumns() {
        rankNumberColumn.setCellValueFactory(cellData -> {
            int index = rankingTableView.getItems().indexOf(cellData.getValue()) + 1;
            return new SimpleStringProperty("#" + index);
        });

        districtNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        riskScoreColumn.setCellValueFactory(cellData -> new SimpleStringProperty(FormatUtil.formatScore(cellData.getValue().getRiskScore())));
        waterScarcityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(FormatUtil.formatPercentage(cellData.getValue().getWaterScarcity())));
        sanitationColumn.setCellValueFactory(cellData -> new SimpleStringProperty(FormatUtil.formatPercentage(cellData.getValue().getSanitationCoverage())));
        populationColumn.setCellValueFactory(cellData -> new SimpleStringProperty(FormatUtil.formatPopulation(cellData.getValue().getPopulation())));

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
                    if (level == RiskLevel.HIGH) badge.getStyleClass().add("badge-high");
                    else if (level == RiskLevel.MODERATE) badge.getStyleClass().add("badge-moderate");
                    else badge.getStyleClass().add("badge-low");

                    HBox container = new HBox(badge);
                    container.setAlignment(Pos.CENTER);
                    setGraphic(container);
                    setText(null);
                }
            }
        });
    }

    private void loadRankingData() {
        if (districtService == null) return;
        try {
            List<District> ranked = districtService.getRiskRanking();
            rankingTableView.setItems(FXCollections.observableArrayList(ranked));

            if (ranked.size() >= 1) populatePodiumCard(ranked.get(0), rank1NameLabel, rank1ScoreLabel, rank1BadgeLabel, rank1MetricsLabel);
            if (ranked.size() >= 2) populatePodiumCard(ranked.get(1), rank2NameLabel, rank2ScoreLabel, rank2BadgeLabel, rank2MetricsLabel);
            if (ranked.size() >= 3) populatePodiumCard(ranked.get(2), rank3NameLabel, rank3ScoreLabel, rank3BadgeLabel, rank3MetricsLabel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populatePodiumCard(District d, Label nameLabel, Label scoreLabel, Label badgeLabel, Label metricsLabel) {
        if (d == null) return;
        nameLabel.setText(d.getName());
        scoreLabel.setText("Risk Score: " + FormatUtil.formatScore(d.getRiskScore()) + " / 100");
        metricsLabel.setText("Scarcity: " + FormatUtil.formatPercentage(d.getWaterScarcity()) + " | Sanitation: " + FormatUtil.formatPercentage(d.getSanitationCoverage()));

        RiskLevel level = d.getRiskLevel();
        badgeLabel.setText(level.getLabel());
        badgeLabel.getStyleClass().removeAll("badge-low", "badge-moderate", "badge-high");
        if (level == RiskLevel.HIGH) badgeLabel.getStyleClass().add("badge-high");
        else if (level == RiskLevel.MODERATE) badgeLabel.getStyleClass().add("badge-moderate");
        else badgeLabel.getStyleClass().add("badge-low");
    }
}
