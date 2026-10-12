package com.waterborne.repository;

import com.waterborne.model.District;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Combines Census 2011 population data with matched NFHS-5 water and sanitation estimates.
 */
public class CensusAndNfhs5DistrictRepository implements DistrictRepository {

    private static final String NFHS_RESOURCE = "/data/NFHS_5_India_Districts_Factsheet_Data.xls";
    private static final String DISTRICT_NAME_HEADER = "District Names";
    private static final String STATE_NAME_HEADER = "State/UT";
    private static final String IMPROVED_WATER_HEADER =
            "Population living in households with an improved drinking-water source1 (%)";
    private static final String IMPROVED_SANITATION_HEADER =
            "Population living in households that use an improved sanitation facility2 (%)";

    private final CensusCsvDistrictRepository censusRepository = new CensusCsvDistrictRepository();

    @Override
    public List<District> findAll() throws Exception {
        List<District> districts = censusRepository.findAll();
        Map<String, District> censusByLocation = new HashMap<>();
        for (District district : districts) {
            String key = locationKey(district.getState(), district.getName());
            if (censusByLocation.putIfAbsent(key, district) != null) {
                throw new IOException("Census dataset contains duplicate state and district: "
                        + district.getState() + " / " + district.getName());
            }
        }

        int matchedDistricts = applyNfhsMetrics(censusByLocation);
        if (matchedDistricts == 0) {
            throw new IOException("No districts matched between Census 2011 and NFHS-5 datasets.");
        }
        return districts;
    }

    private int applyNfhsMetrics(Map<String, District> censusByLocation) throws IOException {
        InputStream input = getClass().getResourceAsStream(NFHS_RESOURCE);
        if (input == null) {
            throw new IOException("Bundled NFHS-5 workbook was not found: " + NFHS_RESOURCE);
        }

        int matchedDistricts = 0;
        try (InputStream workbookStream = input; Workbook workbook = WorkbookFactory.create(workbookStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IOException("NFHS-5 workbook contains no worksheets.");
            }

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IOException("NFHS-5 worksheet has no header row.");
            }

            DataFormatter formatter = new DataFormatter(Locale.US);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Map<String, Integer> columns = indexColumns(headerRow, formatter, evaluator);
            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String districtName = cellText(row.getCell(columns.get(DISTRICT_NAME_HEADER)), formatter, evaluator).trim();
                String stateName = cellText(row.getCell(columns.get(STATE_NAME_HEADER)), formatter, evaluator).trim();
                if (districtName.isEmpty() && stateName.isEmpty()) {
                    continue;
                }
                if (districtName.isEmpty() || stateName.isEmpty()) {
                    throw new IOException("NFHS-5 has an incomplete district key at worksheet row "
                            + (rowIndex + 1) + ".");
                }

                District district = censusByLocation.get(locationKey(stateName, districtName));
                if (district == null) {
                    continue;
                }

                double improvedWater = percentage(
                        row.getCell(columns.get(IMPROVED_WATER_HEADER)), formatter, evaluator,
                        IMPROVED_WATER_HEADER, rowIndex);
                double improvedSanitation = percentage(
                        row.getCell(columns.get(IMPROVED_SANITATION_HEADER)), formatter, evaluator,
                        IMPROVED_SANITATION_HEADER, rowIndex);
                district.applyNfhs5Metrics(improvedWater, improvedSanitation);
                matchedDistricts++;
            }
        }
        return matchedDistricts;
    }

    private Map<String, Integer> indexColumns(Row headerRow, DataFormatter formatter,
                                               FormulaEvaluator evaluator) throws IOException {
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : headerRow) {
            columns.put(cellText(cell, formatter, evaluator).trim(), cell.getColumnIndex());
        }
        String[] requiredHeaders = {
                DISTRICT_NAME_HEADER,
                STATE_NAME_HEADER,
                IMPROVED_WATER_HEADER,
                IMPROVED_SANITATION_HEADER
        };
        for (String header : requiredHeaders) {
            if (!columns.containsKey(header)) {
                throw new IOException("NFHS-5 workbook is missing required column: " + header);
            }
        }
        return columns;
    }

    private double percentage(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator,
                              String column, int rowIndex) throws IOException {
        try {
            double percentage;
            if (cell == null) {
                throw new NumberFormatException("Cell is empty.");
            } else if (cell.getCellType() == CellType.NUMERIC) {
                percentage = cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.FORMULA) {
                CellValue evaluated = evaluator.evaluate(cell);
                if (evaluated == null || evaluated.getCellType() != CellType.NUMERIC) {
                    throw new NumberFormatException("Formula does not produce a numeric value.");
                }
                percentage = evaluated.getNumberValue();
            } else {
                String value = cellText(cell, formatter, evaluator).trim().replace(",", "");
                if (value.endsWith("%")) {
                    value = value.substring(0, value.length() - 1).trim();
                }
                percentage = Double.parseDouble(value);
            }
            if (!Double.isFinite(percentage) || percentage < 0.0 || percentage > 100.0) {
                throw new NumberFormatException("Percentage is outside [0, 100].");
            }
            return percentage;
        } catch (NumberFormatException e) {
            String value = cellText(cell, formatter, evaluator).trim();
            throw new IOException("Invalid NFHS-5 percentage in " + column + " at worksheet row "
                    + (rowIndex + 1) + ": " + value, e);
        }
    }

    private String cellText(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        return cell == null ? "" : formatter.formatCellValue(cell, evaluator);
    }

    private String locationKey(String state, String district) {
        return normalize(state) + "|" + normalize(district);
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value.replace("&", " and "), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        return switch (normalized) {
            case "maharastra" -> "maharashtra";
            case "orissa" -> "odisha";
            case "uttaranchal" -> "uttarakhand";
            case "pondicherry" -> "puducherry";
            default -> normalized;
        };
    }

    @Override
    public Optional<District> findByName(String name) throws Exception {
        return findAll().stream()
                .filter(district -> district.getName().equalsIgnoreCase(name == null ? "" : name.trim()))
                .findFirst();
    }

    @Override
    public boolean save(District district) {
        throw new UnsupportedOperationException("The bundled Census/NFHS datasets are read-only.");
    }

    @Override
    public boolean update(District district) {
        throw new UnsupportedOperationException("The bundled Census/NFHS datasets are read-only.");
    }

    @Override
    public boolean deleteById(int id) {
        throw new UnsupportedOperationException("The bundled Census/NFHS datasets are read-only.");
    }
}
