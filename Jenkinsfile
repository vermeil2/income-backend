pipeline {
    agent any

    environment {
        // Nexus 기본 주소 (호스트 부분만, 리포지토리 경로는 브랜치별로 분기)
        NEXUS_BASE = 'http://nexus.internal:8081'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Determine Version & Repo') {
            steps {
                script {
                    def branch = env.BRANCH_NAME ?: env.GIT_BRANCH?.replace('origin/', '') ?: 'dev'
                    def isMain = branch == 'main'

                    if (isMain) {
                        // main: git 태그 기반 버전 (v1.0.0 형태에서 v 제거, 태그 없으면 fallback)
                        env.PUBLISH_VERSION = sh(
                            script: """
                                RAW=\$(git describe --tags --exact-match 2>/dev/null || echo "0.0.0-${env.BUILD_NUMBER}")
                                echo "\$RAW" | sed 's/^v//'
                            """,
                            returnStdout: true
                        ).trim()
                        env.NEXUS_URL = "${env.NEXUS_BASE}/repository/income-releases/"
                    } else {
                        // dev: 스냅샷 (덮어쓰기) - 고정 SNAPSHOT 버전 사용
                        env.PUBLISH_VERSION = '0.0.1-SNAPSHOT'
                        env.NEXUS_URL = "${env.NEXUS_BASE}/repository/income-snapshots/"
                    }
                    echo "Branch: ${branch} | Version: ${env.PUBLISH_VERSION} | Repo: ${env.NEXUS_URL}"
                }
            }
        }
        
        stage('Build & Test') {
            steps {
                sh 'chmod +x gradlew'
                sh "./gradlew clean build -Pversion=${env.PUBLISH_VERSION}"
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
                            -Pversion=${env.PUBLISH_VERSION} \
                            -PnexusUrl=${env.NEXUS_URL} \
                            -PnexusUsername=\$NEXUS_USER \
                            -PnexusPassword=\$NEXUS_PASS
                    """
                }
            }
        }
    }
}