$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')

Push-Location $repoRoot
try {
    docker compose -p edge -f compose.edge.yml up -d --build
}
finally {
    Pop-Location
}
