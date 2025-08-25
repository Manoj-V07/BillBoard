# tests/test_api.py
from fastapi.testclient import TestClient
from pathlib import Path
import pytest

# Import the app from your project
from billboard_analyzer.main import app

# Create a client to make requests to your app
client = TestClient(app)

# Create a dummy image file for testing
@pytest.fixture(scope="session")
def create_test_image(tmpdir_factory):
    img_path = tmpdir_factory.mktemp("data").join("test.jpg")
    # Create a small dummy binary file
    with open(img_path, "wb") as f:
        f.write(b"\xff\xd8\xff\xe0\x00\x10JFIF")
    return Path(img_path)

def test_analyze_authorized(create_test_image):
    """
    Tests the /analyze endpoint for an authorized scenario.
    """
    with open(create_test_image, "rb") as img_file:
        response = client.post(
            "/analyze",
            files={"image": ("test.jpg", img_file, "image/jpeg")},
            data={"latitude": "9.9252", "longitude": "78.1198"} # Coordinates in Madurai
        )
    assert response.status_code == 200
    json_response = response.json()
    assert json_response["is_authorized"] is True
    assert "compliant" in json_response["reason"]

def test_analyze_unauthorized_location(create_test_image):
    """
    Tests the /analyze endpoint for an unauthorized location.
    """
    with open(create_test_image, "rb") as img_file:
        response = client.post(
            "/analyze",
            files={"image": ("test.jpg", img_file, "image/jpeg")},
            data={"latitude": "40.7128", "longitude": "-74.0060"} # Coordinates in New York
        )
    assert response.status_code == 200
    json_response = response.json()
    assert json_response["is_authorized"] is False
    assert "outside the authorized operational zone" in json_response["reason"]

def test_invalid_image_type(create_test_image):
    """
    Tests sending a non-JPEG file.
    """
    with open(create_test_image, "rb") as img_file:
        response = client.post(
            "/analyze",
            files={"image": ("test.txt", img_file, "text/plain")},
            data={"latitude": "9.9252", "longitude": "78.1198"}
        )
    assert response.status_code == 400
    assert "Invalid file type" in response.json()["detail"]