Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "Starting Waterborne Disease Risk Mapper (JavaFX Desktop App)..." -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
mvn javafx:run
if ($LASTEXITCODE -ne 0) {
    Write-Host "`nBuild failed. Retrying with clean compile..." -ForegroundColor Yellow
    mvn clean compile javafx:run
}
