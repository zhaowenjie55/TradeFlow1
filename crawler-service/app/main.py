import logging
import uuid
from contextvars import ContextVar
from pathlib import Path

from dotenv import load_dotenv
load_dotenv(Path(__file__).resolve().parent.parent / ".env")

from fastapi import FastAPI, Request
from starlette.middleware.base import BaseHTTPMiddleware

from app.routers.amazon import router as amazon_router
from app.routers.asr import router as asr_router
from app.routers.domestic import router as domestic_router
from app.routers.llm import router as llm_router


TRACE_ID_HEADER = "X-Trace-Id"
_trace_id_ctx: ContextVar[str] = ContextVar("trace_id", default="no-trace")


class TraceIdLogFilter(logging.Filter):
    """Injects the current request's trace id into every log record."""

    def filter(self, record: logging.LogRecord) -> bool:
        record.trace_id = _trace_id_ctx.get()
        return True


def _configure_logging() -> None:
    root = logging.getLogger()
    root.setLevel(logging.INFO)
    handler = logging.StreamHandler()
    handler.setFormatter(
        logging.Formatter(
            "%(asctime)s [%(trace_id)s] %(levelname)s %(name)s - %(message)s"
        )
    )
    handler.addFilter(TraceIdLogFilter())
    # Replace existing handlers so our filter is always applied.
    root.handlers = [handler]


class TraceIdMiddleware(BaseHTTPMiddleware):
    """Reads X-Trace-Id from inbound requests (or generates one) and exposes it via context."""

    async def dispatch(self, request: Request, call_next):
        incoming = request.headers.get(TRACE_ID_HEADER)
        trace_id = incoming if incoming else uuid.uuid4().hex
        token = _trace_id_ctx.set(trace_id)
        try:
            response = await call_next(request)
        finally:
            _trace_id_ctx.reset(token)
        response.headers[TRACE_ID_HEADER] = trace_id
        return response


_configure_logging()

app = FastAPI(title="TradeFlow Crawler Service")
app.add_middleware(TraceIdMiddleware)
app.include_router(amazon_router)
app.include_router(domestic_router)
app.include_router(asr_router)
app.include_router(llm_router)
