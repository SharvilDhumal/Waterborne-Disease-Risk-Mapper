package com.waterborne.controller;

import com.waterborne.service.DistrictService;
import com.waterborne.service.MockDistrictDataService;
import com.waterborne.util.AlertUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for MainLayout.fxml.
 * Handles sidebar navigation, view switching, header status updates, and view caching.
 */
public class MainLayoutController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Label viewTitleLabel;
    @FXML private Label statusLabel;

    @FXML private Button btnDashboard;
    @FXML private Button btnExplorer;
    @FXML private Button btnRanking;
    @FXML private Button btnVisualization;
    @FXML private Button btnDataTable;

    private DistrictService districtService;
    private final Map<String, Node> viewCache = new HashMap<>();
    private Button currentActiveButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Default to Mock service for standalone UI development/testing
        this.districtService = new MockDistrictDataService();

        // Load initial Dashboard view
        showDashboard(null);
    }

    public void setDistrictService(DistrictService districtService) {
        this.districtService = districtService;
    }

    public DistrictService getDistrictService() {
        return districtService;
    }

    @FXML
    public void showDashboard(ActionEvent event) {
        setActiveButton(btnDashboard, "Dashboard Overview");
        loadView("/fxml/DashboardView.fxml");
    }

    @FXML
    public void showDistrictExplorer(ActionEvent event) {
        setActiveButton(btnExplorer, "District Explorer & Risk Profile");
        loadView("/fxml/DistrictExplorerView.fxml");
    }

    @FXML
    public void showRiskRanking(ActionEvent event) {
        setActiveButton(btnRanking, "Risk Level Leaderboard & Ranking");
        loadView("/fxml/RiskRankingView.fxml");
    }

    @FXML
    public void showDataVisualization(ActionEvent event) {
        setActiveButton(btnVisualization, "Data Analytics & Visualizations");
        loadView("/fxml/DataVisualizationView.fxml");
    }

    @FXML
    public void showDataTable(ActionEvent event) {
        setActiveButton(btnDataTable, "District Risk Data Table");
        loadView("/fxml/DataTableView.fxml");
    }

    @FXML
    public void handleRefreshData(ActionEvent event) {
        viewCache.clear(); // Clear cached views on manual refresh
        if (currentActiveButton == btnDashboard) showDashboard(null);
        else if (currentActiveButton == btnExplorer) showDistrictExplorer(null);
        else if (currentActiveButton == btnRanking) showRiskRanking(null);
        else if (currentActiveButton == btnVisualization) showDataVisualization(null);
        else if (currentActiveButton == btnDataTable) showDataTable(null);
    }

    private void setActiveButton(Button button, String title) {
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("nav-button-active");
        }
        if (button != null) {
            button.getStyleClass().add("nav-button-active");
            currentActiveButton = button;
        }
        if (viewTitleLabel != null) {
            viewTitleLabel.setText(title);
        }
    }

    private void loadView(String fxmlPath) {
        try {
            Node viewNode = viewCache.get(fxmlPath);
            if (viewNode == null) {
                URL resource = getClass().getResource(fxmlPath);
                if (resource == null) {
                    AlertUtil.showError("Resource Error", "View Not Found", "Could not locate FXML view: " + fxmlPath);
                    return;
                }
                FXMLLoader loader = new FXMLLoader(resource);
                viewNode = loader.load();

                // Pass DistrictService reference to child controller if applicable
                Object controller = loader.getController();
                if (controller instanceof BaseController) {
                    ((BaseController) controller).initService(districtService);
                }

                viewCache.put(fxmlPath, viewNode);
            } else {
                // Refresh data on re-opening view
                Node node = viewCache.get(fxmlPath);
                // Child controller refresh hook can be invoked if needed
            }

            contentArea.getChildren().setAll(viewNode);
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("UI Error", "Failed to Load View", "Error rendering FXML layout: " + e.getMessage());
        }
    }
}
