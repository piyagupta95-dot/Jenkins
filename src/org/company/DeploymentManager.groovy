package org.company

class DeploymentManager implements Serializable {
    def script
    String environment

    DeploymentManager(def script, String environment) {
        this.script = script
        this.environment = environment.toLowerCase()
    }

    void validate() {
        script.echo "VALIDATE: Simulating Helm Linting for ${this.environment}..."
    }

    void deploy() {
        if (this.environment == 'prod') {
            script.echo "PROD: Executing Blue/Green Deployment strategy..."
        } else {
            script.echo "${this.environment.toUpperCase()}: Executing standard rolling update."
        }
    }

    void rollback() {
        script.echo "ROLLBACK: Reverting deployment for ${this.environment}."
    }
}
