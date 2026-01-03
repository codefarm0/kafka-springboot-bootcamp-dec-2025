import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    regular_orders: {
      executor: 'constant-vus',
      vus: 10,
      duration: '1m',
      exec: 'regularOrders',
    },
    high_value_orders: {
      executor: 'constant-vus',
      vus: 5,
      duration: '1m',
      exec: 'highValueOrders',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

const BASE_URL = 'http://localhost:8089/api/orders';
const headers = {
  'Content-Type': 'application/json',
};

export function regularOrders() {
  const payload = JSON.stringify({
    customerId: `customer-${__VU}`,
    productId: 'product-regular',
    quantity: 1,
    totalAmount: 80.00, // <= 100 → regular order
  });

  const res = http.post(BASE_URL, payload, { headers });

  check(res, {
    'regular order status is 2xx': (r) => r.status >= 200 && r.status < 300,
  });

  sleep(1);
}

export function highValueOrders() {
  const payload = JSON.stringify({
    customerId: `customer-${__VU}`,
    productId: 'product-premium',
    quantity: 2,
    totalAmount: 150.00, // > 100 → high value order
  });

  const res = http.post(BASE_URL, payload, { headers });

  check(res, {
    'high value order status is 2xx': (r) => r.status >= 200 && r.status < 300,
  });

  sleep(1);
}
