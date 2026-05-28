@Library('company-deployment-lib') _
import org.company.DeploymentManager

def manager 

pipeline {
    agent any // Changed back to 'any'

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['dev', 'staging', 'prod'], description: 'Select the target environment')
    }

    stages {
        stage('Initialize') {
            steps {
                script { manager = new DeploymentManager(this, params.DEPLOY_ENV) }
            }
        }
        stage('Validate') {
            steps {
                script { manager.validate() }
            }
        }
        stage('Deploy') {
            steps {
                script {
                    try {
                        manager.deploy()
                    } catch (Exception e) {
                        echo "Deployment failed: ${e.message}"
                        manager.rollback()
                        currentBuild.result = 'FAILURE'
                    }
                }
            }
        }
    }
}
