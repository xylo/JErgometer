/**
 * XMLElement.
 *
 * @author Jörg Endrullis
 * @version 1.0
 */

package de.endrullis.xml;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class XMLElement{
  /** Name of the XML element. */
  private String name = "";
  /** Attributes of the element. */
  private Hashtable<String,String> attributes = new Hashtable<String,String>();
  private ArrayList<String> attributesOrdered = new ArrayList<String>();
  private Hashtable<String,String> attributesLowerCase = new Hashtable<String,String>();
  /** Child elements of the element. */
  private ArrayList<XMLElement> childs = new ArrayList<XMLElement>();
  /** Text of the element if it only contains text and no child elements. */
  private String text = "";
  /** Parent element or null if it's the root element. */
  private XMLElement parent = null;

  /**
   * Creates a new XML element.
   */
  public XMLElement(){
  }

  /**
   * Creates a new XML element.
   *
   * @param name element name
   */
  public XMLElement(String name){
    setName(name);
  }

  /**
   * Returns the attribute value of an attribute.
   *
   * @param key attribute name
   * @return attribute value
   */
  public String getAttribute(String key){
    String caseKey = attributesLowerCase.get(key.toLowerCase());
    if(caseKey != null) return attributes.get(caseKey);
    return null;
  }

  /**
   * Returns the child element with the given name/ path.
   *
   * @param names name/path of the child element
   * @return child element or null if not found
   */
  public XMLElement getChildElement(String names){
    return getChildElement(names, true);
  }

  /**
   * Returns the child element with the given name/ path.
   *
   * @param names name/path of the child element
   * @param strict true = return null if not found
   *               false = return last found element on the path
   * @return child element
   */
  public XMLElement getChildElement(String names, boolean strict){
    // split the names of child elements with the separator '.'
    StringTokenizer tokens = new StringTokenizer(names, ".");

    XMLElement element = this;
    while(tokens.hasMoreElements()){
      String name = tokens.nextToken();
      ArrayList<XMLElement> element_childs = element.getChildElements();

      // search for the child element with this name
      boolean found = false;
      for(XMLElement child : element_childs){
        if((child.getName()).equalsIgnoreCase(name)){
          element = child;
          found = true;
          break;
        }
      }
      // if the child element was not found
      if(!found){
        if(strict) return null; else return element;
      }
    }
    // the child element was found
    return element;
  }

  public ArrayList<XMLElement> getChildElements(){
    return childs;
  }

  public Enumeration getAttributeNames(){
    return attributes.keys();
  }

  public String getName(){
    return name.toLowerCase();
  }

  public XMLElement getParent(){
    return parent;
  }

  public String getText(){
    return text;
  }

  /**
   * Adds a child element.
   * 
   * @param element child element
   */
  public void addChildElement(XMLElement element){
    childs.add(element);
    element.setParent(this);
  }

  public void removeAttribute(String key){
    String caseKey = attributesLowerCase.get(key.toLowerCase());
    if(caseKey != null){
      attributes.remove(caseKey);
      attributesLowerCase.remove(key.toLowerCase());
    }
  }

  public void setAttribute(String key, String value){
    attributes.put(key, value);
    attributesLowerCase.put(key.toLowerCase(), key);
    addAttributeOrdered(key);
  }

  public void addAttributeOrdered(String key){
    // remove first if already there
    attributesOrdered.remove(key);
    attributesOrdered.add(key);
  }

  public String getAttributeOrdered(int nr){
    return attributesOrdered.get(nr);
  }

  public void setName(String name){
    this.name = name;
  }

  public void setParent(XMLElement parent){
    this.parent = parent;
  }

  public void setText(String text){
    this.text = text;
  }

  /**
   * Returns the attribute value by the given path. Null is returned if the path is invalid.
   * A path has the format element/subelement/../subsubelement@attribute
   *
   * @param path path to the attribute
   * @return value of the attribute or null if the path is invalid
   */
  public String getAttributeByPath(String path) {
    // search for '/'
    int index = path.indexOf('/');
    if(index >= 0) {
      XMLElement element = getChildElement(path.substring(0, index));
      if(element == null) {
        return null;
      } else {
        return element.getAttributeByPath(path.substring(index + 1));
      }
    }

    // search for '@'
    index = path.indexOf('@');
    if(index >= 0) {
      XMLElement element = getChildElement(path.substring(0, index));
      if(element == null) {
        return null;
      } else {
        return element.getAttribute(path.substring(index + 1));
      }
    }

    return null;
  }

  public String toString(){
    String xmlString;
    boolean selfClosing = childs.size() == 0 && text.equals("");

    // open the XML tag
    xmlString = "<" + name;
    // add attributes
    for(String key : attributesOrdered){
      if(!attributes.containsKey(key)) continue;
      xmlString += " " + key + "=\"" + attributes.get(key) + "\"";
    }
    xmlString += (selfClosing ? " />" : ">\n");

    // add child elements
    for(XMLElement child : childs) xmlString += indent(child.toString()) + "\n";
    // add the text of the element
    xmlString += text;

    // close the XML tag
    if(!selfClosing) xmlString += "</" + name + ">";

    return xmlString;
  }

  private String indent(String xml){
    StringBuffer stringBuffer = new StringBuffer();

    int index = 0;
    for(int lineBreak; (lineBreak = xml.indexOf('\n', index)) != -1; ) {
      stringBuffer.append("  ");
      stringBuffer.append(xml.substring(index, lineBreak + 1));
      index = lineBreak + 1;
    }
    stringBuffer.append("  ");
    stringBuffer.append(xml.substring(index));

    return stringBuffer.toString();
  }
}