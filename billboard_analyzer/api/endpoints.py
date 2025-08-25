# billboard_analyzer/api/endpoints.py
from fastapi import APIRouter, File, UploadFile, Form, HTTPException
from typing import Annotated

# Import schemas and analysis logic from other modules
from billboard_analyzer.api.schemas import AnalysisResponse
from billboard_analyzer.core.analyzer import run_billboard_analysis

# Create a router to organize this endpoint
router = APIRouter()

@router.post("/analyze", response_model=AnalysisResponse)
async def analyze_billboard_endpoint(
    image: Annotated[UploadFile, File(description="A .jpg image of the billboard.")],
    latitude: Annotated[str, Form(description="The latitude coordinate (e.g., '9.9252').")],
    longitude: Annotated[str, Form(description="The longitude coordinate (e.g., '78.1198').")]
):
    """
    Accepts billboard image and GPS coordinates for authorization analysis.
    """
    if image.content_type != "image/jpeg":
        raise HTTPException(
            status_code=400,
            detail="Invalid file type. Only 'image/jpeg' is accepted."
        )

    try:
        lat_float = float(latitude)
        lon_float = float(longitude)
    except (ValueError, TypeError):
        raise HTTPException(
            status_code=400,
            detail="Invalid latitude or longitude format. Must be valid numbers."
        )

    # Call the core logic function
    analysis_result = run_billboard_analysis(image, lat_float, lon_float)

    return analysis_result