package uk.ac.starlink.plastic;

/**
 * Rudimentary documentation command.
 * The <code>main</code> method is invoked if someone attempts 
 * <code>java -jar plastic.jar</code>.
 * It prints a short message telling them where to go.
 *
 * @author   Mark Taylor
 * @since    14 Jul 2006
 */
class PlasticDoc {

    private static final Class[] cmds = new Class[] {
        PlasticHub.class,
        PlasticMonitor.class,
        PlasticRequest.class,
        HubTester.class,
    };

    public static void main( String[] args ) {
        out();
        out( "This is PlasKit." );
        out();
        out( "As well as a PLASTIC toolkit API, it contains the following" );
        out( "command-line utilities:" );

        Class[] mainTypes = new Class[] { String[].class };
        Object[] mainArgs = new Object[] { new String[] { "-help" } };
        for ( int i = 0; i < cmds.length; i++ ) {
            Class cmd = cmds[ i ];
            try {
                cmd.getMethod( "main", mainTypes ).invoke( null, mainArgs );
            }
            catch ( Exception e ) {
                out( cmd.getName() );
            }
        }
        out();
        out( "For further information see:" );
        out( "    http://www.star.bristol.ac.uk/~mbt/plastic/" );
        out();
    }

    /**
     * Writes a string plus newline.
     *
     * @param   line  text to write
     */
    private static void out( String line ) {
        System.out.println( line );
    }

    /**
     * Writes a newline.
     */
    private static void out() {
        out( "" );
    }
}
