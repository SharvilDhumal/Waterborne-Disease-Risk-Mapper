package com.waterborne.repository;

import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MySQL JDBC Implementation of DistrictRepository for Student 2.
 * Connects to MySQL database and executes CRUD operations.
 */
public class DistrictRepositoryImpl implements DistrictRepository {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/waterborne_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private final String url;
    private final String user;
    private final String password;

    public DistrictRepositoryImpl() {
        this(
                environmentOrDefault("WATERBORNE_DB_URL", DEFAULT_URL),
                environmentOrDefault("WATERBORNE_DB_USER", "root"),
                environmentOrDefault("WATERBORNE_DB_PASSWORD", ""));
    }

    public DistrictRepositoryImpl(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public List<District> findAll() throws Exception {
        List<District> list = new ArrayList<>();
        String sql = "SELECT id, name, water_scarcity, sanitation_coverage, population, population_density, risk_score, risk_level FROM districts";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double scarcity = rs.getDouble("water_scarcity");
                double sanitation = rs.getDouble("sanitation_coverage");
                long population = rs.getLong("population");
                double density = rs.getDouble("population_density");
                double score = rs.getDouble("risk_score");
                String levelStr = rs.getString("risk_level");
                RiskLevel level = RiskLevel.valueOf(levelStr);

                list.add(new District(id, name, scarcity, sanitation, population, density, score, level));
            }
        }
        return list;
    }

    @Override
    public Optional<District> findByName(String name) throws Exception {
        String sql = "SELECT id, name, water_scarcity, sanitation_coverage, population, population_density, risk_score, risk_level FROM districts WHERE LOWER(name) = LOWER(?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    District d = new District(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("water_scarcity"),
                            rs.getDouble("sanitation_coverage"),
                            rs.getLong("population"),
                            rs.getDouble("population_density"),
                            rs.getDouble("risk_score"),
                            RiskLevel.valueOf(rs.getString("risk_level"))
                    );
                    return Optional.of(d);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean save(District d) throws Exception {
        String sql = "INSERT INTO districts (name, water_scarcity, sanitation_coverage, population, population_density, risk_score, risk_level) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, d.getName());
            pstmt.setDouble(2, d.getWaterScarcity());
            pstmt.setDouble(3, d.getSanitationCoverage());
            pstmt.setLong(4, d.getPopulation());
            pstmt.setDouble(5, d.getPopulationDensity());
            pstmt.setDouble(6, d.getRiskScore());
            pstmt.setString(7, d.getRiskLevel().name());
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean update(District d) throws Exception {
        String sql = "UPDATE districts SET name=?, water_scarcity=?, sanitation_coverage=?, population=?, population_density=?, risk_score=?, risk_level=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, d.getName());
            pstmt.setDouble(2, d.getWaterScarcity());
            pstmt.setDouble(3, d.getSanitationCoverage());
            pstmt.setLong(4, d.getPopulation());
            pstmt.setDouble(5, d.getPopulationDensity());
            pstmt.setDouble(6, d.getRiskScore());
            pstmt.setString(7, d.getRiskLevel().name());
            pstmt.setInt(8, d.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteById(int id) throws Exception {
        String sql = "DELETE FROM districts WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
}
