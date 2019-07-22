/**
 * 
 */
package uk.ac.starlink.splat.data;

/**
 * Enum of possible object types recognized by SPLAT.
 * 
 * TODO: This is a hacky version for timeseries
 * 
 * @author Andresic
 *
 */
public enum ObjectTypeEnum {
	SPECTRUM,
	TIMESERIES,
	UNKNOWN            // splat will try to open it as spectrum.
}
