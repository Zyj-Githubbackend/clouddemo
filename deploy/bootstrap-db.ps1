$ErrorActionPreference = 'Stop'
if ($PSVersionTable.PSVersion.Major -ge 7) {
    $PSNativeCommandUseErrorActionPreference = $false
}

$mysqlContainer = 'shared-mysql-1'
$mysqlRootPassword = if ($env:MYSQL_ROOT_PASSWORD) { $env:MYSQL_ROOT_PASSWORD } else { '123888' }
$sqlPath = Join-Path $PSScriptRoot 'bootstrap-db.sql'
$containerSqlPath = '/tmp/cloud-demo-bootstrap-db.sql'

if (-not (Test-Path $sqlPath)) {
    throw "Missing SQL bootstrap file: $sqlPath"
}

$ready = $false
for ($i = 0; $i -lt 40; $i++) {
    docker exec -e "MYSQL_PWD=$mysqlRootPassword" $mysqlContainer mysqladmin ping -h 127.0.0.1 -uroot --silent *> $null
    if ($LASTEXITCODE -eq 0) {
        $ready = $true
        break
    }
    Start-Sleep -Seconds 3
}

if (-not $ready) {
    throw "MySQL container '$mysqlContainer' is not ready."
}

docker cp $sqlPath "${mysqlContainer}:$containerSqlPath"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to copy SQL bootstrap file into container."
}

docker exec -e "MYSQL_PWD=$mysqlRootPassword" $mysqlContainer sh -lc "mysql -uroot --default-character-set=utf8mb4 < $containerSqlPath"
if ($LASTEXITCODE -ne 0) {
    docker exec $mysqlContainer rm -f $containerSqlPath *> $null
    throw "Database bootstrap failed."
}

docker exec $mysqlContainer rm -f $containerSqlPath *> $null

Write-Host "Database bootstrap completed."
