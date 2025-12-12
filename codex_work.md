## Work summary (since last commit)

- Added SPA fallback controller and relaxed security rules so non-API routes (`/articles`, `/dashboard`, etc.) serve `index.html`; switched path matching to ant-style to avoid 404/redirect loops.
- Frontend Articles page now shows analysis status badges (`대기/분석중/완료/실패`), defaults importance filter to ALL, and type definitions include `analysisStatus` so cards render processing state.
- AI 요약 파이프라인 개선: URL/메타데이터로 얻은 제목/본문을 DB에 즉시 반영, 요약 파싱 실패 시 `FAILED`로 기록하고 성공 시에만 임베딩 생성; new helper `updateTitleAndContent`.
- Async 분석 실행을 단일 스레드로 제한해 순차 처리 안정성 확보.
- Embedding stack cleanup: pgvector string converter added, ArticleEmbedding/EmbeddingService adjusted (new vector handling/model config), configs updated for local/railway profiles.
- Root cause for missing title/summary on article #2 identified: Claude CLI exited with “Spending cap reached”; new logic now marks such cases as FAILED instead of COMPLETED and keeps updated title/content.
