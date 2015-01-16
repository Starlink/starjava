package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Aggregates two short integer values.
 * Intended (and optimised) for use as a map key.
 *
 * @author   Mark Taylor
 * @since    15 Jan 2015
 */
@Equality
public class ShortPair {

    private final int packed_;

    /**
     * Constructor.
     *
     * @param   x  first value
     * @param   y  second value
     */
    public ShortPair( short x, short y ) {
        packed_ = ( x & 0xffff ) | ( ( y & 0xffff ) << 16 );
    }

    /**
     * Returns X value.
     *
     * @return x
     */
    public short getX() {
        return (short) ( packed_ & 0xffff );
    }

    /**
     * Returns Y value.
     *
     * @return  y
     */
    public short getY() {
        return (short) ( ( packed_ >> 16 ) & 0xffff );
    }

    @Override
    public int hashCode() {
        return packed_;
    }

    @Override
    public boolean equals( Object other ) {
        return other instanceof ShortPair
            && ((ShortPair) other).packed_ == this.packed_;
    }
}
