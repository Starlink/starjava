package uk.ac.starlink.ttools.plot2.task;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

/**
 * Object that defines how groups of (parameter) suffixes are constructed.
 *
 * @author   Mark Taylor
 * @since    14 Sep 2017
 */
public abstract class Suffixer {

    /**
     * Returns a list of suffixes according to this object's policy.
     * Note that, because of the way that these suffixes are used
     * in STILTS plotting commands, no suffix in the returned list should
     * contain any other suffix as a leading substring.
     * Making all the elements the same length and unique is a straightforward
     * way to ensure this, for instance by using something like leading zeroes.
     *
     * @param  n  number of suffixes required
     * @return  <code>n</code>-element list
     */
    public abstract List<String> createSuffixes( int n );

    /**
     * Returns an instance that generates numeric suffixes 1, 2, 3, ...
     * prepended with an optional supplied separator string.
     * Leading zeroes are used when the number of suffixes requied is
     * greater than 9.
     *
     * @param  name  suffixer name
     * @param  separator   string to prepend to all non-blank suffixes,
     *                     may be null
     * @param  isBlank1  if true, then a request for a single-element
     *                   suffix list will be treated specially,
     *                   giving an empty string
     * @return  new suffixer
     */
    public static Suffixer createNumericSuffixer( final String name,
                                                  final String separator,
                                                  final boolean isBlank1 ) {
        return new Suffixer() {
            public List<String> createSuffixes( final int n ) {
                if ( n == 0 || ( n == 1 && isBlank1 ) ) {
                    return Collections.singletonList( "" );
                }
                else {
                    int ndig = 1 + (int) Math.log10( n );
                    StringBuffer fbuf = new StringBuffer( ndig );
                    for ( int i = 0; i < ndig; i++ ) {
                        fbuf.append( '0' );
                    }
                    final NumberFormat fmt =
                        new DecimalFormat( fbuf.toString() );
                    return new AbstractList<String>() {
                        public int size() {
                            return n;
                        }
                        public String get( int i ) {
                            return separator + fmt.format( i + 1 );
                        }
                    };
                }
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    /**
     * Returns an instance that generates alphabetic suffixes a, b, c, ...
     * If there are more than 26, letters are repeated (the output
     * is actually in base 26 with digits 'a'-'z').
     *
     * @param  name  suffixer name
     * @param  separator   string to prepend to all non-blank suffixes,
     *                     may be null
     * @param  isBlank1  if true, then a request for a single-element
     *                   suffix list will be treated specially,
     *                   giving an empty string
     * @param  isUpper   true for upper case letters, false for lower case
     * @return  new suffixer
     */
    public static Suffixer createAlphaSuffixer( final String name,
                                                final String separator,
                                                final boolean isBlank1,
                                                boolean isUpper ) {
        final char c0 = isUpper ? 'A' : 'a';
        final int nc = 26;
        return new Suffixer() {
            public List<String> createSuffixes( final int n ) {
                if ( n == 1 && isBlank1 ) {
                    return Collections.singletonList( "" );
                }
                else {
                    final int ndig =
                        1 + (int) ( Math.log( n - 1 ) / Math.log( nc ) );
                    return new AbstractList<String>() {
                        public int size() {
                            return n;
                        }
                        public String get( int ival ) {
                            StringBuffer sbuf = new StringBuffer( ndig );
                            for ( int jd = 0; jd < ndig; jd++ ) {
                                sbuf.insert( 0, (char) ( ( ival % nc ) + c0 ) );
                                ival = ival / nc;
                            }
                            return separator + sbuf.toString();
                        }
                    };
                }
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }
}
