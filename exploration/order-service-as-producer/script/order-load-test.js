import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * COMPREHENSIVE LOAD TEST
 * - Randomized payload (realistic orders)
 * - Spike load using ramping-arrival-rate
 * - SLA-based thresholds
 */

export const options = {
  scenarios: {
    order_spike_test: {
      executor: 'ramping-arrival-rate',
      startRate: 2,            // requests per second
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 200,
      stages: [
        { target: 20, duration: '5s' },  // warm-up
        { target: 100, duration: '10s' },  // spike
        { target: 100, duration: '10s' },  // sustain
        { target: 0, duration: '5s' },   // cool-down
      ],
    },
  },

  thresholds: {
    http_req_failed: ['rate<0.01'],      // < 1% errors
    http_req_duration: ['p(95)<500'],    // 95% < 500ms
  },
};

export default function () {
  const url = 'http://localhost:8080/api/orders/with-headers';

  // 1️⃣ Randomized, realistic payload
  const quantity = Math.floor(Math.random() * 5) + 1;
  const pricePerItem = 500;

  const payload = JSON.stringify({
    customerId: Math.floor(Math.random() * 10000),
    productId: Math.floor(Math.random() * 20) + 1,
    quantity: quantity,
    totalAmount: quantity * pricePerItem,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(url, payload, params);

  check(res, {
    'status is 200 or 201': (r) =>
      r.status === 200 || r.status === 201,
  });

  // Optional think time (kept small since arrival-rate controls RPS)
  sleep(0.2);
}
