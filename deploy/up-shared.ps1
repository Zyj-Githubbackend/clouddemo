$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')

Push-Location $repoRoot
try {
    docker compose -p shared -f compose.shared.yml up -d --build
    & (Join-Path $PSScriptRoot 'bootstrap-db.ps1')
}
finally {
    Pop-Location
}
