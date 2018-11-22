/**********************************************************************
* Licensed Materials - Property of IBM
*
* (C) Copyright IBM Corp. 2012
*
* US Government Users Restricted Rights - Use, duplication, or
* disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
**********************************************************************/
package io.github.openDFDL;

/**
 * Example implementation of the DFDL IDFDLTrace interface.
 * This simple implementation prints to stdout on each call.
 */

import com.ibm.dfdl.processor.trace.IDFDLTrace;
import com.ibm.dfdl.processor.trace.IDFDLUserTraceListener;
import com.ibm.dfdl.processor.trace.IDFDLServiceTraceListener;

public class TraceListener implements IDFDLUserTraceListener, IDFDLServiceTraceListener {

	public void info(IDFDLTrace traceItem) {
		System.err.println("TraceListener : info() "+traceItem.getMessage() );
	}

	public void warning(IDFDLTrace traceItem) {
		System.err.println("TraceListener : warning() "+traceItem.getMessage() );
	}
	
	public void error(IDFDLTrace traceItem) {
		System.err.println("TraceListener : error() "+traceItem.getMessage() );
	}
	
	public void fatal(IDFDLTrace traceItem) {
		System.err.println("TraceListener : fatal() "+traceItem.getMessage() );
	}

	@Override
	public void detail(IDFDLTrace traceItem) {
		System.err.println("TraceListener : detail() "+traceItem.getMessage() );
	}

	@Override
	public void entry(IDFDLTrace traceItem) {
		// System.err.println("TraceListener : entry() "+traceItem.getMessage() );
	}

	@Override
	public void exit(IDFDLTrace traceItem) {
		// System.err.println("TraceListener : exit() "+traceItem.getMessage() );
	}


}
