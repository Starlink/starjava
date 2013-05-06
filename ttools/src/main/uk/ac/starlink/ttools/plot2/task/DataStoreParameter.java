package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.ttools.plot2.data.CachedDataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.MemoryColumnFactory;
import uk.ac.starlink.ttools.plot2.data.SmartColumnFactory;
import uk.ac.starlink.ttools.plot2.data.SimpleDataStoreFactory;

/**
 * Parameter to control the way that plot data is cached prior to
 * performing one or more plots.
 *
 * @author   Mark Taylor
 * @since    1 Mark 2013
 */
public class DataStoreParameter extends ChoiceParameter<DataStoreFactory> {

    /** Simple storage: data is read on demand from table every time. */
    public static final DataStoreFactory SIMPLE =
        new SimpleDataStoreFactory();

    /** Cached storage: data is first read into arrays in memory. */
    public static final DataStoreFactory CACHED =
        new CachedDataStoreFactory( new MemoryColumnFactory() );

    /** Smart cached storage: like CACHED but tries to spot non-varying columns
     *  etc for more efficient storage. */
    public static final DataStoreFactory SMART =
        new CachedDataStoreFactory(
            new SmartColumnFactory( new MemoryColumnFactory() ) );

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    public DataStoreParameter( String name ) {
        super( name );
        addOption( SIMPLE, "simple" );
        addOption( CACHED, "cache" );
        addOption( SMART, "smart" );
        setDefaultOption( SIMPLE );
    }

    /**
     * Sets whether caching generally is or is not advised.
     * This affects the default value of this parameter.
     *
     * @param  caching  true if caching is likely to be a good strategy
     */
    public void setDefaultCaching( boolean caching ) {
        setDefaultOption( caching ? SMART : SIMPLE );
    }
}
