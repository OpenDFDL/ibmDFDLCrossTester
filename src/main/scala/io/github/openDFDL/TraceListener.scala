package io.github.openDFDL
import com.ibm.dfdl.processor.trace.IDFDLTrace
import com.ibm.dfdl.processor.trace.IDFDLUserTraceListener
import com.ibm.dfdl.processor.trace.IDFDLServiceTraceListener

class TraceListener extends IDFDLUserTraceListener with IDFDLServiceTraceListener {

  private def msg(kind: String, traceItem: IDFDLTrace): Unit =
    System.err.format("TraceListener: %s: %s\n", kind, traceItem.getMessage())

  def info(traceItem: IDFDLTrace): Unit = msg("info", traceItem)
  def warning(traceItem: IDFDLTrace): Unit = msg("warning", traceItem)
  def error(traceItem: IDFDLTrace): Unit = msg("error", traceItem)
  def fatal(traceItem: IDFDLTrace): Unit = msg("fatal", traceItem)
  def detail(traceItem: IDFDLTrace): Unit = msg("detail", traceItem)

  def entry(traceItem: IDFDLTrace) = {}
  def exit(traceItem: IDFDLTrace) = {}
}
