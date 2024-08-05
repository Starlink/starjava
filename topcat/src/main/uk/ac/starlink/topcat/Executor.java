package uk.ac.starlink.topcat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import uk.ac.starlink.util.IOUtils;

/**
 * Defines an object which can execute a system Process.
 * This is basically a convenience wrapper for {@link java.lang.Process}
 * which takes care of the stream output and so on.  It's not completely
 * general purpose, it's designed for running little shell commands -
 * if you try to run something which spews out a huge amount of output
 * you could have trouble.
 *
 * @author   Mark Taylor (Starlink)
 * @since    9 Jun 2005
 */
public abstract class Executor {
    private boolean done_;
    private String out_;
    private String err_;

    /**
     * Returns a system process to be executed.
     *
     * @return   new process
     */
    public abstract Process getProcess() throws IOException;

    /**
     * Returns a representation of the command line executed.
     *
     * @return  command line
     */
    public abstract String getLine();

    /**
     * Returns the standard output which resulted from running the process,
     * if execution was done in capturing mode.
     *
     * @return  stdout content
     */
    public String getOut() {
        return out_;
    }

    /**
     * Returns the standard error which resulted from running the process,
     * if execution was done in capturing mode.
     *
     * @return  stderr content
     */
    public String getErr() {
        return err_;
    }

    /**
     * Calls {@link #getProcess} and attempts to execute it synchronously.
     * Can be run only once on this object.
     *
     * <p>If <code>isCapture</code> is set true, the {@link #getOut} and
     * {@link #getErr} methods can be used after this call to get
     * the results from stdout and stderr.  Otherwise, the process
     * output streams go to the JVM's stdout/stderr.
     *
     * @param  isCapture  true to capture output and return it as
     *                    the outcome message, false to let it go to stdout
     * @return   exit status (0 is OK)
     */
    public int executeSynchronously( boolean isCapture )
            throws IOException, InterruptedException {
        if ( done_ ) {
            throw new IllegalStateException();
        }
        else {
            done_ = true;
        }
        Process proc = getProcess();
        Thread outReader;
        Thread errReader;
        if ( isCapture ) {
            outReader = new ReaderThread( proc.getInputStream() );
            errReader = new ReaderThread( proc.getErrorStream() );
        }
        else {
            outReader = new RedirectThread( proc.getInputStream(), System.out );
            errReader = new RedirectThread( proc.getErrorStream(), System.err );
        }
        outReader.start();
        errReader.start();
        int status = proc.waitFor();
        outReader.join( 500 );
        errReader.join( 500 );
        outReader.interrupt();
        errReader.interrupt();
        out_ = isCapture ? ((ReaderThread) outReader).getData() : null;
        err_ = isCapture ? ((ReaderThread) errReader).getData() : null;
        return status;
    }

    @Override
    public String toString() {
        return getLine();
    }

    /**
     * Returns an executor which executes a command made of words.
     *
     * @param  argv  argument vector
     * @return  executor
     */
    public static Executor createExecutor( final String[] argv ) {
        return new Executor() {
            public Process getProcess() throws IOException {
                return Runtime.getRuntime().exec( argv );
            }
            public String getLine() {
                StringBuffer sbuf = new StringBuffer();
                for ( int i = 0; i < argv.length; i++ ) {
                    if ( i > 0 ) {
                        sbuf.append( ' ' );
                    }
                    sbuf.append( argv[ i ] );
                }
                return sbuf.toString();
            }
        };
    }

    /**
     * Thread which reads the data from an input stream and stores it.
     */
    private static class ReaderThread extends Thread {
        final InputStream strm_;
        final StringBuffer buf_;

        /**
         * Constructor.
         *
         * @param   strm   input stream to read
         */
        ReaderThread( InputStream strm ) {
            strm_ = strm;
            buf_ = new StringBuffer();
        }

        /**
         * Returns the text read from the input stream so far as a string.
         *
         * @return   stream content
         */
        public String getData() {
            return buf_.toString();
        }

        public void run() {
            try {
                for ( int c; ( c = strm_.read() ) >= 0 && ! isInterrupted(); ) {
                    buf_.append( (char) c );
                }
            }
            catch ( IOException e ) {
            }
            finally {
                try {
                    strm_.close();
                }
                catch ( IOException e ) {
                }
            }
        }
    }

    /**
     * Thread which reads the data from an input stream and redirects it.
     */
    private static class RedirectThread extends Thread { 
        final InputStream in_;
        final OutputStream out_;

        /**
         * Constructor.
         *
         * @param  in  source stream
         * @param  out  destination stream
         */
        RedirectThread( InputStream in, OutputStream out ) {
            in_ = in;
            out_ = out;
        }

        public void run() {
            try {
                IOUtils.copy( in_, out_ );
            }
            catch ( IOException e ) {
            }
            finally {
                try {
                    in_.close();
                }
                catch ( IOException e ) {
                }
            }
        }
    }
}
