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
                // gradlew 실행 권한 부여 (리눅스 Jenkins용)
                sh 'chmod +x gradlew'
                // Gradle 빌드 (테스트 제외)
                sh './gradlew clean build -x test'
            }
        }        
    }
}