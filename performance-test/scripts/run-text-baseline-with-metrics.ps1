param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Profile = "baseline",
    [string]$QueryMode = "unique",
    [string]$ResultLabel = "baseline",
    [string]$RunIdPrefix = "",
    [string]$Duration = "40s",
    [int]$TopK = 30,
    [int[]]$VusList = @(10, 15, 20, 25, 30),
    [string]$ResultRoot = "",
    [switch]$SkipActuatorSnapshots
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$performanceRoot = Join-Path $repoRoot "performance-test"
$scriptsRoot = Join-Path $performanceRoot "scripts"
$resultsBase = Join-Path $performanceRoot "results"

if ([string]::IsNullOrWhiteSpace($ResultRoot)) {
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $ResultRoot = Join-Path $resultsBase ("text-" + $ResultLabel + "-prom-" + $stamp)
}

New-Item -ItemType Directory -Path $ResultRoot -Force | Out-Null

$scenarios = @(
    @{ Name = "text-search-baseline"; Script = "text-search-baseline.js" },
    @{ Name = "text-search-image-access-baseline"; Script = "text-search-image-access-baseline.js" }
)

$manifest = @()

function Save-ActuatorSnapshot {
    param(
        [string]$OutputPath
    )

    $response = Invoke-WebRequest -UseBasicParsing "$BaseUrl/actuator/prometheus" -TimeoutSec 30
    $response.Content | Set-Content -Path $OutputPath -Encoding utf8
}

foreach ($scenario in $scenarios) {
    foreach ($vu in $VusList) {
        $name = "{0}-baseline-vu{1}-40s-topk{2}" -f $scenario.Name, $vu, $TopK
        if ($ResultLabel -ne "baseline") {
            $name = "{0}-{1}-vu{2}-40s-topk{3}" -f $scenario.Name, $ResultLabel, $vu, $TopK
        }
        $jsonPath = Join-Path $ResultRoot ($name + ".json")
        $txtPath = Join-Path $ResultRoot ($name + ".txt")
        $beforePath = Join-Path $ResultRoot ($name + "-before.prom")
        $afterPath = Join-Path $ResultRoot ($name + "-after.prom")

        $startTime = Get-Date
        if (-not $SkipActuatorSnapshots) {
            Save-ActuatorSnapshot -OutputPath $beforePath
        }

        $env:BASE_URL = $BaseUrl
        $env:PROFILE = $Profile
        $env:VUS = [string]$vu
        $env:DURATION = $Duration
        $env:TOP_K = [string]$TopK
        $env:QUERY_MODE = $QueryMode
        $env:RUN_ID = if ([string]::IsNullOrWhiteSpace($RunIdPrefix)) { $name } else { "$RunIdPrefix-$name" }

        $scriptPath = Join-Path $scriptsRoot $scenario.Script
        & k6 run --summary-export $jsonPath $scriptPath *>&1 | Tee-Object -FilePath $txtPath | Out-Host
        if ($LASTEXITCODE -ne 0) {
            throw "k6 failed for $($scenario.Script) at VU=$vu"
        }

        $endTime = Get-Date
        if (-not $SkipActuatorSnapshots) {
            Save-ActuatorSnapshot -OutputPath $afterPath
        }

        $manifest += [pscustomobject]@{
            scenario = $scenario.Name
            profile = $Profile
            query_mode = $QueryMode
            result_label = $ResultLabel
            vu = $vu
            duration = $Duration
            top_k = $TopK
            started_at = $startTime.ToString("o")
            ended_at = $endTime.ToString("o")
            json = [System.IO.Path]::GetFileName($jsonPath)
            txt = [System.IO.Path]::GetFileName($txtPath)
            actuator_before = if ($SkipActuatorSnapshots) { "" } else { [System.IO.Path]::GetFileName($beforePath) }
            actuator_after = if ($SkipActuatorSnapshots) { "" } else { [System.IO.Path]::GetFileName($afterPath) }
        }
    }
}

$manifestPath = Join-Path $ResultRoot "manifest.csv"
$manifest | Export-Csv -Path $manifestPath -NoTypeInformation -Encoding utf8

Write-Output $ResultRoot
