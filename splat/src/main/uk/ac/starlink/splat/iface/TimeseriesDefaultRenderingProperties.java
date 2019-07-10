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
public class TimeseriesDefaultRenderingProperties extends DefaultRenderingProperties {

	@Override
	public int getPlotStyle() {
		return SpecData.POINT;		
	}

	@Override
	public int getPointType() {
		return 1; // cross
	}

	@Override
	public double getPointSize() {
		return 10.0;
	}
	
}
