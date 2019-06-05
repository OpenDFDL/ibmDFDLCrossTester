package io.github.openDFDL

import com.ibm.dfdl.sample.sax.reader.XMLSAXContentHandler
import java.net.URI
import java.util.Stack
import org.xml.sax.Attributes
import javax.xml.XMLConstants
import org.xml.sax.SAXException

/**
 * Use a namespace stack, to fix broken namespace prefix handling
 * of XMLSAXContentHandler
 */
class XMLSAXContentHandler1(sb: java.lang.StringBuilder)
  extends XMLSAXContentHandler(sb) {

  private val nsStack = new Stack[String]()
  nsStack.push("")

  // Keep track of indentation
  private var indent = ""
  private def SPACES = "    "
  private trait Event
  private object Event {
    case object START extends Event
    case object END extends Event
    case object CHARS extends Event
    case object NONE extends Event
  }
  private var lastEvent: Event = Event.NONE;

  private val nilURI = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI
  private val nilString = " xsi:nil=\"true\" xmlns:xsi=\"" + nilURI + "\""
  /**
   * Start of Element
   *
   * Note: Does not deal with the contents, just the starting tag.
   */
  override def startElement(uri: String, localName: String, qName: String, attrs: Attributes): Unit = {
    if (lastEvent == Event.START) {
      sb.append("\n")
      indent = indent + SPACES
    }
    sb.append(indent)
    sb.append('<')
    sb.append(localName)
    // Add default namespace declaration if namespace differs from
    // that of the enclosing parent
    if (!nsStack.peek().equals(uri)) {
      sb.append(" xmlns=\"")
      // The namespace may contain special characters.
      val encURI = uri // java.net.URLEncoder.encode(uri, "UTF-8")
      sb.append(encURI)
      sb.append('"')
    }
    nsStack.push(uri) // always push, so we can unconditionally pop in endElement.

    // Only currently supported attribute is xsi:nil as that is all DFDL parsed data will create
    if (attrs != null) {
      var i: Int = 0
      val len = attrs.getLength()
      while (i < len) {
        val attrValue = attrs.getValue(i);
        val attrName = attrs.getLocalName(i);
        val attrUri = attrs.getURI(i);
        if (!attrName.equals("nil") || !attrUri.equals(nilURI)) {
          throw new SAXException("Unsupported attribute: {" + attrUri + "}" + attrName);
        } else {
          if (attrValue.equals("true")) {
            sb.append(nilString);
          } else if (!attrValue.equals("false")) {
            throw new SAXException("Invalid xsi:nil value '" + attrValue + "'");
          }
        }
        i += 1
      }
    }
    sb.append('>')
    lastEvent = Event.START
  }

  override def endElement(uri: String, localName: String, qName: String): Unit = {
    if (lastEvent == Event.END) {
      indent = indent.drop(SPACES.length())
      sb.append(indent)
    }
    sb.append("</")
    sb.append(localName)
    sb.append(">\n")
    lastEvent = Event.END
    nsStack.pop()
  }

}
