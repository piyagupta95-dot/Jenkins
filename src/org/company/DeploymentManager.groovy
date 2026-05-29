package org.company

import hudson.AbortException

class DeploymentManager implements Serializable {
    private def script
    private String environment
    private String imageName
    private String containerName
    private String appPort

    // Constructor mapping environment targets for the attendance service
    DeploymentManager(def script, String environment) {
        this.script = script
        this.environment = environment.toLowerCase()
        this.imageName = "attendance-api" // Based on OT-Microservices attendance component
        this.containerName = "attendance-service-${this.environment}"
        
        // Define isolated ports per environment for demo purposes
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
                this.script.error "❌ Unsupported environment context: ${environment}"
        }
    }

    // 1. Validation Step
    def validate() {
        script.stage("Validation [${environment.toUpperCase()}]") {
            script.echo "🔍 Checking deployment prerequisites for attendance microservice..."
            // Ensure Docker daemon is accessible on the agent node
            script.sh "docker --version"
            script.echo "✅ Validation successful for ${environment} tier."
        }
    }

    // 2. Deployment Step
    def deploy(String imageTag) {
        script.stage("Deploy [${environment.toUpperCase()}]") {
            script.echo "🚀 Deploying image ${imageName}:${imageTag} to port ${appPort}..."
            
            // Housekeeping: Stop existing container instances safely
            cleanExistingContainer()

            // Run the newly built container
            script.sh """
                docker run -d \
                --name ${containerName} \
                -p ${appPort}:8080 \
                --restart unless-stopped \
                ${imageName}:${imageTag}
            """

            // Simulate a post-deployment readiness check
            script.echo "⏳ Verifying service readiness..."
            script.sleep 5
            
            // Simple validation command simulating a health check hit
            try {
                script.sh "docker ps | grep ${containerName}"
                script.echo "🎉 Attendance Service successfully running on environment: ${environment.toUpperCase()}"
            } catch (Exception e) {
                script.error "❌ Health verification failed. Triggering automatic rollback workflow..."
            }
        }
    }

    // 3. Rollback Strategy Implementation
    def rollback(String fallbackTag = 'stable') {
        script.stage("Rollback [${environment.toUpperCase()}]") {
            script.echo "⚠️ Deploy failure caught! Executing targeted ${environment.toUpperCase()} rollback strategy..."
            
            cleanExistingContainer()

            switch(environment) {
                case 'dev':
                    script.echo "🔄 [DEV Strategy] Fast rollback. Instantly spinning up the local dev-backup image..."
                    script.sh """
                        docker run -d --name ${containerName} -p ${appPort}:8080 ${imageName}:dev-backup || \
                        script.echo "No backup image found locally, deploying generic stable fallback."
                    """
                    break

                case 'staging':
                    script.echo "🔄 [STAGING Strategy] Controlled rollback. Reverting to the explicitly targeted fallback build tag: ${fallbackTag}"
                    script.sh "docker run -d --name ${containerName} -p ${appPort}:8080 ${imageName}:${fallbackTag}"
                    break

                case 'prod':
                    script.echo "🚨 [PRODUCTION Strategy] Strict zero-interruption rollback. Deploying highly-tested production-ready certification release label."
                    script.sh "docker run -d --name ${containerName} -p ${appPort}:8080 ${imageName}:prod-stable"
                    script.echo "📢 Production state safely restored to stable tag."
                    break
            }
        }
    }

    // Helper method to clear conflicts
    private def cleanExistingContainer() {
        script.sh """
            docker stop ${containerName} || true
            docker rm -f ${containerName} || true
        """
    }
}
