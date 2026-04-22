$ErrorActionPreference = "Stop"

$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ComposeFile = Join-Path $ScriptRoot "docker-compose.yml"
$EnvFile = Join-Path $ScriptRoot ".env"

$Args = @("compose", "-p", "cloud-demo")
if (Test-Path $EnvFile) {
    $Args += @("--env-file", $EnvFile)
}
$Args += @("-f", $ComposeFile, "down")

docker @Args
