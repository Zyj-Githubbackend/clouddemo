$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')

Push-Location $repoRoot
try {
    docker compose -p stack-a --env-file deploy/stack-a.env -f compose.stack.yml up -d --build
}
finally {
    Pop-Location
}
