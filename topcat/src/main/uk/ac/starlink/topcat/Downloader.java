package uk.ac.starlink.topcat;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages downloading of data that only needs to be got once.
 *
 * @author   Mark Taylor
 * @since    13 Jun 2014
 */
public abstract class Downloader<T> {

    private final Class<T> clazz_;
    private final String dataDescription_;
    private volatile boolean isComplete_;
    private volatile T data_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructor.
     *
     * @param   clazz  type of data downloaded
     * @param   dataDescription  short description of downloaded data,
     *                           may be used in logging messages
     */
    public Downloader( Class<T> clazz, String dataDescription ) {
        clazz_ = clazz;
        dataDescription_ = dataDescription;
    }

    /**
     * Performs the actual download.  Implementations are encouraged
     * to log query and details of success if applicable at the INFO level,
     * but an error will be logged by the Downloader.
     *
     * @return  downloaded data
     */
    public abstract T attemptReadData() throws IOException;

    /**
     * Indicates whether the data has been downloaded.
     * If this method returns true, then {@link #getData} will return the
     * result.
     *
     * @return  true iff download has completed, successfully or otherwise
     */
    public boolean isComplete() {
        return isComplete_;
    }

    /**
     * Immediately returns the downloaded data, or null if it has not been
     * downloaded, or if a download has failed.
     *
     * @return  data
     */
    public T getData() {
        return data_;
    }

    /**
     * Resets the state of this downloader, as if the no download attempt
     * had been made.
     */
    public synchronized void clearData() {
        isComplete_ = false;
        data_ = null;
    }

    /**
     * Downloads the data if necessary, and returns its content.
     * If a download attempt has already been completed, this will return
     * immediately, otherwise it will block.
     * If the download failed, null will be returned.
     *
     * @return   data or null on failure
     */
    public synchronized T waitForData() {
        if ( isComplete_ ) {
            return data_;
        }
        else {
            data_ = readData();
            isComplete_ = true;
            return data_;
        }
    }

    /**
     * Reads the data and logs any error.
     *
     * @return  data, or null on error
     */
    private T readData() {
        try {
            return attemptReadData();
        }
        catch ( IOException e ) {
            logger_.log( Level.WARNING, "Failed to read " + dataDescription_,
                         e );
            return null;
        }
    }
}
