package org.company

class DeploymentManager implements Serializable {
    def script
    String environment

    // Constructor with the pipeline context and environment parameter
    DeploymentManager(def script, String environment) {
        this.script = script
        this.environment = environment
    }

    void validate() {
        // Simple echo statement using the environment variable
        script.echo "Step 1: Validating configuration for the [${environment}] environment..."
    }

    void deploy() {
        script.echo "Step 2: Deploying application to [${environment}]..."
        script.echo "Success: [${environment}] deployment is complete!"
    }

    void rollback() {
        script.echo "ALERT: Something went wrong!"
        script.echo "Rolling back the [${environment}] environment to the previous stable version..."
    }
}
