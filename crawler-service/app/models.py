from typing import Any, Literal, Optional

from pydantic import BaseModel, Field


class SearchRequest(BaseModel):
    keyword: str = Field(..., min_length=1)
    page: int = Field(default=1, ge=1)


class DetailRequest(BaseModel):
    externalItemId: str = Field(..., min_length=1)


class DomesticSearchRequest(BaseModel):
    platform: Literal["1688"] = "1688"
    keyword: str = Field(..., min_length=1)
    page: int = Field(default=1, ge=1)


class DomesticSearchItem(BaseModel):
    platform: str
    externalItemId: Optional[str]
    title: Optional[str]
    price: Optional[float]
    currency: str = "CNY"
    imageUrl: Optional[str]
    productUrl: Optional[str]
    shopName: Optional[str]
    salesText: Optional[str]
    rawData: dict[str, Any] = Field(default_factory=dict)


class DomesticSearchResponse(BaseModel):
    success: bool = True
    platform: str
    keyword: str
    page: int
    count: int
    items: list[DomesticSearchItem]


class DomesticDetailRequest(BaseModel):
    platform: Literal["1688"] = "1688"
    externalItemId: str = Field(..., min_length=1)


class DomesticDetailResponse(BaseModel):
    success: bool = True
    platform: str
    externalItemId: str
    title: Optional[str]
    price: Optional[float]
    currency: str = "CNY"
    imageUrl: Optional[str]
    productUrl: Optional[str]
    shopName: Optional[str]
    brand: Optional[str]
    description: Optional[str]
    images: list[str] = Field(default_factory=list)
    attributes: dict[str, Any] = Field(default_factory=dict)
    shippingText: Optional[str]
    salesText: Optional[str]
    skuData: dict[str, Any] = Field(default_factory=dict)
    rawData: dict[str, Any] = Field(default_factory=dict)
