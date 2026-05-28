package org.company

class DeploymentManager implements Serializable {
    def script
    String environment

    DeploymentManager(def script, String environment) {
        this.script = script
        this.environment = environment.toLowerCase()
    }

    void validate() {
        // EXECUTING REAL VALIDATION: Linting the infrastructure code
        // This will physically fail the pipeline if there are syntax errors in the YAML files
        script.sh "helm lint ./k8s-manifests --values ./k8s-manifests/values-${this.environment}.yaml"
    }

    void deploy() {
        if (this.environment == 'prod') {
            // EXECUTING REAL BLUE/GREEN DEPLOYMENT

            // 1. Deploy the new code to the 'Green' deployment
            script.sh "kubectl apply -f k8s-manifests/deployment-green.yaml -n production"
            
            // 2. Wait for the Green pods to actually become healthy and ready
            script.timeout(time: 5, unit: 'MINUTES') {
                script.sh "kubectl rollout status deployment/my-app-green -n production"
            }
            
            // 3. Flip the LoadBalancer/Service traffic instantly from 'blue' to 'green'
            script.sh "kubectl patch service my-app-service -p '{\"spec\":{\"selector\":{\"version\":\"green\"}}}' -n production"
            
        } else {
            // EXECUTING REAL ROLLING DEPLOYMENT (Dev/Staging)
            
            // 1. Apply the standard manifest
            script.sh "kubectl apply -f k8s-manifests/deployment.yaml -n ${this.environment}"
            
            // 2. Wait for the rollout to complete
            script.sh "kubectl rollout status deployment/my-app -n ${this.environment}"
        }
    }

    void rollback() {
        if (this.environment == 'prod') {
            // EXECUTING REAL BLUE/GREEN ROLLBACK
            
            // Instantly patch the service back to point at the 'blue' (stable) pods
            script.sh "kubectl patch service my-app-service -p '{\"spec\":{\"selector\":{\"version\":\"blue\"}}}' -n production"
            
        } else {
            // EXECUTING REAL ROLLING ROLLBACK (Dev/Staging)
            
            // Tell Kubernetes to undo the previous deployment
            script.sh "kubectl rollout undo deployment/my-app -n ${this.environment}"
        }
    }
}
