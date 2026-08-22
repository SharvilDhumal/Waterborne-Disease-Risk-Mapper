package com.waterborne.repository;

import com.waterborne.model.District;
import java.util.List;
import java.util.Optional;

/**
 * Interface for Database Access Operations (MySQL / JDBC).
 * Implemented by Student 2 (Backend & Database Developer).
 */
public interface DistrictRepository {

    /**
     * Retrieve all district records from the MySQL database.
     * @return List of all districts
     */
    List<District> findAll() throws Exception;

    /**
     * Search a district by unique name.
     * @param name District name
     * @return Optional containing District if found
     */
    Optional<District> findByName(String name) throws Exception;

    /**
     * Insert a new district record into database.
     */
    boolean save(District district) throws Exception;

    /**
     * Update an existing district record in database.
     */
    boolean update(District district) throws Exception;

    /**
     * Delete a district record by ID.
     */
    boolean deleteById(int id) throws Exception;
}
