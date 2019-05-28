package io.github.openDFDL

import com.ibm.dfdl.processor.IDFDLParser
import org.xml.sax.ContentHandler
import org.xml.sax.ErrorHandler
import org.xml.sax.EntityResolver
import org.xml.sax.DTDHandler
import com.ibm.dfdl.processor.exceptions.DFDLNotRecognizedException
import com.ibm.dfdl.sample.sax.reader.DFDLReader;
import com.ibm.dfdl.sample.sax.reader.DFDLToSAXErrorAdapter;
import com.ibm.dfdl.sample.sax.reader.DFDLToSAXEventAdapter1;
import com.ibm.dfdl.processor.IDFDLProcessor
import org.xml.sax.SAXNotRecognizedException
import org.xml.sax.SAXNotSupportedException
import org.xml.sax.SAXException
import com.ibm.dfdl.processor.exceptions.DFDLException
import org.xml.sax.SAXParseException
import org.xml.sax.InputSource
import com.ibm.dfdl.sample.sax.reader.DFDLToSAXEventAdapter

/**
 * We re-implement this class all so that it can construct a
 * documentHandler that has the bug fix in it.
 */
class DFDLReader2(val parser: IDFDLParser) extends DFDLReader(parser) {

  import DFDLReader._

  private var saxContentHandler: ContentHandler = null
  private var saxErrorHandler: ErrorHandler = null
  private var saxDtdHandler: DTDHandler = null
  private var saxEntityResolver: EntityResolver = null
  private var documentHandler: DFDLToSAXEventAdapter = new DFDLToSAXEventAdapter1() // BUG FIX
  private var errorHandler: DFDLToSAXErrorAdapter = new DFDLToSAXErrorAdapter()
  parser.setDocumentHandler(documentHandler)
  parser.setErrorHandler(errorHandler)

  override def getContentHandler() = saxContentHandler
  override def getDTDHandler() = saxDtdHandler
  override def getEntityResolver() = saxEntityResolver
  override def getErrorHandler() = saxErrorHandler;
  override def getProperty(propertyId: String) = throw new SAXException("No properties to get.")
  override def setContentHandler(handler: ContentHandler) = documentHandler.setSaxContentHandler(handler)
  override def setDTDHandler(handler: DTDHandler) = saxDtdHandler = handler
  override def setEntityResolver(handler: EntityResolver) = saxEntityResolver = handler
  override def setErrorHandler(handler: ErrorHandler) = errorHandler.setSaxErrorHandler(handler)
  override def setProperty(propertyId: String, propertyValue: AnyRef) = throw new SAXException("No properties can be set.")
  override def parse(arg0: String) = throw new SAXException("Only input streams allowed")

  override def getFeature(featureId: String) = {
    if (featureId.equals(SAX_FEATURE_NAMESPACES))
      documentHandler.getSaxNamespacesFeature();
    else if (featureId.equals(SAX_FEATURE_VALIDATION))
      parser.getFeature(IDFDLProcessor.DFDL_FEATURE_VALIDATION)
    else
      throw new SAXNotRecognizedException("Unrecognized feature: " + featureId)
  }

  override def setFeature(featureId: String, enable: Boolean) = {
    if (featureId.equals(SAX_FEATURE_NAMESPACES))
      documentHandler.setSaxNamespacesFeatures(enable)
    else if (featureId.equals(SAX_FEATURE_VALIDATION))
      parser.setFeature(IDFDLProcessor.DFDL_FEATURE_VALIDATION, enable)
    else
      throw new SAXNotRecognizedException("Unrecognized feature: " + featureId)
  }

  override def parse(input: InputSource): Unit = {
    parser.setInputDocument(input.getByteStream(), false)
    try {
      parser.parseAll()
    } catch {
      case e: DFDLException => {
        val exception = new SAXParseException(input.getSystemId(), null, e)
        saxErrorHandler.fatalError(exception)
      }
    }
  }
}
