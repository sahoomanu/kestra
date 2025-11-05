"""Minimal asynchronous crawler skeleton for Crawl4AI.

This script illustrates how to:
* Fetch listing pages asynchronously with httpx.
* Fall back to Playwright for dynamic pages.
* Parse detail pages with BeautifulSoup.
* Generate deterministic fingerprints for deduplication.
* Emit normalized records ready for persistence/indexing.

The implementation intentionally focuses on extensibility and clarity so it can
be adapted for additional tender portals.
"""

from __future__ import annotations

import asyncio
import hashlib
import json
from collections.abc import Iterable
from contextlib import asynccontextmanager
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Optional

import httpx
from bs4 import BeautifulSoup

USER_AGENT = "Crawl4AI/1.0 (+mailto:you@org.com)"
DEFAULT_TIMEOUT = httpx.Timeout(30.0, connect=10.0)


@dataclass(slots=True)
class TenderRecord:
    """Canonical tender payload."""

    source: str
    source_url: str
    scrape_datetime: str
    title: Optional[str] = None
    publish_date: Optional[str] = None
    closing_date: Optional[str] = None
    description_text: Optional[str] = None
    organization: Optional[str] = None
    attachments: Optional[list[dict[str, Any]]] = None
    fingerprint: Optional[str] = None

    def to_dict(self) -> dict[str, Any]:
        payload = {
            "source": self.source,
            "source_url": self.source_url,
            "scrape_datetime": self.scrape_datetime,
            "title": self.title,
            "publish_date": self.publish_date,
            "closing_date": self.closing_date,
            "description_text": self.description_text,
            "organization": self.organization,
            "attachments": self.attachments or [],
            "fingerprint": self.fingerprint,
        }
        # Remove keys with None values for cleanliness
        return {k: v for k, v in payload.items() if v is not None}


def build_fingerprint(record: TenderRecord) -> str:
    parts: Iterable[str] = (
        record.title or "",
        record.organization or "",
        record.publish_date or "",
        record.closing_date or "",
    )
    joined = "|".join(part.strip() for part in parts)
    return hashlib.sha256(joined.encode("utf-8")).hexdigest()


async def fetch_html(url: str, client: httpx.AsyncClient) -> tuple[str, httpx.Headers]:
    response = await client.get(url)
    response.raise_for_status()
    return response.text, response.headers


def fetch_with_playwright(url: str) -> tuple[str, dict[str, str]]:
    from playwright.sync_api import sync_playwright

    with sync_playwright() as pw:
        browser = pw.chromium.launch(headless=True)
        page = browser.new_page(user_agent=USER_AGENT)
        page.goto(url, timeout=60_000)
        html = page.content()
        browser.close()
        return html, {}


def parse_detail_html(html: str) -> dict[str, Optional[str]]:
    soup = BeautifulSoup(html, "lxml")

    def text(selector: str) -> Optional[str]:
        element = soup.select_one(selector)
        if element:
            return element.get_text(" ", strip=True)
        return None

    return {
        "title": text("h1"),
        "publish_date": text(".publish-date"),
        "closing_date": text(".closing-date"),
        "description_text": text(".description"),
        "organization": text(".organization"),
    }


async def crawl_detail(url: str, source: str, client: httpx.AsyncClient) -> TenderRecord:
    try:
        html, headers = await fetch_html(url, client)
    except httpx.HTTPError:
        html, headers = fetch_with_playwright(url)

    parsed = parse_detail_html(html)
    record = TenderRecord(
        source=source,
        source_url=url,
        scrape_datetime=datetime.now(timezone.utc).isoformat(),
        **parsed,
    )
    record.fingerprint = build_fingerprint(record)

    # Placeholder for persistence logic: store raw HTML, headers, etc.
    print(json.dumps(record.to_dict(), indent=2))
    return record


async def extract_links(listing_html: str) -> list[str]:
    soup = BeautifulSoup(listing_html, "lxml")
    links = []
    for anchor in soup.select("a.tender-link"):
        href = anchor.get("href")
        if href:
            links.append(href)
    return links


@asynccontextmanager
async def get_client() -> Iterable[httpx.AsyncClient]:
    async with httpx.AsyncClient(
        headers={"User-Agent": USER_AGENT}, timeout=DEFAULT_TIMEOUT
    ) as client:
        yield client


async def crawl_seed(seed_url: str, source: str) -> list[TenderRecord]:
    async with get_client() as client:
        listing_html, _ = await fetch_html(seed_url, client)
        detail_links = await extract_links(listing_html)
        tasks = [
            crawl_detail(link if link.startswith("http") else seed_url + link, source, client)
            for link in detail_links
        ]
        return await asyncio.gather(*tasks)


async def main() -> None:
    seeds = {
        "https://example-tenders.gov/list?page=1": "example-tenders",
    }
    for seed_url, source in seeds.items():
        await crawl_seed(seed_url, source)


if __name__ == "__main__":
    asyncio.run(main())
