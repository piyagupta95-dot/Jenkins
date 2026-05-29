package org.company

class DeploymentManager implements Serializable {
    def script
    String environment
    String namespace

    // Constructor accepting the pipeline context and target environment
    DeploymentManager(def script, String environment) {
        this.script = script
        this.environment = environment.toLowerCase()
        
        // Maps 'prod' to the 'production' namespace, while dev and staging use their own names
        this.namespace = (this.environment == 'prod') ? 'production' : this.environment
    }

    // Structural validation step evaluating Kubernetes manifest formats
    void validate() {
        script.sh "kubectl dry-run=client -f k8s/${this.environment}/ -o yaml"
    }

    // Execution logic handling Blue/Green universally across all environments
    void deploy() {
        // Apply updates to the idle green environment dynamically per environment path
        script.sh "kubectl apply -f k8s/${this.environment}/deployment-green.yaml -n ${this.namespace}"
        script.sh "kubectl rollout status deployment/my-app-green -n ${this.namespace}"
        
        // Route environment-specific traffic to the newly validated green cluster
        script.sh "kubectl patch service my-app-router -p '{\"spec\":{\"selector\":{\"version\":\"green\"}}}' -n ${this.namespace}"
    }

    // Active Blue/Green Rollback Strategy executed universally
    void rollback() {
        // Instant Blue/Green Rollback: Divert traffic straight back to the stable blue pods
        script.sh "kubectl patch service my-app-router -p '{\"spec\":{\"selector\":{\"version\":\"blue\"}}}' -n ${this.namespace}"
    }
}
