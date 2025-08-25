# billboard_analyzer/core/analyzer.py
import logging
from fastapi import UploadFile

logger = logging.getLogger(__name__)

def run_billboard_analysis(image_file: UploadFile, latitude: float, longitude: float) -> dict:
    """
    Performs the analysis of the billboard image and its location.
    
    In a real application, this would contain complex image processing
    and geospatial logic.
    """
    logger.info(f"Analyzing image '{image_file.filename}' at LAT: {latitude}, LON: {longitude}")

    # RULE #1: Geofencing Check (Authorized zone for Madurai, India)
    AUTHORIZED_LAT_MIN, AUTHORIZED_LAT_MAX = 9.8, 10.0
    AUTHORIZED_LON_MIN, AUTHORIZED_LON_MAX = 78.0, 78.2

    if not (AUTHORIZED_LAT_MIN <= latitude <= AUTHORIZED_LAT_MAX and
            AUTHORIZED_LON_MIN <= longitude <= AUTHORIZED_LON_MAX):
        logger.warning(f"Analysis FAILED: Location ({latitude}, {longitude}) is outside the authorized zone.")
        return {
            "is_authorized": False,
            "reason": "Billboard location is outside the authorized operational zone."
        }

    # RULE #2: Image File Size Check
    MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024  # 5 MB
    if image_file.size > MAX_FILE_SIZE_BYTES:
        logger.warning(f"Analysis FAILED: Image size ({image_file.size} bytes) is too large.")
        return {
            "is_authorized": False,
            "reason": f"Billboard image file size exceeds the {MAX_FILE_SIZE_BYTES / 1024 / 1024}MB limit."
        }

    # If all checks pass, the billboard is authorized.
    logger.info("Analysis SUCCEEDED: Billboard is compliant.")
    return {
        "is_authorized": True,
        "reason": "Billboard is compliant with all city regulations."
    }