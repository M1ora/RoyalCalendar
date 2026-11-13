param([string]$Version = '1.0.0')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$sdkRoot = $env:ANDROID_HOME
if (-not $sdkRoot -and $env:LOCALAPPDATA) { $sdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$signer = Join-Path $sdkRoot 'build-tools\36.0.0\apksigner.bat'
$keyFile = Join-Path $projectRoot '.signing\royal-calendar-release.jks'
$passwordFile = Join-Path $projectRoot '.signing\store-password.txt'
if (-not (Test-Path -LiteralPath $keyFile) -or -not (Test-Path -LiteralPath $passwordFile)) {
    throw 'Private release signing files are missing. See docs/PUBLISHING.md.'
}
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to JDK 17.' }
New-Item -ItemType Directory -Force output | Out-Null
$apk = Join-Path $projectRoot "output\RoyalCalendar-v$Version.apk"
& $signer sign --ks $keyFile --ks-key-alias royalcalendar --ks-pass "file:$passwordFile" --out $apk 'app\build\outputs\apk\release\app-release-unsigned.apk'
if ($LASTEXITCODE -ne 0) { throw 'APK signing failed.' }
& $signer verify --verbose $apk
if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
$hash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
[System.IO.File]::WriteAllText("$apk.sha256", "$hash  $([System.IO.Path]::GetFileName($apk))`n", [System.Text.UTF8Encoding]::new($false))
