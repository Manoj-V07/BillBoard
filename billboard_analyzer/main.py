# billboard_analyzer/main.py
from fastapi import FastAPI
from billboard_analyzer.api.endpoints import router as analysis_router

# Create the main FastAPI app instance
app = FastAPI(
    title="Billboard Analysis API",
    description="An API to analyze billboard compliance based on image and location.",
    version="1.0.0"
)

# Include the router from the endpoints file
app.include_router(analysis_router)

# Add a simple root endpoint to check if the server is running
@app.get("/", tags=["Health Check"])
def read_root():
    return {"status": "OK", "message": "Welcome to the Billboard Analysis API!"}