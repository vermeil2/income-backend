pipeline {
    agent any

    environment {
        // Nexus VM 주소 (예: http://192.168.x.x:8081/repository/maven-snapshots/)
        NEXUS_URL = 'http://192.168.106.130:8081/repository/income/'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build & Test') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew clean build'
            }
        }

        stage('Publish to Nexus') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'nexus-credentials',
                    usernameVariable: 'NEXUS_USER',
                    passwordVariable: 'NEXUS_PASS'
                )]) {
                    sh """
                        ./gradlew publish \
                            -PnexusUrl=${env.NEXUS_URL} \
                            -PnexusUsername=\$NEXUS_USER \
                            -PnexusPassword=\$NEXUS_PASS
                    """
                }
            }
        }
    }
}