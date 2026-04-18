$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')

Push-Location $repoRoot
try {
    docker compose -p stack-b --env-file deploy/stack-b.env -f compose.stack.yml up -d --build
}
finally {
    Pop-Location
}
