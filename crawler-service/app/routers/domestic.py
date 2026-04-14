from fastapi import APIRouter, HTTPException

from app.models import (
    DomesticDetailRequest,
    DomesticDetailResponse,
    DomesticSearchRequest,
    DomesticSearchResponse,
)
from app.services.providers.provider_1688 import (
    DomesticDetailError,
    DomesticSearchError,
    DomesticVerificationRequiredError,
    DOMESTIC_SESSION_MANAGER,
    fetch_1688_product_detail,
    search_1688_products,
)


router = APIRouter(prefix="/api/domestic", tags=["domestic"])


@router.get("/session-status")
def domestic_session_status() -> dict:
    return DOMESTIC_SESSION_MANAGER.status()


@router.post("/search", response_model=DomesticSearchResponse)
def domestic_search(request: DomesticSearchRequest) -> DomesticSearchResponse:
    if request.platform != "1688":
        raise HTTPException(status_code=400, detail="Only platform=1688 is supported in this phase.")

    try:
        items = search_1688_products(request.keyword, request.page)
    except DomesticVerificationRequiredError as exc:
        raise HTTPException(
            status_code=423,
            detail={
                "code": "VERIFICATION_REQUIRED",
                "message": str(exc),
            },
        ) from exc
    except DomesticSearchError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

    return DomesticSearchResponse(
        success=True,
        platform=request.platform,
        keyword=request.keyword,
        page=request.page,
        count=len(items),
        items=items,
    )


@router.post("/detail", response_model=DomesticDetailResponse)
def domestic_detail(request: DomesticDetailRequest) -> DomesticDetailResponse:
    if request.platform != "1688":
        raise HTTPException(status_code=400, detail="Only platform=1688 is supported in this phase.")

    try:
        detail = fetch_1688_product_detail(request.externalItemId)
    except DomesticVerificationRequiredError as exc:
        raise HTTPException(
            status_code=423,
            detail={
                "code": "VERIFICATION_REQUIRED",
                "message": str(exc),
            },
        ) from exc
    except DomesticDetailError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

    return DomesticDetailResponse(**detail)
