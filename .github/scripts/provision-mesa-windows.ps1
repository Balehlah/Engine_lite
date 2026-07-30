[CmdletBinding()]
param(
    [string]$ArchivePath,
    [string]$ExpectedArchiveSha256 =
        'd5e90e9ae4d620313b61fbbf8e9a55761454e38b6501c39be6d93449c88780e1',
    [switch]$VerifyArchiveOnly,
    [string[]]$JavaHomes = @($env:JAVA_HOME, $env:JAVA_HOME_25_X64),
    [string]$EnvironmentFile = $env:GITHUB_ENV
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$mesaVersion = '26.1.1'
$mesaSourceCommit = '1e2b696ce9e81e77e17ee6e4787587237ce9d2ed'
$mesaSourceUrl =
    'https://github.com/pal1000/mesa-dist-win/releases/download/26.1.1/' +
    'mesa3d-26.1.1-release-msvc.7z'
$expectedOpenGlSha256 =
    'd2645f47b4dee4f47dcdfc1b2021a70f471655d95a019cfd1fb48415810867ed'
$expectedGalliumSha256 =
    '27f16f9e98119ad529ed915d4f65c3a2e8d84b4f8cbdce2f13cda0637b73e05c'
$mesaLicense = 'MIT (Mesa core; per-file SPDX licenses apply)'

function Assert-Sha256 {
    param(
        [Parameter(Mandatory)]
        [string]$Path,
        [Parameter(Mandatory)]
        [string]$Expected
    )

    $observed = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    Write-Host "sha256.file=$Path"
    Write-Host "sha256.expected=$($Expected.ToLowerInvariant())"
    Write-Host "sha256.observed=$observed"
    if ($observed -ne $Expected.ToLowerInvariant()) {
        throw "SHA-256 mismatch for $Path. Expected $Expected, observed $observed."
    }
    return $observed
}

if ($VerifyArchiveOnly) {
    if ([string]::IsNullOrWhiteSpace($ArchivePath) -or
        -not (Test-Path -LiteralPath $ArchivePath -PathType Leaf)) {
        throw 'VerifyArchiveOnly requires an existing ArchivePath.'
    }
    Assert-Sha256 -Path $ArchivePath -Expected $ExpectedArchiveSha256 | Out-Null
    Write-Host 'mesa.archive.verification=PASS'
    exit 0
}

if ([string]::IsNullOrWhiteSpace($env:RUNNER_TOOL_CACHE)) {
    throw 'RUNNER_TOOL_CACHE is required for ephemeral Mesa provisioning.'
}
$runnerToolCache = [System.IO.Path]::GetFullPath($env:RUNNER_TOOL_CACHE)
$runnerToolCachePrefix = $runnerToolCache.TrimEnd(
    [System.IO.Path]::DirectorySeparatorChar,
    [System.IO.Path]::AltDirectorySeparatorChar
) + [System.IO.Path]::DirectorySeparatorChar

$mesaRoot = Join-Path $runnerToolCache "engine-lite-mesa\$mesaVersion"
if (Test-Path -LiteralPath $mesaRoot) {
    throw "Refusing to overwrite an existing Mesa tool directory: $mesaRoot"
}
[System.IO.Directory]::CreateDirectory($mesaRoot) | Out-Null

if ([string]::IsNullOrWhiteSpace($ArchivePath)) {
    $ArchivePath = Join-Path $mesaRoot "mesa3d-$mesaVersion-release-msvc.7z"
}
$ArchivePath = [System.IO.Path]::GetFullPath($ArchivePath)
if (-not $ArchivePath.StartsWith(
        $runnerToolCachePrefix,
        [System.StringComparison]::OrdinalIgnoreCase
    )) {
    throw "Mesa archive must stay inside RUNNER_TOOL_CACHE: $ArchivePath"
}
if (Test-Path -LiteralPath $ArchivePath) {
    throw "Refusing to overwrite an existing Mesa archive: $ArchivePath"
}

Write-Host "mesa.version=$mesaVersion"
Write-Host "mesa.source.commit=$mesaSourceCommit"
Write-Host "mesa.source.url=$mesaSourceUrl"
Write-Host "mesa.license=$mesaLicense"
Invoke-WebRequest -Uri $mesaSourceUrl -OutFile $ArchivePath -MaximumRedirection 5
$archiveSha256 = Assert-Sha256 `
    -Path $ArchivePath `
    -Expected $ExpectedArchiveSha256

$extractRoot = Join-Path $mesaRoot 'extracted'
if (Test-Path -LiteralPath $extractRoot) {
    throw "Refusing to overwrite an existing Mesa extraction directory: $extractRoot"
}
[System.IO.Directory]::CreateDirectory($extractRoot) | Out-Null

$sevenZip = Get-Command '7z.exe' -ErrorAction Stop
& $sevenZip.Source x "-o$extractRoot" -y $ArchivePath `
    'x64\opengl32.dll' `
    'x64\libgallium_wgl.dll'
if ($LASTEXITCODE -ne 0) {
    throw "7-Zip extraction failed with exit code $LASTEXITCODE."
}

$extractedFiles = Get-ChildItem -LiteralPath $extractRoot -Recurse -File
$extractedRelativePaths = $extractedFiles | ForEach-Object {
    [System.IO.Path]::GetRelativePath($extractRoot, $_.FullName).Replace('\', '/')
}
$expectedExtractedPaths = @('x64/opengl32.dll', 'x64/libgallium_wgl.dll')
if (@(Compare-Object $expectedExtractedPaths $extractedRelativePaths).Count -ne 0) {
    throw "Mesa extraction escaped the two-file allowlist: $extractedRelativePaths"
}

$openGlPath = Join-Path $extractRoot 'x64\opengl32.dll'
$galliumPath = Join-Path $extractRoot 'x64\libgallium_wgl.dll'
$openGlSha256 = Assert-Sha256 -Path $openGlPath -Expected $expectedOpenGlSha256
$galliumSha256 = Assert-Sha256 -Path $galliumPath -Expected $expectedGalliumSha256

$knownDllsPath = 'HKLM:\SYSTEM\CurrentControlSet\Control\Session Manager\KnownDLLs'
$knownDllProperties = (Get-ItemProperty -LiteralPath $knownDllsPath).PSObject.Properties
$knownDllNames = $knownDllProperties |
    Where-Object { -not $_.Name.StartsWith('PS') } |
    ForEach-Object {
        @($_.Name.ToLowerInvariant(), ([string]$_.Value).ToLowerInvariant())
    }
$mesaDllNames = @('opengl32.dll', 'libgallium_wgl.dll')
$knownMesaDlls = $mesaDllNames | Where-Object { $_ -in $knownDllNames }
if ($knownMesaDlls.Count -ne 0) {
    throw "Refusing per-JDK Mesa deployment because KnownDLLs contains: $knownMesaDlls"
}
Write-Host 'mesa.known-dlls=PASS'

$resolvedJavaHomes = $JavaHomes |
    Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
    ForEach-Object { [System.IO.Path]::GetFullPath($_) } |
    Select-Object -Unique
if ($resolvedJavaHomes.Count -ne 2) {
    throw "Expected distinct Java 21 and Java 25 homes; found $resolvedJavaHomes"
}

foreach ($javaHome in $resolvedJavaHomes) {
    $javaHomePrefix = $javaHome.TrimEnd(
        [System.IO.Path]::DirectorySeparatorChar,
        [System.IO.Path]::AltDirectorySeparatorChar
    ) + [System.IO.Path]::DirectorySeparatorChar
    if (-not $javaHomePrefix.StartsWith(
            $runnerToolCachePrefix,
            [System.StringComparison]::OrdinalIgnoreCase
        )) {
        throw "JDK home is outside RUNNER_TOOL_CACHE: $javaHome"
    }
    $javaBin = Join-Path $javaHome 'bin'
    if (-not (Test-Path -LiteralPath $javaBin -PathType Container)) {
        throw "JDK bin directory does not exist: $javaBin"
    }

    foreach ($source in @($openGlPath, $galliumPath)) {
        $destination = Join-Path $javaBin ([System.IO.Path]::GetFileName($source))
        if (Test-Path -LiteralPath $destination) {
            throw "Refusing to overwrite an existing JDK file: $destination"
        }
        [System.IO.File]::Copy($source, $destination, $false)
        $expectedDestinationHash = if ($source -eq $openGlPath) {
            $expectedOpenGlSha256
        } else {
            $expectedGalliumSha256
        }
        Assert-Sha256 -Path $destination -Expected $expectedDestinationHash | Out-Null
    }
    Write-Host "mesa.jdk.bin=$javaBin"
}

if ([string]::IsNullOrWhiteSpace($EnvironmentFile)) {
    throw 'GITHUB_ENV is required to publish Mesa provenance to smoke evidence.'
}
@(
    "ENGINE_LITE_MESA_VERSION=$mesaVersion"
    "ENGINE_LITE_MESA_SOURCE_URL=$mesaSourceUrl"
    "ENGINE_LITE_MESA_SOURCE_COMMIT=$mesaSourceCommit"
    "ENGINE_LITE_MESA_ARCHIVE=$ArchivePath"
    "ENGINE_LITE_MESA_ARCHIVE_SHA256=$archiveSha256"
    "ENGINE_LITE_MESA_OPENGL32_SHA256=$openGlSha256"
    "ENGINE_LITE_MESA_LIBGALLIUM_WGL_SHA256=$galliumSha256"
    "ENGINE_LITE_MESA_LICENSE=$mesaLicense"
) | Add-Content -LiteralPath $EnvironmentFile -Encoding UTF8

Write-Host 'mesa.provisioning=PASS'
