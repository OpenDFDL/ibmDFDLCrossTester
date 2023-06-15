/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.daffodil.processor.tdml

import java.io.StringReader
import java.net.URI

import scala.xml.Node
import scala.xml.Elem
import scala.xml.transform.RuleTransformer
import scala.xml.transform.RewriteRule

import org.apache.commons.io.input.ReaderInputStream
import org.apache.daffodil.lib.api.DaffodilSchemaSource
import org.apache.daffodil.lib.api.DataLocation
import org.apache.daffodil.lib.api.Diagnostic
import org.apache.daffodil.lib.api.EmbeddedSchemaSource
import org.apache.daffodil.lib.api.URISchemaSource
import org.apache.daffodil.lib.api.ValidationMode
import org.apache.daffodil.lib.exceptions.Assert
import org.apache.daffodil.lib.externalvars.Binding
import org.apache.daffodil.lib.util.Maybe
import org.apache.daffodil.lib.xml.DFDLCatalogResolver
import org.apache.daffodil.lib.xml.XMLUtils
import org.apache.daffodil.tdml.processor._

import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.XMLReaderFactory

import com.ibm.dfdl.grammar.DFDLGrammarFactory
import com.ibm.dfdl.grammar.IDFDLGrammar
import com.ibm.dfdl.processor.DFDLProcessorFactory
import com.ibm.dfdl.processor.IDFDLDiagnostic
import com.ibm.dfdl.processor.IDFDLParser
import com.ibm.dfdl.processor.IDFDLProcessor
import com.ibm.dfdl.processor.IDFDLProcessorErrorHandler
import com.ibm.dfdl.processor.exceptions.DFDLException
import com.ibm.dfdl.processor.types.DFDLDiagnosticType

import com.ibm.dfdl.sample.sax.reader.DFDLReader
import com.ibm.dfdl.sample.sax.writer.SAXToDFDLEventAdapter

import io.github.openDFDL.TraceListener
import io.github.openDFDL.DFDLReader2
import io.github.openDFDL.XMLSAXContentHandler1

import org.apache.daffodil.tdml.TDMLTestNotCompatibleException

object IBMDFDLMode extends Enumeration {
  type Type = Value
  val Parse, Unparse, Compile, Configuration = Value
}

final class IDFDLDiagFromThrowable(cause: Throwable) extends IDFDLDiagnostic {
  Assert.usage(cause ne null)

  override def getCode = cause.getClass().getName()

  override def getSummary = cause.getMessage()

  override def getCodeAndSummary = getCode() + ": " + getSummary()

  override def getSchemaLocation: URI = ???

  override def getType = DFDLDiagnosticType.PROCESSINGERROR
}

final class IBMTDMLDiagnostic(iddArg: IDFDLDiagnostic, throwable: Throwable, mode: IBMDFDLMode.Type)
  extends Diagnostic(Maybe.Nope, Maybe.Nope,
    maybeCause = Maybe(throwable),
    maybeFormatString = Maybe(if (iddArg ne null) iddArg.getSummary() else null)) {

  lazy val idd: IDFDLDiagnostic =
    if (iddArg ne null) iddArg
    else new IDFDLDiagFromThrowable(throwable)

  def this(idd: IDFDLDiagnostic, mode: IBMDFDLMode.Type) = this(idd, null, mode)
  def this(cause: Throwable, mode: IBMDFDLMode.Type) = this(null, cause, mode)

  def getType() = idd.getType()

  override def equals(b: Any) = {
    super.equals(b) &&
      {
        val other = b.asInstanceOf[IBMTDMLDiagnostic].idd
        other.getType() == idd.getType() &&
          other.getCode() == idd.getCode()
        other.getSummary() == idd.getSummary()
        other.getSchemaLocation() == idd.getSchemaLocation()
      }
  }

  override def hashCode() = idd.hashCode()

  override def isError = idd.getType() match {
    case DFDLDiagnosticType.WARNING | DFDLDiagnosticType.RECOVERABLEERROR => false
    case _ => true
  }
  /**
   * Define as "Parse", "Unparse", "Schema Definition", "Configuration".
   *
   * This is combined with the word "Error" or "Warning"
   */
  override protected def modeName: String = idd.getType match {
    case DFDLDiagnosticType.PROCESSINGERROR => mode.toString()
    case DFDLDiagnosticType.SCHEMADEFINITIONERROR => "Schema Definition"
    case DFDLDiagnosticType.RECOVERABLEERROR => mode.toString()
    case DFDLDiagnosticType.WARNING => mode.toString()
    case DFDLDiagnosticType.VALIDATIONERROR => mode.toString()
  }

}

object RemoveDafintTransformer {

  /**
   * Daffodil inserts line/col information as attributes sometimes, which IBM DFDL cannot
   * handle. This function removes those attributes
   */
  private lazy val transformer = {
    val removeDafintRule = new RewriteRule() {
      override def transform(node: Node) = node match {
        case Elem(prefix, label, attributes, scope, children @ _ *) => {
          val newAttributes = attributes.filter(!_.prefixedKey.startsWith("dafint:"))
          new Elem(prefix, label, newAttributes, scope, true, children: _*)
        }
        case other => other
      }
    }
    new RuleTransformer(removeDafintRule)
  }

  def apply(node: Node): Node = transformer(node)
}

final class TDMLDFDLProcessorFactory private (
  private var checkAllTopLevel: Boolean,
  var validateDFDLSchemas: Boolean,
  private var bindings: Seq[Binding])
  extends AbstractTDMLDFDLProcessorFactory
  with DiagnosticsMixin {

  override protected type R = TDMLDFDLProcessorFactory

  def this() = this(checkAllTopLevel = false, validateDFDLSchemas = true, bindings = Seq())

  private def copy(
    checkAllTopLevel: Boolean = checkAllTopLevel,
    validateDFDLSchemas: Boolean = validateDFDLSchemas,
    bindings: Seq[Binding] = bindings) =
    new TDMLDFDLProcessorFactory(checkAllTopLevel, validateDFDLSchemas, bindings)

  override def withValidateDFDLSchemas(bool: Boolean): TDMLDFDLProcessorFactory = {
    if (bool == false)
      System.err.println("In this test rig, IBM DFDL always validates DFDL schemas. This cannot be turned off.")
    copy(validateDFDLSchemas = bool)
  }

  override def withCheckAllTopLevel(checkAllTopLevel: Boolean): TDMLDFDLProcessorFactory =
    copy(checkAllTopLevel = checkAllTopLevel)

  override def withTunables(tunables: Map[String, String]): TDMLDFDLProcessorFactory = {
    System.err.println(tunables.map { t => "Tunable ignored: '%s'".format(t.toString()) }.mkString("\n"))
    this
  }

  private def toss(e: Throwable) = {
    val exc = e
    System.err.println("DFDL exception creating grammar: " + exc.getMessage)
    System.err.println(diagnostics.map(_.getSomeMessage.get).mkString("\n"))
    throw exc
  }

  private lazy val traceListener = new TraceListener()

  override def getProcessor(
    schemaSource: DaffodilSchemaSource,
    useSerializedProcessor: Boolean,
    optRootName: Option[String],
    optRootNamespace: Option[String],
    tunables: Map[String, String]): TDML.CompileResult = {

    val rootNamespace = optRootNamespace.getOrElse(null)

    // Construct a grammar from the DFDL schema
    val grammarErrorHandler = compileErrorHandler
    val grammarFactory = new DFDLGrammarFactory
    grammarFactory.setErrorHandler(grammarErrorHandler)
    grammarFactory.setServiceTraceListener(traceListener)
    val schemaUri: URI = schemaSource match {
      case ess: EmbeddedSchemaSource => {
        val newNode = RemoveDafintTransformer(ess.node)
        ess.copy(node = newNode).uriForLoading
      }
      case ss => schemaSource.uriForLoading
    }

    Assert.invariant(schemaSource.isInstanceOf[URISchemaSource])
    val grammar =
      try {
        //
        // Turning on logging here may be helpful to debugging resolver issues
        // where files aren't being found.
        //
        // LoggingDefaults.setLoggingLevel(LogLevel.Resolver)
        //
        val er = DFDLCatalogResolver.get // Note: we're using Daffodil Resolver
        //
        // We insist that we validate schemas, always.
        // This is supposed to be the default.
        //
        Assert.invariant {
          val state = grammarFactory.getFeature(DFDLGrammarFactory.DFDL_FEATURE_SCHEMA_VALIDATION)
          state == true
        }
        grammarFactory.buildGrammarFromSchema(schemaUri, er)
      } catch {
        case e: DFDLException => {
          toss(e)
        }
      }
    if (grammar == null || getDiagnostics.exists(_.isError)) {
      Left(getDiagnostics)
    } else {
      Right((getDiagnostics, new IBMTDMLDFDLProcessor(diagnostics, grammar, bindings, optRootName, rootNamespace)))
    }
  }

  private lazy val compileErrorHandler = new DFDLErrorHandler(IBMDFDLMode.Compile)
}

sealed trait DiagnosticsMixin {

  def implementationName: String = "ibm"

  def isError: Boolean = diagnostics.exists { _.isError }

  def getDiagnostics: Seq[Diagnostic] = diagnostics

  protected var diagnostics: Seq[IBMTDMLDiagnostic] = Seq()

  class DFDLErrorHandler(mode: IBMDFDLMode.Type) extends IDFDLProcessorErrorHandler with ErrorHandler {

    override def processingError(diagnostic: IDFDLDiagnostic): Unit = {
      diagnostics = new IBMTDMLDiagnostic(diagnostic, mode) +: diagnostics
      val msg = diagnostic.getSummary()
      shortcutError(msg)
    }

    private def implString = Some(implementationName)

    private val layerProps = Seq("layerLengthUnits", "layerLengthKind", "layerEncoding", "layerTransform")
    /**
     * If the message mentions DFDL features clearly unsupported by IBM DFDL,
     * then the test is skipped.
     */
    private def shortcutError(msg: String) {
      val hasIVC = msg.contains("inputValueCalc")
      lazy val hasOVC = msg.contains("outputValueCalc")
      lazy val hasHGR = msg.contains("hiddenGroupRef")
      if (hasIVC)
        throw new TDMLTestNotCompatibleException("Test uses dfdl:inputValueCalc", implString)
      if (hasOVC)
        throw new TDMLTestNotCompatibleException("Test uses dfdl:outputValueCalc", implString)
      if (hasHGR)
        throw new TDMLTestNotCompatibleException("Test uses dfdl:hiddenGroupRef", implString)
      layerProps.foreach { propName =>
        if (msg.contains(propName))
          throw new TDMLTestNotCompatibleException("Test uses dfdl:" + propName, implString)
      }
    }

    override def schemaDefinitionError(diagnostic: IDFDLDiagnostic): Unit = {
      diagnostics = new IBMTDMLDiagnostic(diagnostic, mode) +: diagnostics
      val msg = diagnostic.getSummary()
      shortcutError(msg)
    }

    override def validationError(diagnostic: IDFDLDiagnostic): Unit = {
      diagnostics = new IBMTDMLDiagnostic(diagnostic, mode) +: diagnostics
      val msg = diagnostic.getSummary()
      shortcutError(msg)
    }

    override def warning(diagnostic: IDFDLDiagnostic): Unit = {
      diagnostics = new IBMTDMLDiagnostic(diagnostic, mode) +: diagnostics
    }

    /*
     * These methods make this a sax ErrorHandler
     */

    override def error(spe: SAXParseException): Unit = {
      processingError(new IDFDLDiagFromThrowable(spe))
    }

    override def warning(spe: SAXParseException): Unit = {
      warning(new IDFDLDiagFromThrowable(spe))
    }

    override def fatalError(spe: SAXParseException): Unit = {
      processingError(new IDFDLDiagFromThrowable(spe))
    }
  }
}

final class IBMTDMLDFDLProcessor private (
  compilerDiags: Seq[IBMTDMLDiagnostic],
  grammar: IDFDLGrammar,
  bindings: Seq[Binding],
  optRootName: Option[String],
  rootNamespace: String,
  isTraceMode: Boolean,
  shouldValidate: Boolean)
  extends TDMLDFDLProcessor
  with DiagnosticsMixin {

  override protected type R = IBMTDMLDFDLProcessor

  def this(
    compilerDiags: Seq[IBMTDMLDiagnostic],
    grammar: IDFDLGrammar,
    bindings: Seq[Binding],
    optRootName: Option[String],
    rootNamespace: String) = this(compilerDiags, grammar, bindings, optRootName, rootNamespace, isTraceMode = false,
    shouldValidate = false)

  def copy(
    compilerDiags: Seq[IBMTDMLDiagnostic] = compilerDiags,
    grammar: IDFDLGrammar = grammar,
    bindings: Seq[Binding] = bindings,
    optRootName: Option[String] = optRootName,
    rootNamespace: String = rootNamespace,
    isTraceMode: Boolean = isTraceMode,
    shouldValidate: Boolean = shouldValidate) =
    new IBMTDMLDFDLProcessor(
      compilerDiags = compilerDiags,
      grammar = grammar,
      bindings = bindings,
      optRootName = optRootName,
      rootNamespace = rootNamespace,
      isTraceMode = isTraceMode,
      shouldValidate = shouldValidate)

  override def withDebugger(db: Object): IBMTDMLDFDLProcessor = ???
  override def withDebugging(onOff: Boolean): IBMTDMLDFDLProcessor = {
    if (onOff) ???
    this
  }

  override def withExternalDFDLVariables(externalVarBindings: Seq[Binding]): IBMTDMLDFDLProcessor =
    copy(bindings = externalVarBindings)

  override def withTracing(onOff: Boolean): IBMTDMLDFDLProcessor =
    copy(isTraceMode = onOff)

  override def withValidationMode(validationMode: ValidationMode.Type): IBMTDMLDFDLProcessor =
    copy(shouldValidate = validationMode match {
      case ValidationMode.Full => true
      case ValidationMode.Limited => true
      case ValidationMode.Off => false
      case _ => Assert.usageError("validation mode " + validationMode + " is unsupported.")
    })

  diagnostics = compilerDiags // don't lose warnings: https://github.com/OpenDFDL/ibmDFDLCrossTester/issues/4

  private lazy val traceListener = new TraceListener()

  private lazy val processorFactory = new DFDLProcessorFactory

  private val DFDL_NAMESPACE = "http://www.ogf.org/dfdl/dfdl-1.0/"

  override def parse(is: java.io.InputStream, lengthLimitInBits: Long): TDMLParseResult = {

    val parser: IDFDLParser = processorFactory.createParser
    parser.setGrammar(grammar)
    parser.setRootElement(
      optRootName.getOrElse(Assert.usageError("Must call setDistinguishedRootNode before parse.")),
      rootNamespace)

    parser.setVariable("encoding", DFDL_NAMESPACE, "ISO-8859-1")
    parser.setVariable("byteOrder", DFDL_NAMESPACE, "big-endian")

    bindings.foreach { binding =>
      val ns = binding.varQName.namespace.toStringOrNullIfNoNS
      val varName = binding.varQName.local
      val varValue = binding.varValue
      parser.setVariable(varName, ns, varValue)
    }

    val saxInput = new InputSource(is)
    val saxErrorHandler = parseErrorHandler
    val sb = new java.lang.StringBuilder
    val saxContentHandler = new XMLSAXContentHandler1(sb)
    val dfdlReader = new DFDLReader2(parser) // implements the SAX XMLReader interface but uses a DFDL parser
    dfdlReader.setContentHandler(saxContentHandler)
    dfdlReader.setErrorHandler(saxErrorHandler)
    dfdlReader.setFeature(DFDLReader.SAX_FEATURE_NAMESPACES, true)
    dfdlReader.setFeature(DFDLReader.SAX_FEATURE_VALIDATION, shouldValidate)

    if (isTraceMode) {
      // add a trace listener
      // parser.addUserTraceListener(traceListener) // don't need both user and service trace listener.
      parser.addServiceTraceListener(traceListener)
    }

    dfdlReader.parse(saxInput)

    new IBMTDMLParseResult(diagnostics, dfdlReader, sb)
  }

  override def unparse(infosetXML: scala.xml.Node, outputStream: java.io.OutputStream): TDMLUnparseResult = {

    val serializer = processorFactory.createSerializer
    serializer.setGrammar(grammar)

    serializer.setVariable("encoding", DFDL_NAMESPACE, "ISO-8859-1")
    serializer.setVariable("byteOrder", DFDL_NAMESPACE, "big-endian")

    bindings.foreach { binding =>
      val ns = binding.varQName.namespace.toStringOrNullIfNoNS
      val varName = binding.varQName.local
      val varValue = binding.varValue
      serializer.setVariable(varName, ns, varValue)
    }

    serializer.setOutputDocument(outputStream)

    serializer.setFeature(IDFDLProcessor.DFDL_FEATURE_VALIDATION, shouldValidate)

    if (isTraceMode) {
      // add a trace listener
      serializer.addUserTraceListener(traceListener)
      serializer.addServiceTraceListener(traceListener)
    }

    val errorHandler = unparseErrorHandler
    serializer.setErrorHandler(errorHandler)

    val infosetString = RemoveDafintTransformer(infosetXML).toString()
    val inputStream = new ReaderInputStream(new StringReader(infosetString), "UTF-8")

    val saxInput = new InputSource(inputStream)

    val saxContentHandler = new SAXToDFDLEventAdapter(serializer) {

      /**
       * Is called with characters where XML character entities have already been
       * converted to characters.
       *
       * However, we have special use of PUA characters to represent XML-illegal
       * code points that must also be inverted.
       *
       * E.g., U+E000 must become NUL (aka code point zero)
       */
      override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
        val origString = new String(ch, start, length)
        val withoutPUAsArray = XMLUtils.remapPUAToXMLIllegalCharacters(origString)
        super.characters(withoutPUAsArray.toCharArray, 0, withoutPUAsArray.length)
      }
    }

    val saxErrorHandler = unparseErrorHandler

    val myReader = XMLReaderFactory.createXMLReader()
    myReader.setContentHandler(saxContentHandler)
    myReader.setErrorHandler(saxErrorHandler)
    myReader.setFeature("http://xml.org/sax/features/namespaces", true)

    myReader.parse(saxInput)

    new IBMTDMLUnparseResult(diagnostics)
  }

  def unparse(parseResult: TDMLParseResult, outStream: java.io.OutputStream): TDMLUnparseResult = {
    val pr = parseResult.asInstanceOf[IBMTDMLParseResult]
    unparse(pr.getResult, outStream)
  }

  private def parseErrorHandler = new DFDLErrorHandler(IBMDFDLMode.Parse)

  private def unparseErrorHandler = new DFDLErrorHandler(IBMDFDLMode.Unparse)
}

sealed class IBMTDMLResult(diags: Seq[IBMTDMLDiagnostic]) {

  protected var diagnostics = diags

  def isProcessingError: Boolean = diagnostics.exists { _.getType() == DFDLDiagnosticType.PROCESSINGERROR }

  def isValidationError: Boolean = diagnostics.exists { _.getType() == DFDLDiagnosticType.VALIDATIONERROR }

  def getDiagnostics: Seq[Diagnostic] = diagnostics

  def addDiagnostic(diag: Diagnostic): Unit = { diagnostics = diag.asInstanceOf[IBMTDMLDiagnostic] +: diagnostics }

}

final class IBMTDMLParseResult(diags: Seq[IBMTDMLDiagnostic], dfdlReader: DFDLReader, sb: java.lang.StringBuilder)
  extends IBMTDMLResult(diags) with TDMLParseResult {

  override def getResult: Node = scala.xml.XML.loadString(sb.toString())

  override def currentLocation: DataLocation = new IBMTDMLDataLocation(dfdlReader)

}

final class IBMTDMLDataLocation(myReader: DFDLReader) extends DataLocation {
  override def bitPos1b: Long = -1 // shuts off precise length/position checking for unparsing

  override def bytePos1b: Long = -1

  override def toString() = "Unknown data location"
}

final class IBMTDMLUnparseResult(diags: Seq[IBMTDMLDiagnostic])
  extends IBMTDMLResult(diags) with TDMLUnparseResult {

  override def finalBitPos0b: Long = -1 // shuts off precise length/position checking for unparsing

  override def isScannable: Boolean = false

  override def encodingName: String = Assert.usageError("Not to be called.")

  override def bitPos0b: Long = finalBitPos0b
}
