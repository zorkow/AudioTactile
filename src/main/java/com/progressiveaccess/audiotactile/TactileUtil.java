/**
 * 
 */
package com.progressiveaccess.audiotactile;

import org.w3c.dom.svg.SVGAnimatedLength;
import java.util.List;
import org.w3c.dom.svg.SVGLineElement;
import java.util.ArrayList;
import org.w3c.dom.svg.SVGRectElement;
import org.w3c.dom.svg.SVGPolygonElement;
import org.w3c.dom.svg.SVGPolylineElement;
import org.w3c.dom.svg.SVGPointList;
import org.w3c.dom.svg.SVGTransformList;
import org.w3c.dom.svg.SVGPoint;
import org.w3c.dom.svg.SVGTransform;

/**
 * @author sorge
 *
 */
public final class TactileUtil {

  static Double getValue(final SVGAnimatedLength length) {
    return Double.parseDouble("" + length.getBaseVal().getValue());
  }

  static Double getValue(final Float length) {
    return Double.parseDouble("" + length);
  }

  static List<SVGPoint> getPoints(final SVGLineElement line) {
    List<SVGPoint> newPoints = new ArrayList<>();
    final SVGTransformList transforms = line.getTransform().getBaseVal();
    final Float x1 = TactileUtil.getValue(line.getX1()).floatValue();
    final Float y1 = TactileUtil.getValue(line.getY1()).floatValue();
    final Float x2 = TactileUtil.getValue(line.getX2()).floatValue();
    final Float y2 = TactileUtil.getValue(line.getY2()).floatValue();
    SVGPoint newPoint = line.getOwnerSVGElement().createSVGPoint();
    newPoint.setX(x1); newPoint.setY(y1);
    newPoints.add(transformPoint(transforms, newPoint));
    newPoint = line.getOwnerSVGElement().createSVGPoint();
    newPoint.setX(x2); newPoint.setY(y2);
    newPoints.add(transformPoint(transforms, newPoint));
    return newPoints;
  }
  
  static List<SVGPoint> getPoints(SVGRectElement rect) {
    final List<SVGPoint> newPoints = new ArrayList<>();
    final SVGTransformList transforms = rect.getTransform().getBaseVal();
    final Float x = TactileUtil.getValue(rect.getX()).floatValue();
    final Float y = TactileUtil.getValue(rect.getY()).floatValue();
    final Float h = TactileUtil.getValue(rect.getHeight()).floatValue();
    final Float w = TactileUtil.getValue(rect.getWidth()).floatValue();
    SVGPoint newPoint = rect.getOwnerSVGElement().createSVGPoint();
    newPoint.setX(x + w); newPoint.setY(y + h);
    newPoints.add(transformPoint(transforms, newPoint));
    newPoint = rect.getOwnerSVGElement().createSVGPoint();
    newPoint.setX(x + w); newPoint.setY(y);
    newPoints.add(transformPoint(transforms, newPoint));
    newPoint = rect.getOwnerSVGElement().createSVGPoint();
    newPoint.setX(x); newPoint.setY(y + h);
    newPoints.add(transformPoint(transforms, newPoint));
    newPoint = rect.getOwnerSVGElement().createSVGPoint();
    newPoint.setX(x); newPoint.setY(y);
    newPoints.add(transformPoint(transforms, newPoint));
    return newPoints;
  }
  
  static List<SVGPoint> getPoints(final SVGPolygonElement polygon) {
    final List<SVGPoint> newPoints = new ArrayList<>();
    final SVGPointList points = polygon.getPoints();
    final SVGTransformList transforms = polygon.getTransform().getBaseVal();
    for (Integer j = 0; j < points.getNumberOfItems(); j++) {
      SVGPoint point = points.getItem(j);
      point = TactileUtil.transformPoint(transforms, point);
      newPoints.add(point);
    }
    return newPoints;
  }

  
  // TODO(sorge): Combine functions via common interface.
  static List<SVGPoint> getPoints(final SVGPolylineElement polyline) {
    final List<SVGPoint> newPoints = new ArrayList<>();
    final SVGPointList points = polyline.getPoints();
    final SVGTransformList transforms = polyline.getTransform().getBaseVal();
    for (Integer j = 0; j < points.getNumberOfItems(); j++) {
      SVGPoint point = points.getItem(j);
      point = TactileUtil.transformPoint(transforms, point);
      newPoints.add(point);
    }
    return newPoints;
  }

  static private SVGPoint transformPoint(final SVGTransformList transforms,
                                         SVGPoint point) {
    for (Integer i = transforms.getNumberOfItems() - 1; i >= 0 ; i--) {
      point = point.matrixTransform(transforms.getItem(i).getMatrix());
    }
    return point;
  }
  
}
