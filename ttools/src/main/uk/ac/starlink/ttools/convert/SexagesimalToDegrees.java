package uk.ac.starlink.ttools.convert;

import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.func.CoordsDegrees;

/**
 * Converts between Strings in sexagesimal format and numeric values in
 * degrees.
 *
 * @author   Mark Taylor
 * @since    24 Feb 2006
 */
public class SexagesimalToDegrees implements ValueConverter {

    private final boolean hours_;
    private final ValueInfo inInfo_;
    private final DefaultValueInfo outInfo_;

    /**
     * @param  sexInfo   info for data in sexagesimal format
     * @param  hours   true for H:M:S, false for D:M:S
     */
    public SexagesimalToDegrees( ValueInfo sexInfo, boolean hours ) {
        if ( ! String.class.isAssignableFrom( sexInfo.getContentClass() ) ) {
            throw new IllegalArgumentException(
                "Input data must be String, not "
              + sexInfo.getContentClass().getName() );
        }
        inInfo_ = sexInfo;
        hours_ = hours;
        outInfo_ = new DefaultValueInfo( sexInfo );
        outInfo_.setContentClass( Double.class );
        outInfo_.setUnitString( "deg" );
        outInfo_.setNullable( true );
    }

    public ValueInfo getInputInfo() {
        return inInfo_;
    }

    public ValueInfo getOutputInfo() {
        return outInfo_;
    }

    public Object convert( Object in ) {
        if ( in instanceof String ) {
            String sex = ((String) in).trim();
            if ( sex.length() > 0 ) {
                double deg = hours_ ? CoordsDegrees.hmsToDegrees( sex )
                                    : CoordsDegrees.dmsToDegrees( sex );
                return Double.valueOf( deg );
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    public Object unconvert( Object out ) {
        if ( out instanceof Number ) {
            double deg = ((Number) out).doubleValue();
            if ( Double.isNaN( deg ) || Double.isInfinite( deg ) ) {
                return null;
            }
            else {
                return hours_ ? CoordsDegrees.degreesToHms( deg, 3 )
                              : CoordsDegrees.degreesToDms( deg, 2 );
            }
        }
        else {
            return null;
        }
    }

    public String toString() {
        return hours_ ? "HMS->degrees"
                      : "DMS->degrees";
    }
}
