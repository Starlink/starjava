package uk.ac.starlink.table;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import uk.ac.starlink.util.Loader;

/**
 * Class containing <tt>main</tt> method for copying tables.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TableCopy {

    /**
     * Copies a table from one format to another.
     */
    public static void main( String[] args ) throws IOException {

        /* Ensure that the properties are loaded; this may contain JDBC
         * configuration which should be got before DriverManager might be
         * initialised. */
        Loader.loadProperties();

        /* Get the factory objects. */
        StarTableOutput twriter = new StarTableOutput();
        StarTableFactory treader = new StarTableFactory();

        /* Construct the usage message. */
        StringBuffer usage = new StringBuffer();
        usage.append( "\n   Usage: TableCopy " )
             .append( "[-ofmt " );
        boolean first = true;
        for ( Iterator it = twriter.getKnownFormats().iterator(); 
              it.hasNext(); ) {
            if ( ! first ) { 
                usage.append( '|' );
            }
            usage.append( (String) it.next() );
            first = false;
        }
        usage.append( "] " )
             .append( "in-table " )
             .append( "out-table " )
             .append( "\n" );

        /* Process the command line arguments. */
        String ofmt = null;
        String iloc = null;
        String oloc = null;
        for ( Iterator it = Arrays.asList( args ).iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.startsWith( "-" ) && arg.length() > 1 ) {
                if ( arg.equals( "-ofmt" ) ) {
                    if ( it.hasNext() ) {
                        ofmt = (String) it.next();
                    }
                    else {
                        System.err.println( usage );
                        System.exit( 1 );
                    }
                }
                else {
                    System.err.println( usage );
                    System.exit( 1 );
                }
            }
            else if ( iloc == null ) {
                iloc = arg;
            }
            else if ( oloc == null ) {
                oloc = arg;
            }
            else {
                System.err.println( usage );
                System.exit( 1 );
            }
        }

        if ( iloc == null || oloc == null ) {
            System.err.println( usage );
            System.exit( 1 );
        }

        /* Get the input table. */
        StarTable startab = treader.makeStarTable( iloc );

        /* Write it to the requested destination. */
        twriter.writeStarTable( startab, oloc, ofmt );
    }
 
}
