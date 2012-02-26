/**
 * XMLDocument.
 *
 * @author Jörg Endrullis
 * @version 1.0
 */

package de.endrullis.xml;

public class XMLDocument{
  /** Default values for version and encoding. */
  private String xmlVersion = "1.0";
  private String xmlEncoding = "UTF-8";

  /** The root element of the document. */
  private XMLElement rootElement = null;

  /**
   * Returns the root element of the document.
   * 
   * @return root element of the document
   */
  public XMLElement getRootElement(){
    return rootElement;
  }

  /**
   * Returns the encoding of the document.
   * 
   * @return encoding of the document
   */
  public String getXMLEncoding(){
    return xmlEncoding;
  }

  /**
   * Returns the XML version of the document.
   *
   * @return XML version of the document
   */
  public String getXMLVersion(){
    return xmlVersion;
  }

  /**
   * Sets the root element of the document.
   *
   * @param element root element of the document
   */
  public void setRootElement(XMLElement element){
    rootElement = element;
  }

  /**
   * Sets the encoding of the document.
   *
   * @param encoding encoding of the document
   */
  public void setXMLEncoding(String encoding){
    xmlEncoding = encoding;
  }

  /**
   * Sets the version of the document.
   *
   * @param version version of the document
   */
  public void setXMLVerion(String version){
    xmlVersion = version;
  }

  /**
   * Returns the complete XML document as string.
   *
   * @return complete XML document as string
   */
  public String toString(){
    String xmlString = "";

    // add version and encoding
    xmlString = "<?xml version=\"" + xmlVersion + "\" encoding=\"" + xmlEncoding + "\"?>\n\n";
    // add body of the document
    xmlString += rootElement.toString();

    return xmlString;
  }
}