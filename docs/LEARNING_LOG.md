# Engineering Learning Log

## B0 — Foundation

- Created the Spring Boot Maven project using Java 21.
- Started PostgreSQL 16 using Docker Compose.
- Connected Spring Boot to the local PostgreSQL database.
- Confirmed the initial Flyway baseline migration was applied.
- Confirmed the Maven test command succeeds.
- Added shared engineering rules and an initial API contract.

## Decisions Recorded

- Spring Boot 4.0.8 is retained as the project version. Verify compatibility when adding dependencies and features.
- Flyway V1 is an empty baseline migration that has already been applied. Add the actual schema in a new migration; do not edit V1.
- Business API request and response schemas must be agreed upon before implementing analysis endpoints.
- Frontend architecture uses React 19 + TypeScript + Vite + Tailwind CSS v4 on branch `feat/F0-frontend-shell`.
- Primary visual experience is Dark Luxury: cosmic near-black canvas (`#090B10`), translucent glass surfaces, restrained electric cyan glow (`#38BDF8`), and subtle sunshine gold accent (`#FBBF24`). Fully compatible light theme preserved.
- Editorial typography established: Merriweather for bold headings, Cormorant Garamond for italic editorial emphasis, and JetBrains Mono for code and metrics.
- Navigation supports three accessible states: `expanded` (260px), `compact` (72px icon rail), and `hidden` (0px with an edge-mounted reopen control), plus a slide-out mobile drawer.
- Strict simulation transparency: when the backend API is disconnected, the UI displays clear simulation mode badges and genuine empty states (`—`), rather than inventing fake analysis grades.
- The UI strictly never executes submitted Java code in the browser or suggests execution on the API host.

## F0 — Frontend Shell & Design System (Stage 1 & Visual Refinement)

- Established design tokens in `web/src/styles/tokens.css` for dark luxury and light porcelain themes.
- Loaded Google Fonts: `Merriweather`, `Cormorant Garamond`, and `JetBrains Mono` in `index.html`.
- Implemented `NavigationContext` managing three sidebar states (`expanded`, `compact`, `hidden`) and mobile overlay drawer.
- Fixed desktop sidebar collapse bug: inner layout wrapper is constrained and outer `<aside>` enforces `w-0 min-w-0 max-w-0 border-r-0 overflow-hidden`, allowing the main canvas to smoothly expand to 100% of the visible viewport.
- Added accessible edge-mounted reopen toggle tab on the left viewport margin (`fixed left-0 top-24`) with `aria-expanded` and `aria-controls`.
- Added accessible mobile drawer with body scroll lock and `Escape` key dismissal.
- Redesigned `Navbar` with simplified header status, dark glassmorphism, theme toggle, and discoverable sidebar restore controls.
- Re-architected `DashboardPage`:
  - Spacious, cinematic hero opening with editorial typography, radial cyan glow, and single prominent "Analyze Java Code" CTA.
  - Minimalist, de-emphasized KPI stat ribbon with fine hairline dividers.
  - Polished, intentional empty state featuring an interactive 4-step Verification Lifecycle diagram.
  - Componentry-inspired Scroll Split Card layout for the 4 Deterministic Analyzers (sticky left column, flowing right cards with code snippet examples).
  - Closed-loop verification outcome grid explaining the 5 outcome classes.
  - Subtle floating quick dock for secondary shortcuts (top jump, quick analyzer, fixture switcher) with magnetic micro-scaling.
- Verified type checking and linting: `eslint .` and `tsc -b && vite build` both pass with zero errors.

## F0 — TypeScript Data Models & Mock Fixtures (Stage 2)

- Grounded all TypeScript models directly in the Project Bible (`AI Code Quality Auditor — Project Bible.md`), PostgreSQL DDL (Section 9), and Section 11 API specifications:
  - `web/src/types/submission.ts`: `OriginType` (`HUMAN`, `AI_GENERATED`, `AI_ASSISTED`, `AI_REMEDIATED`), `Submission` (1:1 with DB table `submission` including lineage fields), `SubmissionSummary`, `CreateSubmissionRequest`, and `CreateSubmissionResponse`.
  - `web/src/types/analysis.ts`: `RunStatus` (`QUEUED`, `PARSING`, `ANALYZING`, `SCORING`, `COMPLETED`, `PARTIAL`, `PARSE_FAILED`, `FAILED`), `QualityGrade` (`A`, `B`, `C`, `D`, `F`, `N/A`), `DimensionType` (`SECURITY`, `COMPLEXITY`, `DUPLICATION`, `MAINTAINABILITY`), `DIMENSION_CONFIGS` (weights: 35%, 25%, 20%, 20%), `RunDimensionScore` (density and sub-score), `AnalysisRun`, and `AnalysisReport`.
  - `web/src/types/finding.ts`: `FindingSeverity` (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO`), `SEVERITY_WEIGHTS` (10, 5, 2, 1, 0), `RuleConfidence`, and `Finding` with 64-character SHA-256 fingerprint.
  - `web/src/types/remediation.ts`: `RemediationStrategy` (`BARE`, `GUIDED`, `RULE_BASED`), `RemediationOutcome` (`VERIFIED_FIX`, `PARTIAL`, `NO_EFFECT`, `REGRESSION`, `BROKEN`), `VerificationStepName`, `VerificationStep`, and `Remediation`.
  - `web/src/types/explanation.ts`: `ExplanationStatus` (`PENDING`, `VALIDATED`, `REJECTED`), `GuidanceSource`, and `FindingExplanation`.
  - `web/src/types/comparison.ts`: `FindingChangeKind` (`RESOLVED`, `INTRODUCED`, `PERSISTING`), `DimensionDelta`, and `RunComparison`.
- Explicitly documented and tagged all proposed frontend-only fields with rationale (e.g. `startColumn`, `endColumn`, and `scoreDeduction` on `Finding` for code gutter rendering; `diff` on `Remediation` for side-by-side comparison).
- Implemented honest, realistic mock fixtures in `web/src/fixtures/` covering authentic research scenarios:
  - 6 realistic Java snippets in `sampleJavaSnippets.ts`: vulnerable `LoginDao.java`, remediated `LoginDao.java`, complex `OrderProcessingService.java`, clean `TokenCache.java`, unparseable syntax error snippet, and non-Java text.
  - Representative deterministic findings in `mockFindings.ts`: `SEC-001` (CWE-89 SQL injection), `COMP-001` (cyclomatic complexity hotspot), `MAINT-001` (empty catch block smell), `SEC-003` (CWE-327 weak MD5 crypto), and `SYN-001` (syntax parse error).
  - Complete set of 5 closed-loop remediation outcomes in `mockRemediations.ts`: `VERIFIED_FIX`, `PARTIAL`, `NO_EFFECT`, `REGRESSION`, and `BROKEN`.
  - Retrieval-augmented explanation fixture (`MOCK_EXPLANATION_SEC_001`) with citations and deterministic fallback fixture (`MOCK_EXPLANATION_FALLBACK`) with status `REJECTED`.
  - Analysis runs and reports in `mockAnalysisRuns.ts` including the gate rule capping grades at `C` for critical vulnerabilities, and `PARSE_FAILED` runs yielding grade `N/A` rather than `F`.
  - Version comparison fixture (`MOCK_COMPARISON_LOGIN_DAO`) demonstrating finding transitions and delta scoring.
- Verified transparent UI display on `DashboardPage.tsx` using `MOCK_SUBMISSION_SUMMARIES` and the 5 canonical remediation outcome categories.
- Fixed design token import wiring: updated `web/src/main.tsx` application entry point to import `./styles/index.css` (which unifies `@import "tailwindcss"`, `@import "./tokens.css"`, `@layer base` theme overrides, reduced motion safety, and custom glass scrollbars). Verified that design tokens and Tailwind styles are loaded exactly once with zero duplicate imports, CSS ordering issues, or theme-variable conflicts.
- Passed full static verification checks: `npm run lint` (`eslint .`) and `npm run build` (`tsc -b && vite build`) with zero errors and zero warnings.

## F0 — Connected UI Pages to Typed Mock Data (Stage 3)

- Connected all secondary UI pages to typed mock fixtures and models with complete data consistency and zero backend drift:
  - `web/src/utils/codeUtils.ts`: Created lightweight client-side Java Language Gate checker (verifying presence of `class`, `interface`, `enum`, or `record` declarations) and logical LOC counter (stripping blank lines and single/multi-line comments). Follows strict safety invariant: never executes submitted code.
  - `web/src/pages/SubmitPage.tsx`: Built interactive line-numbered editor with live Logical LOC, character count (100,000 max), Language Gate validation badge (simulating HTTP 422 for non-Java input), and quick sample fixture loaders (`LoginDao.java`, `OrderProcessingService.java`, `TokenCache.java`, `MalformedOrder.java`, and Python snippet). Triggers simulated navigation to matching analysis run reports.
  - `web/src/pages/ReportPage.tsx`: Connected to `MOCK_ANALYSIS_RUNS`, `MOCK_SUBMISSIONS`, `MOCK_FINDINGS`, and `MOCK_REMEDIATION_SEC_001`. Features simulated run switcher, overall grade hero, transparent scoring formula string, 4 dimension sub-score/density cards, gate rule enforcement notification (`C` grade cap for `SEC-001`), line-level findings list with score impacts, and expandable 7-step verified AI remediation drawer with unified diff.
  - `web/src/pages/FindingsPage.tsx`: Connected to `MOCK_FINDINGS` and `MOCK_EXPLANATIONS`. Implemented filter toolbar (Severity, Dimension, search query), interactive RAG plain-language explanation cards with MITRE CWE & OWASP Top 10 citations, deterministic fallback alert when validation rejects AI text, and precision triage feedback buttons (`TRUE_POSITIVE` / `FALSE_POSITIVE`).
  - `web/src/pages/HistoryPage.tsx`: Connected to `MOCK_SUBMISSION_SUMMARIES` with provenance filtering (`HUMAN`, `AI_GENERATED`, `AI_REMEDIATED`), multi-generation remediation lineage DAG visualization tracking parent `sub-9b1e-001` to child `sub-7f2a-004`, and taxonomy card for all 5 canonical remediation outcomes (`VERIFIED_FIX`, `PARTIAL`, `NO_EFFECT`, `REGRESSION`, `BROKEN`).
  - `web/src/pages/ComparePage.tsx`: Connected to `MOCK_COMPARISON_LOGIN_DAO`. Displays version compare delta (+29.5 pts), dimension deltas, finding change tri-split (Resolved defects, Persisting smells, Introduced regressions), and unified diff with line-offset fingerprint explanation.
  - `web/src/pages/ExperimentsPage.tsx`: Connected to Project Bible Section 6 research hypotheses (H1 & H2) and Section 11 API `remediation-summary` table with 95% Confidence Intervals for `BARE` vs `GUIDED` conditions and simulated CSV export.
  - `web/src/pages/DashboardPage.tsx`: Linked table inspect buttons to deep-link directly into corresponding run reports (`/report?runId=...`).
- Verification checks: `corepack pnpm --dir web run lint` and `corepack pnpm --dir web run build` both passed with 0 errors and 0 warnings.

## F0 — Functional Verification and Consistency Audit (Stage 3.1)

- Conducted comprehensive functional consistency audit across all pages and fixtures:
  - Eliminated false HTTP 422 claims: updated `web/src/utils/codeUtils.ts` and `web/src/pages/SubmitPage.tsx` so client-side Language Gate validation reports honest pre-validation messages without claiming a live HTTP 422 response occurred.
  - Transparent custom code submission: clarified on `SubmitPage.tsx` that in mock simulation mode without a live compiler, submitting custom code loads the representative pre-computed reference audit report (`LoginDao.java`).
  - Cross-page navigation and deep linking: added direct deep links connecting `ComparePage` (links to Base and Remediated run reports), `ReportPage` (link to compare remediated code), `FindingsPage` (links to inspect finding in run report), and `HistoryPage` (links from lineage DAG nodes and table diff buttons).
  - Derived URL query parameter synchronization: refactored `FindingsPage.tsx` to derive active expanded findings without triggering cascading renders or violating React 19 effect rules.
  - Formatted zero-deduction findings: updated finding score deduction badges on `ReportPage` and `FindingsPage` to display `0 pts impact` rather than `-0 pts`.
  - Added mock explanations for `SEC-003` (CWE-327) and `MAINT-001` (empty catch) in `mockExplanations.ts` to ensure full guidance coverage across all findings.
  - Authentic CSV export: implemented genuine client-side CSV blob download (`trials_experiment_rq1_fixture.csv`) on `ExperimentsPage.tsx` while explicitly labeling the study data as illustrative Project Bible Section 6 benchmark data.
- Full verification: `corepack pnpm --dir web run lint` and `corepack pnpm --dir web run build` passed with zero errors and zero warnings.

## F0 — Backend API Contract Alignment & Integration Layer (Stage 4)

- Audited backend contract (`docs/api/contract.md` and `docs/api/openapi.yaml`): confirmed `GET /actuator/health` is the only active endpoint in the agreed specification; all business endpoints are explicitly marked "TBD".
- Implemented frontend API integration foundation under `web/src/api/` without inventing unauthorized endpoints or breaking mock fixtures:
  - `web/src/api/client.ts`: Typed fetch wrapper (`apiFetch`) supporting configurable `VITE_API_BASE_URL` (defaulting to same-origin), automatic JSON header negotiation, and RFC 7807 (`application/problem+json`) error handling with typed `ApiClientError` and `ProblemDetails`.
  - `web/src/api/healthService.ts`: Operational health check service for `GET /actuator/health` returning safe non-throwing `HealthCheckResult` with statuses (`UP`, `DOWN`, `UNKNOWN`).
  - `web/src/api/index.ts`: Central barrel export for all API foundation utilities.
- Connected health check to `Navbar.tsx`: gracefully checks backend availability on mount; renders dynamic live status indicator ("Live Backend" / green pulse when Spring Boot is reachable, or "Simulation Mode" / amber pulse when disconnected).
- Preserved complete mock fixture autonomy: all audit, finding, comparison, and experiment workflows remain fully functional without depending on a running backend.

## F0 — UI Refinements (User Profile Dropdown & Pastel Baby Blue Light Canvas)

- Implemented interactive User Profile capsule dropdown in `web/src/components/layout/Navbar.tsx`:
  - Accessible trigger button displaying user avatar `D`, "Divya", and animated `ChevronDown` arrow with `aria-haspopup` and `aria-expanded`.
  - Polished dropdown card displaying full name `D. Divya Shree`, role `Lead Code Auditor`, clearly labelled demo email address (`divyashree.demo@auditor.internal`), workspace info (`Admin / Auditor MVP`), and simulation status.
  - Implemented dismiss listeners for outside clicks (`mousedown`, `touchstart`) and keyboard `Escape` dismissal (`keydown`). Strictly avoided introducing real authentication mechanisms.
- Refined light theme page canvas in `web/src/styles/tokens.css` and layout shell:
  - Root cause analysis: `#EFF6FF` has 97.3% lightness with only 3.5%-6% RGB channel delta from `#FFFFFF` (yielding a 1.09:1 contrast ratio against white cards), causing human chromatic adaptation to perceive the background as pure white. Additionally, the scrollable canvas (`<main>`) applied `.bg-tech-grid` with `background-image` without an explicit `background-color`, depending entirely on ancestor transparency.
  - Set `--bg-page: #E2EEFC` (a visibly soft pastel baby blue with 1.18:1 gentle contrast against pure white `#FFFFFF` cards, and a crisp 15.2:1 contrast ratio against `--text-main: #0F172A`).
  - Adjusted elevated tokens: `--bg-surface-elevated: #F1F6FD` and `--bg-subtle: #D8E9FA`.
  - Added explicit `background-color: var(--bg-page);` to `.bg-tech-grid` in `tokens.css` and `bg-[var(--bg-page)]` to `<main>` in `AppShell.tsx`.
  - Updated `Navbar.tsx` borders and elevated cards to use `var(--border-subtle)` and `var(--bg-surface-elevated)` so navbar elements adapt to light theme.
  - Preserved dark luxury theme (`#090B10`) and all layout and card structures completely untouched.
- Static and production verification: `corepack pnpm --dir web run lint` and `corepack pnpm --dir web run build` both passed with zero errors and zero warnings.

## F0 — Final UI Polish: Light-Mode Readability

- Addressed light-mode text and container contrast across the dashboard while leaving the pastel baby-blue canvas (`#E2EEFC`) and the dark theme (`#090B10`) completely intact:
  - `web/src/pages/DashboardPage.tsx`:
    - Replaced the low-contrast dark/gray inner code boxes in the "4 Deterministic Analyzers" section with very light sky-blue tinted surfaces (`bg-sky-50/70 dark:bg-black/40 border border-sky-100 dark:border-white/[0.06]`), paired with deep high-contrast text (`text-slate-800 dark:text-slate-300`) and clear code highlight colors.
    - Upgraded metric strip labels, descriptions, and dividers (`divide-slate-200 dark:divide-white/[0.06]`, `text-slate-600 dark:text-slate-400 font-semibold`) for immediate readability on white cards.
    - Enhanced table headers, dividers, and LOC numbers to deep slate (`text-slate-800 dark:text-slate-300`).
    - Upgraded verification flow step cards in the empty state with soft tinted backgrounds (`bg-sky-50/60 dark:bg-white/[0.02]`) and dark text.
    - Enhanced the 5 closed-loop verification outcome cards with crisp white/pastel backgrounds (`bg-white/90 dark:bg-...`), defined borders, and high-contrast dark text (`text-slate-700 dark:text-slate-400`).
  - `web/src/components/common/Badge.tsx`:
    - Fixed low-contrast light badge labels (`ProvenanceBadge`, `GradeBadge`, `OutcomeBadge`) to render deep accessible tones in light mode (`text-slate-700`, `text-purple-800`, `text-cyan-800`, `text-emerald-800`, `text-amber-800`, `text-rose-800`, `text-red-800`) while preserving neon pastels in dark mode.
  - `web/src/components/layout/Navbar.tsx`:
    - Tuned status indicator text, theme toggle hover, and dropdown simulation text for crisp contrast in light mode.
- Verification checks: `corepack pnpm --dir web run lint` and `corepack pnpm --dir web run build` both passed with zero errors and zero warnings.

## F0 — Comprehensive App-Wide Light-Mode Contrast Fix

- Identified root cause of persistent light-mode low contrast:
  - Throughout the application, dozens of elements across all pages hardcoded Tailwind utility classes (`text-slate-400`, `text-slate-300`, `text-slate-200`, `text-slate-500`, `text-sky-400`, `text-cyan-400`, etc.) and `hover:text-white` without a `dark:` scope prefix. In light mode, these evaluated to light silvery grays and neon pastels on white `#FFFFFF` and baby-blue `#E2EEFC` cards (~1.6:1 to 2.5:1 contrast).
  - The desktop navigation sidebar hardcoded `bg-[#0A0D15]/90` (dark navy) in all modes rather than adapting to light mode.
- Implemented app-wide centralized high-contrast rules in `web/src/styles/index.css` scoped strictly to `html:not(.dark)`:
  - Base body text set to `#1E293B` (Slate 800) and headings to `#0F172A` (Slate 900).
  - All pale utility classes (`text-slate-100`, `text-slate-200`, `text-slate-300`, `text-slate-400`, `text-slate-500`, `text-slate-600`) forced to `#0F172A` / `#1E293B` / `#334155`.
  - All status & accent colors (`text-sky-400`, `text-cyan-400`, `text-emerald-400`, `text-teal-400`, `text-amber-400`, `text-orange-400`, `text-rose-400`, `text-purple-400`, `text-red-400`) deepened to rich 700-level hues (`#0369A1`, `#0E7490`, `#047857`, `#0F766E`, `#B45309`, `#C2410C`, `#BE123C`, `#7E22CE`, `#B91C1C`) for 5.5:1 to 7.5:1 WCAG AA/AAA contrast.
  - Overrode `.hover:text-white` to prevent text turning white on hover in light mode.
  - Added clean light styling for form inputs, textareas, selects, and placeholders.
  - Overrode `[class*="border-white/"]` and `[class*="divide-white/"]` to dark hairline borders (`rgba(15, 23, 42, 0.12)`).
  - Configured subtle inset surfaces (`bg-white/[0.02]`, `bg-black/30`, `bg-black/40`) to light elevated slate (`#F8FAFC` and `#F1F5F9`).
- Updated `web/src/components/layout/Sidebar.tsx` and `AppShell.tsx`:
  - Desktop sidebar and mobile drawer now use clean light surfaces in light mode (`bg-white/95 dark:bg-[#0A0D15]/90 border-r border-[var(--border-subtle)]`).
  - Edge-mounted reopen sidebar tab uses light elevated background with dark text in light mode.
- Updated `web/src/styles/tokens.css`:
  - Set `--text-body: #1E293B` (Slate 800) and `--text-muted: #334155` (Slate 700).
- Validated: `corepack pnpm --dir web run lint` and `corepack pnpm --dir web run build` passed with zero errors and zero warnings. Dark mode (`.dark`) is 100% preserved.

## F1 — Authentication & Security Integration (Phases 1–4)

- Configured Vite development proxy in `web/vite.config.ts`:
  - Proxies `/api` and `/actuator` to Spring Boot at `http://localhost:8080` with `changeOrigin: true`.
  - Preserves same-origin browser context so HttpOnly `refresh_token` cookies and CSRF cookies are properly scoped to `/api/v1/auth`.
- Modeled backend domain DTOs in `web/src/types/auth.ts`:
  - Grounded directly in `RegisterRequest.java`, `LoginRequest.java`, `LoginResponse.java`, `UserResponse.java`, and `CsrfController.java`.
  - Supported roles: `USER`, `RESEARCHER`, `ADMIN`.
- Hardened HTTP Client transport in `web/src/api/client.ts`:
  - Set `credentials: 'include'` on all network requests so HttpOnly refresh cookies and CSRF cookies are always transmitted.
  - Kept JWT access tokens strictly in memory; zero usage of `localStorage` or `sessionStorage`.
  - Added in-memory CSRF token manager and automatic `ensureCsrfToken()` fetching from `GET /api/v1/csrf`.
  - Automatically injected `X-XSRF-TOKEN` for mutating HTTP requests (`POST`, `PATCH`, `PUT`, `DELETE`).
  - Added single-flight mutex token refresh handler preventing race conditions during concurrent 401 token rotations.
- Implemented API service modules:
  - `web/src/api/authService.ts`: `register`, `login`, `refresh`, and `logout` mapped to confirmed backend endpoints.
  - `web/src/api/userService.ts`: `getCurrentUser` (`GET /api/v1/users/me`).
  - `web/src/api/index.ts`: Central barrel exports for API foundation.
- Built React authentication state management:
  - `web/src/context/AuthContextCore.ts` and `web/src/context/AuthContext.tsx`:
    - Safe session restoration on app startup: executes silent `refresh()` on mount via the HttpOnly cookie.
    - Synchronized with `client.ts` refresh interceptor.
  - `web/src/hooks/useAuth.ts`: Convenient consumer hook for auth state and operations.
  - `web/src/App.tsx`: Wired `<AuthProvider>` into application shell.
- Built Authentication UI:
  - `web/src/components/auth/AuthModal.tsx`:
    - Fully accessible dialog (focus trap, `Escape` key and backdrop dismissal, scroll lock).
    - Tabbed interface for "Sign In" and "Create Account".
    - Client-side validation: email format, 10-char password requirement.
    - RFC 7807 problem detail error alerts (401 credentials error, 409 email conflict, 429 rate limit).
    - Seamless auto-login on successful account registration.
    - Matches Dark Luxury cosmic glassmorphism and crisp Light porcelain styling.
  - `web/src/components/layout/Navbar.tsx`:
    - Replaced hardcoded demo profile with live authentication state.
    - Shows "Sign In" button when unauthenticated.
    - Shows real avatar initial, display name, system role, and verified email when authenticated.
    - Working "Sign Out" button revoking session and clearing state.
    - Preserved backend health check indicator ("Live Backend" / "Simulation Mode") and theme switcher.
- Preserved Analysis Fixtures & Separation of Concerns:
  - All seven pages (`Dashboard`, `Submit`, `Report`, `Findings`, `History`, `Compare`, `Experiments`) remain fully operational with their typed mock fixtures.
  - No unbuilt backend submission or analysis endpoints were called or invented.
- Verification checks:
  - `npm run lint` (`eslint .`): 0 errors, 0 warnings.
  - `npm run build` (`tsc -b && vite build`): built production bundles with 0 errors.






