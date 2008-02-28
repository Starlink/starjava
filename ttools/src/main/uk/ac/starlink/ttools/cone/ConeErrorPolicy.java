package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;

/**
 * Defines how errors are treated during a multiple cone search operation.
 * An instance of this class should be applied to a {@link ConeSearcher} 
 * object prior to use in order to make it obey the instance's policy
 * on reacting to errors in the cone searcher's 
 * {@link ConeSearcher#performSearch} method.
 *
 * <p><em>Could one write a more general class which does this one's job using
 * proxies?</em>
 *
 * @author   Mark Taylor
 * @since    24 Jan 2007
 */
public abstract class ConeErrorPolicy {

    private final String name_;

    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /**
     * An error during a cone search results in a failure of the task.
     */
    public static final ConeErrorPolicy ABORT =
            new ConeErrorPolicy( "abort" ) {
        public ConeSearcher adjustConeSearcher( ConeSearcher base ) {
            return base;
        }
    };

    /**
     * Errors during cone searches are treated as if the search had 
     * returned with no results.
     */
    public static final ConeErrorPolicy IGNORE =
            new ConeErrorPolicy( "ignore" ) {
        public ConeSearcher adjustConeSearcher( ConeSearcher base ) {
            return new WrapperConeSearcher( base, "ignore" ) {
                public StarTable performSearch( double ra, double dec,
                                                double sr )
                        throws IOException {
                    try {
                        return base_.performSearch( ra, dec, sr );
                    }
                    catch ( IOException e ) {
                        logger.info( searchString( ra, dec, sr )
                                   + " failed: " + e );
                        return null;
                    }
                }
            };
        }
    };

    /**
     * If an error occurs during a cone search it is retried until a non-error
     * result is obtained.  Use with care.
     */
    public static final ConeErrorPolicy RETRY =
        createRetryPolicy( "retry", -1 );

    /**
     * Constructor.
     *
     * @param  name  policy name
     */
    protected ConeErrorPolicy( String name ) {
        name_ = name;
    }

    /**
     * Returns this object's name.
     */
    public String toString() {
        return name_;
    }

    /**
     * Apply this method to a basic cone searcher to obtain a new one 
     * which uses the error-handling policy defined by this policy object.
     *
     * @param  base  base cone searcher
     * @return  cone searcher based on <code>base</code> with possibly 
     *          modified error handling
     */
    public abstract ConeSearcher adjustConeSearcher( ConeSearcher base );

    /**
     * Constructs an error policy which will retry the search a fixed
     * number of times.
     *
     * @param   name  policy name
     * @param   nTry  maximum number of attempts;
     *                if &lt;=0 will retry indefinitely
     * @return  new error policy
     */
    public static ConeErrorPolicy createRetryPolicy( String name,
                                                     final int nTry ) {
        return new ConeErrorPolicy( name ) {
            public ConeSearcher adjustConeSearcher( ConeSearcher base ) {
                return new RetryConeSearcher( base, nTry );
            }
        };
    }

    /**
     * Returns a new policy which behaves like a base one, but provides
     * a supplied message in the IOException which is thrown if the
     * search fails.
     *
     * @param  policy  base policy
     * @param  advice  message of exception on failure
     */
    public static ConeErrorPolicy addAdvice( ConeErrorPolicy policy,
                                             final String advice ) {
        return new ConeErrorPolicy( policy.toString() ) {
            public ConeSearcher adjustConeSearcher( ConeSearcher base ) {
                return new AdviceConeSearcher( base, advice );
            }
        };
    }

    /**
     * Returns a string representing a given search attempt.
     * Intended for use in logging messages.
     *
     * @param   ra   right ascension in degrees
     * @param   dec  declination in degrees
     * @param   sr   search radius in degrees
     */
    private static String searchString( double ra, double dec, double sr ) {
        return new StringBuffer()
            .append( "search " )
            .append( '(' )
            .append( (float) ra )
            .append( ", " )
            .append( (float) dec )
            .append( ')' )
            .append( '+' )
            .append( (float) sr )
            .toString();
    }

    /**
     * ConeSearcher implementation which wraps an existing one.
     */
    private static abstract class WrapperConeSearcher implements ConeSearcher {
        final ConeSearcher base_;
        final String descrip_;

        /**
         * Constructor.
         *
         * @param   base   base cone searcher
         * @param   descrip  terse description of the function of this wrapper
         */
        WrapperConeSearcher( ConeSearcher base, String descrip ) {
            base_ = base;
            descrip_ = descrip;
        }

        public int getRaIndex( StarTable result ) {
            return base_.getRaIndex( result );
        }

        public int getDecIndex( StarTable result ) {
            return base_.getDecIndex( result );
        }

        public void close() {
            base_.close();
        }

        public String toString() {
            return descrip_ + "(" + base_.toString() + ")";
        }
    }

    /**
     * ConeSearcher implementation which retries the <code>performSearch</code>
     * method a given number of times in case of error.
     */
    private static class RetryConeSearcher extends WrapperConeSearcher {
        private final int nTry_;

        /**
         * Constructor.
         *
         * @param  base   base cone searcher
         * @param  nTry   maximum number of attempts
         *                if &lt;=0 will retry indefinitely
         */
        RetryConeSearcher( ConeSearcher base, int nTry ) {
            super( base, "retry" );
            nTry_ = nTry;
        }

        public StarTable performSearch( double ra, double dec, double sr )
                throws IOException {
            IOException lastError = null;
            for ( int nFail = 0; nTry_ <= 0 || nFail < nTry_; nFail++ ) {
                try {
                    return base_.performSearch( ra, dec, sr );
                }
                catch ( IOException e ) {
                    String count = Integer.toString( nFail + 1 );
                    if ( nTry_ > 0 ) {
                        count += "/" + nTry_;
                    }
                    logger.info( searchString( ra, dec, sr ) + " attempt " 
                               + count + " failed" );
                    lastError = e;
                }
            }
            throw (IOException) new IOException( nTry_ + " attempts failed " )
                               .initCause( lastError );
        }
    }

    /**
     * ConeSearcher implementation which behaves like a base one, but provides
     * a supplied message in the IOException which is thrown if the
     * search fails.
     */
    private static class AdviceConeSearcher extends WrapperConeSearcher {
        private final String advice_;

        /**
         * Constructor.
         *
         * @param  base  base cone searcher
         * @param  advice  message of exception on failure
         */
        AdviceConeSearcher( ConeSearcher base, String advice ) {
            super( base, "advice" );
            advice_ = advice;
        }

        public StarTable performSearch( double ra, double dec, double sr )
                throws IOException {
            try {
                return base_.performSearch( ra, dec, sr );
            }
            catch ( IOException e ) {
                throw (IOException) new IOException( advice_ ).initCause( e );
            }
        }
    }
}
