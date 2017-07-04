/**
 * 
 */
package com.progressiveaccess.audiotactile;

import org.w3c.dom.svg.SVGAnimatedLength;

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

    
  
}
