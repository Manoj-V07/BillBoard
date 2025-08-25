# billboard_analyzer/api/schemas.py
from pydantic import BaseModel

class AnalysisResponse(BaseModel):
    """
    Defines the JSON response structure for the analysis endpoint.
    """
    is_authorized: bool
    reason: str