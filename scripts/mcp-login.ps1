param(
    [Parameter(Mandatory = $true)]
    [string]$McpBaseUrl,

    [Parameter(Mandatory = $true)]
    [string]$Username,

    [Parameter(Mandatory = $true)]
    [string]$Password,

    [string]$TokenEnvVar = "CLOUD_DEMO_MCP_BEARER_TOKEN"
)

$body = @{
    username = $Username
    password = $Password
} | ConvertTo-Json

$response = Invoke-RestMethod `
    -Method Post `
    -Uri ($McpBaseUrl.TrimEnd('/') + "/mcp/auth/login") `
    -ContentType "application/json" `
    -Body $body

if (-not $response.token) {
    throw "Login succeeded but token is empty."
}

[System.Environment]::SetEnvironmentVariable($TokenEnvVar, $response.token, "Process")
[Environment]::SetEnvironmentVariable($TokenEnvVar, $response.token, "User")

Write-Host "Stored token in user environment variable $TokenEnvVar"
Write-Host "Role: $($response.role)"
Write-Host "Username: $($response.username)"
Write-Host "MCP endpoint: $McpBaseUrl/mcp"
