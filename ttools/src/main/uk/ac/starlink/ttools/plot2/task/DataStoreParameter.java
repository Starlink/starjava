package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.ttools.plot2.data.ByteStoreColumnFactory;
import uk.ac.starlink.ttools.plot2.data.CachedDataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.DiskCache;
import uk.ac.starlink.ttools.plot2.data.MemoryColumnFactory;
import uk.ac.starlink.ttools.plot2.data.PersistentDataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.SmartColumnFactory;
import uk.ac.starlink.ttools.plot2.data.SimpleDataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.TupleRunner;

/**
 * Parameter to control the way that plot data is cached prior to
 * performing one or more plots.
 *
 * @author   Mark Taylor
 * @since    1 Mark 2013
 */
public class DataStoreParameter extends ChoiceParameter<DataStoreFactory> {

    /** TupleRunner instance used for DataStoreFactories. */
    private static final TupleRunner TUPLE_RUNNER = TupleRunner.DEFAULT;

    /** Simple storage: data is read on demand from table every time. */
    public static final DataStoreFactory SIMPLE =
        new SimpleDataStoreFactory( TUPLE_RUNNER );

    /** Memory-cached storage: data is first read into arrays in memory. */
    public static final DataStoreFactory BASIC_CACHE =
        new CachedDataStoreFactory( new MemoryColumnFactory(), TUPLE_RUNNER );

    /** Smart memory-cached storage: like BASIC_CACHE but tries to spot
     * non-varying columns etc for more efficient storage. */
    public static final DataStoreFactory MEMORY_CACHE =
        new CachedDataStoreFactory(
            new SmartColumnFactory( new MemoryColumnFactory() ), TUPLE_RUNNER );

    public static final DataStoreFactory PARALLEL_MEMORY_CACHE =
        new CachedDataStoreFactory(
            new SmartColumnFactory( new MemoryColumnFactory() ), TUPLE_RUNNER,
            RowRunner.DEFAULT ) {};

    /** Smart disk-cached storage. */
    public static final DataStoreFactory DISK_CACHE =
        new CachedDataStoreFactory(
                new SmartColumnFactory(
                    new ByteStoreColumnFactory( StoragePolicy.PREFER_DISK ) ),
                TUPLE_RUNNER );

    /** Smart cached storage based on the default Storage Policy. */
    public static final DataStoreFactory POLICY_CACHE =
        new CachedDataStoreFactory(
                new SmartColumnFactory(
                    new ByteStoreColumnFactory( StoragePolicy
                                               .getDefaultPolicy() ) ),
                TUPLE_RUNNER );

    /** Persistent cached storage in default scratch directory. */
    public static final DataStoreFactory PERSISTENT_CACHE =
        new PersistentDataStoreFactory( (DiskCache) null, TUPLE_RUNNER );

    /** Copy of MEMORY_CACHE. */
    private static final DataStoreFactory MEMORY1_CACHE =
        new CachedDataStoreFactory(
            new SmartColumnFactory( new MemoryColumnFactory() ), TUPLE_RUNNER );

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    @SuppressWarnings("this-escape")
    public DataStoreParameter( String name ) {
        super( name, DataStoreFactory.class );
        addOption( SIMPLE, "simple" );
        addOption( MEMORY_CACHE, "memory" );
        addOption( DISK_CACHE, "disk" );
        addOption( POLICY_CACHE, "policy" );
        addOption( MEMORY1_CACHE, "cache" );
        addOption( BASIC_CACHE, "basic-cache" );
        addOption( PERSISTENT_CACHE, "persistent" );
        addOption( PARALLEL_MEMORY_CACHE, "parallel" );
        setDefaultOption( SIMPLE );

        setPrompt( "Data storage policy" );
        setDescription( new String[] {
            "<p>Determines the way that data is accessed when constructing",
            "the plot.",
            "There are two main options, cached or not.",
            "If no caching is used",
            "then rows are read sequentially from the specified input table(s)",
            "every time they are required.",
            "This generally requires a small resource footprint",
            "(though that can depend on how the table is specified)",
            "and makes sense if the data only needs to be scanned once",
            "or perhaps if the table is very large.",
            "If caching is used",
            "then the required data is read once",
            "from the specified input table(s), then prepared and cached",
            "before any plotting is performed,",
            "and plots are done using this cached data.",
            "This may use a significant amount of storage for large tables",
            "but it's usually more sensible (faster)",
            "if the data will need to be scanned multiple times.",
            "There are various options for cache storage.",
            "</p>",
            "<p>The options are:",
            "<ul>",
            "<li><code>simple</code>: ",
                "no caching, data read directly from input table",
                "</li>",
            "<li><code>memory</code>: ",
                "cached to memory; OutOfMemoryError possible",
                "for very large plots",
                "</li>",
            "<li><code>disk</code>: ",
                "cached to disk",
                "</li>",
            "<li><code>policy</code>: ",
                "cached using application-wide default storage policy,",
                "which is usually <em>adaptive</em> (memory/disk hybrid)",
                "</li>",
            "<li><code>persistent</code>: ",
                "cached to persistent files on disk,",
                "in the system temporary directory",
                "(defined by system property <code>java.io.tmpdir</code>).",
                "If this is used, plot data will be stored on disk in a way",
                "that means they can be re-used between STILTS invocations,",
                "so data preparation can be avoided on subsequent runs.",
                "Note however it can leave potentially large files",
                "in your temporary directory.",
                "</li>",
            "<li><code>cache</code>: ",
                "synonym for <code>memory</code> (backward compatibility)",
                "</li>",
            "<li><code>basic-cache</code>: ",
                "dumber version of <code>memory</code>",
                "(no optimisation for constant-valued columns)",
                "</li>",
            "<li><code>parallel</code>: ",
                "experimental version of memory-based cache that reads",
                "into the cache in parallel for large files.",
                "This will make the plot faster to prepare,",
                "but interaction is a bit slower and sequence-dependent",
                "attributes of the plot may not come out right.",
                "This experimental option may be withdrawn or modified",
                "in future releases.",
                "</li>",
            "</ul>",
            "</p>",
        } );
    }

    /**
     * Sets whether caching generally is or is not advised.
     * This affects the default value of this parameter.
     *
     * @param  caching  true if caching is likely to be a good strategy
     */
    public void setDefaultCaching( boolean caching ) {
        setDefaultOption( getDefaultForCaching( caching ) );
    }

    /**
     * Returns the default value for this parameter based on whether
     * caching is believed to be a good idea.
     *
     * @param  isCachingSensible  true if caching is likely
     *                            to be a good strategy
     * @return  best default option
     */
    public DataStoreFactory getDefaultForCaching( boolean isCachingSensible ) {
        return isCachingSensible ? MEMORY_CACHE : SIMPLE;
    }
}
