pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                // Jenkins Job에서 설정한 SCM 정보 사용
                checkout scm
            }
        }
        
        stage('Gradle Build') {
            steps {
                // Gradle 빌드 (테스트 제외)
                sh './gradlew clean build -x test'
            }
        }        
    }
}