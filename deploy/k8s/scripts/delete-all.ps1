$ErrorActionPreference = "Stop"

$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Resolve-Path (Join-Path $ScriptRoot "..\..\..")
$SecretFile = Join-Path $RootDir "deploy\k8s\cloud-demo\secret.yaml"
$IngressNginxManifest = "https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.15.1/deploy/static/provider/cloud/deploy.yaml"

kubectl delete -f (Join-Path $RootDir "deploy\k8s\observability\stack.yaml") --ignore-not-found
kubectl delete -f (Join-Path $RootDir "deploy\k8s\observability\configmaps.yaml") --ignore-not-found
kubectl delete -f (Join-Path $RootDir "deploy\k8s\cloud-demo\ingress.yaml") --ignore-not-found
kubectl delete -f (Join-Path $RootDir "deploy\k8s\cloud-demo\edge-nginx.yaml") --ignore-not-found
kubectl delete -f (Join-Path $RootDir "deploy\k8s\cloud-demo\apps.yaml") --ignore-not-found
kubectl delete -f (Join-Path $RootDir "deploy\k8s\cloud-demo\middleware.yaml") --ignore-not-found
kubectl delete -f (Join-Path $RootDir "deploy\k8s\cloud-demo\configmap.yaml") --ignore-not-found

if (Test-Path $SecretFile) {
    kubectl delete -f $SecretFile --ignore-not-found
}

kubectl delete -f (Join-Path $RootDir "deploy\k8s\namespaces.yaml") --ignore-not-found
kubectl delete -f $IngressNginxManifest --ignore-not-found
Write-Host "Delete requests submitted."
