pipeline {
    agent any

    environment {
        // Nexus 기본 주소 (호스트 부분만, 리포지토리 경로는 브랜치별로 분기)
        NEXUS_BASE = 'http://nexus.internal:8081'
        // Harbor (이미지 푸시용) - 필요 시 Jenkins 환경변수로 덮어쓰기
        HARBOR_URL = 'http://harbor.internal'
        HARBOR_PROJECT = 'income-backend'
        ARTIFACT_GROUP = 'com/example'
        ARTIFACT_ID = 'toss-backend'
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
                    // main 푸시 또는 태그 푸시(tags/v1.0.0 형태) → 릴리즈
                    def isRelease = branch == 'main' || branch.startsWith('tags/')

                    if (isRelease) {
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

        stage('Download JAR from Nexus') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'nexus-credentials',
                    usernameVariable: 'NEXUS_USER',
                    passwordVariable: 'NEXUS_PASS'
                )]) {
                    sh """
                        set -e
                        MAVEN_PATH="${env.ARTIFACT_GROUP}/${env.ARTIFACT_ID}/${env.PUBLISH_VERSION}"
                        METADATA_URL="${env.NEXUS_URL}\${MAVEN_PATH}/maven-metadata.xml"
                        mkdir -p docker-context

                        if echo "${env.PUBLISH_VERSION}" | grep -q SNAPSHOT; then
                          curl -sS -u "\$NEXUS_USER:\$NEXUS_PASS" "\$METADATA_URL" -o docker-context/maven-metadata.xml
                          SNAPSHOT_VER=\$(sed -n 's/.*<value>\\([^<]*SNAPSHOT[^<]*\\)<\\/value>.*/\\1/p' docker-context/maven-metadata.xml | tail -1)
                          JAR_NAME="${env.ARTIFACT_ID}-\${SNAPSHOT_VER}.jar"
                        else
                          JAR_NAME="${env.ARTIFACT_ID}-${env.PUBLISH_VERSION}.jar"
                        fi

                        JAR_URL="${env.NEXUS_URL}\${MAVEN_PATH}/\${JAR_NAME}"
                        curl -sS -u "\$NEXUS_USER:\$NEXUS_PASS" "\$JAR_URL" -o "docker-context/\$JAR_NAME"
                        cp Dockerfile docker-context/
                        echo "Downloaded: \$JAR_NAME"
                    """
                }
            }
        }

        stage('Docker Build & Push to Harbor') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'harbor-credentials',
                    usernameVariable: 'HARBOR_USER',
                    passwordVariable: 'HARBOR_PASS'
                )]) {
                    sh """
                        set -e
                        IMAGE_TAG="${env.ARTIFACT_ID}:${env.PUBLISH_VERSION}-${env.BUILD_NUMBER}"
                        IMAGE_FULL="${env.HARBOR_URL}/${env.HARBOR_PROJECT}/\${IMAGE_TAG}"
                        HARBOR_HOST=\$(echo "${env.HARBOR_URL}" | sed 's|https\\?://||')
                        echo "\$HARBOR_PASS" | docker login -u "\$HARBOR_USER" --password-stdin "\$HARBOR_HOST"
                        docker build -f docker-context/Dockerfile -t "\$IMAGE_FULL" docker-context
                        docker push "\$IMAGE_FULL"
                        docker logout "\$HARBOR_HOST"
                        echo "Pushed: \$IMAGE_FULL"
                    """
                }
            }
        }
    }
}