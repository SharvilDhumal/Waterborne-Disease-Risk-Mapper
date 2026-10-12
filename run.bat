@echo off
echo ================================================================
echo Starting Waterborne Disease Risk Mapper (JavaFX Desktop App)...
echo ================================================================
call mvn javafx:run
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Build failed. Retrying with clean compile...
    call mvn clean compile javafx:run
)
pause
