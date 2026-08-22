# COLLEGE PBL MINI PROJECT REPORT
## Waterborne Disease Risk Mapper

---

### 1. Title of the Project
**Waterborne Disease Risk Mapper: JavaFX & MySQL-Based Environmental Public Health Risk Analytics**

### 2. Abstract
Waterborne disease outbreaks remain a critical public health challenge across urban and rural districts. This project presents a full-featured desktop decision-support application developed in Java 17, JavaFX, and MySQL. It ingests district environmental indicators—including water scarcity %, sanitation coverage %, population, and density—to compute an empirical Disease Risk Score (0–100) and classify districts into LOW, MODERATE, and HIGH risk tiers. The system features a modern JavaFX dashboard, side navigation shell, sortable data table, district profile inspector, leaderboard ranking, visual analytics charts (Bar, Pie, Scatter), and multithreaded database integration.

### 3. Problem Statement
Public health agencies struggle to visually prioritize intervention resources across geographical districts due to fragmented datasets. Without unified analytics correlating water scarcity deficits and sanitation coverage with population density, high-risk districts fail to receive timely preventive measures.

### 4. Objectives
- Develop an intuitive, modern JavaFX desktop UI with KPI overview cards, sortable tables, and charts.
- Implement decoupled architecture allowing modular integration between UI (Student 3), Database (Student 2), and DSA/Risk Algorithms (Student 1).
- Perform district risk score calculation and dynamic visual categorization into LOW, MODERATE, and HIGH risk levels.
- Provide multi-criteria filtering, case-insensitive partial search, and leaderboard risk ranking.

### 5. System Architecture & Design
The application enforces strict Model-View-Controller (MVC) separation:
- **Presentation Layer (Student 3):** `MainApp.java`, `MainLayout.fxml`, `DashboardView.fxml`, `DistrictExplorerView.fxml`, `RiskRankingView.fxml`, `DataVisualizationView.fxml`, `DataTableView.fxml`, and `style.css`.
- **Service & Interface Layer:** Service abstractions (`DistrictService`, `MockDistrictDataService`, `DistrictServiceImpl`) for asynchronous non-blocking JavaFX UI operation using `javafx.concurrent.Task`.
- **Algorithm & Analysis Layer (Student 1):** `RiskCalculatorService` & `RiskCalculatorServiceImpl` using custom QuickSort and weighted scoring logic.
- **Database & Persistence Layer (Student 2):** `DistrictRepository` & `DistrictRepositoryImpl` for MySQL JDBC operations.

### 6. Data Structures & Algorithms Used
- **Custom QuickSort Algorithm:** Implemented in `RiskCalculatorServiceImpl` for sorting districts by Risk Score (Descending/Ascending) in $O(n \log n)$ average time.
- **Searching Algorithms:** Case-insensitive string matching for live district lookup.
- **Statistical Aggregation:** Dynamic computation of mean risk score, total districts, risk level counts, and min/max extremes via `RiskStatistics.java`.

### 7. Modules Description
- **Module 1 — Dashboard View:** Landing page displaying 4 KPI summary cards, Highest Risk District Spotlight Card, Average Score Indicator, Bar Chart, and Pie Chart.
- **Module 2 — District Explorer:** Detailed inspector view presenting district profiles, progress bars for scarcity, sanitation, and density, and automated risk text.
- **Module 3 — Data Table:** Interactive JavaFX `TableView` with color-coded risk level badges (`#FEE2E2` High, `#FEF3C7` Moderate, `#D1FAE5` Low).
- **Module 4 — Risk Ranking:** Ordered leaderboards highlighting Rank #1, #2, #3 podium spotlight districts.
- **Module 5 — Visual Analytics Suite:** Multi-chart suite visualizing water scarcity vs risk score and sanitation coverage vs risk score.

### 8. User Interface (UI/UX) Design
Designed following modern flat-design principles using a dark navy sidebar `#0F172A`, light background `#F1F5F9`, card-based metrics, custom progress bars, and high-contrast badge pills (`#10B981` Low, `#F59E0B` Moderate, `#EF4444` High).

### 9. Database & JDBC Integration
Database schema stores district records with attributes (`id`, `name`, `water_scarcity`, `sanitation_coverage`, `population`, `population_density`, `risk_score`, `risk_level`). Implemented JDBC repository query methods with seamless offline mock fallback.

### 10. Testing & Verification
System verified against 7 core test scenarios including empty search, filtering by risk level, window resizing, and database fallback to local mock data.

### 11. Conclusion & Future Scope
The Waterborne Disease Risk Mapper effectively demonstrates full-stack Java Desktop engineering, combining data structures, database integration, and GUI design. Future extensions include GIS map layer overlays, time-series forecasting, and automated alert notifications.
