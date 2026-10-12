# Waterborne Disease Risk Mapper

A JavaFX desktop application for exploring district-level water, sanitation, and population indicators across India. It combines Census 2011 data with NFHS-5 district factsheet estimates where records can be matched, then calculates a transparent **risk indicator score** to help users compare and prioritize districts.

> **Important:** This score is an analytical indicator derived from water, sanitation, and population metrics. It is not a measured count or probability of waterborne disease cases, and it should not be treated as a clinical or epidemiological prediction.

## Contents

- [What the application does](#what-the-application-does)
- [Data used and how it is combined](#data-used-and-how-it-is-combined)
- [Parameters and risk-score calculation](#parameters-and-risk-score-calculation)
- [Technology and project structure](#technology-and-project-structure)
- [Requirements](#requirements)
- [Run the application](#run-the-application)
- [Optional MySQL data source](#optional-mysql-data-source)
- [Build and verification](#build-and-verification)
- [Interpretation and limitations](#interpretation-and-limitations)

## What the application does

The application provides five screens, available from the navigation sidebar:

1. **Dashboard Overview**
   - Shows the number of districts and counts in the high-, moderate-, and low-risk categories.
   - Highlights the highest-scoring district and summarizes its key indicators.
   - Presents average risk score and risk-category distribution.
   - The district risk bar chart shows the ten highest-scoring districts.

2. **District Explorer**
   - Lets users search and select a district from the district/state list.
   - Displays the score, risk category, water-risk deficit, sanitation coverage, population density, and population.
   - Shows the available Census and NFHS-5 metrics for comparison and provides an automatically generated interpretation of the selected district.

3. **Risk Ranking**
   - Displays the top three districts as a podium and all districts in a ranked leaderboard.
   - Supports district-name search and shows risk scores, categories, risk metrics, and population.

4. **Data Visualization**
   - Shows the top ten districts by risk score in a bar chart.
   - Shows the distribution of districts across risk categories.
   - Plots all loaded districts in scatter charts for water-risk deficit versus score and sanitation coverage versus score.

5. **District Data Table**
   - Provides a sortable table of district-level values.
   - Supports text search, risk-category filtering, and selectable sort orders.
   - Displays both the applied risk metrics and source-specific Census/NFHS-5 measures where available.

The sidebar can be closed with its **×** button. A slim tab appears at the left edge; hover over it to reveal the sidebar again. The header toggle also hides or restores navigation.

## Data used and how it is combined

The default data source is bundled with the project; the application does **not** require a running MySQL server or database password.

| File | Contents used |
| --- | --- |
| [`Backend/india-districts-census-2011.csv`](Backend/india-districts-census-2011.csv) | Census district code, state, district, population, household count, households with an on-premises latrine, and households reporting tap water or tubewell/borehole as their main drinking-water source. |
| [`Backend/NFHS_5_India_Districts_Factsheet_Data.xls`](Backend/NFHS_5_India_Districts_Factsheet_Data.xls) | State and district names, percentage of the population living in households with an improved drinking-water source, and percentage living in households using an improved sanitation facility. |

The repository reads **640 Census district records** and attempts to match NFHS-5 records by both state and district. Matching normalizes capitalization and punctuation and accounts for known spelling variants. The current bundled files produce **618 matches**. NFHS values are applied only when both geographic keys match; the remaining records keep their Census-derived values. District-name-only matching is deliberately avoided because district names can repeat across states.

For a matched district, the risk score uses the NFHS-5 improved-water and improved-sanitation percentages together with Census population and density. For an unmatched district, the score uses Census household-based water and sanitation proxies and Census population and density. The application retains and displays source-specific values to make the origin of the applied metrics visible.

The sources use different definitions and populations:

- **Census water proxy:** uses households reporting tap water or tubewell/borehole as their main drinking-water source. This is not the NFHS improved-water indicator.
- **NFHS-5 water:** population living in households with an improved drinking-water source.
- **Census sanitation proxy:** households reporting an on-premises latrine facility.
- **NFHS-5 sanitation:** population living in households using an improved sanitation facility.

These values are shown for context, but differences between them are **not** a like-for-like change over time.

Maven packages the two source files into the application resources so they can be loaded at runtime without relying on the current working directory.

## Parameters and risk-score calculation

The application uses the following input and derived parameters:

| Parameter | Meaning and calculation |
| --- | --- |
| **Water-risk deficit (%)** | For NFHS-matched records: `100 − NFHS-5 improved drinking-water (%)`. Otherwise: `100 − Census households with tap water or tubewell/borehole (%)`. |
| **Sanitation coverage (%)** | For NFHS-matched records: NFHS-5 improved-sanitation percentage. Otherwise: Census households with an on-premises latrine as a percentage of households. |
| **Population** | Census 2011 district population. |
| **Population density (people/km²)** | In the bundled Census CSV repository, derived as `population ÷ (households × 0.05)` and rounded to one decimal place. This is an application-derived estimate, not an independently supplied district area measurement. |
| **Density index (0–100)** | `min(100, population density ÷ 1500 × 100)`. |

The risk score is calculated as:

```text
Risk score =
    0.45 × water-risk deficit
  + 0.35 × (100 − sanitation coverage)
  + 0.20 × density index
```

The result is rounded to one decimal place. Risk categories are assigned using these thresholds:

| Score | Category |
| --- | --- |
| `< 40` | Low |
| `40–69.9` | Moderate |
| `≥ 70` | High |

The weights and thresholds are project-defined prioritization rules. They are not validated estimates of disease incidence.

## Technology and project structure

- **Java 17**
- **JavaFX 17** for the desktop UI, charts, FXML layouts, and observable table controls
- **Maven** for dependency management, resource packaging, and execution
- **Apache POI** for reading the bundled NFHS-5 Excel workbook
- **MySQL Connector/J** for the optional MySQL source

```text
.
├── Backend/                         # Bundled Census CSV and NFHS-5 workbook
├── database/
│   └── schema.sql                   # Optional MySQL schema and seed data
├── documentation/                   # Project reports and documentation
├── src/main/java/com/waterborne/
│   ├── algorithm/                   # Risk calculation and ranking algorithms
│   ├── config/                      # Data-source selection and wiring
│   ├── controller/                  # JavaFX screen controllers
│   ├── model/                       # District and risk-statistics models
│   ├── repository/                  # Census/NFHS and MySQL data access
│   ├── service/                     # District service layer
│   └── util/                        # Formatting and UI helpers
└── src/main/resources/
    ├── css/                         # Application styling
    └── fxml/                        # JavaFX screen layouts
```

## Requirements

- JDK 17 or later, with `JAVA_HOME` configured.
- Apache Maven installed and available as `mvn`.
- A graphical desktop session to display the JavaFX application.

No Python environment, MySQL server, or database credentials are needed for the default bundled data source.

## Run the application

From the repository root:

```powershell
mvn clean javafx:run
```

On Windows, the included PowerShell script can also be used:

```powershell
.\run.ps1
```

Or run `run.bat` from Command Prompt. The default value of `WATERBORNE_DATA_SOURCE` is the combined Census/NFHS-5 source. The accepted source values are:

| Value | Behavior |
| --- | --- |
| `census` (default) | Loads Census 2011 and overlays matched NFHS-5 metrics. |
| `csv`, `nfhs`, `census+nfhs` | Aliases for the combined Census/NFHS-5 source. |
| `mysql` | Loads records from the configured MySQL database. |
| `mock` | Uses sample data for UI development. |

PowerShell examples:

```powershell
# Default: bundled Census and NFHS-5 files
mvn javafx:run

# Explicitly choose the bundled source
$env:WATERBORNE_DATA_SOURCE = "census"
mvn javafx:run

# Optional: use MySQL (configure the variables before launch)
$env:WATERBORNE_DATA_SOURCE = "mysql"
$env:WATERBORNE_DB_USER = "your_mysql_user"
$env:WATERBORNE_DB_PASSWORD = "your_mysql_password"
mvn javafx:run

# Optional: use mock data for UI development
$env:WATERBORNE_DATA_SOURCE = "mock"
mvn javafx:run
```

MySQL credentials are read from environment variables and are not stored in source code. `WATERBORNE_DB_URL` can optionally override the default URL `jdbc:mysql://localhost:3306/waterborne_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`.

## Optional MySQL data source

The bundled Census/NFHS source is recommended for a simple, password-free run. To use MySQL instead:

1. Install and start MySQL Server.
2. Load [`database/schema.sql`](database/schema.sql) into MySQL. In PowerShell, use `Get-Content database\schema.sql | mysql -u your_mysql_user -p`; this avoids Bash-style `<` input redirection.
3. Set `WATERBORNE_DATA_SOURCE=mysql`, `WATERBORNE_DB_USER`, and `WATERBORNE_DB_PASSWORD` in the environment.
4. Start the application with `mvn javafx:run`.

The server must be running and reachable at the configured URL. The SQL file recreates and populates the `waterborne_db` schema, so inspect it before running it against a database containing data you need to preserve.

## Build and verification

Compile and run the available tests:

```powershell
mvn clean test
```

At the time of writing, the project contains no automated test sources, so Maven's successful test phase confirms compilation but reports that there are no tests to execute.

## Interpretation and limitations

- This is a **district comparison and prioritization dashboard**, not a disease surveillance system. No disease-case counts, incidence rates, pathogen measurements, or outbreak outcomes are used in the bundled-data score.
- Census household proxies and NFHS-5 population-based estimates are not identical indicators. The application uses NFHS values for matched districts as requested, while retaining Census-derived fallback values for unmatched districts.
- The Census CSV path derives density using `households × 0.05` as an area proxy. Treat the resulting density and density-weighted score as an approximation, not official population per square kilometre.
- A risk category depends on the chosen weights, density scaling, and cutoffs. It indicates relative values under this formula and does not establish causal relationships or guarantee that a district has a particular level of disease.
- The files represent their respective survey/census periods; they should not be interpreted as current district conditions without newer validated data.

## Team

College PBL mini project — Waterborne Disease Risk Mapper.
