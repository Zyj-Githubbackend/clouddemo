$ErrorActionPreference = "Stop"

$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Resolve-Path (Join-Path $ScriptRoot "..\..\..")
$SqlFile = Join-Path $RootDir "deploy\common\bootstrap-db.sql"

if (-not (Test-Path $SqlFile)) {
    Write-Error "Missing $SqlFile"
}

kubectl -n cloud-demo rollout status statefulset/mysql --timeout=300s | Out-Null
$MysqlPod = kubectl -n cloud-demo get pod -l app=mysql -o jsonpath='{.items[0].metadata.name}'

if ([string]::IsNullOrWhiteSpace($MysqlPod)) {
    Write-Error "MySQL pod was not found in namespace cloud-demo."
}

$PodSqlFile = "/tmp/bootstrap-db.sql"
Push-Location $RootDir
kubectl -n cloud-demo cp "deploy/common/bootstrap-db.sql" "${MysqlPod}:${PodSqlFile}"
$CopyExitCode = $LASTEXITCODE
Pop-Location
if ($CopyExitCode -ne 0) {
    Write-Error "Failed to copy $SqlFile into MySQL pod $MysqlPod."
}

kubectl -n cloud-demo exec $MysqlPod -- sh -c "mysql --default-character-set=utf8mb4 -uroot -p`"`$MYSQL_ROOT_PASSWORD`" < $PodSqlFile"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to import $SqlFile into MySQL pod $MysqlPod."
}

Write-Host "Imported $SqlFile into MySQL pod $MysqlPod."
