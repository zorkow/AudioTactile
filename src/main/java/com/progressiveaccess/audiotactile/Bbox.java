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
 * @file   Bbox.java
 * @author Volker Sorge
 *          <a href="mailto:V.Sorge@progressiveaccess.com">Volker Sorge</a>
 * @date   Tue Jul  4 00:49:59 2017
 * 
 * @brief  Bounding box generation from SVG elements.
 * 
 * 
 */

package com.progressiveaccess.audiotactile;

import org.w3c.dom.svg.SVGAnimatedPoints;
import org.w3c.dom.svg.SVGLineElement;
import org.w3c.dom.svg.SVGPoint;
import org.w3c.dom.svg.SVGPointList;
import org.w3c.dom.svg.SVGRectElement;

import javax.vecmath.Point2d;
import java.util.List;
import org.w3c.dom.svg.SVGPolygonElement;
import org.w3c.dom.svg.SVGPolylineElement;

/**
 * Class for bounding boxes generated from multiple SVG elements.
 */
public class Bbox {

  // BBOX
  private Point2d topLeft = new Point2d();
  private Point2d bottomRight = new Point2d();
  private Boolean init = true;

  public Point2d getTop() {
    return topLeft;
  }

  public Point2d getBottom() {
    return bottomRight;
  }

  private void combineBbox(final Point2d top, final Point2d bot) {
    if (this.init) {
      this.topLeft.set(top);
      this.bottomRight.set(bot);
      this.init = false;
    }
    this.topLeft.set(Math.min(this.topLeft.x, top.x),
                     Math.min(this.topLeft.y, top.y));
    this.bottomRight.set(Math.max(this.bottomRight.x, bot.x),
                         Math.max(this.bottomRight.y, bot.y));
  }

  public void addPoints(final SVGLineElement line) {
    final Double x1 = TactileUtil.getValue(line.getX1());
    final Double y1 = TactileUtil.getValue(line.getY1());
    final Double x2 = TactileUtil.getValue(line.getX2());
    final Double y2 = TactileUtil.getValue(line.getY2());
    this.combineBbox(new Point2d(Math.min(x1, x2), Math.min(y1, y2)),
                     new Point2d(Math.max(x1, x2), Math.max(y1, y2)));
  }

  public void addPoints(final SVGRectElement rect) {
    final Double x1 = TactileUtil.getValue(rect.getX());
    final Double y1 = TactileUtil.getValue(rect.getY());
    final Double x2 = x1 + TactileUtil.getValue(rect.getWidth());
    final Double y2 = y1 + TactileUtil.getValue(rect.getHeight());
    this.combineBbox(new Point2d(x1, y1), new Point2d(x2, y2));
  }

  public void addPoints(final SVGPolygonElement polygon) {
    final List<SVGPoint> points = TactileUtil.getPoints(polygon);
    this.addPoints(points);
  }

  public void addPoints(final SVGPolylineElement polyline) {
    final List<SVGPoint> points = TactileUtil.getPoints(polyline);
    this.addPoints(points);
  }

  public void addPoints(final List<SVGPoint> points) {
    if (points.size() < 1) {
      return;
    }
    SVGPoint point = points.get(0);
    Double minX = TactileUtil.getValue(point.getX());
    Double minY = TactileUtil.getValue(point.getY());
    Double maxX = minX;
    Double maxY = minY;
    for (Integer j = 1; j < points.size(); j++) {
      point = points.get(j);
      final Double x = TactileUtil.getValue(point.getX());
      final Double y = TactileUtil.getValue(point.getY());
      minX = Math.min(x, minX);
      minY = Math.min(y, minY);
      maxX = Math.max(x, maxX);
      maxY = Math.max(y, maxY);
    }
    this.combineBbox(new Point2d(minX, minY), new Point2d(maxX, maxY));
  }

}
