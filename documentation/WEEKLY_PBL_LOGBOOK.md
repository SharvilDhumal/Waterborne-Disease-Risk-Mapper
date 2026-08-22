# WEEKLY PBL DEVELOPMENT LOGBOOK
**Student Name:** Student 3 (Frontend & Documentation Lead)  
**Project Title:** Waterborne Disease Risk Mapper  

---

### Week 1
**Date:** August 22, 2026  

#### Work Planned:
- Inspect project architecture requirements and establish modular directory structure.
- Define decoupled interface contracts between Frontend (Student 3), Database (Student 2), and Algorithms (Student 1).
- Build main layout navigation shell, FXML screens, controllers, and custom CSS theme.
- Implement 5 core screens: Dashboard, District Explorer, Risk Ranking, Visual Analytics, Data Table.
- Finalize 11-Point Case Study Report, Weekly Logbook, and Test Scenarios.

#### Work Completed:
- Architected clean Maven project structure at `waterborne-disease-mapper`.
- Developed core Java domain models (`District.java`, `RiskLevel.java`, `RiskStatistics.java`).
- Created decoupled interfaces (`DistrictRepository.java`, `RiskCalculatorService.java`, `DistrictService.java`) along with reference implementations (`DistrictRepositoryImpl.java`, `RiskCalculatorServiceImpl.java`, `MockDistrictDataService.java`, `DistrictServiceImpl.java`).
- Built `MainLayout.fxml` with dark navy sidebar (`#0F172A`), active button styling, top header bar, status badge, and FXML view caching.
- Built `DashboardView.fxml` featuring 4 KPI cards, Highest Risk Spotlight card, Average Score indicator, BarChart, and PieChart.
- Built `DataTableView.fxml` featuring live search, Risk Level dropdown filter, multi-column sorting, and color-coded risk badge pill cells.
- Built `DistrictExplorerView.fxml` featuring live search list, factor progress bars (Water Scarcity %, Sanitation %, Density), and automated risk text generator.
- Built `RiskRankingView.fxml` featuring Top 3 Podium Cards (#1 Critical Danger, #2, #3) and full ranked leaderboard table.
- Built `DataVisualizationView.fxml` featuring BarChart, PieChart, and 2 ScatterCharts (Scarcity vs Risk, Sanitation vs Risk) with tooltips.
- Created modern stylesheet (`style.css`), `FormatUtil.java`, and `AlertUtil.java`.
- Finalized comprehensive documentation: `CASE_STUDY_11_POINT_REPORT.md`, `WEEKLY_PBL_LOGBOOK.md`, and `TEST_SCENARIOS.md`.

#### Technical Contribution:
- Authored the complete JavaFX frontend UI architecture, FXML layouts, CSS styling, non-blocking asynchronous `javafx.concurrent.Task` controllers, and project documentation.

#### Problems Encountered:
- Ensuring the UI remains responsive and functional regardless of whether MySQL database is online or offline.

#### Solution:
- Created an automatic fallback layer in `DistrictServiceImpl` that degrades gracefully to `MockDistrictDataService` when MySQL is offline.

#### Learning:
- Mastered JavaFX modular UI design, custom table cell rendering, chart binding, asynchronous multithreaded UI updates, and decoupled software design.

#### Next Week Plan:
- Assist team members (Student 1 & Student 2) in connecting live MySQL database tables and custom algorithm benchmarks into the completed UI.
