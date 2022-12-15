package uk.ac.starlink.ttools.plot2.geom;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.DVMap;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import uk.ac.starlink.ttools.func.Times;
import uk.ac.starlink.ttools.jel.JELUtils;

/**
 * Function operating on a time value as used in a Time plot.
 * The input value of the function is in unix seconds.
 * A number of variables representing time and based on that input
 * value are available, currently: "mjd", "jd", "unixSec" and "decYear".
 * Typically the expression will simply be one of these terms.
 *
 * @author   Mark Taylor
 * @since    15 Dec 2022
 */
public class TimeJELFunction implements DoubleUnaryOperator {

    private final String fexpr_;
    private final TimeResolver tResolver_;
    private final CompiledExpression fCompex_;
    private final Object[] args_;
    private static final Map<String,TimeQuantity> tqMap_ =
        createTimeQuantityMap();

    /**
     * Constructor.
     *
     * @param  fexpr   JEL expression in terms of a time variable
     */
    public TimeJELFunction( String fexpr ) throws CompilationException {
        fexpr_ = fexpr;
        Class<?>[] staticLib =
            JELUtils.getStaticClasses().toArray( new Class<?>[ 0 ] );
        tResolver_ = new TimeResolver();
        Class<?>[] dynamicLib = new Class<?>[] { tResolver_.getClass() };
        Library lib =
            JELUtils.createLibrary( staticLib, dynamicLib, tResolver_ );
        fCompex_ = Evaluator.compile( fexpr, lib, double.class );
        args_ = new Object[] { tResolver_ };
    }

    /**
     * Function of time in seconds since the Unix epoch.
     *
     * @param  unixSec  unix seconds
     * @return   time representation as defined by this function
     */
    public double applyAsDouble( double unixSec ) {
        tResolver_.setUnixSec( unixSec );
        try {
            return fCompex_.evaluate_double( args_ );
        }
        catch ( Throwable e ) {
            return Double.NaN;
        }
    }

    /**
     * Returns the text of the function expression.
     *
     * @return  function expression
     */
    public String getExpression() {
        return fexpr_;
    }

    /**
     * Returns a list of TimeQuantity objects that can be referred to
     * by name from expressions used by this class.
     *
     * @return  ordered list of time quantities
     */
    public static TimeQuantity[] getTimeQuantities() {
        return tqMap_.values().toArray( new TimeQuantity[ 0 ] );
    }

    /**
     * Creates a map in which the values are all the known TimeQuantity
     * instances.  The keys are lower-cased versions of the TimeQuantity name.
     *
     * @return  name -&gt; quantity map
     */
    private static Map<String,TimeQuantity> createTimeQuantityMap() {
        final double daysPerSec = 1. / ( 60 * 60 * 24 );
        final double mjdEpoch = 40587.0;
        final double jdEpoch = 2440587.5;
        TimeQuantity[] tqs = {
            new TimeQuantity( "mjd", "Modified Julian Date",
                              unixSec -> unixSec * daysPerSec + mjdEpoch ),
            new TimeQuantity( "jd", "Julian Day",
                              unixSec -> unixSec * daysPerSec + jdEpoch ),
            new TimeQuantity( "decYear", "decimal year CE",
                              unixSec ->
                                  Times.mjdToDecYear(
                                      Times.unixMillisToMjd(
                                          (long) (unixSec * 1000) ) ) ),
            new TimeQuantity( "unixSec", "seconds since 1970-01-01T00:00:00",
                              unixSec -> unixSec ),
        };
        Map<String,TimeQuantity> map = new LinkedHashMap<>();
        for ( TimeQuantity tq : tqs ) {
            map.put( tq.getName().toLowerCase(), tq );
        }
        return Collections.unmodifiableMap( map );
    }

    /**
     * Defines a quantity representing time that can be used in expressions
     * supplied to this class.
     */
    public static class TimeQuantity {
        final String name_;
        final String description_;
        final DoubleUnaryOperator fromUnixSec_;

        /**
         * Constructor.
         *
         * @param  name  variable name
         * @param  description  short plain-text description
         * @param  fromUnixSec  converts from unix seconds to this quantity
         */
        TimeQuantity( String name, String description,
                      DoubleUnaryOperator fromUnixSec ) {
            name_ = name;
            description_ = description;
            fromUnixSec_ = fromUnixSec;
        }

        /**
         * Returns this quantity's name.
         *
         * @return  variable name
         */
        public String getName() {
            return name_;
        }

        /**
         * Returns a short plain-text description of this quantity.
         *
         * @return  description
         */
        public String getDescription() {
            return description_;
        }

        /**
         * Maps a time in unix seconds to the value represented by this
         * quantity.
         *
         * @param  unixSec   seconds since unix epoch
         * @return  time value in this quantity
         */
        public double fromUnixSeconds( double unixSec ) {
            return fromUnixSec_.applyAsDouble( unixSec );
        }
    }

    /**
     * This public class is an implementation detail,
     * not intended for external use.
     */
    public static class TimeResolver extends DVMap {
        private double unixSec_;
        public String getTypeName( String name ) {
            return tqMap_.containsKey( name.toLowerCase() ) ? "Double" : null;
        }
        public double getDoubleProperty( String name ) {
            TimeQuantity tq = tqMap_.get( name.toLowerCase() );
            return tq != null ? tq.fromUnixSeconds( unixSec_ )
                              : Double.NaN;
        }
        private void setUnixSec( double unixSec ) {
            unixSec_ = unixSec;
        }
    }
}
