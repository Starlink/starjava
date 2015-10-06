package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * DataStoreFactory implementation which allows decoration of the tables
 * used to obtain the data, by providing a custom wrapper for their
 * RowSequence objects.
 *
 * <p>The implementation is fairly ticklish, since various of the objects
 * that we want to wrap have not only to behave (mostly) like the unwrapped
 * objects to which they delegate, but also to be equal to them
 * ({@link uk.ac.starlink.ttools.plot2.Equality}).
 * It works by wrapping each DataSpec in a custom wrapper object any time
 * it is presented to the underlying base DataStoreFactory,
 * either by the methods of this DataStoreFactory implementation
 * or by methods invoked on DataStore objects it dispenses.
 * These wrapper objects obey the equality constraints.
 *
 * @author   Mark Taylor
 * @since    8 Nov 2013
 */
public abstract class WrapperDataStoreFactory implements DataStoreFactory {

    private final DataStoreFactory baseFact_;

    /**
     * Constructor.
     *
     * @param  baseFact  base implementation that actually acquires the data
     */
    protected WrapperDataStoreFactory( DataStoreFactory baseFact ) {
        baseFact_ = baseFact;
    }

    public DataStore readDataStore( DataSpec[] dataSpecs, DataStore prevStore )
            throws IOException, InterruptedException {

        /* Wrap the data specs before the base factory sees them. */
        int nspec = dataSpecs.length;
        DataSpec[] wrapDataSpecs = new DataSpec[ nspec ];
        for ( int i = 0; i < nspec; i++ ) {
            wrapDataSpecs[ i ] = new WrapperDataSpec( dataSpecs[ i ] );
            assert wrapDataSpecs[ i ]
                  .equals( new WrapperDataSpec( dataSpecs[ i ] ) );
        }

        /* Unwrap the prevStore object, so it can be the same object as
         * the base factory dispensed in a previous call, rather than
         * the object this factory dispensed in a previous call.
         * That gives the base factory a chance to recognise and do
         * something clever with it. */
        DataStore basePrevStore = prevStore instanceof WrapperDataStore
                                ? ((WrapperDataStore) prevStore).baseStore_
                                : prevStore;

        /* Let the base factory do its stuff. */
        DataStore baseStore =
            baseFact_.readDataStore( wrapDataSpecs, basePrevStore );

        /* Wrap the returned data store for custom functionality. */
        return new WrapperDataStore( baseStore );
    }

    /**
     * Creates a row sequence from a given table.
     * <p>The obvious implementation is <code>table.getRowSequence()</code>,
     * but implementations may decorate that instance to provide
     * useful functionality.
     *
     * @param   table  table providing data
     * @return   row sequence which will be used to acquire table data
     */
    protected abstract RowSequence createRowSequence( StarTable table )
            throws IOException;

    /**
     * DataStore wrapper implementation.
     * It is a simple wrapper, except that every DataSpec passed to it
     * is wrapped in a WrapperDataSpec.
     */
    private class WrapperDataStore implements DataStore {

        final DataStore baseStore_;
 
        /**
         * Constructor.
         *
         * @param  baseStore  data store to which methods are delegated
         */
        WrapperDataStore( DataStore baseStore ) {
            baseStore_ = baseStore;
        }

        public TupleSequence getTupleSequence( DataSpec spec ) {
            return baseStore_.getTupleSequence( new WrapperDataSpec( spec ) );
        }

        public boolean hasData( DataSpec spec ) {
            return baseStore_.hasData( new WrapperDataSpec( spec ) );
        }
    }

    /**
     * Wraps a DataSpec object.  The only custom functionality added is
     * to fix the source table to return potentially decorated row sequences.
     */
    private class WrapperDataSpec extends AbstractDataSpec {
        private final DataSpec baseSpec_;
        private final StarTable table_;

        /**
         * Constructor.
         *
         * @param  baseSpec   data spec to which most methods are delegated
         */
        public WrapperDataSpec( DataSpec baseSpec ) {
            baseSpec_ = baseSpec;
            final StarTable baseSourceTable = baseSpec.getSourceTable();

            /* Wrap the base table to provide one that will be dispensed
             * by this object.  Note that the equals and hashCode methods
             * are overridden to behave like those of the base table,
             * not of the wrapped one. */
            table_ = new WrapperStarTable( baseSourceTable ) {
                @Override
                public RowSequence getRowSequence() throws IOException {
                    return createRowSequence( baseSourceTable );
                }
                @Override
                public int hashCode() {
                    return baseSourceTable.hashCode();
                }
                @Override
                public boolean equals( Object o ) {
                    return o instanceof WrapperStarTable
                        && ((WrapperStarTable) o).getBaseTable()
                          .equals( this.getBaseTable() );
                }
            };
        }

        public StarTable getSourceTable() {
            return table_;
        }

        public int getCoordCount() {
            return baseSpec_.getCoordCount();
        }

        public Object getMaskId() {
            return baseSpec_.getMaskId();
        }

        public Object getCoordId( int icoord ) {
            return baseSpec_.getCoordId( icoord );
        }

        public Coord getCoord( int icoord ) {
            return baseSpec_.getCoord( icoord );
        }

        public ValueInfo[] getUserCoordInfos( int icoord ) {
            return baseSpec_.getUserCoordInfos( icoord );
        }

        public UserDataReader createUserDataReader() {
            return baseSpec_.createUserDataReader();
        }

        public boolean isCoordBlank( int icoord ) {
            return baseSpec_.isCoordBlank( icoord );
        }
    }
}
