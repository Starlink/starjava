package uk.ac.starlink.hdx;

import uk.ac.starlink.hdx.array.NDArray;
import org.w3c.dom.Element;

/**
 * Provides Ndx services for BridgeNdx.
 *
 * @author Mark Taylor
 * @author Peter W. Draper
 */
interface NdxImpl {
    String getTitle();
    byte getBadBits();
    NDArray getImage();
    NDArray getVariance();
    NDArray getQuality();
    Element getWCSElement();
    boolean hasImage();
    boolean hasVariance();
    boolean hasQuality();
    boolean hasWCS();
}
