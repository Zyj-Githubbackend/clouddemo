$ErrorActionPreference = 'Stop'

& (Join-Path $PSScriptRoot 'up-shared.ps1')
& (Join-Path $PSScriptRoot 'up-stack-a.ps1')
& (Join-Path $PSScriptRoot 'up-stack-b.ps1')
& (Join-Path $PSScriptRoot 'up-edge.ps1')
& (Join-Path $PSScriptRoot 'verify-health.ps1')
