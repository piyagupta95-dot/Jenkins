package org.company

class DeploymentManager implements Serializable {
    def script
    String environment

    // Constructor with the pipeline context and environment parameter
    DeploymentManager(def script, String environment) {
        this.script = script
        this.environment = environment.toLowerCase()
    }

    void validate() {
        script.echo "Step 1: Validating configuration for the [${this.environment}] environment..."
    }

    void deploy() {
        script.echo "Step 2: Deploying application to [${this.environment}]..."
        
        if (this.environment == 'prod') {
            script.echo "PROD: Deploying new release to the 'Green' (idle) environment..."
            script.echo "PROD: Running health checks on the 'Green' environment..."
            script.echo "PROD: Success! Flipping load balancer traffic from 'Blue' to 'Green'."
        } else {
            script.echo "${this.environment.toUpperCase()}: Executing standard rolling update."
        }
    }

    void rollback() {
        script.echo "ALERT: Deployment failed or rollback triggered for [${this.environment}]!"
        
        if (this.environment == 'prod') {
            script.echo "--- INITIATING BLUE/GREEN ROLLBACK ---"
            script.echo "PROD ROLLBACK: Re-routing all live traffic back to the 'Blue' (previous stable) environment."
            
            // Example of what the actual shell command might look like in Kubernetes:
            // script.sh "kubectl patch service my-app-service -p '{\"spec\":{\"selector\":{\"version\":\"blue\"}}}' -n production"
            
            script.echo "PROD ROLLBACK: Traffic successfully restored to the stable 'Blue' version. Downtime avoided."
            script.echo "--------------------------------------"
        } else {
            // Dev and Staging usually don't pay for the duplicate infrastructure required for Blue/Green
            script.echo "${this.environment.toUpperCase()} ROLLBACK: Executing standard rollback to previous replica set."
            
            // Example Kubernetes command for standard environments:
            // script.sh "kubectl rollout undo deployment/my-app -n ${this.environment}"
        }
    }
}
