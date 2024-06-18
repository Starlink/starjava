package uk.ac.starlink.ttools.mode;

import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.Tables;

/**
 * Accumulates statistics for the values in a given column.
 * This object gets treated as a bean; its properties get turned into
 * table columns.  The property accessors (public <code>get*</code> methods)
 * will return formatted strings based on the data which have been 
 * submitted to the {@link #acceptDatum} method.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Mar 2005
 */
public abstract class ColStats {

    private final String name_;

    /**
     * Constructs a new stats object.
     *
     * @param  colInfo   info on the values which will be passed to the
     *                   {@link #acceptDatum} method
     */
    protected ColStats( ColumnInfo colInfo ) {
        name_ = colInfo.getName();
    }

    /**
     * Returns column name.
     *
     * @return   column
     */
    public String getColumn() {
        return name_;
    }

    /**
     * Returns formatted mean value of accepted data.
     *
     * @return  mean
     */
    public String getMean() {
        return formatDouble( getMeanValue() );
    }

    /**
     * Returns formatted variance of accepted data.
     *
     * @return  variance
     */
    public String getVariance() {
        return formatDouble( getVarianceValue() );
    }

    /**
     * Returns formatted standard deviation of accepted data.
     *
     * @return  standard deviation
     */
    public String getStdDev() {
        double var = getVarianceValue();
        return formatDouble( var >= 0 ? Math.sqrt( var ) : Double.NaN );
    }

    /**
     * Returns formatted minimum value of accepted data.
     *
     * @return  minimum
     */
    public String getMin() {
        return formatObject( getMinimumValue() );
    }

    /**
     * Returns formatted maximum value of accepted data.
     *
     * @return  maximum
     */
    public String getMax() {
        return formatObject( getMaximumValue() );
    }

    /**
     * Returns formatted number of non-blank values in accepted data.
     *
     * @return   good value count
     */
    public String getGood() {
        long ngood = getGoodCountValue();
        return ngood < 0 ? null : formatLong( getGoodCountValue() );
    }

    private static String formatDouble( double val ) {
        return Double.isNaN( val ) ? null : Float.toString( (float) val );
    }

    private static String formatObject( Object val ) {
        if ( val == null ) {
            return null;
        }
        else if ( val instanceof Double ) {
            return Float.toString( ((Double) val).floatValue() );
        }
        else {
            return val.toString();
        }
    }

    private static String formatLong( long val ) {
        return Long.toString( val );
    }

    /**
     * Data are submitted to this statistics accumulator using this method.
     *
     * @param   value   data value to be accumulated into totals
     */
    public abstract void acceptDatum( Object value );

    /**
     * Adds the accumulated content of a second ColStats object to this one.
     *
     * @param  other  compatible ColStats object
     */
    public abstract void addStats( ColStats other );

    /**
     * Returns the mean of the accumulated data.
     *
     * @return  mean
     */
    protected abstract double getMeanValue();

    /**
     * Returns the variance of the accumulated data.
     *
     * @return  variance
     */
    protected abstract double getVarianceValue();

    /**
     * Returns the minimum of the accumulated data.
     *
     * @return  min
     */
    protected abstract Object getMinimumValue();

    /**
     * Returns the maximum of the accumulated data.
     *
     * @return max
     */
    protected abstract Object getMaximumValue();

    /**
     * Returns the number of good values in the accumulated data.
     *
     * @return  good value count
     */
    protected abstract long getGoodCountValue();

    /**
     * Factory method which returns a new ColStats value suitable for a
     * given ColumnInfo.
     *
     * @param  info   column description
     * @return  ColStats object which can accumulate stats for <code>info</code>
     */
    public static ColStats makeColStats( ColumnInfo info ) {
        Class<?> clazz = info.getContentClass();
        if ( Number.class.isAssignableFrom( clazz ) ) {
            return new NumberColStats( info );
        }
        else if ( clazz == Boolean.class ) {
            return new BooleanColStats( info );
        }
        else {
            return new BasicColStats( info );
        }
    }

    /**
     * ColStats implementation for non-numeric data.
     */
    private static class BasicColStats extends ColStats {
        private long ngood_;
        public BasicColStats( ColumnInfo colInfo ) {
            super( colInfo );
        }
        public void acceptDatum( Object obj ) {
            if ( ! Tables.isBlank( obj ) ) {
                ngood_++;
            }
        }
        public void addStats( ColStats o ) {
            BasicColStats other = (BasicColStats) o;
            ngood_ += other.ngood_;
        }
        protected double getMeanValue() {
            return Double.NaN;
        }
        protected double getVarianceValue() {
            return Double.NaN;
        }
        protected Object getMinimumValue() {
            return null;
        }
        protected Object getMaximumValue() {
            return null;
        }
        protected long getGoodCountValue() {
            return ngood_;
        }
    }

    /**
     * ColStats implementation for boolean data.
     */
    private static class BooleanColStats extends BasicColStats {
        private long ntrue_;
        private long ngood_;
        public BooleanColStats( ColumnInfo colInfo ) {
            super( colInfo );
        }
        public void acceptDatum( Object obj ) {
            if ( obj instanceof Boolean ) {
                ngood_++;
                if ( ((Boolean) obj).booleanValue() ) {
                    ntrue_++;
                }
            }
        }
        public void addStats( ColStats o ) {
            BooleanColStats other = (BooleanColStats) o;
            ngood_ += other.ngood_;
            ntrue_ += other.ntrue_;
        }
        protected double getMeanValue() {
            return (double) ntrue_ / (double) ngood_;
        }
        protected long getGoodCountValue() {
            return ngood_;
        }
    }

    /**
     * ColStats implementation for numeric data.
     */
    private static class NumberColStats extends ColStats {
        private long ngood_;
        private double sum_;
        private double sum2_;
        private double dmin_ = Double.MAX_VALUE;
        private double dmax_ = -Double.MAX_VALUE;
        private Object min_;
        private Object max_;

        public NumberColStats( ColumnInfo colInfo ) {
            super( colInfo );
        }

        public void acceptDatum( Object obj ) {
            if ( obj instanceof Number ) {
                double dval = ((Number) obj).doubleValue();
                if ( ! Double.isNaN( dval ) ) {
                    ngood_++;
                    sum_ += dval;
                    sum2_ += dval * dval;
                    if ( dval < dmin_ ) {
                        dmin_ = dval;
                        min_ = obj;
                    }
                    if ( dval > dmax_ ) {
                        dmax_ = dval;
                        max_ = obj;
                    }
                }
            }
        }

        public void addStats( ColStats o ) {
            NumberColStats other = (NumberColStats) o;
            ngood_ += other.ngood_;
            sum_ += other.sum_;
            sum2_ += other.sum2_;
            if ( other.dmin_ < dmin_ ) {
                dmin_ = other.dmin_;
                min_ = other.min_;
            }
            if ( other.dmax_ > dmax_ ) {
                dmax_ = other.dmax_;
                max_ = other.max_;
            }
        }

        protected double getMeanValue() {
            return ngood_ > 0 ? sum_ / ngood_
                              : Double.NaN;
        }

        protected double getVarianceValue() {
            return ngood_ > 0 ? ( sum2_ - sum_ * sum_ / ngood_ ) / ngood_
                              : Double.NaN;
        }

        protected Object getMinimumValue() {
            return min_;
        }

        protected Object getMaximumValue() {
            return max_;
        }

        protected long getGoodCountValue() {
            return ngood_;
        }
    }
}
