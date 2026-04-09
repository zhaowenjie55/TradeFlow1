from fastapi import FastAPI

from app.routers.amazon import router as amazon_router
from app.routers.asr import router as asr_router
from app.routers.domestic import router as domestic_router
from app.routers.llm import router as llm_router


app = FastAPI(title="TradeFlow Crawler Service")
app.include_router(amazon_router)
app.include_router(domestic_router)
app.include_router(asr_router)
app.include_router(llm_router)
