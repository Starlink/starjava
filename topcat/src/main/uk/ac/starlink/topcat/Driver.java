package uk.ac.starlink.topcat;

import java.io.File;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.ErrorDialog;
import uk.ac.starlink.util.Loader;

/**
 * Main class for invoking the TOPCAT application from scratch.
 * Contains some useful static configuration-type methods as well
 * as the {@link #main} method itself.
 *
 * @author   Mark Taylor (Starlink)
 * @since    9 Mar 2004
 */
public class Driver {

    private static boolean standalone = false;
    private static boolean securityChecked;
    private static Boolean canRead;
    private static Boolean canWrite;

    /**
     * Determines whether TableViewers associated with this class should
     * act as a standalone application.  If <tt>standalone</tt> is set
     * true, then it will be possible to exit the JVM using menu items
     * etc in the viewer.  Otherwise, no normal activity within the
     * TableViewer GUI will cause a JVM exit.
     *
     * @param  standalone  whether this class should act as a standalone
     *         application
     */
    public static void setStandalone( boolean standalone ) {
        Driver.standalone = standalone;
    }

    /**
     * Indicates whether the TableViewer application is standalone or not.
     *
     * @return  whether this should act as a standalone application.
     */
    public static boolean isStandalone() {
        return standalone;
    }

    /**
     * Indicates whether the security context will permit reads from local
     * disk.
     *
     * @return  true iff reads are permitted
     */
    public static boolean canRead() {
        checkSecurity();
        return canRead.booleanValue();
    }

    /**
     * Indicates whether the security context will permit writes to local
     * disk.
     *
     * @return  true iff writes are permitted
     */
    public static boolean canWrite() {
        checkSecurity();
        return canWrite.booleanValue();
    }

    /**
     * Talks to the installed security manager to find out what is and
     * is not permitted.
     */
    private static void checkSecurity() {
        if ( ! securityChecked ) {
            SecurityManager sman = System.getSecurityManager();
            if ( sman == null ) {
                canRead = Boolean.TRUE;
                canWrite = Boolean.TRUE;
            }
            else {
                String readDir;
                String writeDir;
                try { 
                    readDir = System.getProperty( "user.home" );
                }
                catch ( SecurityException e ) {
                    readDir = ".";
                }
                try {
                    writeDir = System.getProperty( "java.io.tmpdir" );
                }
                catch ( SecurityException e ) {
                    writeDir = ".";
                }
                try {
                    sman.checkRead( readDir );
                    canRead = Boolean.TRUE;
                }
                catch ( SecurityException e ) {
                    canRead = Boolean.FALSE;
                }
                try {
                    sman.checkWrite( new File( writeDir, "tOpCTeSt.tmp" )
                                    .toString() );
                    canWrite = Boolean.TRUE;
                }
                catch ( SecurityException e ) {
                    canWrite = Boolean.FALSE;
                }
            }
            assert canRead != null;
            assert canWrite != null;
        }
    }

    /**
     * Main method for TOPCAT invocation.
     * Under normal circumstances this will pop up a ControlWindow and
     * populate it with tables named in the arguments.
     *
     * @param  args  list of table specifications
     */
    public static void main( String[] args ) {
        String cmdname;
        try {
            Loader.loadProperties();
            cmdname = System.getProperty( "uk.ac.starlink.topcat.cmdname" );
        }
        catch ( SecurityException e ) {
            // never mind
            cmdname = null;
        }
        if ( cmdname == null ) {
            cmdname = Driver.class.getName();
        }
        String usage = new StringBuffer()
              .append( "Usage:\n" )
              .append( "   " + cmdname + " [table ...]\n" )
              .toString();
        setStandalone( true );

        /* If all we need to do is print a usage message and exit, do it 
         * directly without any GUI action. */
        for ( int i = 0; i < args.length; i++ ) {
            if ( args[ i ].startsWith( "-h" ) ) {
                System.out.println( usage );
                System.exit( 0 );
            }
            else if ( args[ i ].startsWith( "-" ) ) {
                System.err.println( usage );
                System.exit( 1 );
            }
        }

        /* Start up the main control window. */
        final ControlWindow control = ControlWindow.getInstance();

        /* Try to interpret each command line argument as a table
         * specification. */
        for ( int i = 0; i < args.length; i++ ) {
            final String arg = args[ i ];
            try {
                StarTable startab = control.getTableFactory()
                                           .makeStarTable( arg );
                if ( startab == null ) {
                    System.err.println( "No table \"" + arg + "\"" );
                }
                else {
                    final StarTable rtab = Tables.randomTable( startab );
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            control.addTable( rtab, arg, false );
                        }
                    } );
                }
            }
            catch ( final Exception e ) {
                final String msg = "Can't open table \"" + arg + "\"";
                System.err.println( msg );
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        ErrorDialog.showError( e, msg, control );
                    }
                } );
            }
        }
    }
}
