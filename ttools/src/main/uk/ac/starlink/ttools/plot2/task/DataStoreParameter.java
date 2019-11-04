package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.ttools.plot2.data.CachedDataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.MemoryColumnFactory;
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

    /** Cached storage: data is first read into arrays in memory. */
    public static final DataStoreFactory BASIC_CACHE =
        new CachedDataStoreFactory( new MemoryColumnFactory(), TUPLE_RUNNER );

    /** Smart cached storage: like BASIC_CACHE but tries to spot
     * non-varying columns etc for more efficient storage. */
    public static final DataStoreFactory SMART_CACHE =
        new CachedDataStoreFactory(
            new SmartColumnFactory( new MemoryColumnFactory() ), TUPLE_RUNNER );

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    public DataStoreParameter( String name ) {
        super( name, DataStoreFactory.class );
        addOption( SIMPLE, "simple" );
        addOption( SMART_CACHE, "cache" );
        addOption( BASIC_CACHE, "basic-cache" );
        setDefaultOption( SIMPLE );

        setPrompt( "Data storage policy" );
        setDescription( new String[] {
            "<p>Determines the way that data is accessed when constructing",
            "the plot.",
            "There are two basic options, cached or not.",
            "</p>",
            "<p>If no caching is used (<code>" + getName( SIMPLE ) + "</code>)",
            "then rows are read sequentially from the specified input table(s)",
            "every time they are required.",
            "This generally requires a small memory footprint",
            "(though that can depend on how the table is specified)",
            "and makes sense if the data only needs to be scanned once",
            "or perhaps if the table is very large.",
            "</p>",
            "<p>If caching is used",
            "(<code>" + getName( SMART_CACHE ) + "</code>)",
            "then the required data is read once",
            "from the specified input table(s) and cached",
            "before any plotting is performed,",
            "and plots are done using this cached data.",
            "This may use a significant amount of memory for large tables",
            "but it's usually more sensible (faster)",
            "if the data will need to be scanned multiple times.",
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
        return isCachingSensible ? SMART_CACHE : SIMPLE;
    }
}
