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


class AsrSegment(BaseModel):
    start: float
    end: float
    text: str


class AsrTranscriptionResponse(BaseModel):
    success: bool = True
    language: Optional[str]
    duration: float = 0.0
    text: str
    segments: list[AsrSegment] = Field(default_factory=list)


class LlmRewriteRequest(BaseModel):
    sourceTitle: str = Field(..., min_length=1)
    context: dict[str, Any] = Field(default_factory=dict)


class LlmRewriteResponse(BaseModel):
    rewrittenText: str
    keywords: list[str] = Field(default_factory=list)
    fallbackUsed: bool = False
    provider: str
    model: str
    fallbackReason: Optional[str] = None
    generatedAt: str


class LlmReportNarrativeRequest(BaseModel):
    productTitle: str
    market: str
    rewrittenQuery: str
    rewrittenKeywords: list[str] = Field(default_factory=list)
    decision: str
    riskLevel: str
    amazonPriceUsd: Optional[float] = None
    amazonPriceRmb: Optional[float] = None
    sourcingCost: Optional[float] = None
    domesticShippingCost: Optional[float] = None
    logisticsCost: Optional[float] = None
    platformFee: Optional[float] = None
    exchangeRateCost: Optional[float] = None
    totalCost: Optional[float] = None
    estimatedProfit: Optional[float] = None
    estimatedMargin: Optional[float] = None
    benchmarkTitle: Optional[str] = None
    domesticMatchTitles: list[str] = Field(default_factory=list)


class LlmReportNarrativeResponse(BaseModel):
    summaryText: str
    recommendations: list[str] = Field(default_factory=list)
    riskNotes: list[str] = Field(default_factory=list)
    fallbackUsed: bool = False
    provider: str
    model: str
    fallbackReason: Optional[str] = None
    generatedAt: str


class LlmReasoningRequest(BaseModel):
    stepName: str
    prompt: str
    context: dict[str, Any] = Field(default_factory=dict)


class LlmReasoningResponse(BaseModel):
    decision: str
    explanation: str
    confidenceScore: float
    fallbackUsed: bool = False
    provider: str
    model: str
    fallbackReason: Optional[str] = None
    generatedAt: str


class LlmTranscriptIntentRequest(BaseModel):
    transcript: str = Field(..., min_length=1)
    sourceType: str = Field(default="unknown")


class LlmTranscriptIntentResponse(BaseModel):
    intent: str
    category: Optional[str] = None
    market: Optional[str] = None
    priceLevel: Optional[str] = None
    sourcing: Optional[str] = None
    keywords: list[str] = Field(default_factory=list)
    sellingPoints: list[str] = Field(default_factory=list)
    painPoints: list[str] = Field(default_factory=list)
    useCases: list[str] = Field(default_factory=list)
    targetAudience: list[str] = Field(default_factory=list)
    fallbackUsed: bool = False
    provider: str
    model: str
    fallbackReason: Optional[str] = None
    generatedAt: str
