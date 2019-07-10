/**
 * 
 */
package uk.ac.starlink.splat.plot.behavior;

import java.io.Serializable;

import uk.ac.starlink.splat.data.ObjectTypeEnum;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.plot.DivaPlot;

/**
 * Behavior for inverting the Y axis in case of magnitudes.
 * 
 * @author and146
 *
 */
public class MagnitudeAxisInvertingBehavior implements Serializable, PlotBehavior {
	
	private static final long serialVersionUID = 1L;

	@Override
	public void setDataLimits(DivaPlot plot) {
		boolean foundAtLeastOneMagnitudes = false;
		// inverse Y axis for timeseries
        if (plot.getSpecDataComp() != null) {
        	if (plot.getSpecDataComp().get() != null) {
        		for (SpecData specData : plot.getSpecDataComp().get()) {
        			if (ObjectTypeEnum.TIMESERIES.equals(specData.getObjectType())) {
        				if (specData.getDataUnits() != null && specData.getDataUnits().contains("mag")) {
        					foundAtLeastOneMagnitudes = true;
        					plot.getDataLimits().setYFlipped(true);
            				break;
        				}
        			}
        		}
        	}
        }
        
        // if magnitudes not found (or removed etc.), set the original state
        if (!foundAtLeastOneMagnitudes) {
        	plot.getDataLimits().setYFlipped(false);
        }
	}

}
