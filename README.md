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
├── database/                                # Database scripts (Student 2)
│   └── schema.sql                           # MySQL table creation & seed data
├── src/                                     # Application source code
│   ├── main/
│   │   ├── java/com/waterborne/             # Java source code
│   │   │   ├── algorithm/                   # Student 1: DSA & QuickSort ranking
│   │   │   ├── controller/                  # Student 3: JavaFX FXML controllers
│   │   │   ├── model/                       # Data entities & statistics
│   │   │   ├── repository/                  # Bundled Census CSV and optional JDBC repositories
│   │   │   ├── service/                     # Business logic and data-source services
│   │   │   ├── util/                        # Formatters & UI alerts
│   │   │   └── MainApp.java                 # Main application launcher
│   │   └── resources/
│   │       ├── css/style.css                # Custom UI styling
│   │       └── fxml/                        # FXML screen layouts
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

### Data Source
The application uses the bundled Census 2011 CSV and NFHS-5 workbook by default. It requires no MySQL installation, database setup, or password; both data files are packaged into the application by Maven.

```powershell
mvn clean javafx:run
```

The app joins NFHS-5 to Census 2011 using normalized district and state names (including known state-name spelling aliases). The current files yield exact state-and-district matches for 618 districts; the remaining 22 Census districts retain Census-derived risk metrics. Unmatched districts are not guessed or merged across state boundaries. For matched districts, risk scoring uses NFHS-5 improved drinking-water and improved sanitation percentages, together with Census population and density.

The indicators are related but not identical: Census water deficit is based only on tap-water and tubewell/borehole household counts, while NFHS-5 reports population living in households with an improved drinking-water source. Census sanitation uses households with an on-premises latrine, while NFHS-5 reports population using an improved sanitation facility. The District Explorer and Data Table show both values for comparison. In the matched district set, the mean NFHS-minus-Census difference is +59.02 percentage points for improved water access versus the Census tap/borehole proxy, and +38.75 points for improved sanitation versus the Census latrine proxy; these differences reflect the distinct definitions and survey populations, not a like-for-like trend.

MySQL remains an optional data source when configured with `WATERBORNE_DATA_SOURCE=mysql`, `WATERBORNE_DB_USER`, and `WATERBORNE_DB_PASSWORD`. For UI development, explicitly select the sample data with `WATERBORNE_DATA_SOURCE=mock`. If unset, `WATERBORNE_DATA_SOURCE` defaults to the combined Census/NFHS source.

---

## 👥 Team Responsibilities
- **Student 1 (Data & Algorithm Engineer):** Risk score calculation, QuickSort/MergeSort ranking algorithms, and search algorithms.
- **Student 2 (Backend & Database Developer):** MySQL database design, JDBC connectivity, CRUD operations, and multithreading.
- **Student 3 (Frontend & Documentation Lead):** JavaFX UI dashboard, TableView, FXML layouts, CSS styling, visual analytics, documentation, and case study report.
