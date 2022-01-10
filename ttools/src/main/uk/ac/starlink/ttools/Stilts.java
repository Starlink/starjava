package uk.ac.starlink.ttools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Level;
import uk.ac.starlink.fits.FitsConstants;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableScheme;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.mode.ProcessingMode;
import uk.ac.starlink.ttools.plot2.task.TypedPlot2Task;
import uk.ac.starlink.ttools.scheme.AttractorScheme;
import uk.ac.starlink.ttools.scheme.SkySimScheme;
import uk.ac.starlink.ttools.task.LineInvoker;
import uk.ac.starlink.util.IOUtils;
import uk.ac.starlink.util.Loader;
import uk.ac.starlink.util.ObjectFactory;
import uk.ac.starlink.util.Pair;
import uk.ac.starlink.util.PropertyAuthenticator;
import uk.ac.starlink.util.URLUtils;

/**
 * Top-level class for invoking tasks in the STILTS package.
 * Invoking the main() method with no arguments will write a usage message.
 *
 * @author   Mark Taylor
 * @since    17 Aug 2005
 */
public class Stilts {

    private static ObjectFactory<Task> taskFactory_;
    private static ObjectFactory<TypedPlot2Task<?,?>> plot2TaskFactory_;
    private static ObjectFactory<ProcessingMode> modeFactory_;
    private static TableScheme[] stdSchemes_;
    static { init(); }

    public static final String VERSION_RESOURCE = "stilts.version";

    /**
     * Main method.  Invoked with no arguments, a usage message will be output.
     *
     * @param   args  argument vector
     */
    public static void main( String[] args ) {
        Loader.loadProperties();
        Loader.setHttpAgent( "STILTS" + "/" + getVersion() ); 
        Loader.setDefaultProperty( "java.awt.Window.locationByPlatform",
                                   "true" );
        LineInvoker invoker = new LineInvoker( "stilts", taskFactory_ );
        int status = invoker.invoke( args, new Runnable() {
            public void run() {
                URLUtils.installCustomHandlers();
                FitsConstants.configureHierarch();
                PropertyAuthenticator.installInstance( true );
            }
        } );
        if ( status != 0 ) {
            System.exit( status );
        }
    }

    /**
     * Returns the factory which can create any of the known output modes.
     *
     * @return   factory which creates processing modes
     */
    public static ObjectFactory<ProcessingMode> getModeFactory() {
        return modeFactory_;
    }

    /**
     * Returns the factory which can create any of the known tasks.
     *
     * @return   factory which creates tasks
     */
    public static ObjectFactory<Task> getTaskFactory() {
        return taskFactory_;
    }

    /**
     * Returns the factory which can create plot2 tasks.
     *
     * @return  plot2 task factory
     */
    public static ObjectFactory<TypedPlot2Task<?,?>> getPlot2TaskFactory() {
        return plot2TaskFactory_;
    }

    /**
     * Returns a list of the TableSchemes which are supposed to be
     * available within the Stilts application,
     * but are not in a default StarTableFactory.
     *
     * @return   list of schemes
     */ 
    public static TableScheme[] getStandardSchemes() {
        return stdSchemes_.clone();
    }

    /**
     * Adds the table schemes specific to STILTS to the given table factory.
     * This utility method just adds each element of the result of
     * {@link #getStandardSchemes}.
     *
     * @param  tfact  table factory
     */
    public static void addStandardSchemes( StarTableFactory tfact ) {
        for ( TableScheme scheme : stdSchemes_ ) {
            tfact.addScheme( scheme );
        }
    }

    /**
     * Returns the version number for the STILTS package.
     *
     * @return  version string
     */
    public static String getVersion() {
        return IOUtils.getResourceContents( Stilts.class, VERSION_RESOURCE,
                                            null );
    }

    /**
     * Returns the revision string for the starjava repository, if available.
     *
     * @return  revision string
     */
    public static String getStarjavaRevision() {
        return IOUtils.getResourceContents( Stilts.class, "revision-string",
                                            Level.CONFIG );
    }

    /**
     * Initialises factories.
     */
    private static void init() {

        /* Set up task factories.
         * I feel like it should be possible to do this without suppressing
         * warnings, but I can't work out how. */
        taskFactory_ = new ObjectFactory<>( Task.class );
        @SuppressWarnings({"unchecked","rawtypes"})
        ObjectFactory<TypedPlot2Task<?,?>> p2fact =
            (ObjectFactory<TypedPlot2Task<?,?>>)
            new ObjectFactory( TypedPlot2Task.class );

        /* Populate main task factory with non-plot tasks. */
        plot2TaskFactory_ = p2fact;
        String taskPkg = "uk.ac.starlink.ttools.task.";
        taskFactory_.register( "calc", taskPkg + "Calc" );
        taskFactory_.register( "cdsskymatch", taskPkg + "CdsUploadSkyMatch" );
        taskFactory_.register( "cone", taskPkg + "TableCone" );
        taskFactory_.register( "coneskymatch", taskPkg + "MultiCone" );
        taskFactory_.register( "datalinklint", taskPkg + "DatalinkLint" );
        taskFactory_.register( "funcs", taskPkg + "ShowFunctions" );
        taskFactory_.register( "pixfoot", taskPkg + "PixFootprint" );
        taskFactory_.register( "pixsample", taskPkg + "PixSample" );
        taskFactory_.register( "plot2d", taskPkg + "TablePlot2D" );
        taskFactory_.register( "plot3d", taskPkg + "TablePlot3D" );
        taskFactory_.register( "plothist", taskPkg + "TableHistogram" );
        taskFactory_.register( "regquery", taskPkg + "RegQuery" );
        taskFactory_.register( "server", taskPkg + "StiltsServer" );
        taskFactory_.register( "sqlclient", taskPkg + "SqlClient" );
        taskFactory_.register( "sqlskymatch", taskPkg + "SqlCone" );
        taskFactory_.register( "sqlupdate", taskPkg + "SqlUpdate" );
        taskFactory_.register( "taplint", taskPkg + "TapLint" );
        taskFactory_.register( "tapquery", taskPkg + "TapQuerier" );
        taskFactory_.register( "tapresume", taskPkg + "TapResume" );
        taskFactory_.register( "tapskymatch", taskPkg + "TapUploadSkyMatch" );
        taskFactory_.register( "tcat", taskPkg + "TableCat" );
        taskFactory_.register( "tcatn", taskPkg + "TableCatN" );
        taskFactory_.register( "tcopy", taskPkg + "TableCopy" );
        taskFactory_.register( "tcube", taskPkg + "TableCube" );
        taskFactory_.register( "tgridmap", taskPkg + "GridDensityMap" );
        taskFactory_.register( "tjoin", taskPkg + "TableJoinN" );
        taskFactory_.register( "tloop", taskPkg + "TableLoop" );
        taskFactory_.register( "tmatch1", taskPkg + "TableMatch1" );
        taskFactory_.register( "tmatch2", taskPkg + "TableMatch2" );
        taskFactory_.register( "tmatchn", taskPkg + "TableMatchN" );
        taskFactory_.register( "tmulti", taskPkg + "MultiCopy" );
        taskFactory_.register( "tmultin", taskPkg + "MultiCopyN" );
        taskFactory_.register( "tpipe", taskPkg + "TablePipe" );
        taskFactory_.register( "tskymap", taskPkg + "SkyDensityMap"  );
        taskFactory_.register( "tskymatch2", taskPkg + "SkyMatch2" );
        taskFactory_.register( "votcopy", taskPkg + "VotCopy" );
        taskFactory_.register( "votlint", taskPkg + "VotLint" );

        /* Add entries for plot2 tasks to both the generic factory and
         * the plot2-specific one. */
        String plot2Pkg = "uk.ac.starlink.ttools.plot2.task.";
        for ( Pair<String> pair :
              Arrays.asList(
                  new Pair<String>( "plot2plane", plot2Pkg + "PlanePlot2Task" ),
                  new Pair<String>( "plot2sky", plot2Pkg + "SkyPlot2Task" ),
                  new Pair<String>( "plot2cube", plot2Pkg + "CubePlot2Task" ),
                  new Pair<String>( "plot2sphere",
                                    plot2Pkg + "SpherePlot2Task" ),
                  new Pair<String>( "plot2time", plot2Pkg + "TimePlot2Task" )
              ) ) {
            String name = pair.getItem1(); 
            String clazzName = pair.getItem2();
            taskFactory_.register( name, clazzName );
            plot2TaskFactory_.register( name, clazzName );
        }

        /* Create and populate output mode factory. */
        modeFactory_ =
            new ObjectFactory<ProcessingMode>( ProcessingMode.class );
        String modePkg = "uk.ac.starlink.ttools.mode.";
        modeFactory_.register( "out", modePkg + "CopyMode" );
        modeFactory_.register( "meta", modePkg + "MetadataMode" );
        modeFactory_.register( "stats", modePkg + "StatsMode" );
        modeFactory_.register( "count", modePkg + "CountMode" );
        modeFactory_.register( "checksum", modePkg + "ChecksumMode" );
        modeFactory_.register( "cgi", modePkg + "CgiMode" );
        modeFactory_.register( "discard", modePkg + "NullMode" );
        modeFactory_.register( "topcat", modePkg + "TopcatMode" );
        modeFactory_.register( "samp", modePkg + "SampMode" );
        modeFactory_.register( "plastic", modePkg + "PlasticMode" );
        modeFactory_.register( "tosql", modePkg + "JdbcMode" );
        modeFactory_.register( "gui", modePkg + "SwingMode" );

        stdSchemes_ = new TableScheme[] {
            new SkySimScheme(),
            new AttractorScheme(),
        };
    }
}
