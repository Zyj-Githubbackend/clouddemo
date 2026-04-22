$ErrorActionPreference = "Stop"

$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Resolve-Path (Join-Path $ScriptRoot "..\..\..")
$SecretFile = Join-Path $RootDir "deploy\k8s\cloud-demo\secret.yaml"
$IngressNginxManifest = "https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.15.1/deploy/static/provider/cloud/deploy.yaml"

if (-not (Test-Path $SecretFile)) {
    Write-Error "Missing $SecretFile. Create it from deploy/k8s/cloud-demo/secret.example.yaml first."
}

kubectl apply -f (Join-Path $RootDir "deploy\k8s\namespaces.yaml")
kubectl -n ingress-nginx get deployment/ingress-nginx-controller *> $null
if ($LASTEXITCODE -ne 0) {
    kubectl apply -f $IngressNginxManifest
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to install ingress-nginx from $IngressNginxManifest"
    }
} else {
    Write-Host "ingress-nginx controller already exists."
}
kubectl -n ingress-nginx rollout status deployment/ingress-nginx-controller --timeout=300s
if ($LASTEXITCODE -ne 0) {
    Write-Error "ingress-nginx controller did not become ready."
}
kubectl apply -f $SecretFile
kubectl apply -f (Join-Path $RootDir "deploy\k8s\cloud-demo\configmap.yaml")
kubectl apply -f (Join-Path $RootDir "deploy\k8s\cloud-demo\middleware.yaml")
kubectl apply -f (Join-Path $RootDir "deploy\k8s\cloud-demo\apps.yaml")
kubectl apply -f (Join-Path $RootDir "deploy\k8s\cloud-demo\edge-nginx.yaml")
kubectl apply -f (Join-Path $RootDir "deploy\k8s\cloud-demo\ingress.yaml")
kubectl apply -f (Join-Path $RootDir "deploy\k8s\observability\configmaps.yaml")
kubectl apply -f (Join-Path $RootDir "deploy\k8s\observability\stack.yaml")

Write-Host "Applied Kubernetes resources."
Write-Host "Next: run deploy/k8s/scripts/init-db.ps1 to import deploy/common/bootstrap-db.sql"
