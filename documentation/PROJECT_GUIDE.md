# Waterborne Disease Risk Mapper — Project Guide

This guide explains the project components, where its data comes from, how the
application transforms and presents that data, and how to run it. For the
complete setup instructions and detailed indicator caveats, see the
[README](../README.md).

## 1. What the project does

This is a Java desktop dashboard for comparing district-level water and
sanitation indicators with population data. It calculates a weighted risk
indicator and presents district rankings, summaries, and charts. Separate
screens explore water-quality monitoring and country/region disease data.

The score is a project-defined comparison indicator. It is **not** a count or
prediction of disease cases, and the disease dataset does not feed into the
district score.

## 2. Technology used

| Component | Purpose |
| --- | --- |
| Java 17 | Application logic and data processing |
| JavaFX 17 | Desktop windows, controls, tables, and charts |
| FXML | Declarative layouts for the main window and screen views |
| CSS | Shared visual styling |
| Maven | Dependency management, compilation, resource packaging, and launch |
| Apache POI | Reading the NFHS-5 Excel workbook |
| MySQL Connector/J | Optional JDBC connection to MySQL |
| Python and pandas | Optional script that generates a MySQL schema/seed file from Census CSV |

The normal bundled-data run does not require Python or a MySQL server.

## 3. Data files and what they contribute

All four files are in `Backend/`. Maven copies them into the application's
classpath under `/data/`, so the running application reads packaged resources
instead of depending on its current working directory.

| Source file | Data used | Application use |
| --- | --- | --- |
| `india-districts-census-2011.csv` | District code, state, district, population, households, on-premises latrine households, and tap/tubewell/borehole households | Base district records and fallback water/sanitation indicators |
| `NFHS_5_India_Districts_Factsheet_Data.xls` | State, district, improved drinking-water percentage, improved sanitation percentage | Overlays the two indicators for matched districts |
| `Water_pond_tanks_2021.csv` | Indian monitoring station/location, state, water-body type, and min/max water measurements | Independent station-level water-quality analysis |
| `water_pollution_disease.csv` | Country, region, year, water-source type, contaminant, clean-water and sanitation access, and disease rates per 100,000 | Independent disease/environment analysis |

The bundled Census file has 640 district records. The Census/NFHS loader matches
records by **both state and district**, after normalizing names and known
variants; the current bundled files yield 618 matches. Census values remain in
the record for comparison and are used as fallback when no NFHS match exists.
Water-quality and disease records do not have compatible district keys, so
they are not merged into this district score.

### District indicator derivation

- **Census water-risk deficit:** `100 − (tap + tubewell/borehole households) ÷ households × 100`
- **Census sanitation coverage:** `latrine households ÷ households × 100`
- **Population density proxy:** `population ÷ (households × 0.05)`
- **NFHS water-risk deficit:** `100 − improved drinking-water coverage`
- **NFHS sanitation coverage:** improved sanitation coverage

Percentages are bounded to 0–100. The density proxy is an application estimate,
not official persons per square kilometre. NFHS measures describe populations
living in households, while Census fallback measures are household-based; they
are not identical indicators.

## 4. How the risk score is calculated

The combined Census/NFHS source uses NFHS water and sanitation values where
matched and Census-derived values otherwise. Both use Census population and
the derived density proxy.

```text
density index = min(100, population density ÷ 1500 × 100)

risk score =
    0.45 × water-risk deficit
  + 0.35 × (100 − sanitation coverage)
  + 0.20 × density index
```

The result is rounded to one decimal place:

| Score | Category |
| --- | --- |
| Below 40 | Low |
| 40 to below 70 | Moderate |
| 70 or higher | High |

The weights, density scaling, and cutoffs are project rules, not validated
epidemiological thresholds.

## 5. How data moves through the application

```text
Backend data files
        ↓ Maven packages them as /data/ resources
MainApp loads MainLayout.fxml and the shared CSS
        ↓
MainLayoutController selects a data source and creates DistrictService
        ↓
Repository reads source data and builds model records
        ↓
DistrictService and RiskCalculatorService provide statistics,
search/filter/sort operations, and district rankings
        ↓
JavaFX controllers load data and update labels, charts, lists, and tables
```

District screens share the configured `DistrictService`. Data loading for
several screens runs in JavaFX background tasks so file/database work does not
block normal UI interaction. The water-quality and disease screens use their
own repositories and services, separate from the district scoring path.

The CSV resource reader supports quoted fields, multiline CSV values, UTF-8,
and Windows-1252 fallback. Numeric values that are blank or invalid in the
independent datasets are represented as missing and omitted from that measure's
averages.

## 6. What each view displays

The project contains seven FXML view/controller pairs. **Five are connected to
the main sidebar** in `MainLayout.fxml`; the Data Table and Risk Ranking views
are implemented but are not currently wired to a sidebar button.

| View | What it shows |
| --- | --- |
| Dashboard Overview | District total, low/moderate/high counts, average score, highest-risk district, top-ten score and deficit charts, and category distribution |
| District Explorer | Search by district/state; selected district's score, category, water deficit, sanitation coverage, density, population, and source indicators |
| Data Visualization | District score and category charts plus comparisons of water deficit and sanitation coverage with score |
| Water Quality Analysis | Monitoring stations filtered by state/type/search; pH, dissolved oxygen, BOD, coliform, water-body summaries, charts, and station measurements |
| Disease & Environmental Analysis | Country/region/year/water-source filters; disease-rate, clean-water, sanitation, and contaminant summaries/charts |
| District Data Table | Sortable/searchable district records, risk-category filter, applied metrics, and Census/NFHS source values |
| Risk Ranking | Score-ranked district leaderboard, search, risk badges, and top-three podium |

The water-quality high-coliform count marks filtered stations at or above the
75th percentile of fecal or total coliform for the full water-quality dataset.
It is a relative indicator, not a regulatory safety determination. The disease
screen presents the records in its CSV at country/region level and does not
validate the district risk categories.

## 7. Main code and resource map

```text
src/main/java/com/waterborne/
├── MainApp.java                 # JavaFX application entry point
├── algorithm/                   # Weighted risk score, classification, ranking, search
├── config/                      # Data source selection and service wiring
├── controller/                  # FXML controllers and display logic
├── model/                       # District, risk statistics, water and disease records
├── repository/                  # CSV/Excel and optional MySQL data access
├── service/                     # District, water-quality, and disease operations
└── util/                        # CSV parsing, formatting, and alerts

src/main/resources/
├── fxml/                        # Main layout and screen layouts
└── css/style.css                # Shared application styling

database/
├── schema.sql                   # Optional MySQL database and Census seed data
└── ingest_census_csv.py         # Generates SQL from the Census CSV

documentation/                   # This guide, reports, scenarios, and logbook
```

## 8. Data source choices and running

`WATERBORNE_DATA_SOURCE` selects the district source:

- `census` (default), `csv`, `nfhs`, or `census+nfhs`: bundled Census plus
  matched NFHS-5 data.
- `mysql`: MySQL district table, configured with `WATERBORNE_DB_URL`,
  `WATERBORNE_DB_USER`, and `WATERBORNE_DB_PASSWORD`.
- `mock`: sample district data for UI development.

Launch from the project root with:

```powershell
mvn clean javafx:run
```

Alternatively, run `.\run.ps1` or `run.bat` on Windows. The bundled Census/NFHS
source is read-only. The MySQL setup script drops and recreates its `districts`
table, so review `database/schema.sql` before running it on a database with
data you need to keep. The SQL seed contains Census-derived values; it does not
include the bundled NFHS overlay.

## 9. Important interpretation limits

- Census records are from 2011; NFHS-5 and the other bundled sources cover their
  own survey/monitoring periods, not necessarily current conditions.
- The density calculation is a proxy derived from household count.
- Matched NFHS and unmatched Census indicators use different definitions.
- The score does not use disease outcomes, measured pathogen data, or outbreak
  records, and it does not establish cause and effect.
- Water-quality stations and disease records remain separate analyses because
  their geographic units do not match district records.
