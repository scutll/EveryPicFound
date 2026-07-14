param(
    [Parameter(Mandatory = $true)]
    [string]$ResultRoot
)

$ErrorActionPreference = "Stop"

function Parse-LabelMap {
    param([string]$LabelText)

    $map = @{}
    if ([string]::IsNullOrWhiteSpace($LabelText)) {
        return $map
    }

    foreach ($part in ($LabelText -split ',')) {
        if ($part -match '^\s*([^=]+)="(.*)"\s*$') {
            $map[$matches[1]] = $matches[2]
        }
    }

    return $map
}

function Parse-PromSnapshot {
    param([string]$Path)

    $entries = New-Object System.Collections.Generic.List[object]
    foreach ($line in Get-Content $Path) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
            continue
        }

        if ($line -match '^([^{\s]+)\{([^}]*)\}\s+([^\s]+)$') {
            $entries.Add([pscustomobject]@{
                name = $matches[1]
                labels = Parse-LabelMap $matches[2]
                value = [double]$matches[3]
            })
            continue
        }

        if ($line -match '^([^\s]+)\s+([^\s]+)$') {
            $entries.Add([pscustomobject]@{
                name = $matches[1]
                labels = @{}
                value = [double]$matches[2]
            })
        }
    }

    return $entries
}

function Get-MetricSum {
    param(
        [object[]]$Entries,
        [string]$Name,
        [hashtable]$RequiredLabels = @{}
    )

    $sum = 0.0
    foreach ($entry in $Entries) {
        if ($entry.name -ne $Name) {
            continue
        }

        $matched = $true
        foreach ($key in $RequiredLabels.Keys) {
            if (-not $entry.labels.ContainsKey($key) -or $entry.labels[$key] -ne $RequiredLabels[$key]) {
                $matched = $false
                break
            }
        }

        if ($matched) {
            $sum += $entry.value
        }
    }

    return $sum
}

function Delta {
    param([double]$After, [double]$Before)
    return ($After - $Before)
}

function Resolve-SearchType {
    param([string]$Scenario)

    if ($Scenario.StartsWith("text-search")) {
        return "text"
    }

    if ($Scenario.StartsWith("image-search")) {
        return "image"
    }

    if ($Scenario.StartsWith("hybrid-search")) {
        return "hybrid"
    }

    throw "Unsupported scenario: $Scenario"
}

function Get-ModelEndpointAverages {
    param(
        [object[]]$Before,
        [object[]]$After,
        [string]$SearchType
    )

    $endpointNames = @()
    if ($SearchType -eq "text") {
        $endpointNames = @("vectorize_text")
    } elseif ($SearchType -eq "image") {
        $endpointNames = @("vectorize_image")
    } elseif ($SearchType -eq "hybrid") {
        $endpointNames = @("vectorize_text", "vectorize_image")
    }

    $results = @{}
    foreach ($endpointName in $endpointNames) {
        $sum = Delta `
            (Get-MetricSum $After 'everypicfound_model_http_duration_seconds_sum' @{ endpoint = $endpointName; result = 'success' }) `
            (Get-MetricSum $Before 'everypicfound_model_http_duration_seconds_sum' @{ endpoint = $endpointName; result = 'success' })

        $count = Delta `
            (Get-MetricSum $After 'everypicfound_model_http_duration_seconds_count' @{ endpoint = $endpointName; result = 'success' }) `
            (Get-MetricSum $Before 'everypicfound_model_http_duration_seconds_count' @{ endpoint = $endpointName; result = 'success' })

        $results[$endpointName] = if ($count -gt 0) {
            [math]::Round(($sum / $count) * 1000, 2)
        } else {
            $null
        }
    }

    return $results
}

$manifest = Import-Csv (Join-Path $ResultRoot "manifest.csv")
$rows = @()

foreach ($item in $manifest) {
    $jsonPath = Join-Path $ResultRoot $item.json
    $beforePath = Join-Path $ResultRoot $item.actuator_before
    $afterPath = Join-Path $ResultRoot $item.actuator_after
    $searchType = Resolve-SearchType $item.scenario

    $json = Get-Content $jsonPath -Raw | ConvertFrom-Json
    $metrics = $json.metrics

    $before = Parse-PromSnapshot $beforePath
    $after = Parse-PromSnapshot $afterPath
    $modelEndpointAverages = Get-ModelEndpointAverages -Before $before -After $after -SearchType $searchType

    $searchQps = if ($metrics.search_iterations) { [double]$metrics.search_iterations.rate } else { [double]$metrics.http_reqs.rate }
    $searchAvgMs = if ($metrics.page_search_duration) { [double]$metrics.page_search_duration.avg } else { [double]$metrics.http_req_duration.avg }
    $searchP95Ms = if ($metrics.page_search_duration) { [double]$metrics.page_search_duration.'p(95)' } else { [double]$metrics.http_req_duration.'p(95)' }
    $searchP99Ms = if ($metrics.page_search_duration) { [double]$metrics.page_search_duration.'p(99)' } else { [double]$metrics.http_req_duration.'p(99)' }
    $pageQps = if ($metrics.page_total_duration) { [double]$metrics.search_iterations.rate } else { $null }
    $pageAvgMs = if ($metrics.page_total_duration) { [double]$metrics.page_total_duration.avg } else { $null }
    $pageP95Ms = if ($metrics.page_total_duration) { [double]$metrics.page_total_duration.'p(95)' } else { $null }
    $pageP99Ms = if ($metrics.page_total_duration) { [double]$metrics.page_total_duration.'p(99)' } else { $null }
    $imageBatchAvgMs = if ($metrics.page_image_batch_duration) { [double]$metrics.page_image_batch_duration.avg } else { $null }
    $imageAvgMs = if ($metrics.image_access_duration) { [double]$metrics.image_access_duration.avg } else { $null }

    $searchCount = Delta `
        (Get-MetricSum $after 'everypicfound_search_requests_total' @{ search_type = $searchType; result = 'success' }) `
        (Get-MetricSum $before 'everypicfound_search_requests_total' @{ search_type = $searchType; result = 'success' })

    $searchSum = Delta `
        (Get-MetricSum $after 'everypicfound_search_duration_seconds_sum' @{ search_type = $searchType; result = 'success' }) `
        (Get-MetricSum $before 'everypicfound_search_duration_seconds_sum' @{ search_type = $searchType; result = 'success' })

    $searchCacheNaCount = Delta `
        (Get-MetricSum $after 'everypicfound_search_requests_total' @{ search_type = $searchType; result = 'success'; cache_result = 'not_applicable' }) `
        (Get-MetricSum $before 'everypicfound_search_requests_total' @{ search_type = $searchType; result = 'success'; cache_result = 'not_applicable' })
    $searchCacheHitCount = Delta `
        (Get-MetricSum $after 'everypicfound_search_requests_total' @{ search_type = $searchType; result = 'success'; cache_result = 'hit' }) `
        (Get-MetricSum $before 'everypicfound_search_requests_total' @{ search_type = $searchType; result = 'success'; cache_result = 'hit' })
    $searchCacheMissCount = Delta `
        (Get-MetricSum $after 'everypicfound_search_requests_total' @{ search_type = $searchType; result = 'success'; cache_result = 'miss' }) `
        (Get-MetricSum $before 'everypicfound_search_requests_total' @{ search_type = $searchType; result = 'success'; cache_result = 'miss' })

    $queryVectorizeSum = Delta `
        (Get-MetricSum $after 'everypicfound_search_stage_duration_seconds_sum' @{ search_type = $searchType; stage = 'query_vectorize'; result = 'success' }) `
        (Get-MetricSum $before 'everypicfound_search_stage_duration_seconds_sum' @{ search_type = $searchType; stage = 'query_vectorize'; result = 'success' })

    $vectorRecallSum = Delta `
        (Get-MetricSum $after 'everypicfound_search_stage_duration_seconds_sum' @{ search_type = $searchType; stage = 'vector_recall'; result = 'success' }) `
        (Get-MetricSum $before 'everypicfound_search_stage_duration_seconds_sum' @{ search_type = $searchType; stage = 'vector_recall'; result = 'success' })

    $backfillSum = Delta `
        (Get-MetricSum $after 'everypicfound_search_stage_duration_seconds_sum' @{ search_type = $searchType; stage = 'backfill'; result = 'success' }) `
        (Get-MetricSum $before 'everypicfound_search_stage_duration_seconds_sum' @{ search_type = $searchType; stage = 'backfill'; result = 'success' })

    $repoSum = Delta `
        (Get-MetricSum $after 'everypicfound_image_asset_repository_duration_seconds_sum' @{ operation = 'find_by_ids'; result = 'success' }) `
        (Get-MetricSum $before 'everypicfound_image_asset_repository_duration_seconds_sum' @{ operation = 'find_by_ids'; result = 'success' })

    $repoCount = Delta `
        (Get-MetricSum $after 'everypicfound_image_asset_repository_duration_seconds_count' @{ operation = 'find_by_ids'; result = 'success' }) `
        (Get-MetricSum $before 'everypicfound_image_asset_repository_duration_seconds_count' @{ operation = 'find_by_ids'; result = 'success' })

    $qdrantSum = Delta `
        (Get-MetricSum $after 'everypicfound_vector_index_duration_seconds_sum' @{ operation = 'search'; result = 'success' }) `
        (Get-MetricSum $before 'everypicfound_vector_index_duration_seconds_sum' @{ operation = 'search'; result = 'success' })

    $qdrantCount = Delta `
        (Get-MetricSum $after 'everypicfound_vector_index_duration_seconds_count' @{ operation = 'search'; result = 'success' }) `
        (Get-MetricSum $before 'everypicfound_vector_index_duration_seconds_count' @{ operation = 'search'; result = 'success' })

    $imageAccessSum = Delta `
        (Get-MetricSum $after 'everypicfound_image_access_duration_seconds_sum' @{ result = 'success' }) `
        (Get-MetricSum $before 'everypicfound_image_access_duration_seconds_sum' @{ result = 'success' })

    $imageAccessCount = Delta `
        (Get-MetricSum $after 'everypicfound_image_access_duration_seconds_count' @{ result = 'success' }) `
        (Get-MetricSum $before 'everypicfound_image_access_duration_seconds_count' @{ result = 'success' })

    $cacheSearchGetSkipped = Delta `
        (Get-MetricSum $after 'everypicfound_cache_duration_seconds_count' @{ cache_name = 'search_result'; operation = 'get'; result = 'skipped' }) `
        (Get-MetricSum $before 'everypicfound_cache_duration_seconds_count' @{ cache_name = 'search_result'; operation = 'get'; result = 'skipped' })
    $cacheSearchGetHit = Delta `
        (Get-MetricSum $after 'everypicfound_cache_duration_seconds_count' @{ cache_name = 'search_result'; operation = 'get'; result = 'hit' }) `
        (Get-MetricSum $before 'everypicfound_cache_duration_seconds_count' @{ cache_name = 'search_result'; operation = 'get'; result = 'hit' })
    $cacheSearchGetMiss = Delta `
        (Get-MetricSum $after 'everypicfound_cache_duration_seconds_count' @{ cache_name = 'search_result'; operation = 'get'; result = 'miss' }) `
        (Get-MetricSum $before 'everypicfound_cache_duration_seconds_count' @{ cache_name = 'search_result'; operation = 'get'; result = 'miss' })

    $cacheTextGetSkipped = Delta `
        (Get-MetricSum $after 'everypicfound_cache_duration_seconds_count' @{ cache_name = 'text_vector'; operation = 'get'; result = 'skipped' }) `
        (Get-MetricSum $before 'everypicfound_cache_duration_seconds_count' @{ cache_name = 'text_vector'; operation = 'get'; result = 'skipped' })
    $cacheTextGetHit = Delta `
        (Get-MetricSum $after 'everypicfound_cache_duration_seconds_count' @{ cache_name = 'text_vector'; operation = 'get'; result = 'hit' }) `
        (Get-MetricSum $before 'everypicfound_cache_duration_seconds_count' @{ cache_name = 'text_vector'; operation = 'get'; result = 'hit' })
    $cacheTextGetMiss = Delta `
        (Get-MetricSum $after 'everypicfound_cache_duration_seconds_count' @{ cache_name = 'text_vector'; operation = 'get'; result = 'miss' }) `
        (Get-MetricSum $before 'everypicfound_cache_duration_seconds_count' @{ cache_name = 'text_vector'; operation = 'get'; result = 'miss' })

    $redisOps = Delta (Get-MetricSum $after 'everypicfound_redis_operations_total') (Get-MetricSum $before 'everypicfound_redis_operations_total')

    $heapBeforeMb = (Get-MetricSum $before 'jvm_memory_used_bytes' @{ area = 'heap' }) / 1MB
    $heapAfterMb = (Get-MetricSum $after 'jvm_memory_used_bytes' @{ area = 'heap' }) / 1MB

    $gcPauseBefore = Get-MetricSum $before 'jvm_gc_pause_seconds_count'
    $gcPauseAfter = Get-MetricSum $after 'jvm_gc_pause_seconds_count'

    $rows += [pscustomobject]@{
        scenario = $item.scenario
        search_type = $searchType
        profile = $item.profile
        query_mode = $item.query_mode
        result_label = $item.result_label
        vu = [int]$item.vu
        started_at = $item.started_at
        ended_at = $item.ended_at
        search_qps = [math]::Round($searchQps, 2)
        search_avg_ms = [math]::Round($searchAvgMs, 2)
        search_p95_ms = [math]::Round($searchP95Ms, 2)
        search_p99_ms = [math]::Round($searchP99Ms, 2)
        page_qps = if ($pageQps -ne $null) { [math]::Round($pageQps, 2) } else { $null }
        page_avg_ms = if ($pageAvgMs -ne $null) { [math]::Round($pageAvgMs, 2) } else { $null }
        page_p95_ms = if ($pageP95Ms -ne $null) { [math]::Round($pageP95Ms, 2) } else { $null }
        page_p99_ms = if ($pageP99Ms -ne $null) { [math]::Round($pageP99Ms, 2) } else { $null }
        image_batch_avg_ms = if ($imageBatchAvgMs -ne $null) { [math]::Round($imageBatchAvgMs, 2) } else { $null }
        image_avg_ms = if ($imageAvgMs -ne $null) { [math]::Round($imageAvgMs, 2) } else { $null }
        server_search_count = [int][math]::Round($searchCount, 0)
        server_search_avg_ms = if ($searchCount -gt 0) { [math]::Round(($searchSum / $searchCount) * 1000, 2) } else { $null }
        stage_query_vectorize_avg_ms = if ($searchCount -gt 0) { [math]::Round(($queryVectorizeSum / $searchCount) * 1000, 2) } else { $null }
        stage_vector_recall_avg_ms = if ($searchCount -gt 0) { [math]::Round(($vectorRecallSum / $searchCount) * 1000, 2) } else { $null }
        stage_backfill_avg_ms = if ($searchCount -gt 0) { [math]::Round(($backfillSum / $searchCount) * 1000, 2) } else { $null }
        model_vectorize_text_avg_ms = $modelEndpointAverages["vectorize_text"]
        model_vectorize_image_avg_ms = $modelEndpointAverages["vectorize_image"]
        qdrant_search_avg_ms = if ($qdrantCount -gt 0) { [math]::Round(($qdrantSum / $qdrantCount) * 1000, 2) } else { $null }
        mysql_backfill_avg_ms = if ($repoCount -gt 0) { [math]::Round(($repoSum / $repoCount) * 1000, 2) } else { $null }
        image_access_server_avg_ms = if ($imageAccessCount -gt 0) { [math]::Round(($imageAccessSum / $imageAccessCount) * 1000, 2) } else { $null }
        search_cache_not_applicable = [int][math]::Round($searchCacheNaCount, 0)
        search_cache_hit = [int][math]::Round($searchCacheHitCount, 0)
        search_cache_miss = [int][math]::Round($searchCacheMissCount, 0)
        search_result_cache_get_skipped = [int][math]::Round($cacheSearchGetSkipped, 0)
        search_result_cache_get_hit = [int][math]::Round($cacheSearchGetHit, 0)
        search_result_cache_get_miss = [int][math]::Round($cacheSearchGetMiss, 0)
        text_vector_cache_get_skipped = [int][math]::Round($cacheTextGetSkipped, 0)
        text_vector_cache_get_hit = [int][math]::Round($cacheTextGetHit, 0)
        text_vector_cache_get_miss = [int][math]::Round($cacheTextGetMiss, 0)
        redis_ops_delta = [math]::Round($redisOps, 0)
        process_cpu_usage_before = [math]::Round((Get-MetricSum $before 'process_cpu_usage'), 4)
        process_cpu_usage_after = [math]::Round((Get-MetricSum $after 'process_cpu_usage'), 4)
        system_cpu_usage_before = [math]::Round((Get-MetricSum $before 'system_cpu_usage'), 4)
        system_cpu_usage_after = [math]::Round((Get-MetricSum $after 'system_cpu_usage'), 4)
        heap_used_before_mb = [math]::Round($heapBeforeMb, 2)
        heap_used_after_mb = [math]::Round($heapAfterMb, 2)
        gc_pause_count_delta = [int][math]::Round(($gcPauseAfter - $gcPauseBefore), 0)
    }
}

$csvPath = Join-Path $ResultRoot "summary-with-metrics.csv"
$rows | Sort-Object profile, scenario, vu | Export-Csv -Path $csvPath -NoTypeInformation -Encoding utf8

Write-Output $csvPath
