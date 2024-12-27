package uk.ac.starlink.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Aggregates two objects of the same type.
 * This utility class does not do anything clever.
 *
 * @author   Mark Taylor
 * @since    12 Sep 2019
 * @see    Bi
 */
public class Pair<T> {

    private final T item1_;
    private final T item2_;

    /**
     * Constructor.
     *
     * @param  item1  first item
     * @param  item2  second item
     */
    public Pair( T item1, T item2 ) {
        item1_ = item1;
        item2_ = item2;
    }

    /**
     * Returns this pair's first item.
     *
     * @return  item
     */
    public T getItem1() {
        return item1_;
    }

    /**
     * Returns this item's second item.
     *
     * @return  item
     */
    public T getItem2() {
        return item2_;
    }

    @Override
    public int hashCode() {
        int code = 4429801;
        code = 23 * code + ( item1_ == null ? 0 : item1_.hashCode() );
        code = 23 * code + ( item2_ == null ? 0 : item2_.hashCode() );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof Pair ) {
            Pair<?> other = (Pair<?>) o;
            return ( this.item1_ == null ? other.item1_ == null
                                         : this.item1_.equals( other.item1_ ) )
                && ( this.item2_ == null ? other.item2_ == null
                                         : this.item2_.equals( other.item2_ ) );
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        return new StringBuffer()
           .append( "(" )
           .append( item1_ )
           .append( "," )
           .append( item2_ )
           .append( ")" )
           .toString();
    }

    /**
     * Splits a collection into two constituent parts.
     *
     * @param  in  input collection; unaffected on exit
     * @return   pair of non-null collections (currently Lists),
     *           each containing about half of the input collection
     */
    public static <S> Pair<Collection<S>> splitCollection( Collection<S> in ) {
        int n = in.size();
        int n1 = n / 2;
        int n2 = n - n1;
        Collection<S> sub1 = new ArrayList<S>( n1 );
        Collection<S> sub2 = new ArrayList<S>( n2 );
        int i = 0;
        for ( S item : in ) {
            ( ( i++ < n1 ) ? sub1 : sub2 ).add( item );
        }
        return new Pair<Collection<S>>( sub1, sub2 );
    }

    /**
     * Splits an array into two consituent parts.
     *
     * @param  in  input array; unaffected on exit
     * @return  pair of non-null arrays,
     *          each containing about half of the input array
     */
    public static <S> Pair<S[]> splitArray( S[] in ) {
        int n = in.length;
        int n1 = n / 2;
        int n2 = n - n1;
        Class<?> clazz = in.getClass().getComponentType();
        @SuppressWarnings("unchecked")
        S[] sub1 = (S[]) Array.newInstance( clazz, n1 );
        @SuppressWarnings("unchecked")
        S[] sub2 = (S[]) Array.newInstance( clazz, n2 );
        System.arraycopy( in, 0, sub1, 0, n1 );
        System.arraycopy( in, n1, sub2, 0, n2 );
        return new Pair<S[]>( sub1, sub2 );
    }
}
