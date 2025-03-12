package uk.ac.starlink.ttools.plot2;

import java.util.Arrays;
import uk.ac.starlink.ttools.func.Maths;
import uk.ac.starlink.ttools.plot.Rounder;

/**
 * Defines a type of axis scaling.
 * A ScaleType is factory for {@link Scale} instances.
 *
 * @author   Mark Taylor
 * @since    20 Mar 2025
 */
public abstract class ScaleType {

    private final String scaleName_;
    private final Param[] params_;

    /**
     * Linear scale type, no parameters.
     */
    public static final ScaleType LINEAR;

    /**
     * Logarithmic scale type, no parameters.
     */
    public static final ScaleType LOG;

    /**
     * Scale type using so-called symmetric log scaling.
     * It can represent a wide dynamic range of values including negative ones.
     * This idea is copied from Matplotlib's symlog scale.
     * The region around the origin is linear, but outside that region
     * values are scaled logarithmically, or negative logarithmically
     * below zero.
     *
     * <p>Scales have two parameters which must both be &gt;0:
     * <ul>
     * <li><code>linthresh</code>:
     *     defines the linear region (-linthresh..linthresh)
     * <li><code>linscale</code>:
     *     ratio of graphical extent of the region (0,linthresh)
     *     to a decade in the logarithmic region
     * </ul>
     * The function's derivatives are in general discontinuous
     * at +/-<code>linthresh</code>
     */
    public static final ScaleType SYMLOG;

    /**
     * Scale type using inverse hyperbolic sin scaling.
     * The scaling function is <code>x -&gt; asinh(x/a)</code>.
     * This function is asymptotically linear near the origin,
     * and asymptotically logarithmic (or negative logarithmic)
     * far from the origin.
     * The "linear width" parameter <code>a</code>, which must be &gt;0,
     * defines the extent of the quasi-linear region.
     */
    public static final ScaleType ASINH;

    private static final ScaleType[] INSTANCES = {
        LINEAR = new LinearScaleType( "linear", Rounder.LINEAR ),
        LOG = new LogScaleType(),
        ASINH = new AsinhScaleType(),
        SYMLOG = new SymlogScaleType(),
    };

    /** Time scale type, linear but with a custom rounding. */
    public static final ScaleType TIME =
        new LinearScaleType( "time", Rounder.TIME_SECOND );

    private static final Rounder DUMMY_ROUNDER = new Rounder() {
        public double round( double value ) {
            return value;
        }
        public double nextUp( double value ) {
            return value;
        }
        public double nextDown( double value ) {
            return value;
        }
    };

    /**
     * Constructor.
     *
     * @param   scaleName  short name for the scale type
     * @param   params  numeric parameters of this scale type
     */
    protected ScaleType( String scaleName, Param[] params ) {
        scaleName_ = scaleName;
        params_ = params;
    }

    /**
     * Constructs a scale.
     * The supplied parameter values must match those defined by
     * the result of the {#getParams} method.
     * Missing later values are permitted, and will be set to the
     * default value of the relevant parameter.
     *
     * @param  paramValues  numeric arguments for this scale
     * @return  new scale
     * @throws  IllegalArgumentException  if the values are not acceptable
     */
    public abstract Scale createScale( double[] paramValues );

    /**
     * Returns the ordered list of parameter definitions that must be
     * supplied for scales produced by this type.
     * This may be an empty array if the scale type is not parameterised.
     *
     * @return  scale parameter specifications
     */
    public Param[] getParams() {
        return params_.clone();
    }

    /**
     * Returns the name of this type.
     *
     * @return  type name
     */
    public String getName() {
        return scaleName_;
    }

    /**
     * Returns an XML description of this scale type.
     *
     * @return  description in XML-friendly text, but not wrapped in any
     *          element
     */
    public abstract String getDescription();

    @Override
    public String toString() {
        return scaleName_;
    }

    /**
     * Extends a supplied parameter value array using parameter defaults
     * for missing values.
     *
     * @param  args  supplied parameter value array, may be missing values
     * @return   parameter value array with length equal to param count
     */
    double[] fillInArgs( double[] args ) {
        int np = params_.length;
        double[] args1 = new double[ np ];
        for ( int ip = 0; ip < np; ip++ ) {
            args1[ ip ] = args.length > ip ? args[ ip ]
                                           : params_[ ip ].getDefault();
        }
        return args1;
    }

    /**
     * Returns a list of the general-purpose instances of this class.
     *
     * @return  instance list
     */
    public static ScaleType[] getInstances() {
        return INSTANCES.clone();
    }

    /**
     * Returns a ScaleType instance with the given name.
     * This is the inverse of the {@link #getName} method.
     *
     * @param  name  name of the scale type
     * @return  scale type matching the given name, or null
     */
    public static ScaleType fromName( String name ) {
        for ( ScaleType type : INSTANCES ) {
            if ( type.scaleName_.equalsIgnoreCase( name ) ) {
                return type;
            }
        }
        return null;
    }

    /**
     * Logarithm to base ten.
     *
     * @param  x  parameter
     * @return  log10(x)
     */
    private static double log10( double x ) {
        return Math.log10( x );
    }

    /**
     * Exponential to base ten.
     *
     * @param  x  parameter
     * @return   10^x
     */
    private static double pow10( double x ) {
        return Math.pow( 10, x );
    }

    /**
     * Defines a numerical parameter for a ScaleType.
     * To create a Scale instance, a value for each Param needs to be
     * supplied to the ScaleType.
     */
    public static abstract class Param {

        private final String name_;
        private final double dflt_;
        private final String description_;

        /**
         * Constructor.
         *
         * @param  name  parameter name
         * @param  dflt  parameter default value
         * @param  description  short text description of parameter
         */
        public Param( String name, double dflt, String description ) {
            name_ = name;
            dflt_ = dflt;
            description_ = description;
        }

        /**
         * Returns the parameter name.
         *
         * @return  name
         */
        public String getName() {
            return name_;
        }

        /**
         * Returns the parameter default value.
         *
         * @return  default value
         */
        public double getDefault() {
            return dflt_;
        }

        /**
         * Returns a short description of this parameter.
         *
         * @return  description
         */
        public String getDescription() {
            return description_;
        }

        /**
         * Given a value for this parameter,
         * returns a smaller number that would be a suitable value.
         *
         * <p>This is used for the GUI.  The scaling will still work,
         * though the GUI will be suboptimal, if this is implemented
         * as the identity.
         *
         * @param  d  example parameter value
         * @return   round number smaller than <code>d</code>
         */
        public abstract double nextDown( double d );

        /**
         * Given a value for this parameter,
         * returns a larger number that would be a suitable value.
         *
         * <p>This is used for the GUI.  The scaling will still work,
         * though the GUI will be suboptimal, if this is implemented
         * as the identity.
         *
         * @param  d  example parameter value
         * @return  round number greater than <code>d</code>
         */
        public abstract double nextUp( double d );

        @Override
        public String toString() {
            return name_;
        }
    }

    /**
     * Partial implementation of Scale.
     */
    private static abstract class AbstractScale implements Scale {

        private final ScaleType type_;
        private final double[] paramValues_;

        /**
         * Constructor.
         *
         * @param  type  scale type
         * @param  params  scale parameter values
         */
        protected AbstractScale( ScaleType type, double[] paramValues ) {
            type_ = type;
            paramValues_ = type.fillInArgs( paramValues );
        }

        public ScaleType getScaleType() {
            return type_;
        }

        public double[] getParamValues() {
            return paramValues_;
        }

        @Override
        public int hashCode() {
            int code = 992210;
            code = 23 * code + type_.hashCode();
            code = 23 * code + Arrays.hashCode( paramValues_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof AbstractScale ) {
                AbstractScale other = (AbstractScale) o;
                return this.type_ == other.type_
                    && Arrays.equals( this.paramValues_, other.paramValues_ );
            }
            else {
                return false;
            }
        }

        @Override
        public String toString() {
            String typeName = type_.getName();
            return paramValues_.length > 0
                 ? new ParsedFunctionCall( typeName, paramValues_ ).toString()
                 : typeName;
        }
    }

    /**
     * ScaleType implementation where the forward transformation is
     * the identity operation.
     */
    private static class LinearScaleType extends ScaleType {
        private final Scale scale_;
        LinearScaleType( String name, Rounder rounder ) {
            super( name, new Param[ 0 ] );
            scale_ = new AbstractScale( this, new double[ 0 ] ) {
                public boolean isPositiveDefinite() {
                    return false;
                }
                public boolean isLinear() {
                    return true;
                }
                public Rounder getScaleRounder() {
                    return rounder;
                }
                public double dataToScale( double d ) {
                    return d;
                }
                public double scaleToData( double s ) {
                    return s;
                }
                public String dataToScaleExpression( String var ) {
                    return var;
                }
            };
        }
        public String getDescription() {
            return "Linear scale";
        }
        public Scale createScale( double[] args ) {
            return scale_;
        }
    }

    /**
     * ScaleType implementation where the forward transformation is
     * the logarithm to base 10.
     */
    private static class LogScaleType extends ScaleType {
        private final Scale scale_;
        LogScaleType() {
            super( "log", new Param[ 0 ] );
            Rounder rounder = new Rounder() {
                public double round( double value ) {
                    return log10( Rounder.LOG.round( pow10( value ) ) );
                }
                public double nextUp( double value ) {
                    return log10( Rounder.LOG.nextUp( pow10( value ) ) );
                }
                public double nextDown( double value ) {
                    return log10( Rounder.LOG.nextDown( pow10( value ) ) );
                }
            };
            scale_ = new AbstractScale( this, new double[ 0 ] ) {
                public boolean isPositiveDefinite() {
                    return true;
                }
                public boolean isLinear() {
                    return false;
                }
                public Rounder getScaleRounder() {
                    return rounder;
                }
                public double dataToScale( double d ) {
                    return log10( d );
                }
                public double scaleToData( double s ) {
                    return pow10( s );
                }
                public String dataToScaleExpression( String var ) {
                    return "log10(" + var + ")";
                }
            };
        }
        public String getDescription() {
            return "Logarithmic scale";
        }
        public Scale createScale( double[] args ) {
            return scale_;
        }
    }

    /**
     * ScaleType implementation where the forward transformation
     * is the so-called symmetric log function.
     */
    private static class SymlogScaleType extends ScaleType {
        private static final Param LINTHRESH =
            new Log10Param( "t", 1, "Limit of linear region (linthresh)" );
        private static final Param LINSCALE =
            new LinscaleParam( "s", 1,
                               "Extent of positive linear region in decades"
                             + " (linscale)");
        
        SymlogScaleType() {
            super( "symlog", new Param[] { LINTHRESH, LINSCALE } );
        }
        public String getDescription() {
            return String.join( "\n",
                "Symmetric logarithmic scale.",
                "This is linear near the origin, and positive/negative",
                "logarithmic for larger values.",
                "It has two parameters,",
                "<code>t</code> (a.k.a. <code>linthresh</code>) and",
                "<code>s</code> (a.k.a. <code>linscale</code>).",
                "Both parameters must be strictly positive.",
                "The linear region is in the range",
                "<code>-t&lt;x&lt;+t</code>,",
                "and the graphical extent of the region from the origin",
                "to <code>t</code> is the same as",
                "<code>s</code> decades in the logarithmic region."
            );
        }
        public Scale createScale( double[] args ) {
            args = fillInArgs( args );
            double linthresh = args[ 0 ];
            double linscale = args[ 1 ];
            return new SymlogScale( linthresh, linscale );
        }

        /**
         * Implements Scale for the symlog scaling.
         */
        private class SymlogScale extends AbstractScale {
            private final double linthresh_;
            private final double linscale_;
            private final double log10thresh_;
            private final double scaleOverThresh_;
            private final double threshOverScale_;

            /**
             * Constructor.
             *
             * @param   linthresh   threshold of linear region
             * @param   linscale    relative extent of linear region
             */
            SymlogScale( double linthresh, double linscale ) {
                super( SymlogScaleType.this,
                       new double[] { linthresh, linscale } );
                if ( ! ( linthresh > 0 && linscale > 0 ) ) {
                    throw new IllegalArgumentException( "Non-positive symlog "
                                                      + "arguments" );
                   
                }
                linthresh_ = linthresh;
                linscale_ = linscale;
                log10thresh_ = log10( linthresh );
                scaleOverThresh_ = linscale / linthresh;
                threshOverScale_ = linthresh / linscale;
            }

            public boolean isPositiveDefinite() {
                return false;
            }

            public boolean isLinear() {
                return false;
            }

            public Rounder getScaleRounder() {
                return DUMMY_ROUNDER;
            }

            public double dataToScale( double d ) {
                if ( d < -linthresh_ ) {
                    return -linscale_ - log10( -d ) + log10thresh_;
                }
                else if ( d > linthresh_ ) {
                    return linscale_ + log10( d ) - log10thresh_;
                }
                else {
                    return d * scaleOverThresh_;
                }
            }

            public double scaleToData( double s ) {
                if ( s < -linscale_ ) {
                    return -linthresh_ * pow10( -s - linscale_ );
                }
                else if ( s > linscale_ ) {
                    return linthresh_ * pow10( s - linscale_ );
                }
                else {
                    return s * threshOverScale_;
                }
            }

            public String dataToScaleExpression( String var ) {
                return new StringBuffer()
                      .append( "symlog(" )
                      .append( linthresh_ )
                      .append( "," )
                      .append( linscale_ )
                      .append( "," )
                      .append( var )
                      .append( ")" )
                      .toString();
            }
        }
    }

    /**
     * ScaleType implementation where the forward transformation is
     * the asinh function.
     */
    private static class AsinhScaleType extends ScaleType {
        private static final Param A =
            new Log10Param( "a", 1.0,
                            "Aprox limit of linear-like region" );
        AsinhScaleType() {
            super( "asinh", new Param[] { A } );
        }
        public String getDescription() {
            return String.join( "\n",
                "Scaling based on inverse hyperbolic sin function.",
                "This is approximately linear near the origin,",
                "and approximately logarithmic far from it.",
                "It has one parameter, the \"linear width\" <code>a</code>,",
                "which controls the extent of the linear-like region,",
                "and must be strictly positive.",
                "The function is <code>x-&gt;asinh(x/a)</code>."
            );
        }
        public Scale createScale( double[] args ) {
            double a = fillInArgs( args )[ 0 ];
            if ( ! ( a > 0 ) ) {
                throw new IllegalArgumentException( "Non-positive asinh "
                                                  + "argument" );
            }
            double a1 = 1.0 / a;
            return new AbstractScale( this, args ) {
                public boolean isPositiveDefinite() {
                    return false;
                }
                public boolean isLinear() {
                    return false;
                }
                public Rounder getScaleRounder() {
                    return DUMMY_ROUNDER;
                }
                public double dataToScale( double d ) {
                    return asinh( d * a1 );
                }
                public double scaleToData( double s ) {
                    return a * sinh( s );
                }
                public String dataToScaleExpression( String var ) {
                    return new StringBuffer()
                          .append( "asinh(" )
                          .append( var )
                          .append( "/" )
                          .append( a )
                          .append( ")" )
                          .toString();
                }
            };
        }

        /**
         * Hyperbolic sin.
         *
         * @param  x  parameter
         * @return  sinh(x)
         */
        private static double sinh( double x ) {
            return Math.sinh( x );
        }

        /**
         * Inverse hyperbolic sin.
         *
         * @param  x  parameter
         * @return  asinh(x)
         */
        private static double asinh( double x ) {
            return Maths.asinh( x );
        }
    }

    /**
     * Parameter with logarithmic steps.
     */
    private static class Log10Param extends Param {

        /**
         * Constructor.
         *
         * @param  name  parameter name
         * @param  dflt  default value
         * @param  descrip  description
         */
        Log10Param( String name, double dflt, String descrip ) {
            super( name, dflt, descrip );
        }
        public double nextDown( double d ) {
            BumpNumber num = new BumpNumber( d );
            num.bumpDown();
            return num.getValue();
        }
        public double nextUp( double d ) {
            BumpNumber num = new BumpNumber( d );
            num.bumpUp();
            return num.getValue();
        }
    }

    /**
     * Param implementation that behaves like Log10Param below 1,
     * but steps up arithmetically by 0.5 above 1.
     */
    private static class LinscaleParam extends Log10Param {
        final double step1_;

        /**
         * Constructor.
         *
         * @param  name  parameter name
         * @param  dflt  default value
         * @param  descrip  description
         */
        LinscaleParam( String name, double dflt, String descrip ) {
            super( name, dflt, descrip );
            step1_ = 0.5;
        }
        public double nextDown( double d ) {
            return d < 1 + step1_
                 ? super.nextDown( d )
                 : ( Math.ceil( d / step1_ ) - 1 ) * step1_;
        }
        public double nextUp( double d ) {
            return d < 1
                 ? super.nextUp( d )
                 : ( Math.floor( d / step1_ ) + 1 ) * step1_;
        }
    }

    /**
     * Mutable utility class for moving through a sequence of round numbers
     * logarithmically.
     */
    private static class BumpNumber {
        double mant_;
        double exp_;

        /**
         * Constructor.
         *
         * @param  d  value
         */
        BumpNumber( double d ) {
            d = Math.abs( d );
            exp_ = Math.floor( log10( d ) );
            mant_ = d / pow10( exp_ );
        }

        /**
         * Returns the current value.
         *
         * @return value
         */
        double getValue() {
            return mant_ * pow10( exp_ );
        }


        /**
         * Increases the value to the next highest round number.
         */
        void bumpUp() {
            if ( mant_ < 2 ) {
                mant_ = 2;
            }
            else if ( mant_ < 5 ) {
                mant_ = 5;
            }
            else {
                mant_ = 1;
                exp_++;
            }
        }

        /**
         * Decreases the value to the next lowest round number.
         */
        void bumpDown() {
            if ( mant_ > 5 ) {
                mant_ = 5;
            }
            else if ( mant_ > 2 ) {
                mant_ = 2;
            }
            else if ( mant_ > 1 ) {
                mant_ = 1;
            }
            else {
                mant_ = 5;
                exp_--;
            }
        }

        @Override
        public String toString() {
            return mant_ + "e" + exp_;
        }
    }
}
