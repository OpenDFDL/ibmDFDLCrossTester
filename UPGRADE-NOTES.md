# Upgrade Notes

## Overview

This document covers the changes made to bring the project up to date with
**ACE v12/v13** and **Daffodil 4.1.0**, and to add an initial test schema for
local schema development.

---

## 1. Java Version Requirement

### Issue
The `sbt-daffodil` plugin resolves Daffodil **4.1.0**, which was compiled with
Java 17 (class file version 61). Running `sbt` under the system default Java 8
(class file version 52) causes an immediate crash:

```
java.lang.UnsupportedClassVersionError: org/apache/daffodil/lib/exceptions/AssertMacros$
has been compiled by a more recent version of the Java Runtime (class file version 61.0),
this version of the Java Runtime only recognizes class file versions up to 52.0
```

### Fix
Pass `-java-home` to every sbt invocation, pointing at the Java 17 JDK bundled
inside the ACE installation:

```
sbt -java-home "C:\Program Files\IBM\ACE\12.0.12.17\common\java17" compile
sbt -java-home "C:\Program Files\IBM\ACE\12.0.12.17\common\java17" test
```

For ACE 13, replace the path with:
`C:\Program Files\IBM\ACE\13.0.6.0\common\java17`

---

## 2. Daffodil 4.1.0 API Migration (`IBM_DFDL.scala`)

The source file `src/main/scala/org/apache/daffodil/tdml/processor/IBM_DFDL.scala`
was written against Daffodil 3.x. The following API changes were required.

### 2.1 Package renames — `lib.api` → `lib.iapi`

Several types moved from `org.apache.daffodil.lib.api` to
`org.apache.daffodil.lib.iapi`:

| Old import | New import |
|----|---|
| `lib.api.DaffodilSchemaSource` | `lib.iapi.DaffodilSchemaSource` |
| `lib.api.EmbeddedSchemaSource` | `lib.iapi.EmbeddedSchemaSource` |
| `lib.api.URISchemaSource` | `lib.iapi.URISchemaSource` |
| `lib.api.Diagnostic` | `lib.iapi.Diagnostic` |
| `lib.api.DataLocation` | `api.DataLocation` |
| `lib.api.ValidationMode` | *(removed — see §2.4)* |

### 2.2 `getDiagnostics` return type — `Seq` → `java.util.List`

The `TDMLResult` interface now returns `java.util.List<api.Diagnostic>` instead
of a Scala `Seq`. Two aliases and a conversion import were added:

```scala
import java.util.{List => JList}
import scala.jdk.CollectionConverters._
import org.apache.daffodil.api.{Diagnostic => ApiDiagnostic}
```

`getDiagnostics` in both `DiagnosticsMixin` and `IBMTDMLResult` was changed to:

```scala
def getDiagnostics: JList[ApiDiagnostic] = (diagnostics: Seq[ApiDiagnostic]).asJava
```

Internal logic that called `.exists()` on the result was changed to call
`.exists()` on the raw `diagnostics: Seq[IBMTDMLDiagnostic]` directly.

### 2.3 `getProcessor` signature — removed `useSerializedProcessor`

The `AbstractTDMLDFDLProcessorFactory.getProcessor` no longer accepts a
`useSerializedProcessor: Boolean` parameter. The override signature was updated
accordingly:

```scala
// Before
override def getProcessor(
  schemaSource: DaffodilSchemaSource,
  useSerializedProcessor: Boolean,   // ← removed
  optRootName: Option[String],
  optRootNamespace: Option[String],
  tunables: Map[String, String]): TDML.CompileResult

// After
override def getProcessor(
  schemaSource: DaffodilSchemaSource,
  optRootName: Option[String],
  optRootNamespace: Option[String],
  tunables: Map[String, String]): Either[JList[ApiDiagnostic], (JList[ApiDiagnostic], TDMLDFDLProcessor)]
```

`TDML.CompileResult` no longer exists; the return type is spelled out in full.

### 2.4 `withValidationMode` → `withValidation(String)`

The `TDMLDFDLProcessor` interface replaced `withValidationMode(ValidationMode.Type)`
with `withValidation(String)`. The `ValidationMode` enum is gone entirely.

```scala
// Before
override def withValidationMode(validationMode: ValidationMode.Type): IBMTDMLDFDLProcessor =
  copy(shouldValidate = validationMode match {
    case ValidationMode.Full    => true
    case ValidationMode.Limited => true
    case ValidationMode.Off     => false
  })

// After
override def withValidation(validationMode: String): IBMTDMLDFDLProcessor =
  copy(shouldValidate = validationMode match {
    case "full"    => true
    case "limited" => true
    case "off"     => false
  })
```

### 2.5 `withDebugging` removed

`TDMLDFDLProcessor.withDebugging(Boolean)` was removed from the interface. The
override was deleted. `withDebugger(Object)` remains and still throws `???`.

### 2.6 `Diagnostic` constructor — added trailing `Nil` argument

The `lib.iapi.Diagnostic` abstract class gained a trailing `Seq[Any]` parameter
for format-string arguments. The `IBMTDMLDiagnostic` super-constructor call was
updated to add `Nil` and to use positional (not named) arguments, which is
required in Scala 3:

```scala
// Before
extends Diagnostic(Maybe.Nope, Maybe.Nope,
  maybeCause = Maybe(throwable),
  maybeFormatString = Maybe(...))

// After
extends Diagnostic(
  Maybe.Nope,
  Maybe.Nope,
  Maybe(throwable),
  Maybe(...),
  Nil)
```

### 2.7 `getSomeMessage.get` → `getMessage`

The `Diagnostic.getSomeMessage` helper method was removed. The standard Java
`getMessage()` method is used instead:

```scala
// Before
diagnostics.map(_.getSomeMessage.get).mkString("\n")

// After
diagnostics.map(_.getMessage).mkString("\n")
```

### 2.8 `EmbeddedSchemaSource.copy` — all arguments now required

The `EmbeddedSchemaSource.copy` method no longer accepts named/optional
parameters. All three fields must be passed explicitly:

```scala
// Before
ess.copy(node = newNode)

// After
ess.copy(newNode, ess.nameHint, ess.optTmpDir)
```

### 2.9 Scala 3 vararg syntax

Two Scala 2 vararg patterns in `RemoveDafintTransformer` were updated to Scala 3
syntax:

```scala
// Before
case Elem(prefix, label, attributes, scope, children @ _ *) =>
  new Elem(prefix, label, newAttributes, scope, true, children: _*)

// After
case Elem(prefix, label, attributes, scope, children*) =>
  new Elem(prefix, label, newAttributes, scope, true, children*)
```

---

## 3. ACE v12 / v13 Jar Changes

The project README documents the ACE 11 jar list. The jars shipped with
ACE 12 and 13 are different:

| ACE 11 name | ACE 12/13 name |
|---|---|
| `gpb.jar` | `protobuf-java-3.25.5.jar` |
| `scd.jar` | *(gone — functionality folded into ibm-dfdl-eclipse-dependencies.jar)* |
| `icu4j-charsets.jar` | `icu4j-charset.jar` |
| `emf.common_2.6.0.jar` | `org.eclipse.emf.common-2.30.0.jar` |
| `emf.ecore_2.6.1.jar` | `org.eclipse.emf.ecore-2.36.0.jar` |
| `emf.ecore.xmi_2.5.0.jar` | `org.eclipse.emf.ecore.xmi-2.37.0.jar` |
| `xsd_2.6.0.jar` | `org.eclipse.xsd-2.12.0.jar` |
| *(new)* | `ibm-dfdl-eclipse-dependencies.jar` |
| *(new)* | `icu4j-localespi.jar` |

Both ACE 12 and ACE 13 ship the same 10 runtime jars plus `dfdlsample_java.jar`
from the samples directory (11 jars total in `lib/`).

---

## 4. New Files Added

### `setup-ace-jars.ps1`

A PowerShell script that copies the required IBM DFDL jars and sample files from
an ACE installation into the project. Accepts an `-AceVersion` parameter
(`"12"` or `"13"`, defaulting to `"12"`):

```powershell
.\setup-ace-jars.ps1              # uses ACE 12
.\setup-ace-jars.ps1 -AceVersion 13
```

What it copies:
- All `*.jar` from `<ACE>\server\dfdl\lib\` → `lib\`
- `dfdlsample_java.jar` from `<ACE>\server\sample\dfdl\` → `lib\`
- `company.*` from `<ACE>\server\sample\dfdl\` → `src\test\resources\`
- `RecordSeparatedFieldFormat.xsd` → `src\test\resources\IBMdefined\`

### `src/test/resources/SimpleEDI.dfdl.xsd`

A self-contained EDI-inspired DFDL schema for use as a development and testing
template. Models an invoice-like message with `HDR`, `DTL` (0–99 occurrences),
and `TRL` segments, delimited by `+` (field separator) and `'` (segment
terminator). Compatible with both IBM DFDL and Daffodil.

### `src/test/resources/SimpleEDI-happy.txt`

Valid test input for `SimpleEDI.dfdl.xsd`:
```
HDR+SENDER01+RECEIVER1+20250301+INVOIC'
DTL+1+WIDGET-A+100+EA'
DTL+2+GADGET-B+50+BX'
TRL+2'
```

### `src/test/resources/SimpleEDI-error.txt`

Intentionally malformed input (TRL missing its `ControlCount` field) used to
verify error detection.

### `src/test/resources/SimpleEDITests.tdml`

TDML test suite with three test cases targeting both IBM DFDL and Daffodil
(`defaultImplementations="ibm daffodil"`):

| Test | What it checks |
|---|---|
| `parseHappy` | Parses the happy-flow file and verifies the infoset |
| `roundTrip` | Two-pass round-trip (parse → unparse → parse) |
| `parseError` | Expects a parse error on the malformed input |

Note: `parseHappy` uses `roundTrip="none"` because the input file contains CRLF
newlines after each segment terminator that IBM DFDL does not reproduce on
unparse (the schema allows but does not require trailing whitespace).

### `src/test/scala/io/github/openDFDL/TestSimpleEDI.scala`

JUnit test runner for the `SimpleEDITests.tdml` suite.

---

## 5. Git Remote

The git remote `origin` was updated to point to the project fork:

```
https://github.com/matthiasblomme/ibmDFDLCrossTester.git
```
