# 프로젝트 가이드: Toss Income 스타일 세금 환급 마이크로서비스

## 1. 프로젝트 목적
- 토스 인컴(Toss Income) 데브옵스 직무 지원을 위한 포트폴리오용 워크로드 구축.
- Kotlin + Spring Boot 기반의 기초적인 세금 환급 계산 API 구현.
- 서비스 로직 자체보다는 **EKS 배포, Istio 트래픽 제어, Observability(Prometheus/Grafana)** 설정에 집중함.

## 2. 서비스 핵심 도메인 (Toss Income 모사)
토스 인컴은 세금 환급 및 공제 서비스를 제공하므로, 아래 두 가지 간단한 API를 구현함.

# 프로젝트: Toss Income DevOps 포트폴리오 (Simulate & Scale)

## 1. 애플리케이션 (Kotlin/Spring Boot)
- **목표:** 복잡한 로직보다는 '배포하기 좋은 상태' 유지.
- **주요 기능:**
    - `POST /api/v1/tax/calculate`: 1년치 총 소득과 지출액을 받아 단순 환급액 계산 ($$환급액 = (지출 - 소득 * 0.2) * 0.15$$ 같은 단순 수식).
    - **Health Check:** `/actuator/health` (Liveness/Readiness Probe용).
    - **Metrics:** `/actuator/prometheus` (k6 부하 시 모니터링용).

## 2. 부하 테스트 전략 (k6)
- **시나리오:** "종합소득세 신고 기간, 초당 500명의 사용자가 동시에 환급액을 조회하는 상황"
- **테스트 항목:**
    - **Varying Load:** 트래픽이 서서히 증가할 때 **HPA**가 Pod를 몇 초 만에 늘리는지 측정.
    - **Error Rate:** 트래픽 폭주 시 **Istio Circuit Breaker**가 작동하여 에러를 어떻게 격리하는지 확인.
    - **Resource Limit:** Pod 1개가 견딜 수 있는 최대 RPS(Request Per Second) 파악 후 CPU/Memory Limit 설정.

## 3. Cursor에게 요청할 프롬프트 (Core)
> "토스 인컴 지원용 심플한 세금 계산 API를 만들어줘. 
> 1. Kotlin/Spring Boot 기반으로 하고, 수입과 지출 정보를 담은 DTO를 받아 간단한 산식을 계산해 응답하는 Controller를 짜줘.
> 2. 나중에 k6로 부하를 줄 거니까, CPU와 Memory 사용량이 Prometheus에 잘 찍히도록 Micrometer 설정을 포함해줘.
> 3. 이 앱을 위한 k6 테스트 스크립트(`script.js`) 예시도 하나 만들어줘. 트래픽이 0에서 200까지 30초 동안 선형적으로 증가하는 시나리오로."

---

## 3. DevOps 구현 목표 (포트폴리오 핵심)

### 단계 1: Kotlin/Spring Boot 기본 구현
- [ ] Spring Initializr 기반 프로젝트 생성 (Web, Actuator, Prometheus 포함).
- [ ] 환급액 계산 로직 구현 및 Dockerfile 작성 (Multi-stage build로 최적화).

### 단계 2: 인프라 구축 (Terraform & EKS)
- [ ] AWS VPC 및 EKS 클러스터 구축.
- [ ] AWS Load Balancer Controller 설치.

### 단계 3: 서비스 메시 및 배포 전략 (Istio & ArgoCD)
- [ ] Istio 설치 및 Ingress Gateway 설정.
- [ ] **Canary Deployment:** 환급액 계산 로직의 'v2(새로운 공제 정책 적용 버전)'를 배포하고 Istio로 트래픽 90:10 분산 테스트.
- [ ] **Circuit Breaker:** 환급액 계산 서버에 장애 발생 시 빠른 실패(Fail-fast) 처리.

### 단계 4: 모니터링 (Observability)
- [ ] Prometheus로 Actuator 메트릭 수집.
- [ ] Grafana 대시보드 구축 (Pod CPU/MEM, HTTP Request Latency).

---

## 4. Cursor에게 요청할 프롬프트 예시 (Copy & Paste 용)

"나는 지금 토스 인컴 데브옵스 지원용 프로젝트를 하고 있어.

1. Kotlin과 Spring Boot로 연봉과 카드 사용액을 입력받아 환급액을 돌려주는 아주 간단한 POST API를 만들어줘. 
2. 비즈니스 로직은 복잡하지 않아도 돼. 
3. 대신, 나중에 Prometheus가 메트릭을 긁어갈 수 있게 Actuator 설정과 Micrometer 설정을 포함해줘. 
4. 그리고 이 앱을 EKS에서 효율적으로 돌릴 수 있는 최적화된 Dockerfile도 작성해줘."