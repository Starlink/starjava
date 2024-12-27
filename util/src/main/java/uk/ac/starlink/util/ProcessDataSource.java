package uk.ac.starlink.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * DataSource that uses the standard output of a System process.
 *
 * @author   Mark Taylor
 * @since    24 Mar 2015 
 */
public class ProcessDataSource extends DataSource {

    private final ProcessBuilder pbuilder_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.util" );
    private static final int NREAD = DEFAULT_INTRO_LIMIT;

    /**
     * Constructor.
     *
     * @param   pbuilder   process builder
     */
    @SuppressWarnings("this-escape")
    public ProcessDataSource( ProcessBuilder pbuilder ) {
        pbuilder_ = pbuilder;
        StringBuffer sbuf = new StringBuffer();
        for ( String word : pbuilder.command() ) {
            sbuf.append( sbuf.length() == 0 ? "(" : " " );
            sbuf.append( word );
        }
        sbuf.append( ")" );
        setName( "<" + sbuf.toString() );
    }

    protected InputStream getRawInputStream() throws IOException {

        /* Create and start the process. */
        logger_.info( "Executing " + pbuilder_.command() );
        Process process = pbuilder_.start();

        /* Read the first few bytes of the input stream and push them back
         * again.  We do this to have a chance of spotting and reporting
         * process execution errors during the execution of this method,
         * since they are most likely to crop up early (probably before
         * the first byte is successfully read), and they cannot easily
         * be reported after the input stream is handed off as the return
         * value of this method. */
        PushbackInputStream pushIn =
            new PushbackInputStream( process.getInputStream(), NREAD );
        byte[] buf = new byte[ NREAD ];
        int count = 0;
        for ( int n;
              ( n = pushIn.read( buf, count, NREAD - count ) ) > 0;
              count += n ) {}
        pushIn.unread( buf, 0, count );

        /* If we have fewer than the requested number of bytes, the input
         * stream must already have terminated.  In that case the process
         * should be finished or nearly finished (that's not a bulletproof
         * assumption, but for the kind of pipeline we expect here, it
         * should be pretty reliable).  So in that case wait for the
         * exit status to find out if completion was successful. */
        Integer status;
        if ( count < NREAD ) {
            try {
                status = Integer.valueOf( process.waitFor() );
            }
            catch ( InterruptedException e ) {
                status = null;
            }
        }
        else {
            status = null;
        }

        /* If the process is finished, read stderr into a string. */
        final String errtxt;
        if ( status != null ) {
            byte[] errbytes =
               IOUtils.readBytes( process.getErrorStream(), 1024 );
            errtxt = new String( errbytes );
        }
        else {
            errtxt = null;
        }

        /* In case of known error status, throw an exception with stderr
         * output as text. */
        if ( status != null && status.intValue() != 0 ) {
            String msg = errtxt == null || errtxt.trim().length() == 0
                       ? "error status " + status
                       : errtxt;
            process.destroy();
            throw new IOException( "System process failed: " + msg );
        }

        /* If there's no known error but the stdout content is zero length,
         * throw an exception, since the point of this is to provide
         * some content from the data source. */
        else if ( count == 0 ) {
            process.destroy();
            throw new IOException( "No output from system process "
                                 + getName() );
        }

        /* Otherwise return the input stream representing the full content
         * of stdout. */
        return pushIn;
    }

    /**
     * Utility method to create a process builder given a shell command line
     * that generates output to standard output.
     * The command line may contain shell syntax like | symbols.
     *
     * <p>This method simply sets up a ProcessBuilder to execute a process
     * with the argv { "sh", "-c", cmdLine }.
     * I haven't tested this exhaustively, but I'd expect it to work on
     * un*x-like systems.  I've got no idea if there's any chance of getting
     * something like this to work on MS Windows, or even if such a thing
     * would be any use.
     *
     * @param  cmdLine  shell command that generates output to stdout
     * @return  process builder to execute cmdLine
     */
    public static ProcessBuilder
            createCommandLineProcessBuilder( String cmdLine ) {
        return new ProcessBuilder( Arrays.asList( new String[] { "sh", "-c",
                                                                 cmdLine } ) );
    }
}
