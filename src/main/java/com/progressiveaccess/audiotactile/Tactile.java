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
 * @file   Tactile.java
 * @author Volker Sorge
 *          <a href="mailto:V.Sorge@progressiveaccess.com">Volker Sorge</a>
 * @date   Thu Jun 23 05:47:46 2016
 *
 * @brief  Utility functions for generation of audio tactile SVG.
 *
 *
 */

//

package com.progressiveaccess.audiotactile;

import com.google.common.base.Joiner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGAnimatedLength;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGLineElement;
import org.w3c.dom.svg.SVGPoint;
import org.w3c.dom.svg.SVGPointList;
import org.w3c.dom.svg.SVGPolygonElement;
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
  // Magic numbers should be parameterisable.
  private static final Double MIN_DIFF = 5.0;
  private static final Double INV_ATOM_SIZE = 10.0;

  private SVGDocument svg = null;
  private Document xml = null;
  private SVGSVGElement root = null;
  private String uri = null;
  private final List<Element> annotations = new ArrayList<>();
  private final Map<String, String> messages = new HashMap<>();

  private final XPath xpath = XPathFactory.newInstance().newXPath();

  /**
   * Constructor.
   *
   * @param svgFile The SVG file.
   * @param xmlFile The XML annotation file.
   */
  public Tactile(final String svgFile, final String xmlFile) {
    try {
      this.svg = FileHandler.loadSvg(svgFile);
    } catch (Exception e) {
      Logger.error("Can't load SVG file " + svgFile + "\n");
      return;
    }
    try {
      this.xml = FileHandler.loadXml(xmlFile);
    } catch (Exception e) {
      Logger.error("Can't load XML file " + xmlFile + "\n");
      return;
    }
    this.annotations();
    this.messages();
    this.enrich();
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
    final NodeList annotations = this.xml.getElementsByTagName("sre:annotation");
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
    String lang = "en";
    if (Cli.hasOption("language")) {
      lang = Cli.getOptionValue("language");
    }
    NodeList messages = null;
    try {
      final XPathExpression expr =
        this.xpath.compile(// 1. Pick all message elements.
                           "//*[local-name()='messages']/" +
                           // 2. Get the contained language element.
                           "*[local-name()='language'" +
                           // 3. Check if it is the language we want.
                           " and text()='" + lang + "']/" +
                           // 4. Then backup and take all message elements.
                           "../*[local-name()='message']");
      messages = (NodeList) expr.evaluate(this.xml, XPathConstants.NODESET);
    }
    catch (XPathExpressionException e) {
      Logger.error("Illegal Xpath Expression " + e.getMessage());
      return;
    }
    if (messages == null || messages.getLength() == 0) {
      Logger.error("Language " + lang + " does not exist.");
      return;
    }
    for (Integer i = 0; i < messages.getLength(); i++) {
      final Element item = (Element) messages.item(i);
      Logger.logging(FileHandler.toString(item));
      this.messages.put(item.getAttribute("sre:msg"), item.getTextContent());
    }
  }

  /** 
   * Folds the XML annotations into the SVG.
   */  
  public void enrich() {
    this.root = this.svg.getRootElement();
    this.uri = this.root.getNamespaceURI();
    // this.addBaseTitles();
    if (Cli.hasOption("iveo")) {
      this.addIveoAnnotations();
    };
    // Tactile.maxPolygon();
  }

  /**
   * Converts and writes the SVG to a file.
   */
  private void writeSvg(String fileName) {
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

  // private static void addInvisibleAtomSet(RichAtomSet atomSet, Node first) {
  //   SreElement annotation = Tactile.annotation.retrieve(atomSet.getId());
  //   Element title = Tactile.atomSetTitle(atomSet, annotation);
  //   Element description = Tactile.atomSetDescription(atomSet, annotation);
  //   if (atomSet.getType() == RichSetType.MOLECULE && Cli.hasOption("iveo")) {
  //     Tactile.root.insertBefore(title, first);
  //     Tactile.root.insertBefore(description, first);
  //   } else {
  //     Element element = Tactile.getInvisibleAtomSet(atomSet);
  //     Node root = Tactile.getTitleRoot(element);
  //     root.appendChild(title);
  //     root.appendChild(description);
  //     Tactile.root.insertBefore(element, first);
  //   }
  // }

  // private static Element getInvisibleAtomSet(RichAtomSet atomSet) {
  //   //BBOX
  //   Point2d topLeft = new Point2d();
  //   Point2d bottomRight = new Point2d();
  //   Point2d tempTop = new Point2d();
  //   Point2d tempBot = new Point2d();
  //   Boolean init = true;
  //   //POLY
  //   List<Double> xs = new ArrayList<>();
  //   List<Double> ys = new ArrayList<>();

  //   // Get all components
  //   List<String> components = new ArrayList<>();
  //   components.addAll(atomSet.getComponents());
  //   // Adds hydrogens and hydrogen bonds to components.
  //   if (Cli.hasOption("tactile_hydrogens")) {
  //     List<String> hydros = Tactile.hydrogens.getAtomSetHydrogens(atomSet.getId());
  //     if (hydros != null) {
  //       components.addAll(hydros);
  //     }
  //     List<String> hydroBonds = Tactile.hydrogens.getAtomSetBonds(atomSet.getId());
  //     if (hydroBonds != null) {
  //       components.addAll(hydroBonds);
  //     }
  //   }
  //   // Adds all components.
  //   for (String name : components) {
  //     Element component = Tactile.root.getElementById(name);
  //     if (component.getTagName() == "line") {
  //       // BBOX
  //       Tactile.pointsFromLine(component, tempTop, tempBot);
  //       Tactile.combineBbox(topLeft, bottomRight, tempTop, tempBot, init);
  //       init = false;
  //       // POLY
  //       Tactile.coordinatesFromLine((SVGLineElement)component, xs, ys);
  //     }
  //     NodeList nodes = component.getElementsByTagNameNS(Tactile.uri, "rect");
  //     for (Integer i = 0; i < nodes.getLength(); i++) {
  //       // BBOX
  //       Node node = nodes.item(i);
  //       Tactile.pointsFromRect(node, tempTop, tempBot);
  //       Tactile.combineBbox(topLeft, bottomRight, tempTop, tempBot, init);
  //       init = false;
  //       // POLY
  //       Tactile.coordinatesFromRectangle((SVGRectElement)node, xs, ys);
  //     }
  //     nodes = component.getElementsByTagNameNS(Tactile.uri, "line");
  //     for (Integer i = 0; i < nodes.getLength(); i++) {
  //       // BBOX
  //       Node node = nodes.item(i);
  //       Tactile.pointsFromLine(node, tempTop, tempBot);
  //       Tactile.combineBbox(topLeft, bottomRight, tempTop, tempBot, init);
  //       init = false;
  //       // POLY
  //       Tactile.coordinatesFromLine((SVGLineElement)node, xs, ys);
  //     }
  //   }
  //   Element group = svg.createElementNS(Tactile.uri, "g");
  //   group.setAttribute("class", "atomset");
  //   group.setAttribute("id", atomSet.getId());
  //   group.setAttribute("opacity", "0.0");
  //   Element rect;
  //   if (Cli.hasOption("tactile_polygons")) {
  //     // POLY
  //     rect = Tactile.polygonFromCoordinates(xs, ys);
  //   } else {
  //     // BBOX
  //     rect = svg.createElementNS(Tactile.uri, "rect");
  //     rect.setAttribute("x", Double.toString(topLeft.x));
  //     rect.setAttribute("y", Double.toString(topLeft.y));
  //     rect.setAttribute("width", Double.toString(bottomRight.x - topLeft.x));
  //     rect.setAttribute("height", Double.toString(bottomRight.y - topLeft.y));
  //   }
  //   rect.setAttribute("visibility", "visible");
  //   // TODO: Add iveo attributes here.
  //   if (Cli.hasOption("iveo")) {
  //     rect.setAttribute("id", atomSet.getId() + "r");
  //     rect.setAttribute("onmouseover", "speakIt(evt)");
  //     rect.setAttribute("onmousedown", "speakIt(evt)");
  //   }
  //   group.appendChild(rect);
  //   return group;
  // }


  // private static void combineBbox(Point2d top, Point2d bot, Point2d ttop, Point2d tbot, Boolean init) {
  //   if (init) {
  //     top.set(ttop);
  //     bot.set(tbot);
  //     return;
  //   }
  //   top.set(Math.min(top.x, ttop.x), Math.min(top.y, ttop.y));
  //   bot.set(Math.max(bot.x, tbot.x), Math.max(bot.y, tbot.y));
  // }


  // private static void pointsFromLine(Node element, Point2d top, Point2d bot) {
  //   SVGLineElement line = (SVGLineElement)element;
  //   Double x1 = Tactile.getValue(line.getX1());
  //   Double y1 = Tactile.getValue(line.getY1());
  //   Double x2 = Tactile.getValue(line.getX2());
  //   Double y2 = Tactile.getValue(line.getY2());
  //   top.set(Math.min(x1, x2), Math.min(y1, y2));
  //   bot.set(Math.max(x1, x2), Math.max(y1, y2));
  // }

  // private static void pointsFromRect(Node element, Point2d top, Point2d bot) {
  //   SVGRectElement wedge = (SVGRectElement)element;
  //   Double x1 = Tactile.getValue(wedge.getX());
  //   Double y1 = Tactile.getValue(wedge.getY());
  //   Double x2 = x1 + Tactile.getValue(wedge.getWidth());
  //   Double y2 = y1 + Tactile.getValue(wedge.getHeight());
  //   top.set(x1, y1);
  //   bot.set(x2, y2);
  // }

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

  // private static Element atomSetTitle(final RichAtomSet atomSet, SreElement annotation) {
  //   Element title = svg.createElementNS(Tactile.uri, "title");
  //   String content = Languages.
  //     get(annotation.getAttributeValue("speech", annotation.getNamespaceURI()), "en");
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
  //     /// TODO: Do this properly with triple bonds
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
  //     poly.setAttribute("onmouseover", "speakIt(evt)");
  //     poly.setAttribute("onmousedown", "speakIt(evt)");
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
  //     rectangle.setAttribute("onmouseover", "speakIt(evt)");
  //     rectangle.setAttribute("onmousedown", "speakIt(evt)");
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
  //   // TODO: This has to iterate over all other points in the structure. Test
  //   //       with pure skeletal chain!
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
  //     rect.setAttribute("onmouseover", "speakIt(evt)");
  //     rect.setAttribute("onmousedown", "speakIt(evt)");
  //   }
  //   group.appendChild(rect);

  //   Tactile.describeAtom(atom, group);
  //   Tactile.root.appendChild(group);
  // }

  // private static Boolean valueClose(Point2d a, Point2d b) {
  //   return a.distance(b) < Tactile.MIN_DIFF;
  // }


  // private static void getAverageBondEnd(Element group, Point2d point1, Point2d point2) {
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

  private static Double getValue(SVGAnimatedLength length) {
    return Double.parseDouble("" + length.getBaseVal().getValue());
  }

  private static Double getValue(Float length) {
    return Double.parseDouble("" + length);
  }

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
    if (Cli.hasOption("iveo_buttons")) {
      this.addIveoButtons();
    }
    String width = this.root.getAttribute("width");
    String height = this.root.getAttribute("height");
    Double widthValue = Double.parseDouble(width);
    Double heightValue = Double.parseDouble(height);
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
    Element paper = svg.createElementNS(Tactile.iveoUri, "iveo:paper");
    paper.setAttribute("top-margin", "1.0 inch");
    paper.setAttribute("width", "8.5 inch");
    paper.setAttribute("right-margin", swap ? "0.5 inch" : "0.6 inch");
    paper.setAttribute("orientation", swap ? "portrait" : "landscape");
    paper.setAttribute("left-margin", "0.75 inch");
    paper.setAttribute("bottom-margin", swap ? "0.6 inch" : "0.5 inch");
    paper.setAttribute("height", "11.0 inch");
    paper.setAttribute("name", "Custom paper size");
    root.appendChild(paper);

    Element layers = svg.createElementNS(Tactile.daisyUri, "daisy:layers");
    this.addLayerItem(layers, "on", "on", "on", "off", "on", "on", "Default");
    this.addLayerItem(layers, "on", "on", "on", "off", "on", "off", "Print");
    this.addLayerItem(layers, "off", "on", "on", "off", "on", "on", "Emboss");
    this.addLayerItem(layers, "off", "on", "on", "off", "on", "on", "Braille");
    this.addLayerItem(layers, "on", "on", "on", "off", "on", "on", "OnLayer");
    this.addLayerItem(layers, "off", "off", "on", "off", "off", "off", "OffLayer");
    root.appendChild(layers);
  }

  private void addLayerItem(Element layers, String print, String display,
                                   String enabledInViewer, String lock, String speak,
                                   String emboss, String name) {
    Element layerItem = svg.createElementNS(Tactile.daisyUri, "daisy:layerItem");
    layerItem.setAttribute("print", print);
    layerItem.setAttribute("display", display);
    layerItem.setAttribute("enabledInViewer", enabledInViewer);
    layerItem.setAttribute("lock", lock);
    layerItem.setAttribute("speak", speak);
    layerItem.setAttribute("emboss", emboss);
    layerItem.setAttribute("name", name);
    layers.appendChild(layerItem);
  }

  private void addIveoButtons() {
    Double width = Double.parseDouble(this.root.getAttribute("width"));
    Double height = Double.parseDouble(this.root.getAttribute("height"));
    // Double radius = height < 0.7 * width ? 0.035 * width : 0.035 * height;
    Double radius = 0.035 * width;
    Double whiteX = .8 * width;
    Double silverX = .9 * width;
    Double buttonY = .9 * height;
    if (Cli.hasOption("iveo_collision")) {
      buttonY = this.avoidIveoCollision(whiteX, buttonY, radius, height);
    };
    // Add buttons.
    this.addCircle(whiteX, buttonY, radius, "white", "remove", "drilldown");
    this.addCircle(silverX, buttonY, radius, "silver", "restore", "restore");
  }

  private Double avoidIveoCollision(Double whiteX, Double buttonY, Double radius, Double height) {
    this.topX = whiteX - (radius + 10);
    this.topY = buttonY - (radius + 10);
    this.collisionY = null;
    this.collisionDetection();
    if (this.collisionY  != null) {
      Double delta = this.collisionY - this.topY;
      buttonY += delta;
      Double newHeight = height + delta;
      this.root.setAttribute("height", newHeight.toString());
    }
    return buttonY;
  }

  private void addCircle(Double x, Double y, Double r, String fill, String name, String id) {
    Element circle = svg.createElementNS(this.uri, "circle");
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

  private void updateCollision(Double x, Double y) {
    if (x < this.topX || y < this.topY) return;
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
    NodeList lines = this.svg.getElementsByTagNameNS(this.uri, "line");
    NodeList rectangles = this.svg.getElementsByTagNameNS(this.uri, "rect");
    NodeList polygons = this.svg.getElementsByTagNameNS(this.uri, "polygon");
    for (Integer i = 0; i < lines.getLength(); i++) {
      SVGLineElement line = (SVGLineElement)lines.item(i);
      this.updateCollision(Tactile.getValue(line.getX1()),
                              Tactile.getValue(line.getY1()));
      this.updateCollision(Tactile.getValue(line.getX2()),
                              Tactile.getValue(line.getY2()));
    }
    for (Integer i = 0; i < rectangles.getLength(); i++) {
      SVGRectElement rectangle = (SVGRectElement)rectangles.item(i);
      Double x = Tactile.getValue(rectangle.getX());
      Double y = Tactile.getValue(rectangle.getY());
      Double w = Tactile.getValue(rectangle.getWidth());
      Double h = Tactile.getValue(rectangle.getHeight());
      this.updateCollision(x + w, y + h);
    }
    for (Integer i = 0; i < polygons.getLength(); i++) {
      SVGPolygonElement polygon = (SVGPolygonElement)polygons.item(i);
      SVGPointList points = polygon.getPoints();
      for (Integer j = 0; j < points.getNumberOfItems(); j++) {
        SVGPoint point = points.getItem(j);
        this.updateCollision(Tactile.getValue(point.getX()),
                                Tactile.getValue(point.getY()));
      }
    }
  }


  private void coordinatesFromLine
    (SVGLineElement line, List<Double> xs, List<Double> ys) {
    xs.add(Tactile.getValue(line.getX1()));
    ys.add(Tactile.getValue(line.getY1()));
    xs.add(Tactile.getValue(line.getX2()));
    ys.add(Tactile.getValue(line.getY2()));
  }

  private void coordinatesFromRectangle(SVGRectElement rectangle,
                                        List<Double> xs, List<Double> ys) {
    Double x = Tactile.getValue(rectangle.getX());
    Double y = Tactile.getValue(rectangle.getY());
    Double h = Tactile.getValue(rectangle.getHeight());
    Double w = Tactile.getValue(rectangle.getWidth());
    xs.add(x + w); ys.add(y + h);
    xs.add(x + w); ys.add(y);
    xs.add(x); ys.add(y + h);
    xs.add(x); ys.add(y);
  }

  private void coordinatesFromPolygon(SVGPolygonElement polygon,
                                      List<Double> xs, List<Double> ys) {
    SVGPointList points = polygon.getPoints();
    for (Integer j = 0; j < points.getNumberOfItems(); j++) {
      SVGPoint point = points.getItem(j);
      Double x = Tactile.getValue(point.getX());
      Double y = Tactile.getValue(point.getY());
      xs.add(x); ys.add(y);
    }
  }

  private Element polygonFromCoordinates(List<Double> xs, List<Double> ys) {
    List<Point2d> convexHull = GrahamScan.getConvexHull
        (xs.toArray(new Double[xs.size()]), ys.toArray(new Double[ys.size()]));
    Element poly = svg.createElementNS(this.uri, "polygon");
    List<String> points = new ArrayList<>();
    for (Point2d p : convexHull) {
      points.add(p.x + "," + p.y);
    }
    final Joiner joiner = Joiner.on(" ");
    poly.setAttribute("points", joiner.join(points));
    poly.setAttribute("opacity", "0.0");
    return poly;
  }

  /**
   * Inserts a maximal convex polygon around all the visible points in the
   * SVG.
   */
  private void maxPolygon() {
    NodeList lines = this.svg.getElementsByTagNameNS(this.uri, "line");
    NodeList rectangles = this.svg.getElementsByTagNameNS(this.uri, "rect");
    NodeList polygons = this.svg.getElementsByTagNameNS(this.uri, "polygon");
    List<Double> xs = new ArrayList<>();
    List<Double> ys = new ArrayList<>();
    for (Integer i = 0; i < lines.getLength(); i++) {
      this.coordinatesFromLine((SVGLineElement) lines.item(i), xs, ys);
    }
    for (Integer i = 0; i < rectangles.getLength(); i++) {
      this.coordinatesFromRectangle((SVGRectElement) rectangles.item(i), xs, ys);
    }
    for (Integer i = 0; i < polygons.getLength(); i++) {
      this.coordinatesFromPolygon((SVGPolygonElement) polygons.item(i), xs, ys);
    }
    this.root.appendChild(this.polygonFromCoordinates(xs, ys));
  }

}
