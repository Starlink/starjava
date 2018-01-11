package uk.ac.starlink.ttools.plot2.layer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines units in which angular extents can be specified.
 *
 * @author   Mark Taylor
 * @since    2 Aug 2017
 */
public enum AngleUnit {

    /** Radians. */
    RADIAN( "radian", "radians", Math.PI / 180.,
            new String[] { "rad", "radian", "radians", } ),

    /** Degrees. */
    DEGREE( "degree", "degrees", 1.0,
            new String[] { "deg", } ),

    /** Minutes. */
    MINUTE( "minute", "arcminutes", 1. / 60.,
            new String[] { "min", "minutes", "mins" } ),

    /** Arcseconds. */
    ARCSEC( "arcsec", "arcseconds", 1. / 3600.,
            new String[] { "as", "arcsecs", "arcsecond", "arcseconds", } ),

    /** Milliarcseconds. */
    MAS( "mas", "milli-arcseconds", 1. / 3.6e6,
         new String[] { "milliarcsec", "milliarcsecond", "milliarcseconds" } ),

    /** Microarcseconds. */
    UAS( "uas", "micro-arcseconds", 1. / 3.6e9,
         new String[] { "microarcsec", "microarcsecond", "microarcseconds" } );

    private final String name_;
    private final String fullName_;
    private final double valueInDegrees_;
    private final Collection<String> allnames_;

    /**
     * Constructor.
     *
     * @param  name   canonical name for parameter values
     * @param  fullName  human-readable name for documentation
     * @param  valueInDegrees  the size of one of these units in degrees
     * @param  otherNames  list of zero or more strings other than
     *                     <code>name</code> and <code>fullName</code>
     *                     that should be recognised to correspond
     *                     to this unit
     */
    AngleUnit( String name, String fullName, double valueInDegrees,
               String[] otherNames ) {
        name_ = name;
        fullName_ = fullName;
        valueInDegrees_ = valueInDegrees;
        Set<String> allnames = new HashSet<String>();
        allnames.add( name );
        allnames.add( fullName );
        for ( String n : otherNames ) {
            allnames.add( n.toLowerCase() );
        }
        allnames_ = Collections.unmodifiableSet( allnames );
    }

    /**
     * Returns the value of this unit in degrees.
     *
     * @return   value in degrees
     */
    public double getValueInDegrees() {
        return valueInDegrees_;
    }

    /**
     * Returns the canonical name of this unit.
     *
     * @return   unit name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns the full-text name of this unit.
     *
     * @return  full unit name
     */
    public String getFullName() {
        return fullName_;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns the AngleUnit instance that corresponds to
     * a user-specified unit name.
     *
     * @param  name  user-specified name
     * @return   named AngleUnit, or null
     */
    public static AngleUnit getNamedUnit( String name ) {
        if ( name != null ) {
            for ( AngleUnit u : values() ) {
                if ( u.allnames_.contains( name.toLowerCase() ) ) { 
                    return u;
                }
            }
        }
        return null;
    }
}
