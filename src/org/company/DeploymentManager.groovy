package org.company

class DeploymentManager implements Serializable {
    def script
    String environment

    // Constructor accepting the pipeline context and target environment
    DeploymentManager(def script, String environment) {
        this.script = script
        this.environment = environment.toLowerCase()
    }

    // Structural validation step evaluating Kubernetes manifest formats
    void validate() {
        script.sh "kubectl dry-run=client -f k8s/${this.environment}/ -o yaml"
    }

    // Execution logic handling Blue/Green for Prod and Rolling updates for non-prod
    void deploy() {
        if (this.environment == 'prod') {
            // Apply updates to the idle green environment
            script.sh "kubectl apply -f k8s/prod/deployment-green.yaml -n production"
            script.sh "kubectl rollout status deployment/my-app-green -n production"
            
            // Route production traffic to the newly validated green cluster
            script.sh "kubectl patch service my-app-router -p '{\"spec\":{\"selector\":{\"version\":\"green\"}}}' -n production"
        } else {
            // Standard rolling update execution for staging and dev
            script.sh "kubectl apply -f k8s/${this.environment}/deployment.yaml -n ${this.environment}"
            script.sh "kubectl rollout status deployment/my-app -n ${this.environment}"
        }
    }

    // Active Rollback Strategy execution
    void rollback() {
        if (this.environment == 'prod') {
            // Instant Blue/Green Rollback: Divert traffic straight back to the stable blue pods
            script.sh "kubectl patch service my-app-router -p '{\"spec\":{\"selector\":{\"version\":\"blue\"}}}' -n production"
        } else {
            // Rolling Rollback: Instruct the cluster cluster control plane to reverse the latest revision
            script.sh "kubectl rollout undo deployment/my-app -n ${this.environment}"
        }
    }
}
