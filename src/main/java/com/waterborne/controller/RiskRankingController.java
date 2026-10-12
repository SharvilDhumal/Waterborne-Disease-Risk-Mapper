package com.waterborne.controller;

import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;
import com.waterborne.service.DistrictService;
import com.waterborne.util.AlertUtil;
import com.waterborne.util.FormatUtil;
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

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Function;

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

    @FXML private TextField searchField;
    @FXML private TableView<District> rankingTableView;
    @FXML private TableColumn<District, String> rankNumberColumn;
    @FXML private TableColumn<District, String> districtNameColumn;
    @FXML private TableColumn<District, Number> riskScoreColumn;
    @FXML private TableColumn<District, RiskLevel> riskLevelColumn;
    @FXML private TableColumn<District, Number> waterScarcityColumn;
    @FXML private TableColumn<District, Number> sanitationColumn;
    @FXML private TableColumn<District, Number> populationColumn;

    private DistrictService districtService;
    private final ObservableList<District> masterData = FXCollections.observableArrayList();
    private FilteredList<District> filteredData;
    private SortedList<District> sortedData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupRankingColumns();
        setupObservableData();
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applySearch(newValue));
    }

    @Override
    public void initService(DistrictService service) {
        this.districtService = service;
        loadRankingData();
    }

    private void setupRankingColumns() {
        rankNumberColumn.setSortable(false);
        rankNumberColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : "#" + (getIndex() + 1));
            }
        });

        districtNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        riskScoreColumn.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getRiskScore()));
        riskScoreColumn.setCellFactory(column ->
                formattedNumberCell(value -> FormatUtil.formatScore(value.doubleValue())));
        waterScarcityColumn.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getWaterScarcity()));
        waterScarcityColumn.setCellFactory(column ->
                formattedNumberCell(value -> FormatUtil.formatPercentage(value.doubleValue())));
        sanitationColumn.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getSanitationCoverage()));
        sanitationColumn.setCellFactory(column ->
                formattedNumberCell(value -> FormatUtil.formatPercentage(value.doubleValue())));
        populationColumn.setCellValueFactory(cell -> new SimpleLongProperty(cell.getValue().getPopulation()));
        populationColumn.setCellFactory(column ->
                formattedNumberCell(value -> FormatUtil.formatPopulation(value.longValue())));

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
    }

    private TableCell<District, Number> formattedNumberCell(Function<Number, String> formatter) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : formatter.apply(value));
            }
        };
    }

    private void setupObservableData() {
        filteredData = new FilteredList<>(masterData, district -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(rankingTableView.comparatorProperty());
        rankingTableView.setItems(sortedData);
        riskScoreColumn.setSortType(TableColumn.SortType.DESCENDING);
        rankingTableView.getSortOrder().setAll(riskScoreColumn);
    }

    private void loadRankingData() {
        if (districtService == null) return;
        Task<List<District>> task = new Task<>() {
            @Override
            protected List<District> call() throws Exception {
                return districtService.getRiskRanking();
            }
        };
        task.setOnSucceeded(event -> {
            List<District> ranked = task.getValue();
            masterData.setAll(ranked);
            applySearch(searchField.getText());

            if (ranked.size() >= 1) populatePodiumCard(ranked.get(0), rank1NameLabel, rank1ScoreLabel, rank1BadgeLabel, rank1MetricsLabel);
            if (ranked.size() >= 2) populatePodiumCard(ranked.get(1), rank2NameLabel, rank2ScoreLabel, rank2BadgeLabel, rank2MetricsLabel);
            if (ranked.size() >= 3) populatePodiumCard(ranked.get(2), rank3NameLabel, rank3ScoreLabel, rank3BadgeLabel, rank3MetricsLabel);
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            System.err.println("[ERROR] Failed to load risk ranking: " + error.getMessage());
            AlertUtil.showError("Data Load Error", "Could Not Load Risk Ranking",
                    "Unable to load district data: " + error.getMessage());
        });

        Thread loader = new Thread(task, "district-ranking-data-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void applySearch(String searchText) {
        String query = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
        filteredData.setPredicate(district ->
                query.isEmpty() || district.getName().toLowerCase(Locale.ROOT).contains(query));
    }

    private void populatePodiumCard(District district, Label nameLabel, Label scoreLabel, Label badgeLabel, Label metricsLabel) {
        nameLabel.setText(district.getName());
        scoreLabel.setText("Risk Score: " + FormatUtil.formatScore(district.getRiskScore()) + " / 100");
        metricsLabel.setText("Water risk deficit: " + FormatUtil.formatPercentage(district.getWaterScarcity())
                + " | Sanitation: " + FormatUtil.formatPercentage(district.getSanitationCoverage()));

        RiskLevel level = district.getRiskLevel();
        badgeLabel.setText(level.getLabel());
        badgeLabel.getStyleClass().removeAll("badge-low", "badge-moderate", "badge-high");
        if (level == RiskLevel.HIGH) badgeLabel.getStyleClass().add("badge-high");
        else if (level == RiskLevel.MODERATE) badgeLabel.getStyleClass().add("badge-moderate");
        else badgeLabel.getStyleClass().add("badge-low");
    }
}
