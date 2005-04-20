package uk.ac.starlink.ttools;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.ttools.lint.DoctypeInterpolator;
import uk.ac.starlink.ttools.lint.LintContext;
import uk.ac.starlink.ttools.lint.Linter;
import uk.ac.starlink.util.DataSource;

/**
 * Top-level class to perform a VOTable lint.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Apr 2005
 */
public class VotLint {

    /**
     * Main method.  Use <tt>-h</tt> flag for usage.
     *
     * @param  args  argument vecctor
     */
    public static void main( String[] args ) {
        String usage = "votlint [-debug] [votable]";

        List argList = new ArrayList( Arrays.asList( args ) );
        boolean debug = false;
        String systemId = null;
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.equals( "-debug" ) ) {
                it.remove();
                debug = true;
            }
            else if ( arg.startsWith( "-h" ) ) {
                System.err.println( usage );
                System.exit( 1 );
            }
            else if ( systemId == null ) {
                it.remove();
                systemId = arg;
            }
        }
        if ( ! argList.isEmpty() ) {
            System.err.println( usage );
            System.exit( 1 );
        }

        final LintContext context = new LintContext();
        context.setDebug( debug );
        try {

            /* Get the input stream. */
            InputStream in;
            if ( systemId == null ) {
                in = System.in;
            }
            else {
                try {
                    in = DataSource.getInputStream( systemId );
                }
                catch ( FileNotFoundException e ) {
                    System.err.println( "No such file " + systemId );
                    System.exit( 1 );
                    throw new AssertionError();
                }
            }

            /* Interpolate the VOTable DOCTYPE declaration if necessary. */
            DoctypeInterpolator interp = new DoctypeInterpolator() {
                public void message( String msg ) {
                    context.info( msg );
                }
            };
            in = interp.getStreamWithDoctype( new BufferedInputStream( in ) );
            String vers = interp.getVotableVersion();
            if ( vers != null ) {
                if ( context.getVersion() == null ) {
                    context.setVersion( vers );
                }
            }

            /* Turn it into a SAX source. */
            InputSource sax = new InputSource( in );
            sax.setSystemId( systemId );

            /* Perform the parse. */
            new Linter( context ).createParser().parse( sax );
            System.exit( 0 );
        }
        catch ( IOException e ) {
            context.message( "ERROR", null, e );
        }
        catch ( SAXException e ) {
            context.message( "ERROR", null, e );
        }
    }
}
