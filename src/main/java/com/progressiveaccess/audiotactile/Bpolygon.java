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
 * @file   Bpolygon.java
 * @author Volker Sorge
 *          <a href="mailto:V.Sorge@progressiveaccess.com">Volker Sorge</a>
 * @date   Tue Jul  4 01:34:56 2017
 * 
 * @brief  Bounding polygon.
 * 
 * 
 */

package com.progressiveaccess.audiotactile;

import org.w3c.dom.svg.SVGAnimatedPoints;
import org.w3c.dom.svg.SVGLineElement;
import org.w3c.dom.svg.SVGPoint;
import org.w3c.dom.svg.SVGPointList;
import org.w3c.dom.svg.SVGRectElement;

import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Point2d;

/**
 * Class of bounding polygons for multiple SVG elements.
 */
public class Bpolygon {

  private List<Double> xs = new ArrayList<>();
  private List<Double> ys = new ArrayList<>();


  public void addCoordinates(final SVGLineElement line) {
    this.xs.add(TactileUtil.getValue(line.getX1()));
    this.ys.add(TactileUtil.getValue(line.getY1()));
    this.xs.add(TactileUtil.getValue(line.getX2()));
    this.ys.add(TactileUtil.getValue(line.getY2()));
  }

  public void addCoordinates(final SVGAnimatedPoints polygon) {
    final SVGPointList points = polygon.getPoints();
    for (Integer j = 0; j < points.getNumberOfItems(); j++) {
      final SVGPoint point = points.getItem(j);
      final Double x = TactileUtil.getValue(point.getX());
      final Double y = TactileUtil.getValue(point.getY());
      this.xs.add(x);
      this.ys.add(y);
    }
  }

  public void addCoordinates(final SVGRectElement rectangle) {
    final Double x = TactileUtil.getValue(rectangle.getX());
    final Double y = TactileUtil.getValue(rectangle.getY());
    final Double h = TactileUtil.getValue(rectangle.getHeight());
    final Double w = TactileUtil.getValue(rectangle.getWidth());
    this.xs.add(x + w);
    this.ys.add(y + h);
    this.xs.add(x + w);
    this.ys.add(y);
    this.xs.add(x);
    this.ys.add(y + h);
    this.xs.add(x);
    this.ys.add(y);
  }

  public List<Point2d> getPolygon() {
    final List<Point2d> convexHull = GrahamScan.getConvexHull(
        this.xs.toArray(new Double[this.xs.size()]),
        this.ys.toArray(new Double[this.ys.size()]));
    return convexHull;
  }

}
