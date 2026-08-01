import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  vus: 100, // 100 virtual users
  duration: '1m', // running continuously for 1 minute
  thresholds: {
    // 95% of requests must complete within 250ms
    http_req_duration: ['p(95)<250'],
    // Error rate must be less than 1%
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  let res = http.get('http://127.0.0.1:8000/');
  check(res, { 'status was 200': (r) => r.status == 200 });
  sleep(1);
}
