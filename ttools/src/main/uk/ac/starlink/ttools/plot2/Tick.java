package uk.ac.starlink.ttools.plot2;

/**
 * Defines a tick on an axis.
 * A tick has a numerical value, used for positioning, and optionally
 * a text label.  Ticks with no label are considered minor.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2013
 */
@Equality
public class Tick {

    private final double value_;
    private final String label_;

    /**
     * Constructs a minor tick.
     * This has no text label.
     *
     * @param  value  numeric value
     */
    public Tick( double value ) {
        this( value, null );
    }

    /**
     * Constructs a tick.
     * As long as the label is non-null, this is considered a major tick.
     * 
     * @param  value  numeric value
     * @param  label  text label
     */
    public Tick( double value, String label ) {
        value_ = value;
        label_ = label;
    }

    /**
     * Returns this tick's numeric value.
     *
     * @return  value
     */
    public double getValue() {
        return value_;
    }

    /**
     * Returns this tick's text label.
     *
     * @return   text label
     */
    public String getLabel() {
        return label_;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof Tick ) {
            Tick other = (Tick) o;
            return other.value_ == this.value_
                && PlotUtil.equals( other.label_, this.label_ );
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits( (float) value_ )
             + PlotUtil.hashCode( label_ );
    }

    @Override
    public String toString() {
        return value_ + "->\"" + label_ + "\"";
    }
}
