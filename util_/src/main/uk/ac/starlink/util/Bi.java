package uk.ac.starlink.util;

import java.util.Objects;

/**
 * Aggregates two typed objects of different types.
 * This utility class does not do anything clever.
 *
 * @author   Mark Taylor
 * @since    18 Jul 2023
 * @see      Pair
 */
public class Bi<A,B> {

    private final A item1_;
    private final B item2_;

    /**
     * Constructor.
     *
     * @param  item1  first item
     * @param  item2  second item
     */
    public Bi( A item1, B item2 ) {
        item1_ = item1;
        item2_ = item2;
    }

    /**
     * Returns this object's first item.
     *
     * @return  item
     */
    public A getItem1() {
        return item1_;
    }

    /**
     * Returns this object's second item.
     *
     * @return  item
     */
    public B getItem2() {
        return item2_;
    }

    @Override
    public int hashCode() {
        int code = 23229;
        code = 23 * code + Objects.hashCode( item1_ );
        code = 23 * code + Objects.hashCode( item2_ );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof Bi ) {
            Bi<?,?> other = (Bi<?,?>) o;
            return Objects.equals( this.item1_, other.item1_ )
                && Objects.equals( this.item2_, other.item2_ );
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        return new StringBuffer()
           .append( '(' )
           .append( item1_ )
           .append( ',' )
           .append( item2_ )
           .append( ')' )
           .toString();
    }
}
