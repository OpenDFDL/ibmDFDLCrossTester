# How to Test a DFDL Schema Against IBM DFDL

This guide covers the quickest path from zero to running IBM DFDL against your
own schema and data file — no TDML knowledge required.

---

## Prerequisites

- IBM ACE 12 or 13 installed (default paths below, or use `-AcePath` for custom)
- SBT installed at `C:\Program Files (x86)\sbt\bin\sbt.bat`
- This project checked out and `lib/` populated (see Step 1)

---

## Step 1 — Populate `lib/` (one-time setup)

Copy the IBM DFDL jars from your ACE installation into the project:

```powershell
# ACE 12 (default)
.\setup-ace-jars.ps1

# ACE 13
.\setup-ace-jars.ps1 -AceVersion 13

# Custom install path
.\setup-ace-jars.ps1 -AcePath "C:\Program Files\IBM\ACE\12.0.12.17"
```

This copies the runtime jars into `lib/` and the IBM sample files into
`src/test/resources/`. You only need to do this once (or when switching ACE versions).

---

## Step 2 — Run the validator

Point `validate.ps1` at your schema and data file:

```powershell
.\validate.ps1 `
    -Schema "D:\GIT\ibmDFDLCrossTester\test\ROCS\ROCS_Inhouse.xsd" `
    -Data   "D:\GIT\ibmDFDLCrossTester\test\ROCS\0001144785.txt"
```

### What it does

1. Resolves the ACE Java 17 installation
2. If your schema imports `IBMdefined/RecordSeparatedFieldFormat.xsd` (relative
   path), copies that folder next to your schema automatically
3. Auto-detects the root element (looks for `ibmSchExtn:docRoot="true"` or
   `ibmDfdlExtn:docRoot="true"` in the schema; falls back to the first top-level
   element)
4. Compiles the schema with IBM DFDL
5. Parses the data file
6. Prints either the parsed XML infoset or a clear error message

### Success output

```
Schema    : D:\...\ROCS_Inhouse.xsd
Data file : D:\...\0001144785.txt
Root      : Inhouse_Dis  namespace: (none)

=== PARSE SUCCESSFUL ===
<Inhouse_Dis>
    <SENDER-ID-ROCS>NTSPCLDN        </SENDER-ID-ROCS>
    ...
</Inhouse_Dis>
```

### Failure output

```
=== PARSE FAILED ===
[Parse error] Unexpected data found after end of parse.
```

---

## Optional switches

| Switch | Default | Description |
|--------|---------|-------------|
| `-Root <name>` | auto-detect | Force a specific root element name |
| `-AceVersion 12\|13` | `12` | Select ACE version by number |
| `-AcePath <path>` | — | Full path to ACE install root (overrides `-AceVersion`) |
| `-Trace` | off | Print IBM DFDL's full service trace to stderr (note: `-Verbose` is reserved by PowerShell) |

Examples:

```powershell
# Schema has two doc-root elements — pick one explicitly
.\validate.ps1 -Schema "..." -Data "..." -Root "Inhouse_Load"

# ACE 13
.\validate.ps1 -Schema "..." -Data "..." -AceVersion 13

# Non-standard install location
.\validate.ps1 -Schema "..." -Data "..." -AcePath "C:\Program Files\IBM\ACE\12.0.12.17"

# Detailed trace output for diagnosing parse failures
.\validate.ps1 -Schema "..." -Data "..." -Trace
```

### Understanding verbose output

With `-Trace`, IBM DFDL's service trace is written to stderr before the
summary. Each line shows the event level, the error code (if any), the byte
offset, and the schema location:

```
TraceListener: error: CTDP3041E: Initiator '2210' not found at offset '1.724' for element '...EITG2210[1]'.
TraceListener: info: Offset: 1724. Parser was unable to resolve data on the current branch ...
TraceListener: info: Offset: 1724. Element 'INHOUSE-CNI-ROCS' is optional or missing. The element will not be included in the infoset.
TraceListener: fatal: CTDP3002E: Unexpected data found at offset '1724' after parsing completed.
=== PARSE FAILED ===
[Fatal error] CTDP3002E: Unexpected data found at offset '1724' after parsing completed.
```

The `error:` lines identify which schema element failed to match and why —
this is the key diagnostic for schema or data mismatches.

---

## Schema requirements

Your DFDL schema must be compatible with IBM DFDL. Common pitfalls:

- **No generic `padChar`** — IBM DFDL requires the explicit property
  (`dfdl:textStringPadCharacter`, not `dfdl:padChar` on `dfdl:format`).
- **`IBMdefined/` imports** — schemas that import
  `IBMdefined/RecordSeparatedFieldFormat.xsd` need that file adjacent to the
  schema. `validate.ps1` handles this automatically using the copy in
  `src/test/resources/IBMdefined/`.
- **Root element annotation** — mark your root element with
  `ibmSchExtn:docRoot="true"` or `ibmDfdlExtn:docRoot="true"` so that
  auto-detection works. If the schema has no such annotation, pass `-Root`.

---

## Adding a repeatable TDML test (optional next step)

Once parsing works, you can capture the expected infoset output and turn it
into a permanent TDML regression test:

1. Copy the printed infoset XML into a new `<tdml:parserTestCase>` in a `.tdml`
   file under `src/test/resources/`
2. Create a matching Scala test class (see `TestSimpleEDI.scala` as a template)
3. Run `sbt test` to verify

This gives you a permanent cross-test that runs against both IBM DFDL and
Daffodil on every build. See `src/test/resources/SimpleEDITests.tdml` and
`src/test/scala/io/github/openDFDL/TestSimpleEDI.scala` for a complete
working example.
