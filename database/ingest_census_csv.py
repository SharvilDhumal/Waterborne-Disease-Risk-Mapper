import pandas as pd
import os

# Define file paths
csv_path = '../Backend/india-districts-census-2011.csv'
sql_output_path = 'schema.sql'

def transform_row(row):
    district_name = str(row['District name']).strip().replace("'", "''")
    pop = int(row['Population'])
    households = int(row['Households']) if int(row['Households']) > 0 else 1
    
    # 1. Sanitation Coverage (%)
    latrine_hh = int(row['Having_latrine_facility_within_the_premises_Total_Households'])
    sanitation_cov = round(min(100.0, max(0.0, (latrine_hh / households) * 100.0)), 2)
    
    # 2. Water Scarcity (%) = 100 - Safe Water Coverage (%)
    tap = int(row['Main_source_of_drinking_water_Tapwater_Households'])
    borewell = int(row['Main_source_of_drinking_water_Tubewell_Borehole_Households'])
    safe_water_hh = tap + borewell
    water_scarcity = round(min(100.0, max(0.0, 100.0 - ((safe_water_hh / households) * 100.0))), 2)
    
    # 3. Population Density Index Proxy (Persons / Estimated Household Spread)
    density = round(pop / (households * 0.05), 1)
    
    # 4. Risk Score Calculation
    # Weights: 45% Water Scarcity + 35% Sanitation Deficit + 20% Population Density Index
    density_index = min(100.0, (density / 1500.0) * 100.0)
    sanitation_deficit = 100.0 - sanitation_cov
    risk_score = round(0.45 * water_scarcity + 0.35 * sanitation_deficit + 0.20 * density_index, 1)
    
    # 5. Risk Level Categorization
    if risk_score >= 70.0:
        risk_level = 'HIGH'
    elif risk_score >= 40.0:
        risk_level = 'MODERATE'
    else:
        risk_level = 'LOW'
        
    return f"('{district_name}', {water_scarcity}, {sanitation_cov}, {pop}, {density}, {risk_score}, '{risk_level}')"

def main():
    if not os.path.exists(csv_path):
        print(f"Error: CSV file not found at {csv_path}")
        return

    print("Reading census dataset...")
    df = pd.read_csv(csv_path)
    
    print(f"Processing {len(df)} districts...")
    value_tuples = df.apply(transform_row, axis=1).tolist()
    
    # Generate SQL schema script
    sql_content = f"""-- Waterborne Disease Vulnerability System
-- Generated from Real India Census 2011 Dataset ({len(df)} Districts)

CREATE DATABASE IF NOT EXISTS waterborne_db;
USE waterborne_db;

DROP TABLE IF EXISTS districts;

CREATE TABLE districts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    water_scarcity DOUBLE NOT NULL,
    sanitation_coverage DOUBLE NOT NULL,
    population INT NOT NULL,
    population_density DOUBLE NOT NULL,
    risk_score DOUBLE NOT NULL,
    risk_level VARCHAR(20) NOT NULL
);

INSERT INTO districts (name, water_scarcity, sanitation_coverage, population, population_density, risk_score, risk_level) 
VALUES
{',\n'.join(value_tuples)};
"""

    with open(sql_output_path, 'w', encoding='utf-8') as f:
        f.write(sql_content)

    print(f"Successfully generated {sql_output_path} with {len(df)} district entries.")

if __name__ == '__main__':
    main()