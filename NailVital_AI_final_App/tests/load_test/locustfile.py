from locust import HttpUser, task, between

class NailVitalUser(HttpUser):
    wait_time = between(1, 5)

    @task(3)
    def check_health(self):
        # Hitting the health check or root endpoint
        self.client.get("/health", catch_response=True)
        # Even if /health returns 404 in current state, we are testing load

    @task(1)
    def test_predict_endpoint(self):
        # Simulate an image upload/prediction payload
        payload = {"image_data": "dummy_base64_string_for_load_testing"}
        self.client.post("/predict", json=payload, catch_response=True)
