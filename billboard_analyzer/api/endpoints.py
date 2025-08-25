from fastapi import APIRouter, File, UploadFile, Form, HTTPException
from typing import Annotated
from billboard_analyzer.api.schemas import AnalysisResponse
# V-- This line is changed
from billboard_analyzer.core.analyzer import run_full_analysis

router = APIRouter()

@router.post("/analyze", response_model=AnalysisResponse)
async def analyze_billboard_endpoint(
    image: Annotated[UploadFile, File(description="A .jpg image.")],
    latitude: Annotated[str, Form()],
    longitude: Annotated[str, Form()]
):
    if image.content_type != "image/jpeg":
        raise HTTPException(status_code=400, detail="Invalid file type.")
    try:
        lat_float, lon_float = float(latitude), float(longitude)
    except (ValueError, TypeError):
        raise HTTPException(status_code=400, detail="Invalid coordinates.")
    
    # Call the new, full analysis pipeline
    # V-- This line is changed
    analysis_result = await run_full_analysis(image, lat_float, lon_float)
    
    return analysis_result