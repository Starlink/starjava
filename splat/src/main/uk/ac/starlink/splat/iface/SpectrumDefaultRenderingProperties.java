/**
 * 
 */
package uk.ac.starlink.splat.iface;

import uk.ac.starlink.splat.data.SpecData;

/**
 * @author David Andresic
 *
 * Default rendering properties for spectrum.
 */
public class SpectrumDefaultRenderingProperties extends DefaultRenderingProperties {

	@Override
	public int getPlotStyle() {
		return SpecData.POLYLINE;
	}

	@Override
	public int getPointType() {
		return 0; // dot
	}

	@Override
	public double getPointSize() {
		return 5.0;
	}

}
