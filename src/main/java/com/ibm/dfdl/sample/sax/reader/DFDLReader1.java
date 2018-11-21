/**********************************************************************
* Licensed Materials - Property of IBM
*
* (C) Copyright IBM Corp. 2014
*
* US Government Users Restricted Rights - Use, duplication, or
* disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
**********************************************************************/

/*
 * A tiny change to this file. (Plus this comment.)
 * 
 * Constructor name is DFDLReader1, inherits from DFDLReader.
 * Obviates import of XMLReader
 * 
 * new DFDLToSAXEventAdapter changed to new DFDLToSAXEventAdapter1 which contains a bug fix.
 */

package com.ibm.dfdl.sample.sax.reader;

import java.io.IOException;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;

import com.ibm.dfdl.processor.IDFDLParser;
import com.ibm.dfdl.processor.IDFDLProcessor;
import com.ibm.dfdl.processor.exceptions.DFDLException;
import com.ibm.dfdl.processor.exceptions.DFDLNotRecognizedException;

/**
 * Implements the SAX XMLReader interface, so that non-XML DFDL-described data may
 * be parsed as if it was XML. The class implements all XMLReader methods.
 *
 * Provides a DFDL document handler (DFDLToSAXEventAdapter) which translates the DFDL infoset 
 * events to equivalent SAX events and calls a user-provided SAX content handler.
 * Provides a DFDL error handler (DFDLToSAXErrorAdapter) which translates any DFDL errors to 
 * equivalent SAX errors and calls a user-provided SAX error handler. 
 * Invokes the provided DFDL parser using parseAll() to parse the non-XML data.
 * 
 * Restrictions:
 * - Only supports SAX pre-defined features 'namespaces' and 'validation'
 * - Ignores any SAX DTD or entity resolvers that are set as they are not used
 * - Only java.io.InputStream allowed as input file
 * - No SAX pre-defined properties are supported as they are not relevant
 */
public class DFDLReader1 extends DFDLReader {

	public static final String SAX_FEATURE = "http://xml.org/sax/features/";
	public static final String SAX_PROPERTY = "http://xml.org/sax/properties/";

	// SAX defined feature ids that are supported
	// Note that SAX validation feature maps to DFDL runtime validation feature
	public static final String SAX_FEATURE_NAMESPACES = "http://xml.org/sax/features/namespaces";
	public static final String SAX_FEATURE_VALIDATION = "http://xml.org/sax/features/validation";

	// User specified SAX handlers
	private ContentHandler saxContentHandler = null;
	private ErrorHandler saxErrorHandler = null;
	private DTDHandler saxDtdHandler = null;  // not used
	private EntityResolver saxEntityResolver = null; // not used
	
	// Provided DFDL classes and handlers
	private IDFDLParser parser = null;
	private DFDLToSAXEventAdapter documentHandler = null;
	private DFDLToSAXErrorAdapter errorHandler = null;

	/** 
	 * DFDLReader constructor
	 * Creates DFDL classes up front so features and handlers can be set on them directly
	 */
	public DFDLReader1(IDFDLParser parser) {
		super(parser); // ignored, but lets us inherit from the type which is convenient
		this.parser = parser;
		errorHandler = new DFDLToSAXErrorAdapter();
		documentHandler = new DFDLToSAXEventAdapter1();
		this.parser.setDocumentHandler(documentHandler);
		this.parser.setErrorHandler(errorHandler);
	}
	
	/** 
	 * Get SAX content handler 
	 */
	@Override
	public ContentHandler getContentHandler() {
		return saxContentHandler;
	}

	/** 
	 * Get SAX DTD handler (not used by DFDL) 
	 */
	@Override
	public DTDHandler getDTDHandler() {
		return saxDtdHandler;
	}

	/** 
	 * Get SAX entity resolver (not used by DFDL)
	 * Note - this is not the same as DFDL's schema entity resolver
	 */
	@Override
	public EntityResolver getEntityResolver() {
		return saxEntityResolver;
	}

	/** 
	 * Get SAX error handler 
	 */
	@Override
	public ErrorHandler getErrorHandler() {
		return saxErrorHandler;
	}

	/** 
	 * Get SAX or DFDL feature setting 
	 */
	@Override
	public boolean getFeature(String featureId) throws SAXNotRecognizedException, SAXNotSupportedException {
		try {
			if (featureId.startsWith(SAX_FEATURE)) {
				if (featureId.equals(SAX_FEATURE_NAMESPACES))
					return documentHandler.getSaxNamespacesFeature();
				else if (featureId.equals(SAX_FEATURE_VALIDATION))
					return parser.getFeature(IDFDLProcessor.DFDL_FEATURE_VALIDATION);
				else 
					throw new SAXNotSupportedException("Unsupported SAX feature: " + featureId);
			}
			else 
				throw new SAXNotRecognizedException("Unrecognized feature: " + featureId);
		}
		catch (DFDLNotRecognizedException e) {
			throw new SAXNotRecognizedException("Unrecognized feature: " + featureId);
		}
	}

	/** 
	 * Get SAX or DFDL property value
	 */
	@Override
	public Object getProperty(String propertyId) throws SAXNotRecognizedException, SAXNotSupportedException {
		if (propertyId.startsWith(SAX_PROPERTY)) {
			throw new SAXNotSupportedException("Unsupported SAX property: " + propertyId);
		}
		else 
			throw new SAXNotRecognizedException("Unrecognized property: " + propertyId);
	}

	/** 
	 * Set SAX content handler
	 * Pass through to the provided DFDL document handler and set
	 * the DFDL document handler on the DFDL parser. 
	 */
	@Override
	public void setContentHandler(ContentHandler handler) {
		documentHandler.setSaxContentHandler(handler);
	}

	/** 
	 * Set SAX DTD handler (not used by DFDL) 
	 */
	@Override
	public void setDTDHandler(DTDHandler handler) {
		saxDtdHandler = handler;
	}

	/** 
	 * Set SAX entity resolver (not used by DFDL)
	 * Note - this is not the same as DFDL's schema entity resolver
	 */
	@Override
	public void setEntityResolver(EntityResolver handler) {
		saxEntityResolver = handler;
	}

	/** 
	 * Set SAX error handler 
	 * Pass through to the provided DFDL error handler and set
	 * the DFDL error handler on the DFDL parser. 
	 */
	@Override
	public void setErrorHandler(ErrorHandler handler) {
		errorHandler.setSaxErrorHandler(handler);
	}

	/** 
	 * Set SAX or DFDL feature setting 
	 */
	@Override
	public void setFeature(String featureId, boolean enable) throws SAXNotRecognizedException, SAXNotSupportedException {
		try {
			if (featureId.startsWith(SAX_FEATURE)) {
				if (featureId.equals(SAX_FEATURE_NAMESPACES))
					documentHandler.setSaxNamespacesFeatures(enable);
				else if (featureId.equals(SAX_FEATURE_VALIDATION))
					parser.setFeature(IDFDLProcessor.DFDL_FEATURE_VALIDATION, enable);
				else
					throw new SAXNotSupportedException("Unsupported SAX feature: " + featureId);
			}
			else 
				throw new SAXNotRecognizedException("Unrecognized feature: " + featureId);
		}
		catch (DFDLNotRecognizedException e) {
			throw new SAXNotRecognizedException("Unrecognized feature: " + featureId);
		}
	}

	/** 
	 * Set SAX or DFDL property value 
	 */
	@Override
	public void setProperty(String propertyId, Object propertyValue) throws SAXNotRecognizedException, SAXNotSupportedException {
		if (propertyId.startsWith(SAX_PROPERTY)) {
			throw new SAXNotSupportedException("Unsupported SAX property: " + propertyId);
		}
		else
			throw new SAXNotRecognizedException("Unrecognized property: " + propertyId);
	}
		
	/** 
	 * Parse input document described by a DFDL schema using DFDL parser 
	 */
	@Override
	public void parse(String arg0) throws IOException, SAXException {
		throw new SAXException("Only input streams allowed");	
	}

	/** 
	 * Parse input document described by a DFDL schema using DFDL parser.
	 * Only java.io.InputStream supported. 
	 */
	@Override
	public void parse(InputSource input) throws IOException, SAXException {

		// Check input source is an input stream
		if (input.getByteStream() == null)
			throw new SAXException("No input stream supplied");

		// Pass the input stream to the DFDL parser
		try {
			parser.setInputDocument(input.getByteStream(), false);
		} catch (DFDLException e) {
			throw new SAXException("DFDL exception setting input document: " + e.getMessage());
		}

		// Invoke parser using parseAll()
		try {
			if (!parser.parseAll())
				throw new SAXException("DFDL parsing failed");
		} catch (DFDLException e) {
            SAXParseException exception = new SAXParseException(input.getSystemId(), null, e);
			saxErrorHandler.fatalError(exception);
			throw exception;
		} 
	}
}
