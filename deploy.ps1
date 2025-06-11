# Configuration
$scriptName = "EZwalk"
$dreambotPath = "$env:USERPROFILE\DreamBot"
$scriptsPath = "$dreambotPath\Scripts"

# Use the local client.jar instead of the one in DreamBot directory
$localClientPath = ".\lib\client.jar"

try {
    # Clean up any existing files
    Remove-Item "$scriptsPath\$scriptName*.jar" -Force -ErrorAction SilentlyContinue
    
    # Create temp directory for compilation
    $tempDir = ".\temp"
    if (Test-Path $tempDir) {
        Remove-Item -Path $tempDir -Recurse -Force
    }
    
    # Create temporary directory structure for EZwalk project
    New-Item -ItemType Directory -Force -Path ".\temp\org\dreambot\scriptmain" | Out-Null

    # Copy main script to package directory
    if (Test-Path ".\$scriptName.java") {
        Copy-Item -Path ".\$scriptName.java" -Destination ".\temp\org\dreambot\scriptmain\"
        Write-Host "Copied $scriptName.java to package directory" -ForegroundColor Green
    } else {
        Write-Host "Error: $scriptName.java not found in root directory" -ForegroundColor Red
        throw "Missing main script file"
    }

    # Change to temp directory for compilation
    Push-Location -Path ".\temp"

    # Set classpath to use local client.jar
    $classpath = "..\$localClientPath"
    
    # Verify client.jar exists
    if (-not (Test-Path "..\$localClientPath")) {
        Write-Host "Error: client.jar not found at $localClientPath" -ForegroundColor Red
        throw "Missing client.jar file"
    }

    # Compile Java files
    Write-Host "Compiling Java files..."
    
    # Compile the main script
    Write-Host "Compiling main script..."
    & javac -cp "$classpath;." org\dreambot\scriptmain\$scriptName.java
    $mainCompileSuccess = $LASTEXITCODE -eq 0
    
    if ($mainCompileSuccess) {
        Write-Host "Main script compilation successful!" -ForegroundColor Green
        
        # Create JAR file with detailed error handling
        Write-Host "Creating JAR file..."
        
        # Create manifest file for the JAR (use EZwalk as the main class)
        Set-Content -Path "MANIFEST.MF" -Value "Main-Class: org.dreambot.scriptmain.EZwalk`r`n"
        
        Write-Host "Running JAR command..."
        & jar -cvfm "$scriptName.jar" "MANIFEST.MF" org 2>&1 | Out-Null
        
        if (Test-Path ".\$scriptName.jar") {
            $jarSize = (Get-Item ".\$scriptName.jar").Length
            Write-Host "JAR file created successfully: $jarSize bytes" -ForegroundColor Green
            
            Write-Host "Moving JAR file to DreamBot Scripts directory..."
            Move-Item -Path ".\$scriptName.jar" -Destination $scriptsPath -Force
            
            if (Test-Path "$scriptsPath\$scriptName.jar") {
                Write-Host "Deployment complete! JAR is at $scriptsPath\$scriptName.jar" -ForegroundColor Green
                $deploymentSuccess = $true
            } else {
                Write-Host "Error: JAR file wasn't moved to $scriptsPath" -ForegroundColor Red
                $deploymentSuccess = $false
            }
        } else {
            Write-Host "Error: JAR file was not created" -ForegroundColor Red
            $deploymentSuccess = $false
        }
    } else {
        Write-Host "Error: Compilation failed for main script" -ForegroundColor Red
        $deploymentSuccess = $false
    }

    Pop-Location
    
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
    $deploymentSuccess = $false
} finally {
    if ((Get-Location).Path -like "*\temp*") {
        Pop-Location
    }
    
    if (Test-Path $tempDir) {
        Write-Host "Cleaning up temporary files..." -ForegroundColor Green
        Remove-Item -Path $tempDir -Recurse -Force
        Write-Host "Temporary directory removed successfully" -ForegroundColor Green
    }
}

if ($deploymentSuccess) {
    Write-Host "=========================" -ForegroundColor Green
    Write-Host "DEPLOYMENT SUCCESSFUL" -ForegroundColor Green
    Write-Host "=========================" -ForegroundColor Green
} else {
    Write-Host "=========================" -ForegroundColor Red
    Write-Host "DEPLOYMENT FAILED" -ForegroundColor Red
    Write-Host "=========================" -ForegroundColor Red
}
