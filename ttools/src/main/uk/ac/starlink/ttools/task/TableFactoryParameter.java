package uk.ac.starlink.ttools.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.util.FileDataSource;

/**
 * Parameter whose value is a StarTableFactory.
 *
 * @author   Mark Taylor
 */
public class TableFactoryParameter extends Parameter<StarTableFactory> {

    private static final String FILE_OPTION = "file";
    private static final String DIRS_PREFIX = "dirs:";
    private static final String LOCCLASS_PREFIX = "locator:";
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     *
     * @param   name   parameter name
     */
    @SuppressWarnings("this-escape")
    public TableFactoryParameter( String name ) {
        super( name, StarTableFactory.class, true );
        setUsage( FILE_OPTION +
            "|" + DIRS_PREFIX + "..." +
            "|" + LOCCLASS_PREFIX + "..." );
        setPrompt( "Define how input table names are mapped to data" );
        setDescription( new String[] {
            "<p>This parameter determines how input table names",
            "(typically the <code>in</code> parameter",
            "of table processing commands)",
            "are used to acquire references to actual table data.",
            "The default behaviour is for input table names to be treated",
            "as filenames, in conjunction with some file type parameter.",
            "While this is usually sensible for local use, in server",
            "situations it may be inappropriate, since you don't want",
            "external users to have read access to your entire filesystem.",
            "</p>",
            "<p>This parameter gives options for alternative ways of",
            "mapping table names to table data items.",
            "The currently available options are:",
            "<ul>",
            "<li><code>file</code>:",
                 "default behaviour - names are treated as filenames",
                 "</li>",
            "<li><code>" + DIRS_PREFIX
                         + "&lt;dir&gt;" + File.pathSeparator
                         + "&lt;dir&gt;" + File.pathSeparator + "..."
                         + "</code>:",
                 "following the \"<code>dirs:</code>\" prefix",
                 "a list of directories is specified which will be",
                 "searched for the file named.",
                 "Note that the directory separator character differs",
                 "between operating systems;",
                 "it is a colon (\"<code>:</code>\") for Unix-like OSs",
                 "and a semi-colon (\"<code>;</code>\") for MS Windows.",
                 "If a given name is identical to the path-less filename",
                 "in one of the <code>&lt;dir&gt;</code> directories,",
                 "that file is used as the referenced table.",
                 "File type information is ignored in this case, so the files",
                 "must be one of the types which STILTS can autodetect,",
                 "currently FITS or VOTable (FITS is more efficient).",
                 "By using this option, clients can be restricted to using",
                 "a fixed set of tables in a restricted part of the server's",
                 "file system.",
                 "</li>",
            "<li><code>" + LOCCLASS_PREFIX + "&lt;class-name&gt;" + "</code>:",
                 "the <code>&lt;class-name&gt;</code> must be the name",
                 "of a Java class on the classpath which implements",
                 "the interface",
                 "<code>" + TableLocator.class.getName() + "</code>",
                 "and which has a no-arg constructor.",
                 "An instance of this class will be used to resolve names",
                 "to tables.",
                 "</li>",
            "</ul>",
            "</p>",
            "<p>The usage and functionality of this parameter is experimental,",
            "and may change significantly in future releases.",
            "</p>",
        } );
        setStringDefault( FILE_OPTION );
    }

    public StarTableFactory stringToObject( Environment env, String sval )
            throws TaskException {
        try {
            return createTableFactory( sval );
        }
        catch ( UsageException e ) {
            throw new ParameterValueException( this, e );
        }
    }

    /**
     * Decodes a string value which represents a particular
     * prescription for resolving table names to table data,
     * returning a functioning StarTableFactory object which
     * embodies this behaviour.
     *
     * @param  sval  string representation of behaviour
     * @return   table factory
     * @throws  UsageException  if the string value cannot be decoded
     */
    public static StarTableFactory createTableFactory( String sval )
            throws UsageException {
        final StarTableFactory tfact;
        if ( sval == null || sval.trim().length() == 0 ||
             sval.equalsIgnoreCase( FILE_OPTION ) ) {
            tfact = createDefaultTableFactory();
        }
        else if ( sval.toLowerCase().startsWith( DIRS_PREFIX ) ) {
            String dirlist = sval.substring( DIRS_PREFIX.length() );
            String[] dirs = dirlist.split( File.pathSeparator );
            if ( dirs.length == 0 ) {
                throw new UsageException( "No directories specified" );
            }
            tfact = new LocatorStarTableFactory( new DirLocator( dirs ) );
        }
        else if ( sval.toLowerCase().startsWith( LOCCLASS_PREFIX ) ) {
            String clazzname = sval.substring( LOCCLASS_PREFIX.length() );
            TableLocator tloc;
            try {
                tloc = (TableLocator) Class.forName( clazzname )
                                           .getDeclaredConstructor()
                                           .newInstance();
            }
            catch ( Throwable e ) {
                throw new UsageException( "Bad TableLocator class name", e );
            }
            tfact = new LocatorStarTableFactory( tloc );
        }
        else {
            throw new UsageException( "Unknown form; "
                                    + "should be dirs:* or locator:*" );
        }
        return tfact;
    }

    /**
     * Returns a table factory based on a TableLocator instance.
     *
     * @param  locator  locator
     * @return   table factory
     */
    public static StarTableFactory createTableFactory( TableLocator locator ) {
        return new LocatorStarTableFactory( locator );
    }

    /**
     * Returns a table factory with standard characteristics for STILTS.
     *
     * @return  new table factory
     */
    private static StarTableFactory createDefaultTableFactory() {
        StarTableFactory tfact = new StarTableFactory();
        Stilts.addStandardSchemes( tfact );
        return tfact;
    }

    /**
     * StarTableFactory implementation based on a TableLocator.
     */
    private static class LocatorStarTableFactory extends StarTableFactory {

        private final TableLocator locator_;

        /**
         * Constructor.
         *
         * @param  locator   table location resolver
         */
        LocatorStarTableFactory( TableLocator locator ) {
            locator_ = locator;
        }

        public StarTable makeStarTable( String location ) throws IOException {
            return locator_.getTable( location );
        }

        public StarTable makeStarTable( String location, String handler )
                throws IOException {
            return locator_.getTable( location );
        }
    }

    /**
     * TableLocator implementation which looks in a number of given directories.
     */
    private static class DirLocator implements TableLocator {
        private final File[] dirs_;
        private final Map<String,StarTable> tableMap_;
        private final StarTableFactory tfact_;

        /**
         * Constructor.
         *
         * @param  dirs  list of directory names to search
         */
        public DirLocator( String[] dirs ) {
            dirs_ = new File[ dirs.length ];
            for ( int i = 0; i < dirs.length; i++ ) {
                dirs_[ i ] = new File( dirs[ i ] );
                if ( ! dirs_[ i ].isDirectory() ) {
                    logger_.warning( dirs[ i ] + " not a directory" );
                }
            }
            tableMap_ = new WeakHashMap<String,StarTable>();
            tfact_ = createDefaultTableFactory();
        }

        public StarTable getTable( String location ) throws IOException {
            StarTable table = tableMap_.get( location );
            if ( table != null ) {
                return table;
            }
            for ( int id = 0; id < dirs_.length; id++ ) {
                File file = new File( dirs_[ id ], location );
                if ( file.exists() ) {
                    StarTable tbl =
                        tfact_.makeStarTable( new FileDataSource( file ) );
                    tableMap_.put( location, tbl );
                    return tbl;
                }
            }
            if ( StarTableFactory.parseSchemeLocation( location ) != null ) {
                return tfact_.makeStarTable( location );
            }
            throw new FileNotFoundException( "No known table " + location );
        }
    }
}
