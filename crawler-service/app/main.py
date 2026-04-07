from fastapi import FastAPI

from app.routers.amazon import router as amazon_router
from app.routers.domestic import router as domestic_router


app = FastAPI(title="TradeFlow Crawler Service")
app.include_router(amazon_router)
app.include_router(domestic_router)
