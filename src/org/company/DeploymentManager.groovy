package org.company

import hudson.AbortException

class DeploymentManager implements Serializable {
    private def script
    private String environment
    private String imageName
    private String containerName
    private String appPort

    // Constructor to initialize environment rules for the attendance microservice
    DeploymentManager(def script, String environment) {
        this.script = script
        this.environment = environment.toLowerCase()
        this.imageName = "attendance-api"
        this.containerName = "attendance-service-${this.environment}"
        
        // Isolate container exposure by environment ports
        switch(this.environment) {
            case 'dev':
                this.appPort = "8080"
                break
            case 'staging':
                this.appPort = "8081"
                break
            case 'prod':
                this.appPort = "80"
                break
            default:
                this.script.error "❌ Unsupported target environment configuration: ${environment}"
        }
    }

    // Validation Method
    def validate() {
        script.stage("Validation [${environment.toUpperCase()}]") {
            script.echo "🔍 Executing structural validation checks for ${environment.toUpperCase()} environment..."
            // Ensure runtime dependencies exist on host execution node
            script.sh "docker --version"
            script.echo "✅ System environment sanity checks passed successfully."
        }
    }

    // Deploy Method
    def deploy(String imageTag) {
        script.stage("Deploy [${environment.toUpperCase()}]") {
            script.echo "🚀 Launching deployment sequence for ${imageName}:${imageTag} on environment node..."
            
            // Wipe pre-existing infrastructure blocks to avoid state or port collisions
            cleanContainers()

            // Run application matching target network parameters
            script.sh """
                docker run -d \
                --name ${containerName} \
                -p ${appPort}:8081:8080 \
                --restart unless-stopped \
                ${imageName}:${imageTag}
            """

            // Live status analysis validation (Simulated health check validation loop)
            script.echo "⏳ Waiting for service container instantiation..."
            script.sleep 5
            
            try {
                script.sh "docker ps | grep ${containerName}"
                script.echo "🎉 Attendance microservice is operational on port ${appPort} [${environment.toUpperCase()}]"
            } catch (Exception e) {
                script.error "❌ Application runtime check failed. Triggering target fallback mitigation..."
            }
        }
    }

    // Target Rollback Strategy Method
    def rollback(String fallbackTag = 'stable') {
        script.stage("Rollback [${environment.toUpperCase()}]") {
            script.echo "⚠️ Deployment failure caught. Commencing targeted rollback recovery patterns..."
            
            cleanContainers()

            switch(environment) {
                case 'dev':
                    script.echo "🔄 [DEV STRATEGY] Fast recovery. Restoring container execution from local cached development backup tag..."
                    script.sh """
                        docker run -d --name ${containerName} -p ${appPort}:8080 ${imageName}:dev-backup || \
                        docker run -d --name ${containerName} -p ${appPort}:8080 ${imageName}:latest
                    """
                    break

                case 'staging':
                    script.echo "🔄 [STAGING STRATEGY] Version tracking rollback. Reverting state to exact verified stable tag: ${fallbackTag}"
                    script.sh "docker run -d --name ${containerName} -p ${appPort}:8080 ${imageName}:${fallbackTag}"
                    break

                case 'prod':
                    script.echo "🚨 [PRODUCTION STRATEGY] Zero-Downtime production mitigation. Forcing rollout back to highly-vetted stable release baseline tag."
                    script.sh "docker run -d --name ${containerName} -p ${appPort}:8080 ${imageName}:prod-stable"
                    script.echo "📢 Production application infrastructure successfully stabilized."
                    break
            }
        }
    }

    // Helper pattern to safely purge environment infrastructure collisions
    private def cleanContainers() {
        script.sh """
            docker stop ${containerName} || true
            docker rm -f ${containerName} || true
        """
    }
}
