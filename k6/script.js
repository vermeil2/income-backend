/**
 * k6 부하 테스트: 세금 환급 계산 API
 * 시나리오: 트래픽이 0 → 200 VUs로 30초 동안 선형 증가
 * (종합소득세 신고 기간, 초당 500명 수준 시뮬레이션 참고용)
 */
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  stages: [
    { duration: '10s', target: 50 },
    { duration: '30s', target: 200 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const payload = JSON.stringify({
    annualIncome: 50000000,
    annualExpenses: 15000000,
  });

  const res = http.post(`${BASE_URL}/api/v1/tax/calculate`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'has refundAmount': (r) => {
      try {
        const body = JSON.parse(r.body);
        return typeof body.refundAmount === 'number';
      } catch {
        return false;
      }
    },
  });

  sleep(0.5);
}
