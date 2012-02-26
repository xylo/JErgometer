/**
 * XMLParser.
 *
 * @author Jörg Endrullis
 * @version 1.0
 */

package de.endrullis.xml;

import java.util.ArrayList;

public class XMLParser{
  public XMLDocument parse(String xmlstring)
          throws XMLException{
    // replace all newlines by spaces
    xmlstring = xmlstring.replace('\n', ' ');
    xmlstring = xmlstring.replace('\r', ' ');

    // filter all comments
    int comment_start, comment_end;
    while((comment_start = xmlstring.indexOf("<!--")) != -1){
      comment_end = xmlstring.indexOf("-->", comment_start);

      if(comment_end == -1){
        // comment goes to the end of the file
        xmlstring = xmlstring.substring(0, comment_start);
        break;
      } else{
        // cut off the comment
        xmlstring = xmlstring.substring(0, comment_start) + xmlstring.substring(comment_end + 3);
      }
    }

    // remove DOCTYPE definition
    int doctype_start = xmlstring.indexOf("<!DOCTYPE");
    if(doctype_start != -1){
      int bracket_open = xmlstring.indexOf('[', doctype_start);
      int bracket_close = xmlstring.indexOf(']', doctype_start);

      int doctype_end = doctype_start;
      if(bracket_open != -1 && bracket_close != -1 && bracket_open < xmlstring.indexOf('>', doctype_start)){
        doctype_end = bracket_close;
      }
      doctype_end = xmlstring.indexOf('>', doctype_end);

      xmlstring = xmlstring.substring(0, doctype_start) + xmlstring.substring(doctype_end + 1);
    }

    // parse the document for XML elements
    XMLElement raw_document = new XMLElement();
    parseXMLString(raw_document, xmlstring);

    XMLDocument document = new XMLDocument();
    // does the string contain an XML type definition?
    ArrayList<XMLElement> childs = raw_document.getChildElements();
    for(int child_nr = 0; child_nr < childs.size(); child_nr++){
      XMLElement element = childs.get(child_nr);
      String name = element.getName();

      if(name.startsWith("?")){
        // is this an XML type definition
        if(name.equals("?xml")){
          // read version and encoding
          if(element.getAttribute("version") != null){
            document.setXMLVerion(element.getAttribute("version"));
          }
          if(element.getAttribute("encoding") != null){
            document.setXMLEncoding(element.getAttribute("encoding"));
          }
        }
      } else{
        // more than one root element? -> would be a fault
        if(child_nr < childs.size() - 1) throw new XMLException("more than one root element");
        // set the root element of the document
        document.setRootElement(element);
      }
    }

    return document;
  }

  private String parseXMLString(XMLElement parent, String xmlstring)
          throws XMLException{
    int search_start;
    int tag_start;
    int tag_ende = -1;
    XMLElement aParent;
    ArrayList<XMLElement> theParents = new ArrayList<XMLElement>();

    aParent = parent;
    theParents.add(aParent);
    while(++tag_ende < xmlstring.length()){

      // trim the string from the left
      search_start = tag_ende;
      while(xmlstring.charAt(search_start) <= '\u0020'){
        if(++search_start >= xmlstring.length()) return "";
      }

      // search for the next tag in the string
      tag_start = xmlstring.indexOf("<", search_start);
      tag_ende = xmlstring.indexOf(">", tag_start);
      if(tag_start == -1 || tag_start > tag_ende){
        throw new XMLException("falsche Syntax in XMLDaten");
      }

      String start_string = xmlstring.substring(search_start, tag_start).trim();
      String tag_string = xmlstring.substring(tag_start + 1, tag_ende).trim();

      // is this an empty / selfclosing tag?
      boolean isEmptyTag = false;
      if(tag_string.endsWith("/") || tag_string.endsWith("?")){
        isEmptyTag = true;
        tag_string = tag_string.substring(0, tag_string.length() - 1);
      }

      // create an XML element of this string
      XMLElement element = parseTagString(tag_string);

      // is this a opening or a closing tag
      if(element.getName().startsWith("/")){
        // we do not allow child tags if the element contains text
        if(!(start_string.equals(""))){
          if(aParent.getChildElements().size() != 0){
            throw new XMLException("element must not contain text: " + start_string);
          } else{
            // element contains only text
            aParent.setText(start_string);
          }
        }

        // closing tag, the tag name must be identical with the parent element
        if(!aParent.getName().equalsIgnoreCase(element.getName().substring(1).trim())){
          throw new XMLException("wrong closing tag: " + element.getName());
        }

        // last parent has been processed, go one level back
        if(theParents.size() > 1){
          theParents.remove(theParents.size() - 1);
          aParent = theParents.get(theParents.size() - 1);
        }
        continue;

      } else{
        // we do not allow child tags if the element contains text
        if(!(start_string.equals(""))){
          throw new XMLException("element must not contain text: " + start_string);
        }

        // add the element as child element
        aParent.addChildElement(element);
      }

      // search for child elements
      if(!isEmptyTag){
        aParent = element;
        theParents.add(aParent);
      }

    }

    return "";
  }


  private XMLElement parseTagString(String tag_string)
          throws XMLException{
    // trim
    tag_string = tag_string.trim();

    // get the tag name
    int index = tag_string.indexOf(" ");
    if(index == -1) return new XMLElement(tag_string);

    // create the XML element
    XMLElement element = new XMLElement(tag_string.substring(0, index));

    // parse the attributes
    tag_string = tag_string.substring(index);
    while(!(tag_string = tag_string.trim()).equals("")){
      index = tag_string.indexOf("=");
      // if there is no '=', the string must be empty
      if(index == -1) throw new XMLException("wrong attribute definition");

      // attribute name before '='
      String attribute_name = tag_string.substring(0, index).trim();
      tag_string = tag_string.substring(index + 1).trim();

      // attribute value after '='
      int index_start_dquote = tag_string.indexOf("\"");
      if(index_start_dquote == -1) index_start_dquote = Integer.MAX_VALUE;
      int index_start_quote = tag_string.indexOf("\'");
      if(index_start_quote == -1) index_start_quote = Integer.MAX_VALUE;

      int index_start = Math.min(index_start_dquote, index_start_quote);
      if(index_start == Integer.MAX_VALUE){
        throw new XMLException("attribute value must be surrounded by quotes: " + attribute_name);
      }
      int index_ende = tag_string.indexOf(tag_string.charAt(index_start), index_start+1);

      // next one
      int index_nexttag = tag_string.indexOf("=", index_ende);
      if(index_nexttag == -1) index_nexttag = Integer.MAX_VALUE;

      index_ende = tag_string.lastIndexOf(tag_string.charAt(index_start), index_nexttag);
      if(index_ende <= index_start){
        throw new XMLException("attribute value must be surrounded by quotes: " + attribute_name);
      }
      if(!(tag_string.substring(0, index_start).trim().equals(""))){
        throw new XMLException("attribute value must be surrounded by quotes: " + attribute_name);
      }

      String attribute_value = tag_string.substring(index_start + 1, index_ende);
      // proceed with the rest of the string
      tag_string = tag_string.substring(index_ende + 1);

      // add the attribute name-value pair
      element.setAttribute(attribute_name, attribute_value);
    }

    return element;
  }
}
