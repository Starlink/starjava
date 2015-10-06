package uk.ac.starlink.topcat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Categorises submitted data values by equality into a set of
 * mutually exclusive groups.
 *
 * @author   Mark Taylor
 * @since    3 Feb 2015
 */
public class Classifier<T> {

    private final Map<T,Count> countMap_;
    private long nTotal_;

    /**
     * Constructor.
     */
    public Classifier() {
        countMap_ = new HashMap<T,Count>();
    }

    /**
     * Submits a value for categorisation.
     *
     * @param  value  value
     */
    public void submit( T value ) {
        nTotal_++;
        countMap_.put( value, Count.increment( countMap_.get( value ) ) );
    }

    /**
     * Returns the number of values submitted so far.
     *
     * @return   total item count
     */
    public long getItemCount() {
        return nTotal_;
    }

    /**
     * Returns the number of distinct values seen so far.
     *
     * @return  number of distinct values
     */
    public long getValueCount() {
        return countMap_.size();
    }

    /**
     * Returns a sorted list of the N most popular values.
     *
     * @param  nValue  maximum number of values to return
     * @return  collection of submitted values,
     *          in descending order of popularity
     */
    public SortedSet<CountedValue<T>> getTopValues( int nValue ) {
        SortedSet<CountedValue<T>> countSet = new TreeSet<CountedValue<T>>();
        for ( Map.Entry<T,Count> entry : countMap_.entrySet() ) {
            CountedValue<T> cv = new CountedValue<T>( entry );
            if ( countSet.size() < nValue ||
                 cv.compareTo( countSet.last() ) < 0 ) {
                countSet.add( cv );
                if ( countSet.size() > nValue ) {
                    countSet.remove( countSet.last() );
                }
            }
            assert countSet.size() <= nValue;
        }
        return countSet;
    }

    /**
     * Returns a sorted list of the values with a certain minimum count.
     *
     * @param  minCount  minimum number of submissions for each returned value
     * @return  collection of submitted values,
     *          in descending order of popularity
     */
    public Collection<CountedValue<T>> getThresholdValues( int minCount ) {
        List<CountedValue<T>> list = new ArrayList<CountedValue<T>>();
        for ( Map.Entry<T,Count> entry : countMap_.entrySet() ) {
            CountedValue<T> cv = new CountedValue<T>( entry );
            if ( cv.getCount() >= minCount ) {
                list.add( cv );
            }
        }
        Collections.sort( list );
        return list;
    }

    /**
     * Aggregates a value and the number of times it has been submitted.
     * The natural comparison sequence of these objects sorts them in
     * decreasing order of Count (number of submissions).
     */
    public static class CountedValue<T> implements Comparable<CountedValue<T>> {
        private final T value_;
        private final long count_;

        /**
         * Constructs a CountedValue from a suitable map entry.
         *
         * @param   entry  map entry
         */
        private CountedValue( Map.Entry<T,Count> entry ) {
            this( entry.getKey(), entry.getValue().getValue() );
        }

        /**
         * Constructor.
         *
         * @param  value  submitted value
         * @param  count  number of times it has been submitted
         */
        public CountedValue( T value, long count ) {
            value_ = value;
            count_ = count;
        }

        /**
         * Returns the value.
         *
         * @return  value
         */
        public T getValue() {
            return value_;
        }

        /**
         * Returns the count.
         *
         * @return  number of times the value has been submitted
         */
        public long getCount() {
            return count_;
        }

        @Override
        public String toString() {
            return String.valueOf( value_ ) + ": " + count_;
        }

        @Override
        public int hashCode() {
            int code = 9993;
            code = 23 * code + ( value_ == null ? 0 : value_.hashCode() );
            code = 23 * code + (int) count_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof CountedValue ) {
                CountedValue<?> other = (CountedValue<?>) o;
                return this.value_ == null ? other.value_ == null
                                           : this.value_.equals( other.value_ )
                    && this.count_ == other.count_;
            }
            else {
                return false;
            }
        }

        /**
         * Decreasing comparison on count, with appropriate tie-breakers.
         */
        public int compareTo( CountedValue<T> cv2 ) {
            CountedValue<T> cv1 = this;
            long c1 = cv1.count_;
            long c2 = cv2.count_;
            if ( c1 > c2 ) {
                return -1;
            }
            else if ( c1 < c2 ) {
                return +1;
            }

            /* Tie-breakers are required so that compareTo is compatible with
             * equals, otherwise instances will not sit happily in Sets. */
            else {
                T v1 = cv1.value_;
                T v2 = cv2.value_;
                if ( v1 == null && v2 == null ) {
                    return 0;
                }
                else if ( v1 == null ) {
                    return -1;
                }
                else if ( v2 == null ) {
                    return +1;
                }
                else if ( v1.equals( v2 ) ) {
                    return 0;
                }
                else {
                    int hash1 = v1.hashCode();
                    int hash2 = v2.hashCode();
                    if ( hash1 < hash2 ) {
                        return -1;
                    }
                    else if ( hash1 > hash2 ) {
                        return +1;
                    }
                    else {
                        return v1.toString().compareTo( v2.toString() );
                    }
                }
            }
        }
    }
}
