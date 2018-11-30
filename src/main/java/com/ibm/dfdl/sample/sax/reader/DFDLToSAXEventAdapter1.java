
package com.ibm.dfdl.sample.sax.reader;

import org.apache.daffodil.xml.XMLUtils;

import com.ibm.dfdl.processor.exceptions.DFDLUserException;
import com.ibm.dfdl.processor.types.DFDLSchemaType;
import scala.collection.mutable.StringBuilder;

/** 
 * DFDL document handler to receive DFDL infoset events emitted by the IBM DFDL parser. 
 * The DFDL events are converted to SAX events and passed to a user-provided SAX content handler.
 * For use with DFDLReader implementation of XMLReader interface.
*/
public class DFDLToSAXEventAdapter1 extends DFDLToSAXEventAdapter {

	
	/** 
	 * Fix bug where XML chars are not escaped.
	 * 
	 * This will be BROKEN if DFDLToSAXEventAdapter gets fixed. Alas they didn't make the 
	 * underlying elementValue(String) method protected. So we can't call that here, which 
	 * means we're depending on super.elementValue(String, DFDLSchemaType) NOT doing the escaping again.
	 * 
	 * I.e., we're counting on the bug.
	 * 
	 * This does give us an opportunity to escape the characters using Daffodil's escaper,
	 * which does more than the usual XML escaping. It remaps all XML-illegal characters into the 
	 * private use area. So, for example if the characters contain a NUL, that will be remapped to &E000;
	 */
	@Override
	public void elementValue(String value, DFDLSchemaType baseSchemaType) throws DFDLUserException {
		StringBuilder sb = new StringBuilder();
		String escapedValue = XMLUtils.escape(value, sb).toString();
		super.elementValue(escapedValue, baseSchemaType);
	}
	
}
