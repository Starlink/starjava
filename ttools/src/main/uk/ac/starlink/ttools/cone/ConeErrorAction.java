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
public abstract class ConeErrorAction {

    private final String name_;

    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /**
     * An error during a cone search results in a failure of the task.
     */
    public static final ConeErrorAction ABORT =
            new ConeErrorAction( "abort" ) {
        public ConeSearcher adjustConeSearcher( ConeSearcher base ) {
            return base;
        }
    };

    /**
     * Errors during cone searches are treated as if the search had 
     * returned with no results.
     */
    public static final ConeErrorAction IGNORE =
            new ConeErrorAction( "ignore" ) {
        public ConeSearcher adjustConeSearcher( ConeSearcher base ) {
            return new WrapperConeSearcher( base ) {
                public StarTable performSearch( double ra, double dec,
                                                double sr )
                        throws IOException {
                    try {
                        return base_.performSearch( ra, dec, sr );
                    }
                    catch ( IOException e ) {
                        logger.warning( searchString( ra, dec, sr )
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
    public static final ConeErrorAction RETRY =
        createRetryAction( "retry", -1 );

    /**
     * Constructor.
     *
     * @param  name  error action name
     */
    protected ConeErrorAction( String name ) {
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
     * which uses the error-handling policy defined by this action object.
     *
     * @param  base  base cone searcher
     * @return  cone searcher based on <code>base</code> with possibly 
     *          modified error handling
     */
    public abstract ConeSearcher adjustConeSearcher( ConeSearcher base );

    /**
     * Constructs an error action which will retry the search a fixed
     * number of times.
     *
     * @param   error action name
     * @param   nTry  maximum number of attempts;
     *                if &lt;=0 will retry indefinitely
     * @return  new error action
     */
    public static ConeErrorAction createRetryAction( String name,
                                                     final int nTry ) {
        return new ConeErrorAction( name ) {
            public ConeSearcher adjustConeSearcher( ConeSearcher base ) {
                return new RetryConeSearcher( base, nTry );
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

        /**
         * Constructor.
         *
         * @param   base   base cone searcher
         */
        WrapperConeSearcher( ConeSearcher base ) {
            base_ = base;
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
            super( base );
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
                    logger.warning( searchString( ra, dec, sr ) + " attempt " 
                                  + count + " failed" );
                    lastError = e;
                }
            }
            throw (IOException) new IOException( nTry_ + " attempts failed " )
                               .initCause( lastError );
        }
    }
}
