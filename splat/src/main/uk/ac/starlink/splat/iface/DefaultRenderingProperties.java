/**
 * 
 */
package uk.ac.starlink.splat.iface;

import java.awt.Color;

/**
 * @author David Andresic
 *
 * An abstract class for rendering properties common for all
 * supported objects. Use @link{DefaultRenderingPropertiesFactory}
 * to instatiate.
 * 
 */
public abstract class DefaultRenderingProperties {
	private double alpha = 1.0;
    private double lineStyle = 1.0;
    private int errorColour = Color.red.getRGB();
    private int errorFrequency = 1;
    private int errorScale = 1;
    private int lineColour = Color.blue.getRGB();
    private int lineThickness = 1;
    
	public final double getAlpha() {
		return alpha;
	}
	public final double getLineStyle() {
		return lineStyle;
	}
	public final int getErrorColour() {
		return errorColour;
	}
	public final int getErrorFrequency() {
		return errorFrequency;
	}
	public final int getErrorScale() {
		return errorScale;
	}
	public final int getLineColour() {
		return lineColour;
	}
	public final int getLineThickness() {
		return lineThickness;
	}
	public abstract int getPlotStyle();
	
	public abstract int getPointType();
    
	public abstract double getPointSize();
	
}
