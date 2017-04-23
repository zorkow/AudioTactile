// Copyright 2015 Volker Sorge
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @file   FileHandler.java
 * @author Volker Sorge
 *         <a href="mailto:V.Sorge@progressiveaccess.com">Volker Sorge</a>
 * @date   Thu Feb 26 19:06:25 2015
 *
 * @brief  File handler utility functions.
 *
 *
 */

//

package com.progressiveaccess.audiotactile;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.svg.SVGDocument;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;

/**
 * Utility class for handling CML files and other chem file formats.
 */

public final class FileHandler {

  /** Dummy constructor. */
  private FileHandler() {
    throw new AssertionError("Instantiating utility class...");
  }


  /**
   * Loads current file into the molecule IAtomContainer.
   *
   * @param fileName
   *          File to load.
   *
   * @return The molecule loaded.
   *          
   * @throws IOException
   *           Problems with loading file.
   * @throws CDKException
   *           Problems with input file format.
   *
   */
  public static Document loadXml(final String fileName)
      throws IOException, ParserConfigurationException, SAXException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    final DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(new File(fileName));
    return doc;
  }


  /**
   * Loads an SVG image from file.
   *
   * @param fileName
   *          The SVG file to be loaded.
   *
   * @return The SVG document.
   *
   * @throws IOException
   *           Problems with StringWriter
   */
  public static SVGDocument loadSvg(final String fileName) throws IOException {
    final File file = new File(fileName);
    final URI uri = file.toURI();
    String parser = XMLResourceDescriptor.getXMLParserClassName();
    SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
    SVGDocument doc = factory.createSVGDocument(uri.toString());
    return doc;
  }


  /**
   *
   * @param document
   *          The output document.
   * @param path
   *          The output path.
   * @return True if successful. O/w false.
   */
  private static Boolean toFile(final Document document, final String file) {
    return FileHandler.toStream(document, new StreamResult(new File(file)));
  }

  /**
   *
   * @param document
   *          The output document.
   * 
   * @return The XML document as a string.
   */
  public static String toString(final Document document) {
    final StreamResult result = new StreamResult(new StringWriter());
    return FileHandler.toStream(document, result) ?
      result.getWriter().toString() : "";
  }

  public static String toString(final Element document) {
    final StreamResult result = new StreamResult(new StringWriter());
    return FileHandler.toStream(document, result) ?
      result.getWriter().toString() : "";
  }

  private static Boolean toStream(final Document document,
                                  final StreamResult stream) {
    //
    // prepare the transformer
    final DOMSource domSource = new DOMSource(document);
    final TransformerFactory transformerFactory = TransformerFactory
        .newInstance();
    try {
      final Transformer transformer = transformerFactory.newTransformer();
      // set the output configuration
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
      transformer.setOutputProperty(
          "{http://xml.apache.org/xslt}indent-amount", "2");
      // write the XML to the file
      transformer.transform(domSource, stream);

      return true;
    } catch (final TransformerConfigurationException ex) {
      Logger.error("Can't configure XML transformation: " + ex.getMessage() + "\n");
    } catch (final TransformerException ex) {
      Logger.error("Can't transform XML document: " + ex.getMessage() + "\n");
    }
    return false;
  }
  
  private static Boolean toStream(final Element document,
                                  final StreamResult stream) {
    //
    // prepare the transformer
    final DOMSource domSource = new DOMSource(document);
    final TransformerFactory transformerFactory = TransformerFactory
        .newInstance();
    try {
      final Transformer transformer = transformerFactory.newTransformer();
      // set the output configuration
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
      transformer.setOutputProperty(
          "{http://xml.apache.org/xslt}indent-amount", "2");
      // write the XML to the file
      transformer.transform(domSource, stream);

      return true;
    } catch (final TransformerConfigurationException ex) {
      Logger.error("Can't configure XML transformation: " + ex.getMessage() + "\n");
    } catch (final TransformerException ex) {
      Logger.error("Can't transform XML document: " + ex.getMessage() + "\n");
    }
    return false;
  }
  
  /**
   * Writes a document to a XML type file.
   *
   * @param doc
   *          The output document.
   * @param fileName
   *          The base filename.
   */
  public static void writeXml(final Document doc, final String fileName) {
    FileHandler.toFile(doc, fileName);
  }

  /**
   * Writes a document to a SVG file.
   *
   * @param svg
   *          The SVG document.
   * @param fileName
   *          The base filename.
   */
  public static void writeSvg(final Document svg, final String fileName) {
    FileHandler.toFile(svg, fileName);
  }


  /**
   * Writes a document to standard output.
   *
   * @param doc
   *          The output document.
   * @param fileName
   *          The base filename.
   */
  public static void printXml(final Document doc) {
    System.out.println(FileHandler.toString(doc));
  }

  public static void printXml(final Element doc) {
    System.out.println(FileHandler.toString(doc));
  }

}
