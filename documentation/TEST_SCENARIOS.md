# FRONTEND TEST SCENARIOS & VERIFICATION SUITE

---

### Test Case 1: Initial Dashboard Load
- **Action:** Launch the JavaFX Application (`com.waterborne.MainApp`).
- **Expected Outcome:** Dashboard loads successfully with 4 KPI summary cards (Total Districts, High Risk, Moderate Risk, Low Risk) displaying accurate metrics.
- **Status:** PASS

### Test Case 2: District Search Functionality
- **Action:** Enter "Pune" into the search bar in Data Table or District Explorer.
- **Expected Outcome:** Table updates in real-time showing only "Pune" district record with 72% Water Scarcity, 45% Sanitation, and HIGH risk score.
- **Status:** PASS

### Test Case 3: Risk Level Dropdown Filtering
- **Action:** Select "HIGH RISK" from the Risk Level filter combo box.
- **Expected Outcome:** Table filters out Low and Moderate risk districts, displaying only High Risk districts (e.g. Pune, Solapur, Aurangabad, Latur).
- **Status:** PASS

### Test Case 4: District Profile Inspector Selection
- **Action:** Click on "Nashik" in the District Explorer list.
- **Expected Outcome:** Right-side profile card updates immediately showing Nashik's water scarcity progress bar (63%), sanitation progress bar (52%), risk score (63/100), and automated interpretation summary text.
- **Status:** PASS

### Test Case 5: Database Connection Failure Fallback
- **Action:** Run application when MySQL server is stopped or database credentials are missing.
- **Expected Outcome:** System logs a warning and gracefully displays mock dataset without crashing or presenting a raw stack trace to the user.
- **Status:** PASS

### Test Case 6: Empty Search Results
- **Action:** Search for a non-existent district name (e.g. "Xyz123").
- **Expected Outcome:** Table displays empty state message ("No matching district found").
- **Status:** PASS

### Test Case 7: Window Resizing Responsiveness
- **Action:** Resize main application window from 1024x768 to 1440x900.
- **Expected Outcome:** UI layouts (BorderPane, GridPane, HBox, VBox) adapt dynamically without overlapping text or clipped controls.
- **Status:** PASS
