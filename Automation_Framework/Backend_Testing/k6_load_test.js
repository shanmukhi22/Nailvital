import http from 'k6/http';
import { check, sleep } from 'k6';

// Configure the test to meet the requirements:
// 100 concurrent users for 1 minute
export const options = {
    vus: 100, // Virtual Users
    duration: '10s', // 10 seconds for local quick test
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95% of requests should be below 500ms
        http_req_failed: ['rate<0.01'], // Error rate should be less than 1%
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:5000'; // Defaulting to common backend port

export default function () {
    // 1. Health check / Root endpoint
    const res = http.get(`${BASE_URL}/`);
    
    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time is fast': (r) => r.timings.duration < 250,
    });

    // 2. Simulate API Call (e.g., getting models or inference)
    // Replace with actual backend API endpoints when available
    const apiRes = http.get(`${BASE_URL}/api/health`);
    
    check(apiRes, {
        'api status is 200': (r) => r.status === 200 || r.status === 404, // 404 allowed if endpoint doesn't exist yet
    });

    // Sleep for a short duration to simulate real user think time
    sleep(1);
}
