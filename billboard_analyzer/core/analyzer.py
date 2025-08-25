import logging
from typing import Tuple
import cv2
import numpy as np
from fastapi import UploadFile
from ultralytics import YOLO

# --- AI Model Setup ---
try:
    model = YOLO("yolov8n.pt")
except Exception as e:
    logging.error(f"Failed to load YOLO model: {e}")
    model = None

logger = logging.getLogger(__name__)

def is_billboard_in_image(img: np.ndarray) -> bool:
    """
    NEW SIMPLIFIED LOGIC:
    - By default, we assume a billboard is present.
    - We only return False if the AI is certain it detected NOTHING in the image.
    - This is a very lenient approach to maximize successful detections.
    """
    if model is None:
        logger.error("YOLO model is not available. Skipping image analysis.")
        return False # Fail safely if the model didn't load

    results = model(img, verbose=False)
    
    # Check if the results list is empty or if the first result has zero detected boxes.
    if not results or len(results[0].boxes) == 0:
        logger.warning("AI model detected absolutely no objects in the image.")
        return False
    
    # If we reach here, the model detected at least one object.
    # We will assume this is the billboard and proceed.
    logger.info("An object was detected in the image. Assuming it's the billboard.")
    return True


def check_location_authorization_by_zone(lat: float, lon: float) -> Tuple[bool, str]:
    """
    Checks if the given coordinates fall within an authorized or unauthorized zone.
    """
    # Define a commercial zone in Madurai where billboards are allowed.
    COMMERCIAL_ZONE = {
        "lat_min": 9.9150, "lat_max": 9.9280,
        "lon_min": 78.1100, "lon_max": 78.1250
    }
    
    # Define a residential zone where billboards are forbidden.
    RESIDENTIAL_ZONE = {
        "lat_min": 9.9300, "lat_max": 9.9450,
        "lon_min": 78.1300, "lon_max": 78.1450
    }

    if (COMMERCIAL_ZONE["lat_min"] <= lat <= COMMERCIAL_ZONE["lat_max"] and
            COMMERCIAL_ZONE["lon_min"] <= lon <= COMMERCIAL_ZONE["lon_max"]):
        reason = "Location authorized: Falls within a designated commercial zone."
        return True, reason

    if (RESIDENTIAL_ZONE["lat_min"] <= lat <= RESIDENTIAL_ZONE["lat_max"] and
            RESIDENTIAL_ZONE["lon_min"] <= lon <= RESIDENTIAL_ZONE["lon_max"]):
        reason = "Unauthorized location: Falls within a residential zone where billboards are prohibited."
        return False, reason

    default_reason = "Unauthorized location: Does not fall within any known commercial zone."
    return False, default_reason


async def run_full_analysis(image_file: UploadFile, latitude: float, longitude: float) -> dict:
    """Performs the full analysis pipeline with the new simplified logic."""
    contents = await image_file.read()
    nparr = np.frombuffer(contents, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

    if not is_billboard_in_image(img):
        return {
            "is_authorized": False,
            "reason": "Analysis failed: No billboard was detected in the image."
        }
        
    is_authorized, reason = check_location_authorization_by_zone(latitude, longitude)
    
    return {"is_authorized": is_authorized, "reason": reason}