## Requirements

### Requirement: Standards skills cover GB/T 38634 and ISO 29119-3 mapping

The project SHALL provide AI-consumable skills under `readme/development/skills/standards/` that summarize GB/T 38634 parts 1–4 and map them to ISO/IEC/IEEE 29119 counterparts, with emphasis on Part 3 test documentation information items, common header elements, and conformance (full vs tailored).

#### Scenario: Index lists standard skills

- **WHEN** an agent opens `readme/development/skills/SKILL-INDEX.md`
- **THEN** it MUST find entries pointing to standards skills for concepts, process, documentation, and techniques (as applicable after OCR)

#### Scenario: Documentation skill names information items

- **WHEN** an agent reads the documentation standards skill
- **THEN** it MUST be able to identify the standard’s test documentation information items (e.g. test plan, test status report, test completion report, incident report) and whether each is a standalone document or a section within another document per the standard

### Requirement: OCR sources retained for GB scans

The change SHALL OCR the provided GB/T 38634 PDF scans into searchable text sources under `readme/development/skills/` (or a documented `_sources` subdirectory) before finalizing Chinese standards skill content that claims GB wording coverage.

#### Scenario: GB OCR precedes GB-claiming skill finalization

- **WHEN** a standards skill asserts GB/T 38634 normative Chinese content
- **THEN** corresponding OCR (or manually transcribed) source text for that part MUST exist in the repository path documented by the skill index

### Requirement: Segmented reading of large standard sources

Because national-standard PDFs and OCR corpora are large, agents SHALL use a segmented read strategy: full text MAY be stored on disk, but MUST NOT be loaded wholesale into a single model context when authoring skills or templates. Sources SHALL be split by part/chapter/page-range with a manifest; skills SHALL prefer structured summaries with pointers to source slices; `SKILL-INDEX.md` (or equivalent) SHALL document the load-on-demand protocol.

#### Scenario: Manifest enables selective load

- **WHEN** an agent needs GB/T 38634.3 content about test completion reporting
- **THEN** it MUST load the index/manifest and only the relevant chapter or page-range slice(s), not the entire Part 3 OCR file in one pass

#### Scenario: Skills stay summary-sized

- **WHEN** a standards skill is written for agent consumption
- **THEN** it MUST NOT embed an entire standard part as inline body text and MUST reference `_sources` slices (path + page range) for normative detail

#### Scenario: Parallel agents do not each ingest all volumes

- **WHEN** multiple agents process standards in parallel
- **THEN** work SHALL be partitioned by volume or chapter so that no single agent is instructed to read all four GB parts end-to-end in one context

### Requirement: Template boundary follows the standard

Standards skills and taxonomy SHALL instruct agents that report template file boundaries MUST follow the national/international standard’s definition of documents and information items (including allowed tailoring), and MUST NOT assume a hard rule of one template file per document-type name regardless of the standard.

#### Scenario: Combined sections when standard defines one document

- **WHEN** the standard defines multiple information elements as required parts of a single document type (e.g. sections of a test completion report)
- **THEN** the taxonomy MUST recommend a single template (or a primary template plus explicitly labeled appendices), not unrelated separate templates that fragment mandatory sections without standard basis

#### Scenario: Separate templates when standard defines separate documents

- **WHEN** the standard defines two deliverables as distinct documents
- **THEN** the taxonomy MUST allow separate default templates for those deliverables unless a documented tailored-conformance merge is justified
