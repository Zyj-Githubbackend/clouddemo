$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')

Push-Location $repoRoot
try {
    docker compose -p edge -f compose.edge.yml down
    docker compose -p stack-b --env-file deploy/stack-b.env -f compose.stack.yml down
    docker compose -p stack-a --env-file deploy/stack-a.env -f compose.stack.yml down
    docker compose -p shared -f compose.shared.yml down
}
finally {
    Pop-Location
}
