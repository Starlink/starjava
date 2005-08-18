package uk.ac.starlink.ttools.build;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Captures the output of a java main() method, wraps it in a CDATA marked
 * section, and outputs it.  This class is designed to be used from
 * its main() method.
 *
 * @author   Mark Taylor
 * @since    17 Aug 2005
 */
public class OutputCapture {

    private final Class clazz_;
    private final String[] args_;

    private OutputCapture( Class clazz, String[] args ) {
        clazz_ = clazz;
        args_ = args;
    }

    private String getXml() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream pout = new PrintStream( bout );
        PrintStream oldOut = System.out;
        System.setOut( pout );
        try {
            clazz_.getMethod( "main", new Class[] { String[].class } )
                  .invoke( null, new Object[] { args_ } );
            pout.flush();
        }
        finally {
            System.setOut( oldOut );
        }
        String stdout = new String( bout.toByteArray() )
                       .replaceFirst( "\\A\\n*", "" )
                       .replaceFirst( "\\n*\\Z", "" );
        String[] lines = stdout.split( "\\n" );
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "<![CDATA[\n" );
        for ( int i = 0; i < lines.length; i++ ) {
            String line = lines[ i ];
            if ( line.trim().length() > 0 ) {
                sbuf.append( "   " )
                    .append( line );
            }
            sbuf.append( '\n' );
        }
        sbuf.append( "]]>" );
        return sbuf.toString();
    }

    /**
     * Writes CDATA-wrapped output.
     * First element of <code>args</code> is the name of a class,
     * subsequent elements are arguments to be passed to the main
     * method of that class.
     * Blank lines are stripped and a blank is prepended to each line.
     *
     * @param   args  argument vector
     */
    public static void main( String[] args ) throws Exception {
        Class clazz = Class.forName( args[ 0 ] );
        String[] subArgs = new String[ args.length - 1 ];
        System.arraycopy( args, 1, subArgs, 0, args.length - 1 );
        System.out.print( new OutputCapture( clazz, subArgs ).getXml() );
    }
}
