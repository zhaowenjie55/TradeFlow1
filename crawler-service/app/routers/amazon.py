from fastapi import APIRouter

from app.models import DetailRequest, SearchRequest
from app.services.amazon import fetch_amazon_product_detail, fetch_amazon_products


router = APIRouter()


@router.post("/api/search")
def search_products(request: SearchRequest) -> dict:
    items = fetch_amazon_products(request.keyword, request.page)
    return {
        "success": True,
        "keyword": request.keyword,
        "page": request.page,
        "count": len(items),
        "items": items,
    }


@router.post("/api/detail")
def product_detail(request: DetailRequest) -> dict:
    return fetch_amazon_product_detail(request.externalItemId)
