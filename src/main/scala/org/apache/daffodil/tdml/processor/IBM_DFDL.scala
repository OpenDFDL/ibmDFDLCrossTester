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

package org.apache.daffodil.tdml.processor

import java.io.{ FileNotFoundException, InputStream, ByteArrayOutputStream }
import java.net.URI

import org.apache.daffodil.api.{ DataLocation, URISchemaSource, ValidationMode, Diagnostic, DaffodilSchemaSource }
import org.apache.daffodil.exceptions.Assert
import org.apache.daffodil.externalvars.Binding
import org.apache.daffodil.util.Maybe
import org.apache.daffodil.xml.DFDLCatalogResolver
import org.apache.daffodil.util.LoggingDefaults
import org.apache.daffodil.util.LogLevel
import com.ibm.dfdl.processor.{ IDFDLDiagnostic, IDFDLErrorHandler, IDFDLParser, IDFDLProcessorErrorHandler, DFDLProcessorFactory }
import com.ibm.dfdl.processor.types.DFDLDiagnosticType
import com.ibm.dfdl.grammar.{ IDFDLGrammar, DFDLGrammarFactory }
import com.ibm.dfdl.processor.exceptions.DFDLException
import org.xml.sax.{ XMLReader, SAXException, ErrorHandler, InputSource, SAXParseException, ContentHandler }
import com.ibm.dfdl.sample.sax.reader
import com.ibm.dfdl.sample.sax.reader.DFDLReader1 // HAS BUG FIX IN IT
import com.ibm.dfdl.sample.sax.reader.DFDLReader
import com.ibm.dfdl.sample.sax.reader.XMLSAXContentHandler

import scala.xml.Node
import org.apache.daffodil.xml.DaffodilXMLLoader
import org.xml.sax.helpers.XMLReaderFactory

import com.ibm.dfdl.sample.sax.writer.SAXToDFDLEventAdapter
import com.ibm.dfdl.processor.IDFDLSerializer
import java.io.StringReader
import org.apache.commons.io.input.ReaderInputStream
import com.ibm.dfdl.processor.IDFDLProcessor
import io.github.openDFDL.TraceListener
import org.apache.daffodil.xml.XMLUtils
import scala.xml.Utility

object IBMDFDLMode extends Enumeration {
  type Type = Value
  val Parse, Unparse, Compile, Configuration = Value
}

class IDFDLDiagFromThrowable(cause: Throwable) extends IDFDLDiagnostic {
  Assert.usage(cause ne null)

  override def getCode = cause.getClass().getName()

  override def getSummary = cause.getMessage()

  override def getCodeAndSummary = getCode() + ": " + getSummary()

  override def getSchemaLocation: URI = ???

  override def getType = DFDLDiagnosticType.PROCESSINGERROR
}

class IBMTDMLDiagnostic(iddArg: IDFDLDiagnostic, throwable: Throwable, mode: IBMDFDLMode.Type) extends Diagnostic(Maybe.Nope, Maybe.Nope,
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

class TDMLDFDLProcessorFactory()
  extends AbstractTDMLDFDLProcessorFactory
  with DiagnosticsMixin {

  override def implementationName = "ibm"

  private var checkAllTopLevel: Boolean = false

  private var validateDFDLSchemas_ : Boolean = true

  override def validateDFDLSchemas = validateDFDLSchemas_

  override def setValidateDFDLSchemas(bool: Boolean): Unit = {
    if (bool == false)
      System.err.println("In this test rig, IBM DFDL always validates DFDL schemas. This cannot be turned off.")
    this.validateDFDLSchemas_ = bool
  }

  override def setCheckAllTopLevel(checkAllTopLevel: Boolean): Unit = {
    this.checkAllTopLevel = checkAllTopLevel
  }

  override def setTunables(tunables: Map[String, String]): Unit = {
    System.err.println(tunables.map { t => "Tunable ignored: '%s'".format(t.toString()) }.mkString("\n"))
  }

  private var bindings: Seq[Binding] = Seq()

  override def setExternalDFDLVariables(externalVarBindings: Seq[Binding]): Unit = { bindings = externalVarBindings }

  private var optRootName: Option[String] = None
  private var rootNamespace: String = _

  override def setDistinguishedRootNode(name: String, namespace: String): Unit = {
    optRootName = Some(name)
    rootNamespace = namespace
  }

  private def toss(e: Throwable) = {
    val exc = e
    System.err.println("DFDL exception creating grammar: " + exc.getMessage)
    System.err.println(diagnostics.map(_.getSomeMessage.get).mkString("\n"))
    throw exc
  }

  private val traceListener = new TraceListener()

  override def getProcessor(schemaSource: DaffodilSchemaSource, useSerializedProcessor: Boolean): TDML.CompileResult = {
    // Construct a grammar from the DFDL schema
    val grammarErrorHandler = compileErrorHandler
    val grammarFactory = new DFDLGrammarFactory
    grammarFactory.setErrorHandler(grammarErrorHandler)
    grammarFactory.setServiceTraceListener(traceListener)
    val schemaUri: URI = schemaSource.uriForLoading
    Assert.invariant(schemaSource.isInstanceOf[URISchemaSource])
    val grammar =
      try {
        // LoggingDefaults.setLoggingLevel(LogLevel.Resolver)
        val er = DFDLCatalogResolver.get
        // System.err.println("Using Daffodil Entity Resolver")
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
    if (grammar == null) {
      Left(getDiagnostics)
    } else {
      Right((getDiagnostics, new IBMTDMLDFDLProcessor(diagnostics, grammar, bindings, optRootName, rootNamespace)))
    }
  }

  private lazy val compileErrorHandler = new DFDLErrorHandler(IBMDFDLMode.Compile)
}

trait DiagnosticsMixin {

  def isError: Boolean = diagnostics.exists { _.isError }

  def getDiagnostics: Seq[Diagnostic] = diagnostics

  protected var diagnostics: Seq[IBMTDMLDiagnostic] = Seq()

  class DFDLErrorHandler(mode: IBMDFDLMode.Type) extends IDFDLProcessorErrorHandler with ErrorHandler {

    override def processingError(diagnostic: IDFDLDiagnostic): Unit = {
      diagnostics = new IBMTDMLDiagnostic(diagnostic, mode) +: diagnostics
    }

    override def schemaDefinitionError(diagnostic: IDFDLDiagnostic): Unit = {
      diagnostics = new IBMTDMLDiagnostic(diagnostic, mode) +: diagnostics
    }

    override def validationError(diagnostic: IDFDLDiagnostic): Unit = {
      diagnostics = new IBMTDMLDiagnostic(diagnostic, mode) +: diagnostics
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

class IBMTDMLDFDLProcessor(
  compilerDiags: Seq[IBMTDMLDiagnostic],
  grammar: IDFDLGrammar,
  bindings: Seq[Binding],
  optRootName: Option[String],
  rootNamespace: String)
  extends TDMLDFDLProcessor
  with DiagnosticsMixin {

  private val traceListener = new TraceListener()

  override def setDebugging(onOff: Boolean): Unit = {
    if (onOff) ???
  }

  private var isTraceMode = false

  override def setTracing(onOff: Boolean): Unit = {
    isTraceMode = onOff
  }

  override def setDebugger(db: AnyRef): Unit = {
    ???
  }

  private def processorFactory = new DFDLProcessorFactory

  val DFDL_NAMESPACE = "http://www.ogf.org/dfdl/dfdl-1.0/"

  private var shouldValidate: Boolean = false

  override def setValidationMode(validationMode: ValidationMode.Type): Unit = {
    shouldValidate = validationMode match {
      case ValidationMode.Full => true
      case ValidationMode.Limited => true
      case ValidationMode.Off => false
    }
  }

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
    val saxContentHandler = new XMLSAXContentHandler(sb)
    val dfdlReader = new DFDLReader1(parser) // implements the SAX XMLReader interface but uses a DFDL parser
    dfdlReader.setContentHandler(saxContentHandler)
    dfdlReader.setErrorHandler(saxErrorHandler)
    dfdlReader.setFeature(DFDLReader.SAX_FEATURE_NAMESPACES, true)
    dfdlReader.setFeature(DFDLReader.SAX_FEATURE_VALIDATION, shouldValidate)

    if (isTraceMode) {
      // add a trace listener
      parser.addUserTraceListener(traceListener)
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

    val infosetString = infosetXML.toString()
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
      private val remapper = XMLUtils.remapPUAToXMLIllegalChar(false) _

      override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
        val withoutPUAsArray = ch.map { remapper(_) }
        super.characters(withoutPUAsArray, start, length)
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

class IBMTDMLDataLocation(myReader: DFDLReader) extends DataLocation {
  override def isAtEnd = true // shuts off left-over data checks for parsing

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
