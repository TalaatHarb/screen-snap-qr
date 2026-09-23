# AGENTS.md

Instructions and conventions for anyone (human or AI agent) working on this
repository. Read this before making changes.

## Repository layout

- Maven module root (`pom.xml`, source, tests) is **`screen-snap-qr/screen-snap-qr/`**,
  *not* the repository root (`screen-snap-qr/`). Always `cd` into the nested
  directory before running `mvn`.
- Main sources: `src/main/java/net/talaatharb/screensnapqr/...`
- Tests: `src/test/java/net/talaatharb/screensnapqr/...` (same package structure
  as main sources, mirrored 1:1).
- FXML/CSS resources: `src/main/resources/net/talaatharb/screensnapqr/ui/`.
- `README.md` (repo root) documents user-facing features and the schema-viewer
  roadmap — keep it in sync whenever a feature is added, changed, or completed.

## Build, test, and run commands

Always run Maven from `screen-snap-qr/screen-snap-qr/`.

- Compile only: `mvn -o compile`
- Unit tests only (fast, Surefire, `*Test` classes): `mvn -o test`
- Full verification (unit + integration tests + coverage check + PMD, etc.):
  `mvn -o verify`
- Run the app directly (JavaFX, via the `javafx-maven-plugin`): `mvn javafx:run`
- Package a runnable fat-jar: `mvn -o package` (produces
  `target/screen-snap-qr-*-jar-with-dependencies.jar`)
- Prefer `-o` (offline) once dependencies are resolved; only drop it if a new
  dependency/plugin needs to be downloaded.
- JDK 21 is enforced by `maven-enforcer-plugin`; make sure `JAVA_HOME` points at
  a JDK 21 install (see README "Requirements" section for the PowerShell snippet).
- PowerShell quirk: multi-value `-Dtest=A,B` arguments must be fully quoted,
  e.g. `mvn -o test "-Dtest=FooTest,BarTest"`, otherwise PowerShell's comma list
  splitting breaks the argument.

## Test coverage (JaCoCo)

- `mvn verify` enforces a **minimum 60% line coverage per Java package**
  (`minimum-coverage-ratio` property in `pom.xml`). The build **fails** if any
  package (excluding `*Application.*` classes and `**/config/*`) drops below
  this threshold — treat this as a hard gate, not a suggestion.
- Unit tests (Surefire) and integration tests (Failsafe, `*IT` classes) are
  instrumented with **separate** JaCoCo agents/exec files
  (`target/jacoco.exec` and `target/jacoco-it.exec`, via distinct
  `surefireArgLine`/`failsafeArgLine` properties), then **merged**
  (`target/jacoco-merged.exec`) before the coverage report/check runs. This is
  intentional: a lot of JavaFX UI code (controllers, `TreeCell`/`StringConverter`
  factories, capture overlays) is only exercised by `*IT` TestFX tests, not
  plain unit tests, so the check must see combined coverage.
- If you add code that lowers a package's coverage below 60%, add tests in the
  same change — don't lower the threshold or move code to a `config`/
  `*Application` exclusion to dodge the check.
- When investigating coverage gaps, inspect
  `target/site/jacoco-merged/<package>/index.html` (or the `jacoco.xml` in the
  same directory) rather than guessing; it shows exact missed lines per class.

## Testing conventions

- **Unit tests**: plain JUnit 5 classes named `*Test`, run by Surefire. Use
  Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`) for
  collaborators. These should not require the JavaFX toolkit unless the class
  under test genuinely needs it.
- **Integration tests**: classes named `*IT`, run by Failsafe, `extends
  org.testfx.framework.junit5.ApplicationTest` for anything touching real
  JavaFX controls/scenes/stages. Patterns used throughout the test suite:
  - Fields are wired manually in `@BeforeEach` via `Platform.runLater(...)`
    (controllers aren't loaded through the real FXML loader in tests).
  - **Always** follow that `Platform.runLater` setup with an
    `Awaitility.await()...untilAsserted(...)` check that a field is non-null
    before the test body touches it from the test thread — `Platform.runLater`
    is async, and skipping this wait causes flaky/racy `NullPointerException`s
    (this bit us once; see `MainUiControllerIT`/`QRCardControllerIT`).
  - Use `interact(() -> ...)` (from `ApplicationTest`) to run assertions/code
    that must execute on the FX Application Thread.
  - Use `org.awaitility.Awaitility.await()` (already a test dependency) for
    polling on background-thread results (e.g. capture threads, Platform
    runnables) instead of `Thread.sleep` + manual checks.
  - This project runs against the **real Windows desktop/windowing system**
    (no Monocle/headless config is set up) — TestFX tests open real, visible
    windows. Don't assume a headless CI display; don't add code that requires
    exclusive foreground focus without considering this.
- Full end-to-end coverage for something like a `TreeCell`/`Cell` factory
  requires actually showing a `Stage`/`Scene` containing the control and
  forcing layout (e.g. `lookupAll(".tree-cell")`), not just calling the
  controller method that builds the callback — see
  `QRCardControllerIT#testZipTreeCellFactoryBuildsSchemaAwareContextMenus` for
  the pattern.

## Structured schema viewer pattern

Several features (Romanian e-prescription, EU e-invoicing, SMART Health Cards,
US DSCSA) follow an **identical, deliberate pattern** — replicate it exactly
for any new schema viewer (see README "Roadmap" section for candidate
schemas):

1. **Package** `net.talaatharb.screensnapqr.ui.<schema>` containing:
   - `<Schema>Document` — typed model/record holding parsed fields.
   - `<Schema>Field` / `<Schema>FieldDictionary` — maps raw schema field codes
     to human-readable labels (and any lookup tables, e.g. CVX vaccine codes).
   - `<Schema>Formatting` — value formatting helpers (dates, percentages, etc.).
   - `<Schema>Parser` — `tryParse(String content) -> Optional<Document>`;
     performs schema **detection** (must be conservative/signature-based to
     avoid false positives on unrelated content) and parsing. Must fail safely
     (return `Optional.empty()`/`null`) rather than throwing on non-matching or
     malformed input.
   - `<Schema>Exporter` — builds plain-text and JSON export representations.
2. **Viewer UI**: `net.talaatharb.screensnapqr.ui.<schema>.<Schema>View`
   (the actual layout/controls) + `net.talaatharb.screensnapqr.ui.viewer.
   <Schema>Viewer` (the standalone resizable stage/window wrapper), mirroring
   `PrescriptionView`/`PrescriptionViewer`. Requirements for every viewer:
   - Responsive layout that reflows to the window size.
   - All field values copyable (individually and via a "Copy all details"
     action).
   - An **Export...** action (plain text + JSON).
   - Styled via new `<schema>-*` CSS classes added to `theme.css`, following
     the existing `prescription-*`/`invoice-*`/`shc-*`/`dscsa-*` class
     families (`*-view`, `*-scroll`, `*-title`, `*-section`, `*-grid`,
     `*-field-label`, `*-field-value`).
3. **Wiring** in `QRCardController`:
   - A ZIP-tree right-click menu item ("View ...") + `open<Schema>Viewer
     (ZipNode)` method, added inside `setupZipTreeView()`'s `TreeCell`
     callback, gated behind `<Schema>Parser.tryParse(...).isPresent()`.
   - A "Rendered" tab right-click menu item + `openRendered<Schema>Viewer()`
     method, added inside `setupContentContextMenu()`, so a scanned payload
     that isn't wrapped in a ZIP can still be opened directly.
4. **Tests**: `<Schema>ParserTest`, `<Schema>ExporterTest` (plain unit tests),
   plus IT tests added to `QRCardControllerIT` following the existing
   `testOpen<Schema>ViewerWithMatchingSchema` /
   `testOpenRendered<Schema>ViewerUsesScannedContentDirectly` naming pattern.
5. Update `README.md`: add a feature bullet (matching the style/wording of the
   existing e-invoice/prescription/SHC/DSCSA bullets, including any known
   limitations) and update the "Roadmap" section's ordered list / "Further
   candidates" list as items are implemented.

## Code style

- Lombok is used extensively (`@Data`, `@Getter`/`@Setter`,
  `@RequiredArgsConstructor`, `@Slf4j`) — prefer it over hand-written
  boilerplate to stay consistent.
- MapStruct is used for DTO/model mapping in some packages (see
  `net.talaatharb.screensnapqr.mapper`) — follow existing mapper interfaces
  rather than writing manual mapping code where one already exists.
- Parsers must not throw on malformed/non-matching input; always return
  `Optional`/`null` safely so schema **detection** across multiple parsers
  (tried in sequence by the controller) can proceed without one bad parser
  crashing the pipeline.

## Housekeeping

- Don't leave scratch/debug files (temporary `.java` scripts, ad-hoc
  `check_coverage.py`-style analysis scripts, generated logs) in the repo
  after a task — clean them up once done investigating.
- Do not add markdown planning/notes files unless explicitly requested.
