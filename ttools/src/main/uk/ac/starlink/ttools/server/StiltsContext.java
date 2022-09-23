package uk.ac.starlink.ttools.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ForkJoinPool;
import java.util.function.UnaryOperator;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.plot2.SplitRunner;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.DiskCache;
import uk.ac.starlink.ttools.plot2.data.PersistentDataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.TupleRunner;
import uk.ac.starlink.ttools.task.TableFactoryParameter;
import uk.ac.starlink.ttools.task.TableLocator;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.SplitPolicy;
import uk.ac.starlink.util.URLDataSource;

/**
 * Encapsulates servlet context aspects specific to the STILTS server mode.
 *
 * @author   Mark Taylor
 * @since    20 Oct 2008
 */
public class StiltsContext {

    private final ServletContext context_;

    /**
     * Name of the Servlet initialisation parameter which defines the
     * base URL on the server at which the TaskServlet runs.
     * A task can be accessed at the value of this parameter with
     * "/(taskname)" appended.
     */
    public static final String TASKBASE_PARAM = "stiltsTaskBase";

    /**
     * Name of the Servlet initialisation parameter which defines the
     * tasks which this servlet will provide over HTTP.
     * The value of this parameter is a space-separated list of the
     * tasks to provide.  If it is absent or empty, all tasks will be
     * provided.
     */
    public static final String TASKLIST_PARAM = "stiltsTasks";

    /**
     * Name of a Servlet initialisation parameter which can be used to
     * customise table location.
     * If no value is supplied, tables with a leading "/" represent
     * paths in the server filesystem, while others are relative to the
     * servlet root directory.
     * If the value is non-null it will be passed to the
     * {@link
     *    uk.ac.starlink.ttools.task.TableFactoryParameter#createTableFactory}
     * method to come up with a StarTableFactory which is used instead of the
     * default one.
     */
    public static final String TABLEFACTORY_PARAM = "tableFactory";

    /**
     * Name of a Servlet initialisation parameter giving the directory
     * to use for persistent data cache files used during plotting.
     * Files in this directory will not be cleared out by the servlet.
     * If not supplied, the java temporary directory as supplied by the
     * <code>java.io.tmpdir</code> system property is used.
     */
    public static final String CACHEDIR_PARAM = "cacheDir";

    /**
     * Name of a Servlet initialisation parameter indicating the maximum
     * size for the persistent data cache used during plotting.
     * If positive, it's the maximum cache size in bytes.
     * If negative, it's the minimum number of bytes that should be
     * preserved on the cache filesystem.
     * If zero, some adaptive value is used.
     * Note that the cache size may increase beyond this size sometimes.
     */
    public static final String CACHELIMIT_PARAM = "cacheLimit";

    /**
     * Name of a servlet initalisation parameter giving the maximum
     * number of cores to use in the ForkJoinPool used during parallel
     * plotting.  If not supplied, the common pool is used.
     */
    public static final String PLOTPARALLELISM_PARAM = "plotParallelism";

    /**
     * Name of a servlet initialisation parameter giving allowed origins
     * for permissible Cross-Origin requests.  This will provide the
     * content of the CORS <code>Access-Control-Allow-Origin</code> header
     * where that is issued.  If this parameter is not set some default
     * behaviour will be used; that may be allowing all cross-origin
     * requests, depending on the perceived security implications.
     * To deny all requests, set it to the empty string.
     */
    public static final String ALLOWORIGINS_PARAM = "allowOrigins";

    /** 
     * Constructor.
     *
     * @param  context  servlet context which provides the information for
     *                  this object
     */
    public StiltsContext( ServletContext context ) {
        context_ = context;
    }

    /**
     * Acquires a StarTableFactory suitable for use from a servlet
     * from the servlet context.
     *
     * @return  table factory
     */
    public StarTableFactory getTableFactory() throws ServletException {
        String tfactSpec = context_.getInitParameter( TABLEFACTORY_PARAM );
        if ( tfactSpec == null || tfactSpec.trim().length() == 0 ) {
            final StarTableFactory tfact = new StarTableFactory();
            Stilts.addStandardSchemes( tfact );
            final boolean allowAbsolute = true;
            return TableFactoryParameter.createTableFactory( new TableLocator(){
                public StarTable getTable( String loc ) throws IOException {
                    return getServletTable( tfact, loc, allowAbsolute );
                }
            } );
        }
        else {
            try {
                return TableFactoryParameter.createTableFactory( tfactSpec );
            }
            catch ( UsageException e ) {
                throw new ServletException( e );
            }
        }
    }

    /**
     * Returns the server URL below which task servlets can be accessed.
     *
     * @return   base task servlet server URL
     */
    public String getTaskBase() {
        return context_.getInitParameter( TASKBASE_PARAM );
    }

    /**
     * Returns the DataStoreFactory to be used for plots.
     *
     * @return data store factory
     */
    public DataStoreFactory getDataStoreFactory() throws ServletException {
        DiskCache cache =
            createCache( dir -> PersistentDataStoreFactory.toCacheDir( dir ),
                         0.9 );
        TupleRunner tupleRunner = createTupleRunner();
        return new PersistentDataStoreFactory( cache, tupleRunner );
    }

    /**
     * Returns a cache that can be used for caching plotted image files.
     *
     * @return  image cache
     */
    public DiskCache getImageCache() throws ServletException {
        return createCache( dir -> DiskCache.toCacheDir( dir, "plot2-img" ),
                            0.1 );
    }

    /**
     * Returns the intended content of the CORS Access-Control-Allow-Origin
     * header.  A null value should be taken to mean default behaviour,
     * whatever that is in the context of the servlet.
     * An empty string means that no domains are permitted
     * (no Access-Control-Allow-Origin header to be inserted).
     */
    public String getAllowOrigins() throws ServletException {
        return context_.getInitParameter( ALLOWORIGINS_PARAM );
    }

    /**
     * Returns a TupleRunner for use with server tasks.
     *
     * @return  tuple runner
     */
    private TupleRunner createTupleRunner() throws ServletException {
        String ncoreTxt = context_.getInitParameter( PLOTPARALLELISM_PARAM );
        if ( ncoreTxt == null || ncoreTxt.trim().length() == 0 ) {
            return null;
        }
        else {
            int ncore;
            try {
                ncore = Integer.parseInt( ncoreTxt );
            }
            catch ( RuntimeException e ) {
                throw new ServletException( "Unsuitable value \"" + ncoreTxt
                                          + "\" for " + PLOTPARALLELISM_PARAM );
            }
            if ( ncore == ForkJoinPool.getCommonPoolParallelism() ) {
                return null;
            }
            else {
                SplitPolicy policy =
                    new SplitPolicy( () -> new ForkJoinPool( ncore ),
                                     -1, (short) -1 );
                return new TupleRunner( SplitRunner
                                       .createStandardRunner( policy ) );
            }
        }
    }

    /**
     * Returns a DiskCache object for use with a servlet.
     *
     * @param  dirMapping maps a base cache directory to a suitable
     *                    (sub-)directory into which cache files
     *                    will actually be written
     * @param  proportion   proportion of total cache limit that can be
     *                      occupied by the files in this cache before
     *                      some files are deleted
     */
    private DiskCache createCache( UnaryOperator<File> dirMapping,
                                   double proportion )
            throws ServletException {
        String cacheTxt = context_.getInitParameter( CACHEDIR_PARAM );
        String cachelimitTxt = context_.getInitParameter( CACHELIMIT_PARAM );
        File cacheDir = cacheTxt == null
                      ? DiskCache.getSystemTmpDir()
                      : new File( cacheTxt );
        File subDir = dirMapping.apply( cacheDir );
        final long cacheLimit;
        if ( cachelimitTxt == null || cachelimitTxt.trim().length() == 0 ) {
            cacheLimit = 0;
        }
        else {
            try {
                cacheLimit = Long.parseLong( cachelimitTxt );
            }
            catch ( RuntimeException e ) {
                throw new ServletException( "Unsuitable value \""
                                          + cachelimitTxt + "\" for "
                                          + CACHELIMIT_PARAM );
            }
        }
        long pCacheLimit = cacheLimit > 0 ? (long) ( proportion * cacheLimit )
                                          : cacheLimit;
        return new DiskCache( subDir, cacheLimit );
    }

    /**
     * Creates a table with a location relative to the servlet defined
     * by this context.
     *
     * @param  tfact  basic table factory
     * @param  loc   location relative to servlet root
     * @param  allowAbsolute  if true, leading "/" means root of filesystem;
     *                        if false, it means servlet base directory
     */
    private StarTable getServletTable( StarTableFactory tfact, String loc,
                                       boolean allowAbsolute )
            throws IOException {
        if ( StarTableFactory.parseSchemeLocation( loc ) != null ) {
            return tfact.makeStarTable( loc );
        }
        String fsPath = ( allowAbsolute &&
                          loc.length() > 0 && loc.charAt( 0 ) == '/' )
                      ? loc
                      : context_.getRealPath( "/" + loc );
        File file = new File( fsPath );
        if ( file.exists() ) {
            return tfact.makeStarTable( new FileDataSource( file ) );
        }
        URL resource;
        try {
            resource = context_.getResource( "/" + loc );
        }
        catch ( Throwable e ) {
            resource = null;
        }
        if ( resource != null ) {
            return tfact.makeStarTable( new URLDataSource( resource ) );
        }
        throw new FileNotFoundException( "No such resource in servlet: "
                                       + loc );
    }
}
