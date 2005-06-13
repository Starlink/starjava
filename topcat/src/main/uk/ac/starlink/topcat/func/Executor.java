package uk.ac.starlink.topcat.func;

import java.io.IOException;
import java.io.InputStream;

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
abstract class Executor {
    private boolean done_;
    private String out_;
    private String err_;

    /**
     * Returns a system process to be executed.
     *
     * @return   new process
     */
    abstract Process getProcess() throws IOException;

    /**
     * Returns a representation of the command line executed.
     *
     * @return  command line
     */
    abstract String getLine();

    /**
     * Returns the standard output which resulted from running the process.
     *
     * @return  stdout
     */
    public String getOut() {
        return out_;
    }

    /**
     * Returns the standard error which resulted from running the process.
     *
     * @return  stderr
     */
    public String getErr() {
        return err_;
    }

    /**
     * Calls {@link #getProcess} and attempts to execute it synchronously.
     * Can be run only once on this object.
     * The {@link #getOut} and {@link #getErr} methods can be used
     * after this call to get the results from stdout and stderr.
     *
     * @return   exit status (0 is OK)
     */
    public int executeSynchronously()
            throws IOException, InterruptedException {
        if ( done_ ) {
            throw new IllegalStateException();
        }
        else {
            done_ = true;
        }
        Process proc = getProcess();
        ReaderThread outReader = new ReaderThread( proc.getInputStream() );
        ReaderThread errReader = new ReaderThread( proc.getErrorStream() );
        outReader.start();
        errReader.start();
        int status = proc.waitFor();
        outReader.join( 500 );
        errReader.join( 500 );
        outReader.interrupt();
        errReader.interrupt();
        out_ = outReader.getData();
        err_ = errReader.getData();
        return status;
    }

    public String toString() {
        return getLine();
    }

    /**
     * Returns an executor which executes a single command line.
     *
     * @param   line   shell command line
     * @return  executor
     */
    public static Executor createExecutor( final String line ) {
        return new Executor() {
            public Process getProcess() throws IOException {
                return Runtime.getRuntime().exec( line );
            }
            public String getLine() {
                return line;
            }
        };
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
     * Thread which reads the data from an input stream.
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

        /**
         * Run method.  Reads the stream as long as it's open and this
         * thread has not been interrupted.
         */
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
}
