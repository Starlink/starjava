package uk.ac.starlink.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Thread which reads data from a pipe.  Having got an instance
 * of this class, you can call its getOutputStream method to 
 * acquire a stream to write into, and then implement its 
 * {@link #doReading} method to process the data; this method runs
 * in the new thread.
 * <p>
 * Here is an example of using the class to count the bytes written down
 * a stream:
 * <pre>
 *     PipeReaderThread reader = new PipeReaderThread() {
 *        protected void doReading( InputStream dataIn ) throws IOException {
 *            int i;
 *            while ( dataIn.read() >= 0 ) i++;
 *            System.out.println( i );
 *        }
 *     };
 *     reader.start();
 *     OutputStream dataOut = reader.getOutputStream();
 *     // write bytes down dataOut ...
 *     dataOut.close();
 *     reader.finishReading();
 * </pre>
 * Other uses will look pretty similar, but just override <tt>doReading</tt>
 * in different ways.  Note that any exceptions thrown by <tt>doReading</tt>
 * are caught and eventually thrown in the caller thread by
 * <tt>finishReading</tt>.
 * <p>
 * This class serves two purposes.  Firstly it copes with IOExceptions 
 * encountered during the read, and makes sure they get thrown at the
 * writing end of the pipe (<code>doReading</code> is declared to
 * throw <code>IOException</code>).
 * Secondly it shields the user from the implementation of the piped 
 * connection.
 * Performance of the 
 * {@link java.io.PipedInputStream}/{@link java.io.PipedOutputStream}
 * is dismal - this class may be able to do better.
 * <p>
 * The current implementation uses a couple of drop-in Piped*Stream 
 * replacements, but performance still isn't great - it may be possible
 * to do better in future.  You can provide your own paired pipes
 * by overriding both {@link #getInputStream} and {@link #getOutputStream}.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class PipeReaderThread extends Thread {

    private final FastPipedInputStream pipeIn = new FastPipedInputStream();
    private final OutputStream pipeOut = new FastPipedOutputStream( pipeIn );
    private IOException caught;

    /**
     * Constructs a new reader thread.
     */
    public PipeReaderThread() throws IOException {
        super( "Stream reader" );
    }

    /**
     * Returns the stream at the input end of the pipe.
     * 
     * @return  input stream
     */
    protected InputStream getInputStream() {
        return pipeIn;
    }

    /**
     * Returns the stream at the output end of the pipe.
     *
     * @return  output stream
     */
    public OutputStream getOutputStream() {
        return pipeOut;
    }

    /**
     * Implements the thread's <tt>run</tt> method to invoke doReading,
     * catching and saving IOExceptions.
     */
    public void run() {
        try {
            doReading( getInputStream() );
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
    protected abstract void doReading( InputStream dataIn ) throws IOException;

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
            }
        }
        if ( caught != null ) {
            throw caught;
        }
    }
}
