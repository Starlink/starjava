package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;

/**
 * Defines how errors are treated during a multiple cone search operation.
 * It also makes sure that interruptions are checked for,
 * which is important to stop threads continuing to submit requests to
 * remote services after the client no longer has any need for them.
 *
 * @author   Mark Taylor
 * @since    24 Jan 2007
 */
public abstract class ConeErrorPolicy {

    private final String name_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /**
     * An error during a cone search results in a failure of the task.
     */
    public static final ConeErrorPolicy ABORT =
            new ConeErrorPolicy( "abort" ) {
        public StarTable performConeSearch( ConeSearcher cs, double ra,
                                            double dec, double sr )
                throws IOException, InterruptedException {
            return invokeConeSearcher( cs, ra, dec, sr );
        }
    };

    /**
     * Errors during cone searches are treated as if the search had
     * returned with no results.
     */
    public static final ConeErrorPolicy IGNORE =
            new ConeErrorPolicy( "ignore" ) {
        public StarTable performConeSearch( ConeSearcher cs, double ra,
                                            double dec, double sr )
                throws IOException, InterruptedException {
            try {
                return invokeConeSearcher( cs, ra, dec, sr );
            }
            catch ( IOException e ) {
                logger_.info( searchString( ra, dec, sr ) + " failed: " + e );
                return null;
            }
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
     * Uses the given cone searcher object to perform an actual cone search.
     * This may result in more than one call to the given cone searcher's
     * <code>performSearch</code> method.
     *
     * <p>Implementations are expected to check for thread interruption status
     * and throw an InterruptedException if interruption has happened.
     * They should do this in such a way that the cone search query is
     * not performed (especially multiple times) following an interruption.
     * 
     * @param  cs   cone searcher providing basic cone search capabilities
     * @param  ra   right ascension in degrees of search region centre
     * @param  dec  declination in degrees of search region centre
     * @param  sr   search radius in degrees 
     * @return  table containing records in the given cone,
     *                or possibly null if no records are found
     * @throws   IOException  if an IO error occurs
     * @throws   InterruptedException  if the thread was interrupted
     * @see   ConeSearcher#performSearch
     */
    public abstract StarTable performConeSearch( ConeSearcher cs, double ra,
                                                 double dec, double sr )
            throws IOException, InterruptedException;

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
            public StarTable performConeSearch( ConeSearcher cs, double ra,
                                                double dec, double sr )
                    throws IOException, InterruptedException {
                IOException lastError = null;
                for ( int nFail = 0; nTry <= 0 || nFail < nTry; nFail++ ) {
                    try {
                        return invokeConeSearcher( cs, ra, dec, sr );
                    }
                    catch ( IOException e ) {
                        String count = Integer.toString( nFail + 1 );
                        if ( nTry > 0 ) {
                            count += "/" + nTry;
                        }
                        logger_.info( searchString( ra, dec, sr ) + " attempt "
                                    + count + " failed" );
                        lastError = e;
                    }
                }
                throw (IOException)
                      new IOException( nTry + " attempts failed" )
                     .initCause( lastError );
            }
        };
    }

    /**
     * Returns a policy like {@link #ABORT}, except that if an error occurs,
     * the IOException thrown is populated with some custom text.
     *
     * @param   name  policy name
     * @param   extraAdvice  advice to user in case of cone search failure
     * @return  new abort-like policy
     */
    public static ConeErrorPolicy
                  createAdviceAbortPolicy( String name,
                                           final String extraAdvice ) {
        return new ConeErrorPolicy( name ) {
            public StarTable performConeSearch( ConeSearcher cs, double ra,
                                                double dec, double sr )
                    throws IOException, InterruptedException {
                try {
                    return invokeConeSearcher( cs, ra, dec, sr );
                }
                catch ( IOException e ) {
                    throw (IOException) new IOException( extraAdvice )
                                       .initCause( e );
                }
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
     * @return   human-readable string characterising search
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
     * Calls the <code>performSearch</code> method of a cone searcher object
     * on some given parameters, but additionally checks the thread for
     * interrupted status, and throws an InterruptedException if set.
     *
     * @param   cs  cone searcher
     * @param   ra  right ascension in degrees
     * @param   dec  declination in degrees
     * @param   sr   search radius in degrees
     * @return  output table
     * @throws  InterruptedException  if the thread is in an interrupted state
     *          before or after the invocation
     */
    private static StarTable invokeConeSearcher( ConeSearcher cs, double ra,
                                                 double dec, double sr )
            throws IOException, InterruptedException {
        if ( Thread.interrupted() ) {
            throw new InterruptedException();
        }
        StarTable table = cs.performSearch( ra, dec, sr );
        if ( Thread.interrupted() ) {
            throw new InterruptedException();
        }
        return table;
    }
}
