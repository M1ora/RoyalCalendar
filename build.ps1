param(
    [switch]$Offline,
    [switch]$Tests,
    [ValidateSet('Debug', 'Release')][string]$Configuration = 'Debug'
)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

# A local, untracked JDK is optional; a clean clone only requires JAVA_HOME.
if (-not $env:JAVA_HOME) {
    $localTools = Join-Path $PSScriptRoot 'tools'
    if (Test-Path -LiteralPath $localTools) {
        $localJdk = Get-ChildItem -LiteralPath $localTools -Directory -Filter 'jdk-17*' | Select-Object -First 1
        if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
    }
}
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to a JDK 17 installation.' }
if (-not $env:ANDROID_HOME) {
    if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_HOME = $env:ANDROID_SDK_ROOT }
    elseif ($env:LOCALAPPDATA) { $env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
}
if (-not $env:ANDROID_HOME -or -not (Test-Path -LiteralPath $env:ANDROID_HOME)) {
    throw 'Set ANDROID_HOME to your Android SDK directory.'
}

$buildTasks = @('--no-daemon', "assemble$Configuration")
if ($Offline) { $buildTasks += '--offline' }
if ($Tests) { $buildTasks += 'assembleDebugAndroidTest' }
& (Join-Path $PSScriptRoot 'gradlew.bat') @buildTasks
if ($LASTEXITCODE -ne 0) { throw 'Android build failed.' }

$outputDir = Join-Path $PSScriptRoot 'output'
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
if ($Configuration -eq 'Debug') {
    Copy-Item -LiteralPath 'app\build\outputs\apk\debug\app-debug.apk' -Destination (Join-Path $outputDir 'RoyalCalendar.apk')
} else {
    Copy-Item -LiteralPath 'app\build\outputs\apk\release\app-release-unsigned.apk' -Destination (Join-Path $outputDir 'RoyalCalendar-unsigned.apk')
}
