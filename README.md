# Waterborne Disease Risk Mapper

> **College PBL Mini Project — JavaFX Desktop Application**  
> *A data analytics dashboard for visualizing district-level waterborne disease risk across environmental and public-health indicators.*

---

## 📌 Features

- **Dashboard Overview:** Key Performance Indicator (KPI) cards, spotlight risk alert, bar charts, and pie charts.
- **District Explorer:** Searchable district selection list, risk factor progress bars (Water Scarcity %, Sanitation Coverage %, Density), and automated risk interpretation text generator.
- **Risk Ranking Leaderboard:** Rank #1, #2, #3 podium spotlight cards and ordered leaderboards powered by custom QuickSort / MergeSort algorithms.
- **Visual Analytics Suite:** JavaFX BarChart, PieChart, and 2 ScatterCharts (Scarcity vs Risk, Sanitation vs Risk) with interactive tooltips.
- **Interactive Data Table:** Filterable, sortable `TableView` with color-coded risk level badge pills (`LOW`, `MODERATE`, `HIGH`).
- **Decoupled Architecture:** Clean service/repository interfaces allowing seamless integration with MySQL JDBC database (Student 2) and DSA algorithms (Student 1).

---

## 📁 Repository Folder Structure

```
waterborne-disease-mapper/
├── pom.xml                                  # Maven build configuration
├── .gitignore                               # Specifies untracked files
├── run.bat / run.ps1                        # 1-click execution scripts
├── src/                                     # Application source code
│   ├── main/
│   │   ├── java/com/waterborne/             # Java source code (Controllers, Models, Services)
│   │   └── resources/                       # FXML screens & CSS stylesheet
└── documentation/                           # College PBL documentation suite
    ├── CASE_STUDY_11_POINT_REPORT.md        # 11-point case study report
    ├── WEEKLY_PBL_LOGBOOK.md                # Weekly progress logbook
    └── TEST_SCENARIOS.md                    # Verification test suite
```

---

## 🚀 How to Run

### Quick Launch (Windows)
Double-click `run.bat` or run in Command Prompt:
```cmd
.\run.bat
```

### Standard Maven Command
```bash
mvn clean javafx:run
```

---

## 👥 Team Responsibilities
- **Student 1 (Data & Algorithm Engineer):** Risk score calculation, QuickSort/MergeSort ranking algorithms, and search algorithms.
- **Student 2 (Backend & Database Developer):** MySQL database design, JDBC connectivity, CRUD operations, and multithreading.
- **Student 3 (Frontend & Documentation Lead):** JavaFX UI dashboard, TableView, FXML layouts, CSS styling, visual analytics, documentation, and case study report.
