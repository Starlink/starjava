package uk.ac.starlink.ttools;

import java.io.IOException;
import java.io.InputStream;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.mode.ProcessingMode;
import uk.ac.starlink.ttools.task.LineInvoker;
import uk.ac.starlink.util.IOUtils;
import uk.ac.starlink.util.Loader;
import uk.ac.starlink.util.URLUtils;

/**
 * Top-level class for invoking tasks in the STILTS package.
 * Invoking the main() method with no arguments will write a usage message.
 *
 * @author   Mark Taylor
 * @since    17 Aug 2005
 */
public class Stilts {

    private static ObjectFactory taskFactory_;
    private static ObjectFactory modeFactory_;
    static { init(); }

    public static final String VERSION_RESOURCE = "stilts.version";

    /**
     * Main method.  Invoked with no arguments, a usage message will be output.
     *
     * @param   args  argument vector
     */
    public static void main( String[] args ) {
        Loader.loadProperties();
        URLUtils.installCustomHandlers();
        new LineInvoker( "stilts", taskFactory_ ).invoke( args );
    }

    /**
     * Returns the factory which can create any of the known output modes.
     *
     * @return   factory which creates
     *           {@link uk.ac.starlink.ttools.mode.ProcessingMode} objects.
     */
    public static ObjectFactory getModeFactory() {
        return modeFactory_;
    }

    /**
     * Returns the factory which can create any of the known tasks.
     *
     * @return   factory which creates
     *           {@link uk.ac.starlink.task.Task} objects.
     */
    public static ObjectFactory getTaskFactory() {
        return taskFactory_;
    }

    /**
     * Returns the version number for the STILTS package.
     *
     * @return  version string
     */
    public static String getVersion() {
        return IOUtils.getResourceContents( Stilts.class, VERSION_RESOURCE );
    }

    /**
     * Initialises factories.
     */
    private static void init() {
        taskFactory_ = new ObjectFactory( Task.class );
        String taskPkg = "uk.ac.starlink.ttools.task.";
        taskFactory_.register( "calc", taskPkg + "Calc" );
        taskFactory_.register( "multicone", taskPkg + "MultiCone" );
        taskFactory_.register( "regquery", taskPkg + "RegQuery" );
        taskFactory_.register( "tcat2", taskPkg + "TableCat2" );
        taskFactory_.register( "tcopy", taskPkg + "TableCopy" );
        taskFactory_.register( "tcube", taskPkg + "TableCube" );
        taskFactory_.register( "tmatch2", taskPkg + "TableMatch2" );
        taskFactory_.register( "tpipe", taskPkg + "TablePipe" );
        taskFactory_.register( "votcopy", taskPkg + "VotCopy" );
        taskFactory_.register( "votlint", taskPkg + "VotLint" );

        modeFactory_ = new ObjectFactory( ProcessingMode.class );
        String modePkg = "uk.ac.starlink.ttools.mode.";
        modeFactory_.register( "out", modePkg + "CopyMode" );
        modeFactory_.register( "meta", modePkg + "MetadataMode" );
        modeFactory_.register( "stats", modePkg + "StatsMode" );
        modeFactory_.register( "count", modePkg + "CountMode" );
        modeFactory_.register( "cgi", modePkg + "CgiMode" );
        modeFactory_.register( "discard", modePkg + "NullMode" );
        modeFactory_.register( "topcat", modePkg + "TopcatMode" );
        modeFactory_.register( "plastic", modePkg + "PlasticMode" );
        modeFactory_.register( "tosql", modePkg + "JdbcMode" );
    }

}
