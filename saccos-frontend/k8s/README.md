# Kubernetes Deployment for SACCOS Frontend

This directory contains Kubernetes manifests for deploying the SACCOS frontend using ArgoCD.

## Files

- **namespace.yaml** - Creates the `saccos` namespace
- **deployment.yaml** - Defines the frontend deployment with 3 replicas, resource limits, health checks
- **service.yaml** - Exposes the deployment as a ClusterIP service
- **configmap.yaml** - Contains environment variables and nginx configuration
- **hpa.yaml** - Horizontal Pod Autoscaler for auto-scaling (optional)
- **ingress.yaml** - Routes external traffic to the service (requires nginx ingress controller) - REMOVED

## Prerequisites

1. Kubernetes cluster (v1.20+)
2. ArgoCD installed on the cluster
3. nginx ingress controller installed
4. cert-manager (optional, for HTTPS)

## Setup Instructions

### 1. Update the image registry

Edit `deployment.yaml` and replace the image:
```yaml
image: your-registry/saccos-frontend:latest
```

Example:
```yaml
image: docker.io/yourusername/saccos-frontend:latest
```

### 2. Update the domain name

Edit `ingress.yaml` and replace `saccos.example.com` with your actual domain:
```yaml
- host: saccos.example.com
```

### 3. Update API backend URL

Edit `configmap.yaml` and update the API URL:
```yaml
api-url: "http://saccos-backend:8080"
```

### 4. Create ArgoCD Application

Create an ArgoCD Application manifest in your GitOps repo:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: saccos-frontend
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/yourusername/Utsaccos-system
    targetRevision: main
    path: saccos-frontend/k8s
  destination:
    server: https://kubernetes.default.svc
    namespace: saccos
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
    - CreateNamespace=true
```

### 5. Deploy with kubectl (alternative to ArgoCD)

```bash
# Apply all manifests
kubectl apply -f namespace.yaml
kubectl apply -f configmap.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml

# Or apply entire directory
kubectl apply -f .
```

### 6. Access the application

Since ingress is not configured, use port forwarding:

```bash
kubectl port-forward -n saccos svc/saccos-frontend 8080:80
```

Then access at: `http://localhost:8080`

## Verify Deployment

```bash
# Check deployment status
kubectl get deployment -n saccos

# Check pods
kubectl get pods -n saccos

# Check service
kubectl get svc -n saccos

# Check ingress
kubectl get ingress -n saccos

# View logs
kubectl logs -n saccos -l app=saccos-frontend -f
```

## Environment Variables

The following environment variables are available in the deployment:

- `VITE_API_URL` - Backend API base URL (from ConfigMap)

## Resource Allocation

- **Requests**: 100m CPU, 128Mi Memory (guaranteed)
- **Limits**: 500m CPU, 512Mi Memory (maximum)

Adjust these values based on your traffic patterns.

## Auto-Scaling (Optional)

To enable Horizontal Pod Autoscaling, create an HPA manifest:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: saccos-frontend
  namespace: saccos
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: saccos-frontend
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

## Troubleshooting

### Pods not starting
```bash
kubectl describe pod <pod-name> -n saccos
```

### Service not accessible
Check if the service is running and use port forwarding:
```bash
kubectl get svc -n saccos
kubectl port-forward -n saccos svc/saccos-frontend 8080:80
```

Then access at: `http://localhost:8080`

### Logs
```bash
kubectl logs <pod-name> -n saccos
```

## For ArgoCD Sync

Once the repository is pushed to GitHub, ArgoCD will automatically detect and sync the manifests when you create the Application resource above.
