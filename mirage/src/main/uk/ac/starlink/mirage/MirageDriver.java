package uk.ac.starlink.mirage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.TableLoadPanel;
import uk.ac.starlink.table.gui.FilestoreTableLoadDialog;
import uk.ac.starlink.util.AuxClassLoader;

/**
 * Main application class for the StarTable Mirage front-end.
 * The class named by {@link #MIRAGE_CLASS} must be on the JVM's classpath
 * in order for this to work.  No Mirage components need to be available
 * during compilation or distribution of this code however.
 * This is desirable since Mirage is distributed under a more restrictive
 * licence than the GPL.
 *
 * @author   Mark Taylor (Starlink)
 * @see      <a href="http://www.bell-labs.com/project/mirage/">Mirage</a>
 */
public class MirageDriver {

    /** Name of the main Mirage application class. */
    public static final String MIRAGE_CLASS = "mirage.Mirage";

    /** Name of the property used to locate the {@link #MIRAGE_CLASS} class. */
    public static final String MIRAGE_CLASSPATH_PROP = 
        "uk.ac.starlink.mirage.class.path";

    /**
     * Invoke the Mirage application on a list of named StarTables.
     * Using the <code>-d</code> flag this command can also be used to
     * dump the mirage-formatted version of a given table to 
     * standard output.
     *
     * @param  args  flags and names of the StarTables to pass to Mirage
     */
    public static void main( String[] args )
            throws IOException, Exception {
        StarTable table = null;
        boolean dump = false;
        String usage = 
            "Usage:\n" +
            "   MirageDriver [-help] | [-dump] table | [mirageflags] table\n";
        List mirageArgs = new ArrayList();

        /* If invoked with no command line arguments (direct from jar file?) 
         * use a dialog box to get a table. */
        if ( args.length == 0 ) {
            StarTable[] tables = 
                TableLoadPanel
               .loadTables( null, new FilestoreTableLoadDialog(),
                            new StarTableFactory( false ) );
            table = tables != null && tables.length > 0 
                  ? tables[ 0 ]
                  : null;
            if ( table == null ) {
                System.err.println( "No table selected" );
                System.exit( 1 );
            }
        }

        /* If we have command line arguments, process them. */
        else {
            String tableName = null;
            for ( Iterator it = Arrays.asList( args ).iterator(); 
                  it.hasNext(); ) {
                String arg = (String) it.next();

                /* Dump flag. */
                if ( arg.equals( "-dump" ) ) {
                    dump = true;
                }

                /* Help flag - show usage and bail out. */
                else if ( arg.equals( "-help" ) || arg.equals( "-h" ) ) {
                    System.out.println( usage );
                    System.exit( 0 );
                }

                /* Known flags to pass on to Mirage. */
                else if ( arg.equals( "-log" ) ||
                          arg.equals( "-cmd" ) ||
                          arg.equals( "-path" ) ) {
                    try {
                        mirageArgs.add( arg );
                        mirageArgs.add( (String) it.next() );
                    }
                    catch ( NoSuchElementException e ) {
                        System.err.println( usage );
                        System.exit( 1 );
                    }
                }
                else if ( arg.equals( "-off" ) ) {
                    mirageArgs.add( arg );
                }
                else if ( arg.startsWith( "-" ) ) {
                    mirageArgs.add( arg );
                }

                /* Assume it's a table. */
                else {
    
                    /* Bail out if we already have one. */
                    if ( tableName != null ) {
                        System.err.println( usage );
                        System.exit( 1 );
                    }
                    else {
                        tableName = arg;
                    }
                }
            }
            if ( tableName == null ) {
                System.err.println( usage );
                System.exit( 1 );
            }

            /* Get a StarTable from the supplied name. */
            try {
                table = new StarTableFactory( false )
                       .makeStarTable( tableName );
            }
            catch ( IOException e ) {
                System.err.println( e );
                System.exit( 1 );
            }
        }

        /* Either dump the tables to standard output. */
        if ( dump ) {
            MirageFormatter mf = new MirageFormatter( System.out );
            mf.writeMirageFormat( table );
        }

        /* Or attempt to invoke Mirage on them. */
        else {

            /* Invoke Mirage if possible. */
            if ( isMirageAvailable() ) {
                invokeMirage( table, mirageArgs );
            }

            /* Otherwise explain why we can't. */
            else {
                System.err.println( "Mirage application not found." );
                System.err.println( 
                    "The class " + MIRAGE_CLASS + " must exist on the path " +
                    "named by the property " + MIRAGE_CLASSPATH_PROP );
                System.exit( 1 );
            }
        }
    }

    /**
     * Indicates whether the Mirage application is available in this JVM.
     * Unless the relevant class is on the classpath, it won't be.
     * The {@link #invokeMirage} method will only work if this method
     * returns <code>true</code>.
     *
     * @return  <code>true</code> iff the class MIRAGE_CLASS does not
     *         exist on the path named by the property MIRAGE_CLASSPATH_PROP
     */
    public static boolean isMirageAvailable() {
        try {
            String miragePath = System.getProperty( MIRAGE_CLASSPATH_PROP );
            Class mirageClass = new AuxClassLoader( miragePath )
                               .loadClass( MIRAGE_CLASS );
            return true;
        }
        catch ( ClassNotFoundException e ) {
            return false;
        }
    }

    /**
     * Invokes the Mirage application on a StarTable object.
     *
     * @param  table  the StarTable to pass to Mirage
     * @param  margs  a list of other arguments (Strings) to pass as arguments
     *         to Mirage
     * @throws ClassNotFoundException  if {@link #isMirageAvailable} 
     *         returns <code>false</code>
     */
    public static void invokeMirage( StarTable table, List margs )
            throws ClassNotFoundException, Exception {

        /* Get the class of the Mirage application - this may throw a
         * ClassNotFoundException. */
        String miragePath = System.getProperty( MIRAGE_CLASSPATH_PROP );
        Class mirageClass = new AuxClassLoader( miragePath )
                           .loadClass( MIRAGE_CLASS );

        /* Create a temporary file to hold the data for this table. */
        File tmpfile = File.createTempFile( "mdata", ".dat" );
        tmpfile.deleteOnExit();

        /* Write the table data to the temp file in Mirage format. */
        OutputStream ostrm = new FileOutputStream( tmpfile );
        PrintStream pstrm = new PrintStream( ostrm );
        new MirageFormatter( pstrm ).writeMirageFormat( table );
        ostrm.close();
        pstrm.close();

        /* Accumulate loading commands to pass to Mirage. */
        if ( margs == null ) {
            margs = new ArrayList();
        }
        margs.add( "-data" );
        margs.add( tmpfile.getPath() );

        /* Invoke Mirage with the prepared data. */
        invokeMain( mirageClass, (String[]) margs.toArray( new String[ 0 ] ) );
    }

    /**
     * Invokes the <code>main</code> method of a given class
     * with given arguments.
     *
     * @param   clazz  the class containing a 
     *                 <code>public static main(String[])</code> method
     * @param   args   command-line arguments for the Mirage application
     * @throws  Exception  if various other things go wrong
     */
    private static void invokeMain( Class clazz, String[] args )
            throws Exception {

        /* Locate the static main() method. */
        Class[] mainArgsClasses = new Class[] { String[].class };
        Method mainMethod = clazz.getMethod( "main", mainArgsClasses );

        /* Invoke it with the given arguments. */
        mainMethod.invoke( null, new Object[] { args } );
    }

}
