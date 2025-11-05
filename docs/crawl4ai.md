# Crawl4AI Architecture & Implementation Plan

This document captures the high-level architecture, data model, and implementation considerations for the "Crawl4AI" tender intelligence platform.

## 1. Goal / MVP

* Collect tenders from multiple sources.
* Produce canonical tender JSON records with metadata, contacts, pricing, and supporting documents.
* Retain raw HTML/PDF artifacts for provenance and reprocessing.

## 2. System Architecture Overview

### Crawler / Harvester
* Orchestrates site-specific jobs and pagination.
* Maintains seed discovery lists and monitors sitemap/search endpoints.

### Fetcher
* Performs HTTP requests via `httpx` (async) with optional Playwright rendering for dynamic pages.
* Downloads attachments (PDFs, images) and captures response metadata for provenance.

### Parsers
* Site-specific extractors implemented with CSS selectors/XPath.
* Fallback ML/LLM extraction pipeline for noisy or changing layouts.

### Document OCR / Parser
* Text extraction for PDFs via PyMuPDF/pdfminer.
* OCR for image-based documents using Tesseract.
* Table extraction with Camelot/Tabula where required.

### Normalizer / Mapper
* Maps extracted values to the canonical schema.
* Standardizes dates, currency, contact formats, and validates against JSON Schema.

### Deduplicator
* Generates fingerprints from key fields and performs fuzzy matching to reduce duplicates and detect updates.

### Storage
* Raw blobs: S3-compatible storage (e.g., MinIO).
* Structured data: PostgreSQL for canonical records and history.
* Search/index: OpenSearch/Elasticsearch for full-text and faceted search.
* Optional vector store (Milvus/Weaviate/Pinecone) for semantic retrieval.

### Scheduler & Orchestration
* Airflow or Kubernetes CronJobs + Celery workers for production scale.
* APScheduler suitable for local/small deployments.

### API / UI
* REST/GraphQL API for downstream consumers.
* Dashboard for monitoring crawl health, manual review queue, and alerting.

### Monitoring & Logging
* Prometheus + Grafana for metrics.
* Sentry/Elastic APM for error tracing.
* Capture crawl success/failure rates, parsing confidence, retry counts.

### Proxy & Anti-blocking Layer
* Respect `robots.txt` and site terms of service.
* Token-bucket rate limiting per site.
* Rotate User-Agents and integrate proxy pools/residential proxies when necessary.
* Detect CAPTCHAs and route to human-in-the-loop review if legally permissible.

## 3. Canonical Tender Schema (JSON)

```json
{
  "source": "name_of_website",
  "source_url": "https://...",
  "scrape_datetime": "2025-11-05T12:00:00+05:30",
  "external_id": "site123-98765",
  "title": "Tender for supply of X",
  "description_text": "...",
  "tender_value": {
    "currency": "INR",
    "amount": 12345678.0
  },
  "organization": "Narayana Hospital",
  "department": "Procurement",
  "publish_date": "2025-10-20",
  "closing_date": "2025-11-30",
  "locations": ["Bengaluru", "Karnataka"],
  "contacts": [{"name": "Procurement officer", "email": "proc@org.com", "phone": "+91..."}],
  "attachments": [
    {"filename": "doc.pdf", "url": "s3://...", "type": "pdf", "sha256": "..."}
  ],
  "raw_html_blob": "s3://...",
  "extraction_metadata": {
    "parser": "site-rule-v1",
    "confidence": 0.87
  },
  "fingerprint": "sha256-hash",
  "status": "active|closed|unknown"
}
```

## 4. Data Pipeline Flow

1. **Discovery** – Maintain seeds for list/search endpoints and sitemaps per site.
2. **Listing Scrape** – Crawl paginated lists to capture tender summary metadata and detail URLs.
3. **Detail Fetch** – Retrieve tender detail pages and download attachments; capture headers and response codes.
4. **Extraction** – Apply deterministic parsers; fallback to ML/LLM extraction for missing fields.
5. **Document Processing** – Parse PDFs/images via text extraction and OCR; normalize tables when needed.
6. **Normalization** – Canonicalize values (dates, currency, contact fields) and validate against schema.
7. **Deduplication** – Use fingerprints and fuzzy similarity to avoid duplicates and detect updates.
8. **Persistence** – Store raw content in blob storage; save structured record in PostgreSQL; index in OpenSearch.
9. **Event Emission** – Publish webhook/message queue events for downstream systems.
10. **Monitoring & QA** – Track metrics, maintain manual review queue, update parsing rules.

## 5. Extraction Techniques

* **Rule-based parsing** with CSS/XPath selectors for stable sites.
* **Hybrid ML/LLM** fallback for unstructured pages: prompt models with cleaned text and parse JSON output.
* **PDF Processing**: Use PyMuPDF/pdfminer for text-based PDFs and Tesseract OCR for image-based ones; apply regex/heuristics for key fields.
* **Attachments**: Store with SHA256 hashes for idempotency.

## 6. Deduplication & Change Detection

* Maintain both raw content hash (HTML/PDF) and semantic fingerprint (title + organization + publish/closing dates + tender value).
* Apply fuzzy similarity (RapidFuzz, MinHash) to catch near duplicates.
* Version records on updates and retain change history.

## 7. Monitoring & Quality Assurance

* Track crawl throughput, parse success rates, and latency.
* Flag low-confidence parses for manual review and rule tuning.
* Provide tools to edit and promote new parsing rules to production.

## 8. Legal & Ethical Considerations

* Review legal constraints for each site and jurisdiction.
* Prefer official APIs (RSS/JSON) when available.
* Clearly identify the crawler (custom User-Agent) and provide contact information.

## 9. Deployment & Scalability Roadmap

* **MVP**: Single-node deployment with Docker Compose (PostgreSQL, MinIO, OpenSearch, worker services).
* **Scale-up**: Containerize workers and deploy on Kubernetes with Helm; use Horizontal Pod Autoscaler.
* **Scheduling**: Leverage Airflow or Kubernetes CronJobs for job orchestration.
* **Observability**: Centralize logs (ELK) and metrics (Prometheus/Grafana).

## 10. Initial Feature Checklist

* Seeds for the top 10 tender portals.
* Listing + detail crawlers with attachment download.
* Extraction of title, publish/close dates, organization, description, attachments.
* PDF text extraction and OCR pipeline.
* Deduplication service.
* Persistence to PostgreSQL and OpenSearch.
* Dashboard for monitoring failures and manual review queue.

## 11. Next Steps

1. Stand up local development environment (Docker Compose for DB + storage + search).
2. Implement base crawler skeleton (see `script/crawl4ai_skeleton.py`).
3. Add configuration-driven site definitions (selectors, rate limits, attachment handling).
4. Integrate PDF parsing/OCR pipelines.
5. Implement normalization and deduplication services.
6. Expose REST API endpoints for tender retrieval and status monitoring.
7. Build initial dashboard to visualize crawl status and manual review queue.

