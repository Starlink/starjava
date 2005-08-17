package uk.ac.starlink.ttools.build;

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

    private void writeXml() throws Exception {
        System.out.println( "<![CDATA[" );
        clazz_.getMethod( "main", new Class[] { String[].class } )
              .invoke( null, new Object[] { args_ } );
        System.out.println( "]]>" );
        System.out.flush();
    }

    /**
     * Writes CDATA-wrapped output.
     * First element of <code>args</code> is the name of a class,
     * subsequent elements are arguments to be passed to the main
     * method of that class.
     *
     * @param   args  argument vector
     */
    public static void main( String[] args ) throws Exception {
        Class clazz = Class.forName( args[ 0 ] );
        String[] subArgs = new String[ args.length - 1 ];
        System.arraycopy( args, 1, subArgs, 0, args.length - 1 );
        new OutputCapture( clazz, subArgs ).writeXml();
    }
}
