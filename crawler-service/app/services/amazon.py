import os
from typing import Any

import requests
from fastapi import HTTPException


SERPAPI_URL = "https://serpapi.com/search.json"
SERPAPI_KEY = os.getenv("SERPAPI_KEY")


def normalize_amazon_item(raw: dict[str, Any]) -> dict[str, Any]:
    return {
        "platform": "amazon",
        "externalItemId": raw.get("asin"),
        "title": raw.get("title"),
        "price": raw.get("price"),
        "imageUrl": raw.get("thumbnail"),
        "productUrl": raw.get("link"),
        "rating": raw.get("rating"),
        "reviewCount": raw.get("reviews"),
        "rawData": raw,
    }


def fetch_amazon_products(keyword: str, page: int) -> list[dict[str, Any]]:
    if not SERPAPI_KEY:
        raise HTTPException(status_code=500, detail="SERPAPI_KEY is not set")

    params = {
        "engine": "amazon",
        "k": keyword,
        "page": page,
        "api_key": SERPAPI_KEY,
    }

    try:
        response = requests.get(SERPAPI_URL, params=params, timeout=20)
        response.raise_for_status()
    except requests.RequestException as exc:
        raise HTTPException(status_code=502, detail=f"SerpApi request failed: {exc}") from exc

    data = response.json()
    organic_results = data.get("organic_results", [])

    if not isinstance(organic_results, list):
        raise HTTPException(status_code=502, detail="SerpApi returned an invalid organic_results payload")

    return [normalize_amazon_item(item) for item in organic_results if isinstance(item, dict)]


def normalize_string_list(raw: Any) -> list[str]:
    if isinstance(raw, list):
        normalized = [str(item).strip() for item in raw if str(item).strip()]
        return normalized
    if isinstance(raw, str) and raw.strip():
        return [raw.strip()]
    return []


def normalize_image_list(raw: Any) -> list[str]:
    if not isinstance(raw, list):
        return []

    images: list[str] = []
    for item in raw:
        if isinstance(item, str) and item.strip():
            images.append(item.strip())
            continue
        if isinstance(item, dict):
            for key in ("link", "image", "thumbnail", "url"):
                value = item.get(key)
                if isinstance(value, str) and value.strip():
                    images.append(value.strip())
                    break
    return images


def fetch_amazon_product_detail(external_item_id: str) -> dict[str, Any]:
    if not SERPAPI_KEY:
        raise HTTPException(status_code=500, detail="SERPAPI_KEY is not set")

    params = {
        "engine": "amazon_product",
        "product_id": external_item_id,
        "api_key": SERPAPI_KEY,
    }

    try:
        response = requests.get(SERPAPI_URL, params=params, timeout=20)
        response.raise_for_status()
    except requests.RequestException as exc:
        raise HTTPException(status_code=502, detail=f"SerpApi request failed: {exc}") from exc

    data = response.json()
    product = data.get("product_results", {})
    buybox = data.get("buybox_winner", {})

    return {
        "externalItemId": external_item_id,
        "title": product.get("title"),
        "description": product.get("description"),
        "features": normalize_string_list(product.get("feature_bullets") or product.get("features")),
        "price": product.get("price") or buybox.get("price"),
        "images": normalize_image_list(data.get("media") or product.get("images")),
        "rating": product.get("rating"),
        "reviewCount": product.get("reviews"),
        "rawData": data,
    }
