package uk.ac.starlink.hapi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.util.CgiQuery;
import uk.ac.starlink.util.IOConsumer;
import uk.ac.starlink.util.IOSupplier;

/**
 * Handles reading HAPI data streams in chunks to cope with data requests
 * that encounter service errors with the status
 * 1408 "Bad Status - too much time or data requested".
 *
 * @author   Mark Taylor
 * @since    26 Jan 2024
 */
public class ChunkStreamer {

    private final HapiService service_;
    private final int chunkLimit_;
    private final IOConsumer<String> limitCallback_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.hapi" );

    /**
     * Constructor.
     *
     * <p>A limit must be supplied for the maximum number of chunks that
     * will be retrieved for a given request.
     * If this limit is exceeded, the supplied callback object,
     * if any, will be called and the stream will terminate.
     * If the callback throws an IOException, it will be encountered
     * by the code reading the stream.
     *
     * @param  service  service to which requests will be made
     * @param  chunkLimit   maximum number of chunks that a data stream will
     *                      include
     * @param  limitCallback  called with a failure message
     *                        if chunk limit is exceeded; may be null
     */
    public ChunkStreamer( HapiService service, int chunkLimit,
                          IOConsumer<String> limitCallback ) {
        service_ = service;
        chunkLimit_ = chunkLimit;
        limitCallback_ = limitCallback;
    }

    /**
     * Returns the largest number of chunks that this streamer is prepared
     * to request for a given download.
     *
     * @return  chunk limit
     */
    public int getChunkLimit() {
        return chunkLimit_;
    }

    /**
     * Returns an input stream corresponding to the supplied HAPI data request
     * URL, but which may be assembled from the concatenation of
     * multiple actual requests to the service.
     *
     * @param  requestUrl  data request URL
     * @return  input stream containing all data in requested range,
     *          subject to chunk limit
     */
    public InputStream openMultiChunkStream( URL requestUrl )
            throws IOException {

        /* Determine start and stop times from URL. */
        Map<String,String> reqParams =
            HapiService.getRequestParameters( requestUrl );
        Limit tmin = null;
        Limit tmax = null;
        for ( HapiVersion version : HapiVersion.getStandardVersions() ) {
            if ( tmin == null ) {
                String startParam = version.getStartRequestParam();
                String start = reqParams.get( startParam );
                if ( start != null ) {
                    tmin = new Limit( startParam, start );
                }
            }
            if ( tmax == null ) {
                String stopParam = version.getStopRequestParam();
                String stop = reqParams.get( stopParam );
                if ( stop != null ) {
                    tmax = new Limit( stopParam, stop );
                }
            }
        }

        /* Make a chunked request. */
        return chunkStream( requestUrl, tmin, tmax,
                            new Progress( tmin.isoTime_, tmax.isoTime_ ) );
    }

    /**
     * Recursive load of URL data in chunks.
     *
     * @param  requestUrl  HAPI data request URL
     * @param  tmin   start time
     * @param  tmax   end time
     * @param  progress  progress object
     * @return  input stream containing all data in requested range,
     *          subject to chunk limit
     */
    private InputStream chunkStream( URL requestUrl, Limit tmin, Limit tmax,
                                     Progress progress )
            throws IOException {

        /* Check if chunk limit has been exceeded. */
        if ( progress.ichunk_ >= chunkLimit_ ) {
            if ( ! progress.hasExceeded_ ) {
                progress.hasExceeded_ = true;
                if ( limitCallback_ != null ) {
                    String msg = "HAPI load chunk limit exceeded"
                               + " (" + chunkLimit_ + " "
                               + ( chunkLimit_ == 1 ? "chunk" : "chunks" )
                               + ", " + progress.getTimePercent( tmax.isoTime_ )
                               + "%)";
                    limitCallback_.accept( msg );
                }
            }
            return null;
        }

        /* Attempt a direct request to the service for the whole range. */
        try {
            return openStream( requestUrl, tmin, tmax, progress );
        }

        /* If that fails on a 1408, split the request in two and recurse. */
        catch ( HapiServiceException e ) {
            if ( e.getHapiCode() == HapiServiceException.CODE_TOOMUCH ) {
                String midIso = getIsoMidpoint( tmin.isoTime_, tmax.isoTime_ );
                List<IOSupplier<InputStream>> streamSuppliers = Arrays.asList(
                    () -> chunkStream( requestUrl,
                                       tmin, tmax.replaceTime( midIso ),
                                       progress ),
                    () -> chunkStream( requestUrl,
                                       tmin.replaceTime( midIso ), tmax,
                                       progress )
                );
                return new SeqInputStream( streamSuppliers.iterator() );
            }
            else {
                throw e;
            }
        }
    }

    /**
     * Performs a direct request to the HAPI service.
     *
     * @param  templateUrl   original request URL for the whole range
     * @param  tmin    lower bound time for this request
     * @param  tmax    upper bound time for this request
     * @param  progress object
     * @return   input stream for the whole range if possible
     * @throws   IOException if request failed;
     *           may be a 1408 HapiServiceException
     */
    private InputStream openStream( URL templateUrl, Limit tmin, Limit tmax,
                                    Progress progress )
            throws IOException {

        /* Assemble the actual request URL. */
        String urlBase = templateUrl.toString();
        urlBase = urlBase.substring( 0, urlBase.indexOf( '?' ) );
        Map<String,String> reqParams =
            HapiService.getRequestParameters( templateUrl ); 
        if ( progress.ichunk_ > 0 ) {
            reqParams.remove( "include" );
        }
        reqParams.put( tmin.reqParam_, tmin.isoTime_ );
        reqParams.put( tmax.reqParam_, tmax.isoTime_ );
        CgiQuery query = new CgiQuery( urlBase );
        query.allowUnencodedChars( ":," );
        for ( Map.Entry<String,String> entry : reqParams.entrySet() ) {
            query.addArgument( entry.getKey(), entry.getValue() );
        }

        /* Make the request.  This may fail with an IOException. */
        InputStream in = service_.openStream( query.toURL() );

        /* If the request was successful, update and log progress. */
        progress.ichunk_++;
        logger_.info( "HAPI data chunk request " + progress.ichunk_
                    + " of max " + chunkLimit_ + ", "
                    + progress.getTimePercent( tmax.isoTime_ ) + "%" );

        /* If successful, return the input stream. */
        return in;
    }

    /**
     * Returns the average of two ISO-8601 timestamps.
     *
     * @param  isoMin   start timestamp
     * @param  isoMax   end timestamp
     * @return  midpoint timestamp
     */
    private static String getIsoMidpoint( String isoMin, String isoMax ) {
        double secMin = Times.isoToUnixSeconds( isoMin );
        double secMax = Times.isoToUnixSeconds( isoMax );
        double secMid = 0.5 * ( secMax + secMin );
        return Times.formatUnixSeconds( (long) secMid,
                                        "yyyy-MM-dd'T'HH:mm:ss" );
    }

    /**
     * Characterises an epoch sent as part of a HAPI data request.
     */
    private static class Limit {
        final String reqParam_;
        final String isoTime_;

        /**
         * Constructor.
         *
         * @param  reqParam  hapi request parameter name
         * @param  isoTime   time in ISO-8601 format
         */
        Limit( String reqParam, String isoTime ) {
            reqParam_ = reqParam;
            isoTime_ = isoTime;
        }

        /**
         * Returns a limit like this one but with a different time value.
         *
         * @param  isoTime   time in ISO-8601 format
         * @return  new limit instance
         */
        Limit replaceTime( String isoTime ) {
            return new Limit( reqParam_, isoTime );
        }
    }

    /**
     * Encapsulates chunking progress.
     */
    private static class Progress {
        final String startIso_;
        final String stopIso_;
        int ichunk_;
        boolean hasExceeded_;
 
        /**
         * Constructor.
         *
         * @param  startIso  full request start time
         * @param  stopIso   full request stop time
         */
        Progress( String startIso, String stopIso ) {
            startIso_ = startIso;
            stopIso_ = stopIso;
        }

        /**
         * Returns the fraction of the full request that a given
         * epoch string corresponds to.
         *
         * @param  epochIso  time in ISO-8601 format
         * @return  fraction of time between start and stop
         */
        public double getTimeFraction( String epochIso ) {
            double epochSec = Times.isoToUnixSeconds( epochIso );
            double startSec = Times.isoToUnixSeconds( startIso_ );
            double stopSec = Times.isoToUnixSeconds( stopIso_ );
            return ( epochSec - startSec ) / ( stopSec - startSec );
        }

        /**
         * Returns the fraction of the full request that a given
         * epoch string corresponds to as a percentage.
         *
         * @param  epochIso  time in ISO-8601 format
         * @return  percentage of time between start and stop
         */
        public int getTimePercent( String epochIso ) {
            return (int) Math.round( 100 * getTimeFraction( epochIso ) );
        }
    }

    /**
     * InputStream composed of a sequence of other input streams
     * concatenated together.
     * This does something similar to {@link java.io.SequentialInputStream}
     * but it works with InputStream IOSuppliers, which means that
     * constructing them can throw IOExceptions.
     *
     * <p> Most of the implementation is copied from
     * <code>SequentialInputStream</code>.
     */
    private static class SeqInputStream extends InputStream {

        private final Iterator<IOSupplier<InputStream>> streamIt_;
        private InputStream in_;

        /**
         * Constructor.
         * If any of the suppliers iterated over returns null,
         * the stream terminates without error and
         * any subsequent streams are ignored.
         *
         * @param  streamIt  iterator over InputStream suppliers
         */
        public SeqInputStream( Iterator<IOSupplier<InputStream>> streamIt )
                throws IOException {
            streamIt_ = streamIt;
            nextStream();
        }

        @Override
        public int available() throws IOException {
            return in_ == null ? 0 : in_.available();
        }

        @Override
        public int read() throws IOException {
            if ( in_ == null ) {
                return -1;
            }
            int c = in_.read();
            if ( c == -1 ) {
                nextStream();
                return read();
            }
            return c;
        }

        @Override
        public int read( byte b[], int off, int len ) throws IOException {
            if ( in_ == null ) {
                return -1;
            }
            int n = in_.read( b, off, len );
            if ( n <= 0 ) {
                nextStream();
                return read( b, off, len );
            }
            return n;
        }

        @Override
        public void close() throws IOException {
            do {
                nextStream();
            } while ( in_ != null );
        }

        /**
         * Advances to the next stream in the list.
         */
        private void nextStream() throws IOException {
            if ( in_ != null ) {
                in_.close();
            }
            if ( streamIt_.hasNext() ) {
                in_ = streamIt_.next().get();
                if ( in_ == null ) {
                    close();
                }
            }
            else {
                in_ = null;
            }
        }
    }
}
