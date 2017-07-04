// Copyright 2017 Volker Sorge
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


/**
 * @file Tactile.java
 * @author Volker Sorge <a href="mailto:V.Sorge@progressiveaccess.com">Volker
 *         Sorge</a>
 * @date Thu Jun 23 05:47:46 2016
 *
 * @brief Utility functions for generation of audio tactile SVG.
 *
 *
 */

//
// General remark: We have to avoid SVG paths!
//

package com.progressiveaccess.audiotactile;

import com.google.common.base.Joiner;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGLineElement;
import org.w3c.dom.svg.SVGPoint;
import org.w3c.dom.svg.SVGPointList;
import org.w3c.dom.svg.SVGPolygonElement;
import org.w3c.dom.svg.SVGPolylineElement;
import org.w3c.dom.svg.SVGRectElement;
import org.w3c.dom.svg.SVGSVGElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Point2d;
import javax.xml.XMLConstants;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;


/**
 * Class for generation of audio tactile SVG to work with IVEO.
 */

public final class Tactile {

  private static String daisyUri = "http://www.daisy.org/z3986/2005/";
  private static String iveoUri = "http://viewplus.com/iveo";
  private static String sreUri = "http://www.chemaccess.org/sre-schema";
  private static String svgUri = "http://www.w3.org/2000/svg";
  private static final String SRE_PREFIX = "sre:";
  private static final String TITLE_ATTR = "speech";
  private static final String DESCR_ATTR = "speech2";
  private static final String MSG_ATTR = "msg";

  private SVGDocument svg = null;
  private Document xml = null;
  private SVGSVGElement root = null;
  private String uri = null;
  private boolean useSpeechAttr = false;
  private final List<Element> annotations = new ArrayList<>();
  private final Map<String, String> messages = new HashMap<>();
  private final XPath xpath = XPathFactory.newInstance().newXPath();

  /**
   * Constructor.
   *
   * @param svgFile
   *          The SVG file.
   * @param xmlFile
   *          The XML annotation file.
   */
  public Tactile(final String svgFile, final String xmlFile) {
    try {
      this.svg = FileHandler.loadSvg(svgFile);
    } catch (final Exception e) {
      Logger.error("Can't load SVG file " + svgFile + "\n");
      return;
    }
    try {
      this.xml = FileHandler.loadXml(xmlFile);
    } catch (final Exception e) {
      Logger.error("Can't load XML file " + xmlFile + "\n");
      return;
    }
    this.annotations();
    this.messages();
    this.enrich();

    // cherden, add all missing <title> and <desc> nodes to tactile elements
    this.addTitlesAndDesc();

    // Output
    if (Cli.hasOption("o")) {
      this.writeSvg(Cli.getOptionValue("o"));
    } else {
      FileHandler.printXml(this.svg);
    }
  }

  /**
   * Initialises the list of annotation elements.
   */
  public void annotations() {
    final NodeList annotations = this.xml
        .getElementsByTagName("sre:annotation");
    for (Integer i = 0; i < annotations.getLength(); i++) {
      final Element item = (Element) annotations.item(i);
      Logger.logging(FileHandler.toString(item));
      this.annotations.add(item);
    }
  }

  /**
   * Initialises the mapping of messages for the given language. Default is
   * English.
   */
  public void messages() {
    NodeList messages = null;
    final boolean hasLang = Cli.hasOption("language");
    String lang = "en";
    if (hasLang) {
      lang = Cli.getOptionValue("language");
      messages = this.getMessages(lang);
    }
    if (messages == null || messages.getLength() == 0) {
      if (hasLang) {
        Logger.error("Language " + lang + " does not exist. " +
            "Attempting English as default.");
      }
      messages = this.getMessages("en");
    }
    if (messages == null || messages.getLength() == 0) {
      Logger.error("No localisation found. Using attribute values directly.");
      this.useSpeechAttr = true;
      return;
    }
    for (Integer i = 0; i < messages.getLength(); i++) {
      final Element item = (Element) messages.item(i);
      Logger.logging(FileHandler.toString(item));
      this.messages.put(this.getSreAttributeValue(item, MSG_ATTR),
          item.getTextContent());
    }
  }

  /**
   * Retrievs the message element in the annotations.
   *
   * @param language
   *          A ISO indicator for the language.
   *
   * @return The message element, if it exists. O/w null.
   */
  private NodeList getMessages(final String language) {
    NodeList messages = null;
    try {
      final XPathExpression expr = this.xpath.compile(// 1. Pick all message
                                                      // elements.
          "//*[local-name()='messages']/" +
          // 2. Get the contained language element.
              "*[local-name()='language'" +
              // 3. Check if it is the language we want.
              " and text()='" + language + "']/" +
              // 4. Then backup and take all message elements.
              "../*[local-name()='message']");
      messages = (NodeList) expr.evaluate(this.xml, XPathConstants.NODESET);
    } catch (final XPathExpressionException e) {
      Logger.error("Illegal Xpath Expression " + e.getMessage());
      return null;
    }
    return messages;
  }

  /**
   * Folds the XML annotations into the SVG.
   */
  public void enrich() {
    this.root = this.svg.getRootElement();
    this.uri = this.root.getNamespaceURI();
    this.addBaseTitles();
    if (Cli.hasOption("iveo")) {
      this.addIveoAnnotations();
    }
    ;
    // Tactile.maxPolygon();
  }

  /**
   * Converts and writes the SVG to a file.
   */
  private void writeSvg(final String fileName) {
    if (!Cli.hasOption("iveo")) {
      FileHandler.writeSvg(this.svg, fileName);
      return;
    }
    this.root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
        "xmlns:iveo", Tactile.iveoUri);
    this.root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
        "xmlns:daisy", Tactile.daisyUri);
    FileHandler.writeSvg(this.svg, fileName);
  }

  /**
   * Gets the title element for an annotation.
   *
   * @param annotation
   *          The XML element.
   *
   * @return The speech attribute for the title.
   */
  private String getTitle(final Element annotation) {
    return this.getSpeech(annotation, Tactile.TITLE_ATTR);
  }

  /**
   * Gets the description element for an annotation.
   *
   * @param annotation
   *          The XML element.
   *
   * @return The speech attribute for the description.
   */
  private String getDescr(final Element annotation) {
    return this.getSpeech(annotation, Tactile.DESCR_ATTR);
  }

  /**
   * Gets the attibute value for a speech annotation.
   *
   * @param annotation
   *          The XML element.
   * @param attribute
   *          The attribute name.
   *
   * @return The speech attribute for the annotation element.
   */
  private String getSpeech(final Element annotation, final String attribute) {
    final String speech = annotation.getAttributeNS(sreUri, attribute);
    return this.useSpeechAttr ? speech : this.messages.get(speech);
  }

  /**
   * Adds titles and descriptions to the annotation elements.
   */
  private void addBaseTitles() {
    for (final Element element : this.annotations) {
      // System.out.println(this.getTitle(element));
      // System.out.println(this.getDescr(element));
    }
  }

  // private static void addBaseTitles() {
  //   Language.reset("en");
  //   for (final RichBond bond : RichStructureHelper.getBonds()) {
  //     String id = bond.getId();
  //     Tactile.addInvisibleBond(id);
  //     SreElement annotation = Tactile.annotation.retrieve(id);
  //     String content = Languages.get
  //       (annotation.getAttributeValue
  //        ("speech", annotation.getNamespaceURI()), "en");
  //     Tactile.addBondTitle(id, content);
  //   }
  //   for (final String bond: Tactile.hydrogens.getBonds().keySet()) {
  //     Tactile.addInvisibleBond(bond);
  //     Tactile.addBondTitle(bond, "Single bond");
  //   }

  //   for (final RichAtom atom : RichStructureHelper.getAtoms()) {
  //     Element element = Tactile.root.getElementById(atom.getId());
  //     if (element == null) {
  //       Tactile.addInvisibleAtom(atom);
  //       continue;
  //     }
  //     Tactile.swapRectangle(element);
  //     Tactile.describeAtom(atom, element);
  //   }
  //   for (final String atom: Tactile.hydrogens.getHydrogens()) {
  //     Element element = Tactile.root.getElementById(atom);
  //     if (element != null) {
  //       Tactile.swapRectangle(element);
  //       Tactile.addAtomTitle(element, "Hydrogen");
  //     }
  //   }

  //   List<RichAtomSet> sets = RichStructureHelper.getAtomSets();
  //   Collections.sort(sets, new SizeComparator());
  //   Node first = Tactile.root.getFirstChild();
  //   if (Cli.hasOption("iveo")) {
  //     for (Integer i = sets.size() - 1; i >= 0; i--) {
  //       RichAtomSet atomSet = sets.get(i);
  //       Tactile.addInvisibleAtomSet(atomSet, first);
  //     }
  //   } else {
  //     for (final RichAtomSet atomSet : sets) {
  //       Tactile.addInvisibleAtomSet(atomSet, first);
  //     }
  //   }
  // }

  private void addInvisibleGroup(final String name, final Element group) {
    Logger.logging("Adding grouped element " + name);
    final Node component = getDirectChild(group, "sre:component");
    if (component == null) {
      Logger.error("Group without children: " + name);
      return;
    }
    final NodeList componentNodes = component.getChildNodes();
    final List<String> components = new ArrayList<>();
    for (Integer i = 0; i < componentNodes.getLength(); i++) {
      final Node comp = componentNodes.item(i);
      if (comp instanceof Element) {
        components.add(comp.getTextContent().trim());
      }
    }
    final Element svgGroup = this.getInvisibleGroup(name, components);
    if (svgGroup == null) {
      Logger.error("Invisible group " + name + " could not be built.");
      return;
    }
    final String title = this.getSreAttributeValue(svgGroup, TITLE_ATTR);
    final String desc = this.getSreAttributeValue(svgGroup, DESCR_ATTR);
    this.addOrReplaceElement(svgGroup, "title", title);
    this.addOrReplaceElement(svgGroup, "desc", desc);
    final Node first = this.root.getFirstChild();
    this.root.insertBefore(svgGroup, first);
  }


  // TODO (sorge): Separate Bpoly and Bbox!
  private Element getInvisibleGroup(final String id,
      final List<String> components) {
    Bbox bbox = new Bbox();
    Bpolygon bpoly = new Bpolygon();

    for (final String name : components) {
      final Element component = this.root.getElementById(name);
      // This is the case of invisible junctions (atoms)!
      if (component == null) {
        System.out.println("Invisible: " + name);
        continue;
      }
      System.out.println("TagName: " + component.getTagName());
      if (component.getTagName() == "line") {
        // BBOX
        bbox.addPoints((SVGLineElement) component);
        // POLY
        bpoly.addCoordinates((SVGLineElement) component);
      }
      NodeList nodes = component.getElementsByTagNameNS(this.uri, "rect");
      for (Integer i = 0; i < nodes.getLength(); i++) {
        // BBOX
        final SVGRectElement node = (SVGRectElement) (nodes.item(i));
        bbox.addPoints(node);
        // POLY
        bpoly.addCoordinates(node);
      }
      nodes = component.getElementsByTagNameNS(this.uri, "line");
      for (Integer i = 0; i < nodes.getLength(); i++) {
        // BBOX
        final SVGLineElement node = (SVGLineElement) (nodes.item(i));
        bbox.addPoints(node);
        // POLY
        bpoly.addCoordinates(node);
      }
      if (component.getTagName() == "polygon") {
        // BBOX
        bbox.addPoints((SVGPolygonElement) component);
        // POLY
        bpoly.addCoordinates((SVGPolygonElement) component);
      }
      nodes = component.getElementsByTagNameNS(this.uri, "polygon");
      for (Integer i = 0; i < nodes.getLength(); i++) {
        // BBOX
        final SVGPolygonElement node = (SVGPolygonElement) (nodes.item(i));
        bbox.addPoints(node);
        // POLY
        bpoly.addCoordinates(node);
      }
      if (component.getTagName() == "polyline") {
        // BBOX
        bbox.addPoints((SVGPolylineElement) component);
        // POLY
        bpoly.addCoordinates((SVGPolylineElement) component);
      }
      nodes = component.getElementsByTagNameNS(this.uri, "polyline");
      for (Integer i = 0; i < nodes.getLength(); i++) {
        // BBOX
        final SVGPolylineElement node = (SVGPolylineElement) (nodes.item(i));
        bbox.addPoints(node);
        // POLY
        bpoly.addCoordinates(node);
      }
    }
    final Element group = this.svg.createElementNS(this.uri, "g");
    group.setAttribute("class", "atomset");
    group.setAttribute("id", id);
    group.setAttribute("opacity", "0.0");
    Element rect;
    if (Cli.hasOption("polygons")) {
      // POLY
      rect = this.polygonFromCoordinates(bpoly);
    } else {
      // BBOX
      rect = this.svg.createElementNS(this.uri, "rect");
      Point2d top = bbox.getTop();
      Point2d bot = bbox.getBottom();
      rect.setAttribute("x", Double.toString(top.x));
      rect.setAttribute("y", Double.toString(top.y));
      rect.setAttribute("width", Double.toString(bot.x - top.x));
      rect.setAttribute("height", Double.toString(bot.y - top.y));
    }
    rect.setAttribute("visibility", "visible");
    // TODO : Add iveo attributes here.
    if (Cli.hasOption("iveo")) {
      rect.setAttribute("id", id + "r");
      rect.setAttribute("onmouseover", "register(evt)");
      rect.setAttribute("onmousedown", "register(evt)");
    }
    group.appendChild(rect);
    return group;
  }

  // private static void describeAtomSet(final RichAtomSet atomSet, Element element) {
  // }

  // private static Element atomSetDescription(final RichAtomSet atomSet, SreElement annotation) {
  //   Element desc = svg.createElementNS(Tactile.uri, "desc");
  //   String description = Languages.
  //     get(annotation.getAttributeValue("speech2", annotation.getNamespaceURI()), "en");
  //   Text descText = svg.createTextNode(description);
  //     // (description.replaceFirst(content + " [0-9]*", content + " atom"));
  //   desc.appendChild(descText);
  //   return desc;
  // }

  // private static Element atomSetTitle(
  //     final RichAtomSet atomSet, SreElement annotation) {
  //   Element title = svg.createElementNS(Tactile.uri, "title");
  //   String content = Languages.
  //     get(annotation.getAttributeValue(
  //             "speech", annotation.getNamespaceURI()), "en");
  //   Text text = svg.createTextNode(content);
  //   title.appendChild(text);
  //   return title;
  // }

  // private static void addBondTitle(String bond, String content) {
  //   Element element = Tactile.root.getElementById(bond);
  //   if (element != null) {
  //     Element title = svg.createElementNS(Tactile.uri, "title");
  //     Text text = svg.createTextNode(content);
  //     Node root = Tactile.getTitleRoot(element, "polygon");
  //     title.appendChild(text);
  //     root.appendChild(title);
  //   }
  // }

  // /**
  //  * Adds an invisible polygon around a bond to combine multi-bonds or widen
  //  * single bonds.
  //  *
  //  * @param bond
  //  */
  // private static void addInvisibleBond(final String bond) {
  //   Element element = Tactile.root.getElementById(bond);
  //   if (element == null) {
  //     return;
  //   }
  //   Element poly = svg.createElementNS(Tactile.uri, "polygon");
  //   List<String> points = new ArrayList<>();
  //   List<Point2d> convexHull;
  // // Case of single bond
  //   if (element.getTagName() == "line") {
  //     SVGLineElement line = (SVGLineElement)element;
  //     Element group = svg.createElementNS(Tactile.uri, "g");
  //     group.setAttribute("class", line.getAttribute("class"));
  //     group.setAttribute("id", line.getAttribute("id"));
  //     line.removeAttribute("id");
  //     line.removeAttribute("class");
  //     line.getParentNode().replaceChild(group, line);
  //     group.appendChild(element);
  //     element = group;
  //     Double x1 = Tactile.getValue(line.getX1());
  //     Double y1 = Tactile.getValue(line.getY1());
  //     Double x2 = Tactile.getValue(line.getX2());
  //     Double y2 = Tactile.getValue(line.getY2());

  //       // x coordinates
  //     Double[] xs = {x1, x1 - 2.0, x1 + 2.0, x2, x2 - 2.0, x2 + 2.0,
  //                    x1 - 2.0, x1 + 2.0, x2, x2 - 2.0, x2 + 2.0};

  //       // y coordinates
  //     Double[] ys = {y1, y1 - 2.0, y1 + 2.0, y2, y2 - 2.0, y2 + 2.0,
  //                    y1 + 2.0, y1 - 2.0, y2, y2 + 2.0, y2 - 2.0};
  //     convexHull = GrahamScan.getConvexHull(xs, ys);

  //   } else {
  //     NodeList lines = element.getElementsByTagNameNS(Tactile.uri, "line");
  //     Integer length = lines.getLength();
  //     if (length == 0) {
  //       return;
  //     }
  //     /// TODO : Do this properly with triple bonds
  //     Double[] ys = new Double[2 * length];
  //     Double[] xs = new Double[2 * length];

  //     for (Integer i = 0; i < length; i++) {
  //       SVGLineElement line =  (SVGLineElement)lines.item(i);
  //       xs[2 * i] = Tactile.getValue(line.getX1());
  //       ys[2 * i] = Tactile.getValue(line.getY1());
  //       xs[2 * i + 1] = Tactile.getValue(line.getX2());
  //       ys[2 * i + 1] = Tactile.getValue(line.getY2());
  //     }
  //     convexHull = GrahamScan.getConvexHull(xs, ys);
  //   }
  //   // find the convex hull
  //   for (Point2d p : convexHull) {
  //     points.add(p.x + "," + p.y);
  //   }
  //   // Case of multi bond
  //   final Joiner joiner = Joiner.on(" ");
  //   poly.setAttribute("points", joiner.join(points));
  //   poly.setAttribute("opacity", "0.0");
  //   if (Cli.hasOption("iveo")) {
  //     poly.setAttribute("id", bond + "r");
  //     poly.setAttribute("onmouseover", "register(evt)");
  //     poly.setAttribute("onmousedown", "register(evt)");
  //   }
  //   element.appendChild(poly);
  // };


  // private static void addAtomTitle(Element element, String speech) {
  //   Element title = svg.createElementNS(Tactile.uri, "title");
  //   Text text = svg.createTextNode(speech);
  //   title.appendChild(text);
  //   Node root = Tactile.getTitleRoot(element);
  //   root.appendChild(title);
  // }

  // // The element to attach description and title to.
  // private static void describeAtom(final RichAtom atom, Element element) {
  //   String id = atom.getId();
  //   SreElement annotation = Tactile.annotation.retrieve(id);
  //   String description = Languages.
  //     get(annotation.getAttributeValue("speech", annotation.getNamespaceURI()), "en");
  //   Tactile.addAtomTitle(element, description);
  // }

  // private static void swapRectangle(final Element element) {
  //   NodeList nodes = element.getElementsByTagNameNS(Tactile.uri, "rect");
  //   if (nodes.getLength() == 0) {
  //     return;
  //   }
  //   Element rectangle = (Element)nodes.item(0).cloneNode(true);
  //   if (Cli.hasOption("iveo")) {
  //     String id = element.getAttribute("id");
  //     rectangle.setAttribute("id", id + "r");
  //     rectangle.setAttribute("onmouseover", "register(evt)");
  //     rectangle.setAttribute("onmousedown", "register(evt)");
  //   }
  //   rectangle.setAttribute("opacity", "0.0");
  //   element.appendChild(rectangle);
  // }


  // // This will be ugly!
  // /**
  //  * Adds a rectangle element around an invisible carbon atom.
  //  *
  //  * @param atom
  //  */
  // private static void addInvisibleAtom(final RichAtom atom) {
  //   Point2d currentPoint = null;
  //   Point2d point1 = null;
  //   Point2d point2 = null;
  //   String id = atom.getId();
  //   // Find the central point for the invisible atom.
  //   Set<String> external = atom.getExternalBonds();
  //   List<String> hydrogens = Tactile.hydrogens.getAtom(id);
  //   if (hydrogens != null) {
  //     external.addAll(hydrogens);
  //   }
  //   for (String bond : external) {
  //     // Work out if this is a hydrogen bond!
  //     Element svgBond = Tactile.root.getElementById(bond);
  //     if (point1 == null) {
  //       if (svgBond.getTagName() == "g") {
  //         point1 = new Point2d();
  //         point2 = new Point2d();
  //         Tactile.getAverageBondEnd(svgBond, point1, point2);
  //         continue;
  //       }
  //       SVGLineElement line = (SVGLineElement)svgBond;
  //       point1 = new Point2d(Tactile.getValue(line.getX1()),
  //                            Tactile.getValue(line.getY1()));
  //       point2 = new Point2d(Tactile.getValue(line.getX2()),
  //                            Tactile.getValue(line.getY2()));
  //       continue;
  //     }
  //     Point2d tempPoint1 = new Point2d();
  //     Point2d tempPoint2 = new Point2d();
  //     if (svgBond.getTagName() == "g") {
  //       Tactile.getAverageBondEnd(svgBond, tempPoint1, tempPoint2);
  //     } else {
  //       SVGLineElement line = (SVGLineElement)svgBond;
  //       tempPoint1 = new Point2d(Tactile.getValue(line.getX1()),
  //                                Tactile.getValue(line.getY1()));
  //       tempPoint2 = new Point2d(Tactile.getValue(line.getX2()),
  //                                Tactile.getValue(line.getY2()));
  //     }
  //     if (currentPoint == null) {
  //       if (valueClose(tempPoint1, point1)) {
  //         currentPoint = point1;
  //         continue;
  //       }
  //       if (valueClose(tempPoint1, point2)) {
  //         currentPoint = point2;
  //         continue;
  //       }
  //       if (valueClose(tempPoint2, point1)) {
  //         currentPoint = point1;
  //         continue;
  //       }
  //       if (valueClose(tempPoint2, point2)) {
  //         currentPoint = point2;
  //         continue;
  //       }
  //       System.out.println("Error1: Something is wrong with bond ends!");
  //     }
  //     if (valueClose(tempPoint1, currentPoint)) {
  //       continue;
  //     }
  //     if (valueClose(tempPoint2, currentPoint)) {
  //       continue;
  //     }
  //     System.out.println("Error2: Something is wrong with bond ends!");
  //   }
  //   // TODO : This has to iterate over all other points in the structure.
  //   //        Test with pure skeletal chain!
  //   if (currentPoint == null) {
  //     currentPoint = point1;
  //   }
  //   Element group = svg.createElementNS(Tactile.uri, "g");
  //   group.setAttribute("class", "atom");
  //   group.setAttribute("id", id);
  //   //group.setAttribute("visibility", "hidden");
  //   group.setAttribute("opacity", "0.0");
  //   Element rect = svg.createElementNS(Tactile.uri, "rect");
  //   rect.setAttribute("x", Double.toString
  //                     (currentPoint.x - Tactile.INV_ATOM_SIZE / 2));
  //   rect.setAttribute("y", Double.toString
  //                     (currentPoint.y - Tactile.INV_ATOM_SIZE / 2));
  //   rect.setAttribute("width", Double.toString(Tactile.INV_ATOM_SIZE));
  //   rect.setAttribute("height", Double.toString(Tactile.INV_ATOM_SIZE));
  //   if (Cli.hasOption("iveo")) {
  //     rect.setAttribute("id", id + "r");
  //     rect.setAttribute("onmouseover", "register(evt)");
  //     rect.setAttribute("onmousedown", "register(evt)");
  //   }
  //   group.appendChild(rect);

  //   Tactile.describeAtom(atom, group);
  //   Tactile.root.appendChild(group);
  // }

  // private static Boolean valueClose(Point2d a, Point2d b) {
  //   return a.distance(b) < Tactile.MIN_DIFF;
  // }


  // private static void getAverageBondEnd(
  //     Element group, Point2d point1, Point2d point2) {
  //   Integer number = 0;
  //   Point2d ref1 = null;
  //   Point2d ref2 = null;
  //   NodeList nodes = group.getChildNodes();
  //   for (Integer i = 0; i < nodes.getLength(); i++) {
  //     Node node = nodes.item(i);
  //     if (node.getNodeType() != 1) {
  //       continue;
  //     }
  //     Element element = (Element)node;
  //     if (element.getTagName() != "line") continue;
  //     SVGLineElement line = (SVGLineElement)element;
  //     number++;
  //     Point2d temp1 = new Point2d(Tactile.getValue(line.getX1()),
  //                                Tactile.getValue(line.getY1()));
  //     Point2d temp2 = new Point2d(Tactile.getValue(line.getX2()),
  //                                Tactile.getValue(line.getY2()));
  //     if (ref1 == null) {
  //       ref1 = temp1;
  //       ref2 = temp2;
  //     }
  //     if (ref1.distance(temp1) < ref1.distance(temp2)) {
  //       point1.add(temp1);
  //       point2.add(temp2);
  //     } else {
  //       point1.add(temp2);
  //       point2.add(temp1);
  //     }
  //   }
  //   point1.scale(1.0 / number);
  //   point2.scale(1.0 / number);
  // }


  // private static Node getTitleRoot(Element element) {
  //   return Tactile.getTitleRoot(element, "rect");
  // }

  // private static Node getTitleRoot(Element element, String type) {
  //   if (!Cli.hasOption("iveo")) {
  //     return element;
  //   }
  //   NodeList nodes = element.getElementsByTagNameNS(Tactile.uri, type);
  //   Integer length = nodes.getLength();
  //   return length > 0 ? nodes.item(length - 1) : element;
  // }


  private void addIveoAnnotations() {
    final String width = this.root.getAttribute("width");
    final String height = this.root.getAttribute("height");
    final Double widthValue = Double
        .parseDouble(width.replaceAll("[^\\d.]", ""));
    final Double heightValue = Double
        .parseDouble(height.replaceAll("[^\\d.]", ""));
    if (Cli.hasOption("iveo_buttons")) {
      this.addIveoButtons(widthValue, heightValue);
    }
    Double finalWidth = widthValue;
    Double finalHeight = heightValue;
    // If true portrait mode.
    Boolean swap = false;
    if (heightValue < 0.71 * widthValue) {
      finalHeight = 0.71 * widthValue;
    }
    if (0.7 * widthValue < heightValue && heightValue < widthValue) {
      finalWidth = heightValue / 0.71;
    }
    if (widthValue < heightValue && heightValue < 1.31 * widthValue) {
      finalHeight = 1.31 * widthValue;
      swap = true;
    }
    if (heightValue > 1.31 * widthValue) {
      finalWidth = heightValue / 1.31;
      swap = true;
    }

    this.root.setAttribute("width", finalWidth.toString());
    this.root.setAttribute("height", finalHeight.toString());
    final Element paper = this.svg.createElementNS(Tactile.iveoUri,
        "iveo:paper");
    paper.setAttribute("top-margin", "1.0 inch");
    paper.setAttribute("width", "8.5 inch");
    paper.setAttribute("right-margin", swap ? "0.5 inch" : "0.6 inch");
    paper.setAttribute("orientation", swap ? "portrait" : "landscape");
    paper.setAttribute("left-margin", "0.75 inch");
    paper.setAttribute("bottom-margin", swap ? "0.6 inch" : "0.5 inch");
    paper.setAttribute("height", "11.0 inch");
    paper.setAttribute("name", "Custom paper size");
    this.root.appendChild(paper);

    final Element layers = this.svg.createElementNS(Tactile.daisyUri,
        "daisy:layers");
    this.addLayerItem(layers, "on", "on", "on", "off", "on", "on", "Default");
    this.addLayerItem(layers, "on", "on", "on", "off", "on", "off", "Print");
    this.addLayerItem(layers, "off", "on", "on", "off", "on", "on", "Emboss");
    this.addLayerItem(layers, "off", "on", "on", "off", "on", "on", "Braille");
    this.addLayerItem(layers, "on", "on", "on", "off", "on", "on", "OnLayer");
    this.addLayerItem(layers, "off", "off", "on", "off", "off", "off",
        "OffLayer");
    this.root.appendChild(layers);
  }

  private void addLayerItem(final Element layers, final String print,
      final String display,
      final String enabledInViewer, final String lock, final String speak,
      final String emboss, final String name) {
    final Element layerItem = this.svg.createElementNS(Tactile.daisyUri,
        "daisy:layerItem");
    layerItem.setAttribute("print", print);
    layerItem.setAttribute("display", display);
    layerItem.setAttribute("enabledInViewer", enabledInViewer);
    layerItem.setAttribute("lock", lock);
    layerItem.setAttribute("speak", speak);
    layerItem.setAttribute("emboss", emboss);
    layerItem.setAttribute("name", name);
    layers.appendChild(layerItem);
  }

  private void addIveoButtons(final Double width, final Double height) {
    // Double radius = height < 0.7 * width ? 0.035 * width : 0.035 * height;
    final Double radius = 0.035 * width;
    final Double whiteX = .8 * width;
    final Double silverX = .9 * width;
    Double buttonY = .9 * height;
    if (Cli.hasOption("iveo_collision")) {
      buttonY = this.avoidIveoCollision(whiteX, buttonY, radius, height);
    }
    ;
    // Add buttons.
    this.addCircle(whiteX, buttonY, radius, "white", "remove", "drilldown");
    this.addCircle(silverX, buttonY, radius, "silver", "restore", "restore");
  }

  private Double avoidIveoCollision(final Double whiteX, Double buttonY,
      final Double radius, final Double height) {
    this.topX = whiteX - (radius + 10);
    this.topY = buttonY - (radius + 10);
    this.collisionY = null;
    this.collisionDetection();
    if (this.collisionY != null) {
      final Double delta = this.collisionY - this.topY;
      buttonY += delta;
      final Double newHeight = height + delta;
      this.root.setAttribute("height", newHeight.toString());
    }
    return buttonY;
  }

  private void addCircle(final Double x, final Double y, final Double r,
      final String fill, final String name, final String id) {
    final Element circle = this.svg.createElementNS(this.uri, "circle");
    circle.setAttribute("cx", x.toString());
    circle.setAttribute("cy", y.toString());
    circle.setAttribute("r", r.toString());
    circle.setAttribute("fill", fill);
    circle.setAttribute("id", id);
    circle.setAttribute("stroke", "rgb(0, 0, 0)");
    circle.setAttribute("stroke-width", "1.0");
    circle.setAttribute("onmousedown", "drill(evt)");
    // Element title = svg.createElementNS(this.uri, "title");
    // Text text = svg.createTextNode(name);
    // title.appendChild(text);
    // circle.appendChild(title);
    this.root.appendChild(circle);
  }

  // Collision detection algorithm
  private Double topX = null;
  private Double topY = null;
  private Double collisionY = null;

  private void updateCollision(final Double x, final Double y) {
    if (x < this.topX || y < this.topY) {
      return;
    }
    if (this.collisionY == null || y > this.collisionY) {
      this.collisionY = y;
    }
  }

  private void collisionDetection() {
    //
    // For all points from lines, rectangles, polygons
    // Remove point if less than (left of) min x value
    // Remove point if less than (above from) min y value
    // Retain point with max y value
    //
    // If no point is found, we are good.
    // O/w extend the height by max y - min y.
    //
    final NodeList lines = this.svg.getElementsByTagNameNS(this.uri, "line");
    final NodeList rectangles = this.svg.getElementsByTagNameNS(this.uri,
        "rect");
    final NodeList polygons = this.svg.getElementsByTagNameNS(this.uri,
        "polygon");
    final NodeList polylines = this.svg.getElementsByTagNameNS(this.uri,
        "polyline");
    // TODO (sorge): Integrate Paths.
    // NodeList paths = this.svg.getElementsByTagNameNS(this.uri, "path");
    for (Integer i = 0; i < lines.getLength(); i++) {
      final SVGLineElement line = (SVGLineElement) lines.item(i);
      for (SVGPoint point: TactileUtil.getPoints(line)) {
        this.updateCollision(TactileUtil.getValue(point.getX()),
                             TactileUtil.getValue(point.getY()));
      }
    }
    for (Integer i = 0; i < rectangles.getLength(); i++) {
      final SVGRectElement rectangle = (SVGRectElement) rectangles.item(i);
      for (SVGPoint point: TactileUtil.getPoints(rectangle)) {
        this.updateCollision(TactileUtil.getValue(point.getX()),
                             TactileUtil.getValue(point.getY()));
      }
    }
    for (Integer i = 0; i < polygons.getLength(); i++) {
      final SVGPolygonElement polygon = (SVGPolygonElement) polygons.item(i);
      for (SVGPoint point: TactileUtil.getPoints(polygon)) {
        this.updateCollision(TactileUtil.getValue(point.getX()),
                             TactileUtil.getValue(point.getY()));
      }
    }
    for (Integer i = 0; i < polylines.getLength(); i++) {
      final SVGPolylineElement polyline = (SVGPolylineElement) polylines
          .item(i);
      for (SVGPoint point: TactileUtil.getPoints(polyline)) {
        this.updateCollision(TactileUtil.getValue(point.getX()),
                             TactileUtil.getValue(point.getY()));
      }
    }
  }

  private Element polygonFromCoordinates(Bpolygon polygon) {
    final Element poly = this.svg.createElementNS(this.uri, "polygon");
    final List<String> points = new ArrayList<>();
    for (final Point2d p : polygon.getPolygon()) {
      points.add(p.x + "," + p.y);
    }
    final Joiner joiner = Joiner.on(" ");
    poly.setAttribute("points", joiner.join(points));
    poly.setAttribute("opacity", "0.0");
    return poly;
  }

  /**
   * Inserts a maximal convex polygon around all the visible points in the SVG.
   */
  private void maxPolygon() {
    final NodeList lines = this.svg.getElementsByTagNameNS(this.uri, "line");
    final NodeList rectangles = this.svg.getElementsByTagNameNS(this.uri,
        "rect");
    final NodeList polygons = this.svg.getElementsByTagNameNS(this.uri,
        "polygon");
    final NodeList polylines = this.svg.getElementsByTagNameNS(this.uri,
        "polyline");
    final Bpolygon bpoly = new Bpolygon();
    for (Integer i = 0; i < lines.getLength(); i++) {
      bpoly.addCoordinates((SVGLineElement) lines.item(i));
    }
    for (Integer i = 0; i < rectangles.getLength(); i++) {
      bpoly.addCoordinates((SVGRectElement) rectangles.item(i));
    }
    for (Integer i = 0; i < polygons.getLength(); i++) {
      bpoly.addCoordinates((SVGPolygonElement) polygons.item(i));
    }
    for (Integer i = 0; i < polylines.getLength(); i++) {
      bpoly.addCoordinates((SVGPolylineElement) polylines.item(i));
    }
    this.root.appendChild(this.polygonFromCoordinates(bpoly));
  }

  private static Element getDirectChild(final Element parent,
      final String name) {
    for (Node child = parent.getFirstChild(); child != null; child = child
        .getNextSibling()) {
      if (child instanceof Element && name.equals(child.getNodeName())) {
        return (Element) child;
      }
    }
    return null;
  }


  /*****************
   * begin insert cherden 5/29/2017
   *
   * functions for adding all <title/> and <desc/> elements for tactile objects.
   *
   *****************/
  private void addTitle(final SVGDocument svg, final Node elem,
      final String innertitle) {
    if (innertitle.length() == 0) {
      return;
    }
    final String id = elem.getTextContent().trim();
    final Element noddy = svg.getElementById(id);

    if (noddy == null) {
      return;
    }
    this.addOrReplaceElement(noddy, "title", innertitle);
  }

  private void addDescription(final SVGDocument svg, final Node elem,
      final String innertitle) {
    if (innertitle.length() == 0) {
      return;
    }

    final String id = elem.getTextContent().trim();

    final Element noddy = svg.getElementById(id);

    if (noddy == null) {
      return;
    }

    this.addOrReplaceElement(noddy, "desc", innertitle);
  }

  private Element findFirstChildElement(final Node node) {
    final NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      if (children.item(i).getNodeType() != 1) {
        continue;
      }
      return (Element) children.item(i);
    }

    return null;
  }

  private Node findAnnotationByName(final String group, final String name) {

    for (final Element node : this.annotations) {
      final Element fairchild = getDirectChild(node, group);
      if (fairchild == null) {
        continue;
      }
      if (fairchild != null && fairchild.getTextContent().trim().equals(name)) {
        return node;
      }

    }

    return null;
  }

  private String getSreAttributeValue(final Element element,
      final String attribute) {
    final String attr = this.getSreAttribute(element, attribute);
    return (attr != null) ? attr : "";
  }

  private String getSreAttribute(final Element element,
      final String attribute) {
    final NamedNodeMap attrs = element.getAttributes();
    Node attr = attrs.getNamedItem(attribute);
    if (attr == null) {
      attr = attrs.getNamedItem(SRE_PREFIX + attribute);
    }
    return (attr != null) ? attr.getNodeValue() : null;
  }

  private Boolean isGrouped(final Element element) {
    final String tag = element.getTagName();
    return tag == SRE_PREFIX + "grouped" || tag == SRE_PREFIX + "atomSet" ||
        tag == "grouped" || tag == "atomSet";
  }

  private Boolean isActive(final Element element) {
    final String tag = element.getTagName();
    return tag == SRE_PREFIX + "active" || tag == SRE_PREFIX + "atom" ||
        tag == "active" || tag == "atom";
  }

  private Boolean isPassive(final Element element) {
    final String tag = element.getTagName();
    return tag == SRE_PREFIX + "passive" || tag == SRE_PREFIX + "bond" ||
        tag == "passive" || tag == "bond";
  }

  private void addTitlesAndDesc() {
    for (final Element node : this.annotations) {
      if (node.hasChildNodes()) {
        final Element elem = this.findFirstChildElement(node);
        final String name = elem.getTextContent().trim();
        final Node parent = getDirectChild(node, "sre:parents");

        if (this.isGrouped(elem)) {
          if (parent == null || !parent.hasChildNodes()) {
            Logger.logging("found the SVG root element: " + name);
            final SVGSVGElement root = this.svg.getRootElement();
            final String title = this.getSreAttributeValue(node, TITLE_ATTR);
            final String desc = this.getSreAttributeValue(node, DESCR_ATTR);
            Logger.logging("   <title>" + title + "</title>");
            Logger.logging("   <desc>" + desc + "</desc>");
            this.addOrReplaceElement(root, "title", title);
            this.addOrReplaceElement(root, "desc", desc);
            continue;
          }
          // TODO (sorge): Order with respect to size and overlap!
          this.addInvisibleGroup(name, node);
        }

        // Node parents = getDirectChild( node, "sre:parents" );

        final String attrTitle = this.getSreAttribute(node, TITLE_ATTR);
        final String attrDesc = this.getSreAttribute(node, DESCR_ATTR);

        String title = "";
        String desc = "";

        // if no speech attribute exists go and fetch the next
        // speech from its components.
        if (attrTitle == null) {
          Logger.logging(name + " annotation has no sre:speech attribute");

          Node component = getDirectChild(node, "sre:component");

          if (component != null && component.hasChildNodes()) {
            Logger.logging("<sre:component/> has children");

            final Node fairchild = this.findFirstChildElement(component);
            final String componentName = fairchild.getTextContent().trim();

            final Node annotation = this
                .findAnnotationByName(fairchild.getNodeName(), componentName);
            if (annotation != null) {
              final Node localattr = annotation.getAttributes()
                  .getNamedItem(TITLE_ATTR);

              if (localattr != null) {
                title += localattr.getNodeValue();
                Logger.logging("found useful speech in <"
                    + fairchild.getNodeName() + ">"
                    + title + "</" + fairchild.getNodeName() + ">");
              }
            }
          }

          component = getDirectChild(node, "sre:parents");

          if (component != null && component.hasChildNodes()) {
            Logger.logging("<sre:parent/> has children");

            final Node lonelyparent = this.findFirstChildElement(component);
            final String parentName = lonelyparent.getTextContent().trim();

            final Node annotation = this
                .findAnnotationByName(lonelyparent.getNodeName(), parentName);
            if (annotation != null) {
              final Node localattr = annotation.getAttributes()
                  .getNamedItem(TITLE_ATTR);

              if (localattr != null) {
                title += localattr.getNodeValue();
                Logger.logging("found useful title in <"
                    + lonelyparent.getNodeName()
                    + ">" + title + "</"
                    + lonelyparent.getNodeName() + ">");
              } else {
                final String type = this
                    .getSreAttributeValue((Element) annotation, "type");

                if (!type.equals("")) {
                  desc += "Type " + type;
                  Logger.logging("found useful desc in <"
                      + lonelyparent.getNodeName() + ">"
                      + desc + "</"
                      + lonelyparent.getNodeName() + ">");
                }
              }
            }
          }
        } else {
          title = attrTitle;
          if (attrDesc != null) {
            desc = attrDesc;
          }

          Logger.logging(name + " adding annotation <title>"
              + title + "</title>");
          Logger.logging(name + " adding annotation <desc>"
              + desc + "</desc>");
        }

        this.addTitle(this.svg, elem, title);
        this.addDescription(this.svg, elem, desc);
      }

    }
  }

  private void addOrReplaceElement(final Element element, final String tag,
      final String content) {
    Element elti = getDirectChild(element, tag);
    if (elti == null) {
      elti = this.svg.createElementNS(Tactile.svgUri, tag);
      elti.setTextContent(content);
      element.appendChild(elti);
    } else {
      elti.setTextContent(content);
    }
  }


  /*****************
   * end insert cherden 5/29/2017
   *
   * functions for adding all <title> and <desc> elements for tactile objects
   *****************/
}
