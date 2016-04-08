/**
 * 
 */
package uk.ac.starlink.splat.iface;

import uk.ac.starlink.splat.data.ObjectTypeEnum;
import uk.ac.starlink.splat.data.SpecData;

/**
 * @author David Andresic
 *
 * Factory for default rendering settings of the given object type.
 * 
 */
public class DefaultRenderingPropertiesFactory {

	private static ObjectTypeEnum defaultObjectType = ObjectTypeEnum.SPECTRUM;
	
	private static SpectrumDefaultRenderingProperties spectrumPropertiesInstance = null;
	private static TimeseriesDefaultRenderingProperties timeseriesPropertiesInstance = null;
	
	public static DefaultRenderingProperties create(SpecData data){
		ObjectTypeEnum objectType = null;
		
		if (data == null) { // e.g. initialization
			objectType = defaultObjectType;
		}
		
		if (objectType == null) {
			objectType = data.getObjectType();
		}
		
		if (objectType == null) { // not properly initialized data
			objectType = defaultObjectType;
		}
		
		return createInternal(objectType);
	}
	
	private static DefaultRenderingProperties createInternal(ObjectTypeEnum objectType){
		switch(objectType){
			case SPECTRUM:
				if (spectrumPropertiesInstance == null) {
					spectrumPropertiesInstance = new SpectrumDefaultRenderingProperties();
				}
				return spectrumPropertiesInstance;
			case TIMESERIES:
				if (timeseriesPropertiesInstance == null) {
					timeseriesPropertiesInstance = new TimeseriesDefaultRenderingProperties();
				}
				return timeseriesPropertiesInstance;
			default:
				throw new IllegalStateException(String.format("Unsupported object type: %s", objectType));
		}
	}
}
