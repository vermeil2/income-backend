pipeline {
    agent any

    environment {
        // Nexus VM 주소 (trailing slash 제거 권장 - Broken pipe 이슈 완화)
        NEXUS_URL = 'http://192.168.106.130:8081/repository/maven-snapshots/'
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
                        export GRADLE_OPTS="-Dhttp.connectionTimeout=120000 -Dhttp.socketTimeout=120000 -Dhttp.keepAlive=false"
                        ./gradlew publish --no-daemon \
                            -PnexusUrl=${env.NEXUS_URL} \
                            -PnexusUsername=\$NEXUS_USER \
                            -PnexusPassword=\$NEXUS_PASS
                    """
                }
            }
        }
    }
}