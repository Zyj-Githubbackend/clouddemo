$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')

function Wait-ContainerHealthy {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [int]$TimeoutSeconds = 600
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $status = docker inspect --format "{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}" $Name 2>$null
        if ($LASTEXITCODE -eq 0) {
            $status = $status.Trim()
            if ($status -eq 'healthy' -or $status -eq 'running') {
                return
            }
            if ($status -eq 'unhealthy' -or $status -eq 'exited' -or $status -eq 'dead') {
                throw "Container '$Name' is in bad status: $status"
            }
        }
        Start-Sleep -Seconds 3
    }

    throw "Timeout waiting for container '$Name' to become healthy/running."
}

Push-Location $repoRoot
try {
    Wait-ContainerHealthy -Name 'shared-mysql-1' -TimeoutSeconds 600
    Wait-ContainerHealthy -Name 'shared-mcp-service-1' -TimeoutSeconds 600
    Wait-ContainerHealthy -Name 'stack-a-gateway-service-1' -TimeoutSeconds 600
    Wait-ContainerHealthy -Name 'stack-b-gateway-service-1' -TimeoutSeconds 600
    Wait-ContainerHealthy -Name 'edge-edge-nginx-1' -TimeoutSeconds 600

    $body = @{ username = 'admin'; password = 'password123' } | ConvertTo-Json -Compress
    $resA = Invoke-RestMethod -Uri 'http://localhost:8081/api/user/login' -Method Post -Body $body -ContentType 'application/json' -Headers @{ 'X-Stack-Id' = 'a' }
    if ($resA.code -ne 200) {
        throw "Stack A login smoke test failed: $($resA | ConvertTo-Json -Depth 5)"
    }

    $resB = Invoke-RestMethod -Uri 'http://localhost:8081/api/user/login' -Method Post -Body $body -ContentType 'application/json' -Headers @{ 'X-Stack-Id' = 'b' }
    if ($resB.code -ne 200) {
        throw "Stack B login smoke test failed: $($resB | ConvertTo-Json -Depth 5)"
    }

    Write-Host 'Health verification passed.'
}
finally {
    Pop-Location
}
