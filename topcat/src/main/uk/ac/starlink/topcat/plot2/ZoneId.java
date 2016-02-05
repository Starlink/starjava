package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Provides a label corresponding to one zone of a multi-zone plot.
 *
 * <p>This is just a typed wrapper round a Comparable object.
 * Its purpose is to document that certain parameters in the API
 * represent zone identifiers, rather than to provide additional behaviour.
 * It could be replaced by a marker interface.
 *
 * @author   Mark Taylor
 * @since    5 Feb 2015
 */
@Equality
public class ZoneId implements Comparable<ZoneId> {
    private final Comparable value_;

    /**
     * Constructor.
     *
     * @param   value  value object, should have equality semantics
     */
    public ZoneId( Comparable value ) {
        value_ = value;
    }

    /**
     * Returns the value object underlying this identifier.
     *
     * @return  value
     */
    public Comparable getValue() {
        return value_;
    }

    public int compareTo( ZoneId other ) {
        return value_.compareTo( other.value_ );
    }

    @Override
    public int hashCode() {
        return value_.hashCode();
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof ZoneId ) {
            ZoneId other = (ZoneId) o;
            return this.value_.equals( other.value_ );
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        return value_.toString();
    }
}
