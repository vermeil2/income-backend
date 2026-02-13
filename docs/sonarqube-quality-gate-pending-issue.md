# SonarQube Quality Gate PENDING 상태 문제 해결

## 문제 상황

Jenkins 파이프라인에서 `waitForQualityGate` 단계가 **PENDING** 상태에서 멈추는 경우가 있습니다.

```
SonarQube task 'xxx-xxx-xxx' status is 'PENDING'
```

## 원인

1. **정상적인 경우**: SonarQube 서버가 분석 결과를 처리하는 중 (보통 1-5분 소요)
2. **비정상적인 경우**:
   - SonarQube 서버가 응답하지 않거나 느림
   - 타임아웃 설정이 없어서 무한 대기
   - SonarQube 서버에 문제가 있음

## 해결 방법

### 1. 타임아웃 추가 (권장)

`waitForQualityGate`를 `timeout` 블록으로 감싸서 최대 대기 시간을 설정합니다.

```groovy
stage('Quality Gate') {
    steps {
        script {
            timeout(time: 10, unit: 'MINUTES') {
                def qg = waitForQualityGate abortPipeline: false
                
                // Quality Gate 결과 처리
                if (qg.status != 'OK') {
                    error("SonarQube Quality Gate failed: ${qg.status}")
                }
            }
        }
    }
}
```

**장점**:
- 최대 10분 후 자동으로 타임아웃되어 파이프라인이 멈추지 않음
- SonarQube 서버가 응답하지 않아도 파이프라인이 계속 진행 가능

### 2. SonarQube 서버 상태 확인

PENDING 상태가 계속되면 SonarQube 서버를 확인하세요:

1. **SonarQube 웹 UI 접속**
   - `http://sonarqube.internal:9000` 접속
   - 로그인 후 **Administration** → **System** → **Background Tasks** 확인
   - 분석 작업이 진행 중인지, 에러가 있는지 확인

2. **서버 로그 확인**
   ```bash
   # SonarQube 서버에서
   tail -f /opt/sonarqube/logs/sonar.log
   ```

3. **네트워크 연결 확인**
   - Jenkins에서 SonarQube 서버로 접근 가능한지 확인
   - 방화벽/보안 그룹 설정 확인

### 3. 대기 시간 조정

프로젝트가 크거나 분석이 오래 걸리는 경우 타임아웃을 늘릴 수 있습니다:

```groovy
timeout(time: 15, unit: 'MINUTES') {  // 10분 → 15분
    def qg = waitForQualityGate abortPipeline: false
    // ...
}
```

### 4. PENDING 상태 무시하고 계속 진행 (비권장)

Quality Gate 확인을 건너뛰고 파이프라인을 계속 진행하려면:

```groovy
stage('Quality Gate') {
    steps {
        script {
            try {
                timeout(time: 5, unit: 'MINUTES') {
                    def qg = waitForQualityGate abortPipeline: false
                    echo "Quality Gate status: ${qg.status}"
                }
            } catch (Exception e) {
                echo "Quality Gate check timeout or failed: ${e.message}"
                echo "Continuing pipeline..."
                // 파이프라인 계속 진행
            }
        }
    }
}
```

**주의**: 이 방법은 Quality Gate를 우회하므로 권장하지 않습니다.

## 일반적인 대기 시간

| 프로젝트 크기 | 예상 대기 시간 |
|--------------|---------------|
| 작은 프로젝트 (< 1000 LOC) | 30초 - 2분 |
| 중간 프로젝트 (1000-10000 LOC) | 1-5분 |
| 큰 프로젝트 (> 10000 LOC) | 5-15분 |

## 적용된 수정 사항

현재 Jenkinsfile에는 다음이 적용되어 있습니다:

```groovy
timeout(time: 10, unit: 'MINUTES') {
    def qg = waitForQualityGate abortPipeline: false
    // Quality Gate 결과 처리 및 Slack 알림
}
```

이제 PENDING 상태에서 10분 이상 걸리면 자동으로 타임아웃되어 파이프라인이 실패 처리됩니다.

## 추가 디버깅

더 자세한 로그를 보려면 Jenkins Console Output에서 다음을 확인하세요:

```
Checking status of SonarQube task 'xxx' on server 'SonarQube'
SonarQube task 'xxx' status is 'PENDING'
```

이 메시지가 반복되면 SonarQube 서버가 분석을 처리하는 중입니다.  
5분 이상 계속되면 SonarQube 서버에 문제가 있을 수 있습니다.
