package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * DataStoreFactory implementation that does no caching.
 * It reads the data as required every time.
 * This has low memory requirements.
 * It may also be faster to use for one-pass plots, but probably not if
 * the same column is used for multiple purposes.
 *
 * @author   Mark Taylor
 * @since    11 Feb 2013
 */
public class SimpleDataStoreFactory implements DataStoreFactory, DataStore {

    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2" );

    public boolean hasData( DataSpec spec ) {
        return true;
    }

    /**
     * Executes instantly and returns this object.
     */
    public DataStore readDataStore( DataSpec[] specs, DataStore prevStore ) {
        return this;
    }

    public TupleSequence getTupleSequence( DataSpec spec ) {
        StarTable table = spec.getSourceTable();
        if ( table.isRandom() && table.getRowCount() > 2 ) {
            return new RandomSimpleTupleSequence( spec, table );
        }
        else {
            try {
                RowSequence rseq = spec.getSourceTable().getRowSequence();
                return new SequentialSimpleTupleSequence( spec, rseq );
            }
            catch ( IOException e ) {
                logger_.log( Level.WARNING, "Error reading plot data", e );
                return PlotUtil.EMPTY_TUPLE_SEQUENCE;
            }
        }
    }

    /**
     * Utility method to work out the domain mappers for a given
     * coordinate of a DataSpec.
     * For the requested coord, it returns a mapper array with elements
     * filled, in with any mapper known for the given input coordinates
     * that has the sub-type appropriate for that coordinate.
     *
     * @param   dataSpec  data specification object
     * @param   icoord   index of coordinate in <code>dataSpec</code>
     * @return   mapper array for decoding values of one coordinate
     *           of a data spec
     */
    public static DomainMapper[] getUserCoordMappers( DataSpec dataSpec,
                                                      int icoord ) {
        ValueInfo[] userInfos = dataSpec.getUserCoordInfos( icoord );
        Input[] inputs = dataSpec.getCoord( icoord ).getInputs();
        int nu = inputs.length;
        DomainMapper[] mappers = new DomainMapper[ nu ];
        for ( int iu = 0; iu < nu; iu++ ) {
            Class<? extends DomainMapper> reqClazz = inputs[ iu ].getDomain();
            if ( reqClazz != null ) {
                DomainMapper[] infoMappers = userInfos[ iu ].getDomainMappers();
                for ( int im = 0;
                      im < infoMappers.length && mappers[ iu ] == null;
                      im++ ) {
                    if ( reqClazz.isInstance( infoMappers[ im ] ) ) {
                        mappers[ iu ] = infoMappers[ im ];
                    }
                }
            }
        }
        return mappers;
    }

    /**
     * Abstract superclass for tuple sequences dispatched by this factory.
     * It simply reads the user data every time and converts it to
     * storage format as the sequence iterates.
     */
    private static abstract class SimpleTupleSequence implements TupleSequence {

        final DataSpec spec_;
        final UserDataReader reader_;
        final DomainMapper[][] mappers_;
        RowSequence rowseq_;
        long irow_;
        boolean failed_;

        /**
         * Constructor.  Subclasses must initialise the rowseq_ member.
         *
         * @param   spec  data specification
         */
        SimpleTupleSequence( DataSpec spec ) {
            spec_ = spec;
            reader_ = spec.createUserDataReader();
            int nc = spec.getCoordCount();
            mappers_ = new DomainMapper[ nc ][];
            for ( int ic = 0; ic < nc; ic++ ) {
                mappers_[ ic ] = getUserCoordMappers( spec, ic );
            }
            irow_ = -1;
        }

        public long getRowIndex() {
            return irow_;
        }

        public Object getObjectValue( int icol ) {
            try {
                Object[] userCoords =
                    reader_.getUserCoordValues( rowseq_, irow_, icol );
                Object value =
                    spec_.getCoord( icol )
                         .inputToStorage( userCoords, mappers_[ icol ] );
                assert value != null;
                return value;
            }
            catch ( IOException e ) {
                logError( e );
                return null;
            }
        }

        public double getDoubleValue( int icol ) {
            Object obj = getObjectValue( icol );
            return obj instanceof Number ? ((Number) obj).doubleValue()
                                         : Double.NaN;
        }

        public int getIntValue( int icol ) {
            Object obj = getObjectValue( icol );
            return obj instanceof Number ? ((Number) obj).intValue()
                                         : Integer.MIN_VALUE;
        }

        public long getLongValue( int icol ) {
            Object obj = getObjectValue( icol );
            return obj instanceof Number ? ((Number) obj).longValue()
                                         : Long.MIN_VALUE;
        }

        public boolean getBooleanValue( int icol ) {
            return Boolean.TRUE.equals( getObjectValue( icol ) );
        }

        void logError( IOException err ) {
            failed_ = true;
            logger_.log( Level.WARNING,
                         "Error reading plot data - truncating sequence", err );
        }
    }

    /**
     * SimpleTupleSequence concrete implementation for sequential data;
     * there is no attempt to split.
     */
    private static class SequentialSimpleTupleSequence
            extends SimpleTupleSequence {

        /**
         * Constructor.
         *
         * @param   spec  data specification
         * @param   rseq  row sequence from spec's source table
         */
        SequentialSimpleTupleSequence( DataSpec spec, RowSequence rseq ) {
            super( spec );
            rowseq_ = rseq;
        }

        public boolean next() {
            try {
                while ( ! failed_ && rowseq_.next() ) {
                    if ( reader_.getMaskFlag( rowseq_, ++irow_ ) ) {
                        return true;
                    }
                }
            }
            catch ( IOException e ) {
                logError( e );
            }
            try {
                rowseq_.close();
            }
            catch ( IOException e ) {
                // ignore
            }
            return false;
        }

        public TupleSequence split() {
            return null;
        }

        public long splittableSize() {
            return spec_.getSourceTable().getRowCount();
        }
    }

    /**
     * SimpleTupleSequence concrete implementation for random access data;
     * it can be split.
     */
    private static class RandomSimpleTupleSequence extends SimpleTupleSequence {

        private final StarTable table_;
        private long nrow_;

        /**
         * Constructor for external use.
         *
         * @param   spec  data specification
         * @param   table  source table
         */
        RandomSimpleTupleSequence( DataSpec spec, StarTable table ) {
            this( spec, table, -1, table.getRowCount() );
        }

        /**
         * Constructor for internal use (recursion).
         *
         * @param   spec  data specification
         * @param   table  source table
         * @param   irow   initial row index
         * @param   nrow   first index after iteration run
         */
        RandomSimpleTupleSequence( DataSpec spec, StarTable table,
                                   long irow, long nrow ) {
            super( spec );
            rowseq_ = new RowSequence() {
                public Object getCell( int icol ) throws IOException {
                    return table_.getCell( irow_, icol );
                }
                public Object[] getRow() throws IOException {
                    return table_.getRow( irow_ );
                }
                public boolean next() {
                    return ++irow_ < nrow_;
                }
                public void close() {
                }
            };
            table_ = table;
            irow_ = irow;
            nrow_ = nrow;
        }

        public boolean next() {
            try {
                while ( ! failed_ && irow_ < nrow_ - 1 ) {
                    if ( reader_.getMaskFlag( rowseq_, ++irow_ ) ) {
                        return true;
                    }
                }
            }
            catch ( IOException e ) {
                logError( e );
            }
            return false;
        }

        public TupleSequence split() {
            if ( nrow_ - irow_ > 2 ) {
                long mid = ( irow_ + nrow_ ) / 2;
                TupleSequence split =
                    new RandomSimpleTupleSequence( spec_, table_, irow_, mid );
                irow_ = mid - 1;
                return split;
            }
            else {
                return null;
            }
        }

        public long splittableSize() {
            return nrow_ - irow_;
        }
    }
}
