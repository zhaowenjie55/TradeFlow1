from fastapi import APIRouter

from app.models import (
    LlmReasoningRequest,
    LlmReasoningResponse,
    LlmReportNarrativeRequest,
    LlmReportNarrativeResponse,
    LlmRewriteRequest,
    LlmRewriteResponse,
    LlmTranscriptIntentRequest,
    LlmTranscriptIntentResponse,
)
from app.services.llm import (
    analyze_transcript,
    generate_reasoning,
    generate_report_narrative,
    rewrite_title,
)


router = APIRouter(prefix="/api/llm", tags=["llm"])


@router.post("/rewrite", response_model=LlmRewriteResponse)
def llm_rewrite(request: LlmRewriteRequest) -> LlmRewriteResponse:
    return rewrite_title(request)


@router.post("/report-narrative", response_model=LlmReportNarrativeResponse)
def llm_report_narrative(request: LlmReportNarrativeRequest) -> LlmReportNarrativeResponse:
    return generate_report_narrative(request)


@router.post("/reasoning", response_model=LlmReasoningResponse)
def llm_reasoning(request: LlmReasoningRequest) -> LlmReasoningResponse:
    return generate_reasoning(request)


@router.post("/transcript-intent", response_model=LlmTranscriptIntentResponse)
def llm_transcript_intent(request: LlmTranscriptIntentRequest) -> LlmTranscriptIntentResponse:
    return analyze_transcript(request)

