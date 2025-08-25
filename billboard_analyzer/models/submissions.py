# billboard_analyzer/models/submission.py
from typing import Optional
from sqlmodel import Field, SQLModel
from datetime import datetime

class Submission(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    image_filename: str
    latitude: float
    longitude: float
    is_authorized: bool
    reason: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)