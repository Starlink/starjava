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
        String usage = "votlint [-help] [-debug] [-novalid] [-version 1.0|1.1]"
                  +  "\n        [votable]";

        List argList = new ArrayList( Arrays.asList( args ) );
        boolean debug = false;
        boolean validate = true;
        String systemId = null;
        String version = null;
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.equals( "-debug" ) ) {
                it.remove();
                debug = true;
            }
            else if ( arg.equals( "-novalid" ) ) {
                it.remove();
                validate = false;
            }
            else if ( arg.equals( "-valid" ) ) {
                it.remove();
                validate = true;
            }
            else if ( arg.equals( "-version" ) ) {
                it.remove();
                if ( it.hasNext() ) {
                    version = (String) it.next();
                    it.remove();
                }
                else {
                    System.err.println( usage );
                    System.exit( 1 );
                }
            }
            else if ( arg.equals( "-h" ) || arg.equals( "-help" ) ) {
                System.out.println( usage );
                return;
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

        final LintContext context = new LintContext( version );
        context.setDebug( debug );
        context.setValidating( validate );
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

            /* Buffer the input stream for efficiency. */
            in = new BufferedInputStream( in );

            /* Interpolate the VOTable DOCTYPE declaration if necessary. */
            if ( validate ) {
                DoctypeInterpolator interp = new DoctypeInterpolator() {
                    public void message( String msg ) {
                        context.info( msg );
                    }
                };
                in = interp.getStreamWithDoctype( (BufferedInputStream) in );
                String vers = interp.getVotableVersion();
                if ( vers != null ) {
                    if ( context.getVersion() == null ) {
                        context.setVersion( vers );
                    }
                }
            }

            /* Turn it into a SAX source. */
            InputSource sax = new InputSource( in );
            sax.setSystemId( systemId );

            /* Perform the parse. */
            new Linter( context ).createParser( validate ).parse( sax );
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
