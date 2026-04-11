param(
    [Parameter(Mandatory = $true)]
    [string]$McpBaseUrl,

    [Parameter(Mandatory = $true)]
    [string]$Username,

    [Parameter(Mandatory = $true)]
    [string]$Password,

    [switch]$ShowMetadata
)

$body = @{
    username = $Username
    password = $Password
} | ConvertTo-Json

$normalizedBaseUrl = $McpBaseUrl.TrimEnd('/')

$response = Invoke-RestMethod `
    -Method Post `
    -Uri ($normalizedBaseUrl + "/mcp/auth/login") `
    -ContentType "application/json" `
    -Body $body

if (-not $response.token) {
    throw "Login succeeded but token is empty."
}

if ($ShowMetadata) {
    Write-Host "Role: $($response.role)"
    Write-Host "Username: $($response.username)"
    Write-Host "MCP endpoint: $normalizedBaseUrl/mcp"
    Write-Host "Token:"
}

Write-Output $response.token
