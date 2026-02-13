pipeline {
    agent any

    environment {
        // Nexus 기본 주소 (호스트 부분만, 리포지토리 경로는 브랜치별로 분기)
        NEXUS_BASE = 'http://nexus.internal:8081'
        // Harbor (이미지 푸시용) - HTTPS 사용, 필요 시 Jenkins 환경변수로 덮어쓰기
        HARBOR_URL = 'https://pjjharbor.com'
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

        // 정적 코드 분석: Jenkins에 등록한 SonarQube 서버 이름과 일치시킬 것 (기본: SonarQube)
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh """
                        ./gradlew sonar \
                            -Pversion=${env.PUBLISH_VERSION} \
                            -Dsonar.gradle.skipCompile=true \
                            -Dsonar.host.url=\$SONAR_HOST_URL \
                            -Dsonar.token=\$SONAR_AUTH_TOKEN
                    """
                }
            }
        }

        // Quality Gate 확인 및 Slack 알림
        // Slack 알림을 사용하려면 Jenkins 환경 변수 SLACK_WEBHOOK_URL 설정 또는
        // 자격 증명 'slack-webhook-url' (Secret text) 등록 필요
        stage('Quality Gate') {
            steps {
                script {
                    // SonarQube 서버가 분석 결과를 처리하는 동안 대기 (최대 10분)
                    timeout(time: 10, unit: 'MINUTES') {
                        def qg = waitForQualityGate abortPipeline: false
                        echo "=== Quality Gate 상태: ${qg.status} ==="
                        
                        // Quality Gate 결과에 따른 Slack 알림 (성공/실패 모두 전송)
                        def slackWebhookUrl = env.SLACK_WEBHOOK_URL
                        
                        if (slackWebhookUrl) {
                            echo "=== Slack 알림 전송 시도 ==="
                            def branch = env.BRANCH_NAME ?: env.GIT_BRANCH?.replace('origin/', '') ?: 'dev'
                            def status = qg.status
                            def statusEmoji = status == 'OK' ? '✅' : '❌'
                            def statusText = status == 'OK' ? '통과' : '실패'
                            def color = status == 'OK' ? 'good' : 'danger'
                            
                            def payload = """
                            {
                                "text": "${statusEmoji} SonarQube Quality Gate ${statusText}",
                                "attachments": [
                                    {
                                        "color": "${color}",
                                        "fields": [
                                            {
                                                "title": "프로젝트",
                                                "value": "${env.ARTIFACT_ID}",
                                                "short": true
                                            },
                                            {
                                                "title": "브랜치",
                                                "value": "${branch}",
                                                "short": true
                                            },
                                            {
                                                "title": "빌드 번호",
                                                "value": "#${env.BUILD_NUMBER}",
                                                "short": true
                                            },
                                            {
                                                "title": "Quality Gate 상태",
                                                "value": "${status}",
                                                "short": true
                                            }
                                        ],
                                        "footer": "Jenkins",
                                        "ts": ${System.currentTimeMillis() / 1000}
                                    }
                                ]
                            }
                            """.trim()
                            
                            try {
                                def curlResult = sh(
                                    script: """
                                        curl -X POST -H 'Content-type: application/json' \
                                            --data '${payload}' \
                                            --write-out '%{http_code}' \
                                            --silent --show-error \
                                            ${slackWebhookUrl}
                                    """,
                                    returnStdout: true
                                ).trim()
                                
                                echo "Slack 알림 전송 결과 HTTP 코드: ${curlResult}"
                                if (curlResult == '200') {
                                    echo "✅ Slack 알림 전송 성공"
                                } else {
                                    echo "⚠️ Slack 알림 전송 실패 (HTTP ${curlResult})"
                                }
                            } catch (Exception e) {
                                echo "❌ Slack 알림 전송 중 오류: ${e.message}"
                            }
                        } else {
                            echo "⚠️ SLACK_WEBHOOK_URL 환경 변수가 설정되지 않아 Slack 알림을 전송하지 않습니다."
                            echo "   Slack 알림을 사용하려면 Jenkins → Manage Jenkins → Configure System → Global properties → Environment variables에 SLACK_WEBHOOK_URL을 추가하세요."
                        }
                        
                        // Quality Gate 실패 시 파이프라인 실패 처리 (선택사항)
                        // abortPipeline: false로 설정했으므로 여기서 명시적으로 실패 처리
                        if (qg.status != 'OK') {
                            error("SonarQube Quality Gate failed: ${qg.status}")
                        }
                    }
                }
            }
        }

        // SonarQube 브랜치 테스트용: Nexus/Harbor 단계 비활성화 (복구 시 when 블록 삭제)
        stage('Publish to Nexus') {
            when { expression { return false } }
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
            when { expression { return false } }
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
            when { expression { return false } }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'harbor-credentials',
                    usernameVariable: 'HARBOR_USER',
                    passwordVariable: 'HARBOR_PASS'
                )]) {
                    sh """
                        set -e
                        HARBOR_HOST=\$(echo "${env.HARBOR_URL}" | sed 's|https\\?://||')
                        IMAGE_TAG="${env.ARTIFACT_ID}:${env.PUBLISH_VERSION}-${env.BUILD_NUMBER}"
                        IMAGE_FULL="\${HARBOR_HOST}/${env.HARBOR_PROJECT}/\${IMAGE_TAG}"
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