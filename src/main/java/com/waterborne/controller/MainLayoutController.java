package com.waterborne.controller;

import com.waterborne.service.DistrictService;
import com.waterborne.config.DatabaseConfig;
import com.waterborne.util.AlertUtil;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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

    @FXML private BorderPane rootLayout;
    @FXML private VBox sidebar;
    @FXML private StackPane contentArea;
    @FXML private Label viewTitleLabel;
    @FXML private Label statusLabel;
    @FXML private Button btnToggleSidebar;
    @FXML private Button btnRevealSidebar;

    @FXML private Button btnDashboard;
    @FXML private Button btnExplorer;
    @FXML private Button btnRanking;
    @FXML private Button btnVisualization;
    @FXML private Button btnDataTable;

    private DistrictService districtService;
    private final Map<String, Node> viewCache = new HashMap<>();
    private Button currentActiveButton;
    private boolean sidebarVisible = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            DatabaseConfig.DataSource dataSource = DatabaseConfig.getDataSource();
            this.districtService = DatabaseConfig.createDistrictService(dataSource);
            if (statusLabel != null) {
                statusLabel.setText(switch (dataSource) {
                    case CENSUS_AND_NFHS5 -> "Census 2011 + NFHS-5 Data";
                    case MYSQL -> "MySQL Data Source Configured";
                    case MOCK -> "🟡 Mock Data (Explicit)";
                });
                statusLabel.getStyleClass().removeAll(
                        "system-status-online", "system-status-warning", "system-status-error");
                if (dataSource == DatabaseConfig.DataSource.MOCK) {
                    statusLabel.getStyleClass().add("system-status-warning");
                } else {
                    statusLabel.getStyleClass().add("system-status-online");
                }
            }
        } catch (IllegalStateException e) {
            if (statusLabel != null) {
                statusLabel.setText("🔴 Data source configuration error");
                statusLabel.getStyleClass().removeAll("system-status-online", "system-status-warning");
                statusLabel.getStyleClass().add("system-status-error");
            }
            AlertUtil.showError("Data Source Configuration Error", "Unable to Configure Data Source", e.getMessage());
            return;
        }

        // Load initial Dashboard view
        showDashboard(null);
        updateSidebarToggle();
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

    @FXML
    public void handleToggleSidebar(ActionEvent event) {
        sidebarVisible = !sidebarVisible;
        rootLayout.setLeft(sidebarVisible ? sidebar : null);
        updateSidebarToggle();
    }

    @FXML
    public void handleCloseSidebar(ActionEvent event) {
        sidebarVisible = false;
        rootLayout.setLeft(null);
        updateSidebarToggle();
    }

    @FXML
    public void handleRevealSidebar(Event event) {
        showSidebar();
    }

    private void showSidebar() {
        sidebarVisible = true;
        rootLayout.setLeft(sidebar);
        updateSidebarToggle();
    }

    private void updateSidebarToggle() {
        String action = sidebarVisible ? "Hide navigation sidebar" : "Show navigation sidebar";
        btnToggleSidebar.setTooltip(new Tooltip(action));
        btnToggleSidebar.setAccessibleText(action);
        btnRevealSidebar.setVisible(!sidebarVisible);
        btnRevealSidebar.setManaged(!sidebarVisible);
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
