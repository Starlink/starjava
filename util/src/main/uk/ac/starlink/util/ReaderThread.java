package uk.ac.starlink.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Thread which reads data from a pipe.
 * The {@link #doReading} method of this abstract class should be implemented
 * to do something with the bytes in an input stream.
 * <p>
 * Here is an example of using the class to count the bytes written down
 * a stream:
 * <pre>
 *    PipedOutputStream dataOut = new PipedOutputStream();
 *    ReaderThread reader = new ReaderThread( dataOut ) {
 *        public void doReading( InputStream dataIn ) throws IOException {
 *            int i;
 *            while ( dataIn.read() >= 0 ) i++;
 *            System.out.println( i );
 *        }
 *    };
 *    reader.start();
 *    // ... write bytes down dataOut ...
 *    dataOut.close();
 *    reader.finishReading();
 * </pre>
 * Other uses will look pretty similar, but just override <tt>doReading</tt>
 * in different ways.  Note that any exceptions thrown by <tt>doReading</tt>
 * are caught and eventually thrown in the caller thread by 
 * <tt>finishReading</tt>.
 * 
 * @author   Mark Taylor (Starlink)
 */
public abstract class ReaderThread extends Thread {

    private PipedInputStream dataIn;
    private IOException caught;

    /**
     * Constructs a new reader thread.
     *
     * @param  dataOut  stream down which bytes will be written
     */
    public ReaderThread( PipedOutputStream dataOut ) throws IOException {
        super( "Stream reader" );
        dataIn = new PipedInputStream( dataOut );
    }

    /**
     * Implements the thread's <tt>run</tt> method to invoke doReading,
     * catching and saving IOExceptions.
     */
    public void run() {
        try {
            doReading( dataIn );
        }
        catch ( IOException e ) {
            caught = e;
        }
    }

    /**
     * This method should be implemented to consume all the bytes in
     * the given input stream.  It is probably a good idea for implementations
     * to buffer the supplied input stream for efficiency.
     * Note that any implementation of this method which does not read
     * <tt>dataIn</tt> to the end of the stream (either closing it early or
     * just stopping reading) may cause an IOException to be thrown in 
     * the thread which is writing to the PipedOutputStream.
     *
     * @param  dataIn  stream which will supply bytes
     * @throws  IOException if any I/O error occurs; this exception will
     *          be saved and thrown later by the <tt>finishReading</tt> method
     */
    protected abstract void doReading( InputStream dataIn )
            throws IOException;

    /**
     * Waits until the <tt>doReading</tt> method has finished reading
     * the bytes written down the output stream, and returns.
     * Any IOException which has occurred during the read will be thrown
     * by this method.
     */
    public void finishReading() throws IOException {
        try {
            join();
        }
        catch ( InterruptedException e ) {
            if ( caught == null ) {
                caught = 
                    new IOException( "Thread trouble joining stream reader" );
                caught.initCause( e );
            }
        }
        if ( caught != null ) {
            throw caught;
        }
    }
}
