param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Profile = "baseline",
    [string]$QueryMode = "fixed-pool",
    [string]$ResultLabel = "baseline",
    [string]$RunIdPrefix = "",
    [string]$Duration = "40s",
    [int]$TopK = 30,
    [int[]]$VusList = @(10, 15, 20, 25, 30),
    [string[]]$ScenarioNames = @(
        "text-search-baseline",
        "text-search-image-access-baseline",
        "image-search-baseline",
        "image-search-image-access-baseline",
        "hybrid-search-baseline",
        "hybrid-search-image-access-baseline"
    ),
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
    $ResultRoot = Join-Path $resultsBase ("search-" + $ResultLabel + "-prom-" + $stamp)
}

New-Item -ItemType Directory -Path $ResultRoot -Force | Out-Null

$scenarioMap = @{
    "text-search-baseline" = "text-search-baseline.js"
    "text-search-image-access-baseline" = "text-search-image-access-baseline.js"
    "image-search-baseline" = "image-search-baseline.js"
    "image-search-image-access-baseline" = "image-search-image-access-baseline.js"
    "hybrid-search-baseline" = "hybrid-search-baseline.js"
    "hybrid-search-image-access-baseline" = "hybrid-search-image-access-baseline.js"
}

$manifest = @()

function Save-ActuatorSnapshot {
    param(
        [string]$OutputPath
    )

    $response = Invoke-WebRequest -UseBasicParsing "$BaseUrl/actuator/prometheus" -TimeoutSec 30
    $response.Content | Set-Content -Path $OutputPath -Encoding utf8
}

foreach ($scenarioName in $ScenarioNames) {
    if (-not $scenarioMap.ContainsKey($scenarioName)) {
        throw "Unsupported scenario name: $scenarioName"
    }

    foreach ($vu in $VusList) {
        $name = "{0}-{1}-vu{2}-40s-topk{3}" -f $scenarioName, $ResultLabel, $vu, $TopK
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

        $scriptPath = Join-Path $scriptsRoot $scenarioMap[$scenarioName]
        $k6Output = & k6 run --summary-export $jsonPath $scriptPath 2>&1
        $k6Output | Set-Content -Path $txtPath -Encoding utf8

        $endTime = Get-Date
        if (-not $SkipActuatorSnapshots) {
            Save-ActuatorSnapshot -OutputPath $afterPath
        }

        $manifest += [pscustomobject]@{
            scenario = $scenarioName
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
