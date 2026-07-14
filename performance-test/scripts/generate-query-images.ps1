param(
    [int]$Count = 24
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$dataRoot = Join-Path $scriptRoot "..\\data"
$outputDir = Join-Path $dataRoot "generated-query-images"
$manifestPath = Join-Path $dataRoot "image-query-manifest.json"

New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

Add-Type -AssemblyName System.Drawing

$shapes = @(
    "circle and rectangle composition",
    "striped gradient poster",
    "layered diagonal blocks",
    "minimal badge illustration",
    "offset geometric collage",
    "high contrast symbol art"
)

$tones = @(
    "warm orange blue palette",
    "green yellow palette",
    "red black palette",
    "cyan navy palette",
    "pink gray palette",
    "lime purple palette"
)

$items = @()

for ($index = 1; $index -le $Count; $index++) {
    $bitmap = New-Object System.Drawing.Bitmap 512, 512
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

    $background = [System.Drawing.Color]::FromArgb(
        255,
        (($index * 53) % 256),
        (($index * 97) % 256),
        (($index * 149) % 256)
    )
    $graphics.Clear($background)

    $accentBrush = New-Object System.Drawing.SolidBrush (
        [System.Drawing.Color]::FromArgb(
            230,
            (($index * 181) % 256),
            (($index * 41) % 256),
            (($index * 67) % 256)
        )
    )
    $lightBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(220, 250, 250, 250))
    $linePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(200, 20, 20, 20), 10)
    $font = New-Object System.Drawing.Font "Arial", 28, ([System.Drawing.FontStyle]::Bold)

    $graphics.FillEllipse($accentBrush, 40 + (($index * 13) % 140), 50, 180, 180)
    $graphics.FillRectangle($lightBrush, 220, 180 + (($index * 7) % 60), 180, 120)
    $graphics.DrawLine($linePen, 40, 420, 460, 120 + (($index * 9) % 200))
    $graphics.DrawString("Q$index", $font, $lightBrush, 185, 420)

    $fileName = ("query-{0:d3}.png" -f $index)
    $filePath = Join-Path $outputDir $fileName
    $bitmap.Save($filePath, [System.Drawing.Imaging.ImageFormat]::Png)

    $hybridText = "{0} with {1}" -f $shapes[($index - 1) % $shapes.Length], $tones[($index - 1) % $tones.Length]

    $items += [ordered]@{
        id = "query-$index"
        path = "generated-query-images/$fileName"
        hybridText = $hybridText
    }

    $font.Dispose()
    $linePen.Dispose()
    $lightBrush.Dispose()
    $accentBrush.Dispose()
    $graphics.Dispose()
    $bitmap.Dispose()
}

$manifest = [ordered]@{
    version = 1
    generatedAt = (Get-Date).ToString("s")
    items = $items
}

$json = $manifest | ConvertTo-Json -Depth 5
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($manifestPath, $json, $utf8NoBom)
