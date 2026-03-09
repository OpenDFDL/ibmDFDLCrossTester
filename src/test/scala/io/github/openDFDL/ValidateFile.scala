package io.github.openDFDL

import java.io.{File, FileInputStream}

import scala.xml.XML

import org.xml.sax.{ErrorHandler => SaxErrorHandler, InputSource, SAXParseException}
import org.junit.Test
import org.junit.Assert

import com.ibm.dfdl.grammar.DFDLGrammarFactory
import com.ibm.dfdl.processor.{DFDLProcessorFactory, IDFDLDiagnostic, IDFDLProcessorErrorHandler}
import com.ibm.dfdl.sample.sax.reader.DFDLReader

import org.apache.daffodil.lib.xml.DFDLCatalogResolver

/**
 * Ad-hoc IBM DFDL validator driven entirely by system properties.
 * Run via validate.ps1 or:
 *   sbt -Dvalidate.schema=<path> -Dvalidate.data=<path> [-Dvalidate.root=<name>] \
 *       "testOnly io.github.openDFDL.ValidateFile"
 */
class ValidateFile {

  private val errors = scala.collection.mutable.ListBuffer[String]()

  /** Combined IBM DFDL + SAX error handler that collects all errors. */
  private val errorHandler = new IDFDLProcessorErrorHandler with SaxErrorHandler {
    override def processingError(d: IDFDLDiagnostic): Unit   = errors += s"[Processing error] ${d.getSummary}"
    override def schemaDefinitionError(d: IDFDLDiagnostic): Unit = errors += s"[Schema error] ${d.getSummary}"
    override def validationError(d: IDFDLDiagnostic): Unit   = errors += s"[Validation error] ${d.getSummary}"
    override def warning(d: IDFDLDiagnostic): Unit           = System.err.println(s"[Warning] ${d.getSummary}")
    override def error(e: SAXParseException): Unit           = errors += s"[Parse error] ${e.getMessage}"
    override def fatalError(e: SAXParseException): Unit      = errors += s"[Fatal error] ${e.getMessage}"
    override def warning(e: SAXParseException): Unit         = System.err.println(s"[Warning] ${e.getMessage}")
  }

  /**
   * Reads the schema XSD and returns (rootElementName, targetNamespace).
   * Prefers elements annotated with ibmSchExtn:docRoot="true" or ibmDfdlExtn:docRoot="true",
   * falls back to the first top-level xsd:element.
   */
  private def autoDetectRoot(schemaFile: File): (String, String) = {
    val schema   = XML.loadFile(schemaFile)
    val targetNs = (schema \ "@targetNamespace").text

    val ibmSchExtn  = "http://www.ibm.com/schema/extensions"
    val ibmDfdlExtn = "http://www.ibm.com/dfdl/extensions"

    val topLevelElems = schema \ "element"
    val docRootElem = topLevelElems.find { e =>
      e.attribute(ibmSchExtn, "docRoot").exists(_.text == "true") ||
      e.attribute(ibmDfdlExtn, "docRoot").exists(_.text == "true")
    }

    val elem = docRootElem.orElse(topLevelElems.headOption).getOrElse(
      sys.error("No top-level elements found in schema — use -Dvalidate.root=<name>")
    )
    val name = (elem \ "@name").text
    (name, if (targetNs.isEmpty) null else targetNs)
  }

  @Test def validate(): Unit = {
    val schemaPath = sys.props.getOrElse("validate.schema",
      throw new IllegalArgumentException("Required: -Dvalidate.schema=<path>"))
    val dataPath = sys.props.getOrElse("validate.data",
      throw new IllegalArgumentException("Required: -Dvalidate.data=<path>"))

    val schemaFile = new File(schemaPath)
    val dataFile   = new File(dataPath)
    require(schemaFile.exists(), s"Schema not found: $schemaPath")
    require(dataFile.exists(),   s"Data file not found: $dataPath")

    val rootProp = sys.props.get("validate.root").filter(_.nonEmpty)
    val (rootName, rootNamespace) = rootProp match {
      case Some(r) => (r, sys.props.get("validate.namespace").orNull)
      case None    => autoDetectRoot(schemaFile)
    }

    val verbose = sys.props.get("validate.verbose").contains("true")

    println(s"Schema    : $schemaPath")
    println(s"Data file : $dataPath")
    println(s"Root      : $rootName  namespace: ${Option(rootNamespace).getOrElse("(none)")}")
    if (verbose) println("Verbose   : on (IBM DFDL service trace enabled)")
    println("")

    // Optional trace listener — attaches to both grammar factory and parser when verbose=true
    val tracer = if (verbose) Some(new TraceListener()) else None

    // --- 1. Compile schema ---
    val grammarFactory = new DFDLGrammarFactory()
    grammarFactory.setErrorHandler(errorHandler)
    tracer.foreach(grammarFactory.setServiceTraceListener)
    val grammar = grammarFactory.buildGrammarFromSchema(schemaFile.toURI, DFDLCatalogResolver.get)

    if (grammar == null || errors.nonEmpty) {
      System.err.println("=== SCHEMA COMPILATION FAILED ===")
      errors.foreach(System.err.println)
      Assert.fail("Schema compilation failed:\n" + errors.mkString("\n"))
    }

    // --- 2. Parse data file ---
    val processorFactory = new DFDLProcessorFactory()
    val parser = processorFactory.createParser
    parser.setGrammar(grammar)
    parser.setRootElement(rootName, rootNamespace)
    tracer.foreach(parser.addServiceTraceListener)

    val sb             = new java.lang.StringBuilder
    val contentHandler = new XMLSAXContentHandler1(sb)
    val dfdlReader     = new DFDLReader2(parser)
    dfdlReader.setContentHandler(contentHandler)
    dfdlReader.setErrorHandler(errorHandler)
    dfdlReader.setFeature(DFDLReader.SAX_FEATURE_NAMESPACES, true)
    dfdlReader.parse(new InputSource(new FileInputStream(dataFile)))

    if (errors.nonEmpty) {
      System.err.println("=== PARSE FAILED ===")
      errors.foreach(System.err.println)
      Assert.fail("Parse failed:\n" + errors.mkString("\n"))
    }

    println("=== PARSE SUCCESSFUL ===")
    println(sb.toString())
  }
}
