package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowData;
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

    private final TupleRunner runner_;

    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2" );

    /**
     * Constructor.
     *
     * @param  runner  tuple runner dispensed with DataStores
     */
    public SimpleDataStoreFactory( TupleRunner runner ) {
        runner_ = runner;
    }

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
        try {
            if ( table.isRandom() && table.getRowCount() > 2 ) {
                return new RandomSimpleTupleSequence( spec, table );
            }
            else {
                RowSequence rseq = spec.getSourceTable().getRowSequence();
                return new SequentialSimpleTupleSequence( spec, rseq );
            }
        }
        catch ( IOException e ) {
            logger_.log( Level.WARNING, "Error reading plot data", e );
            return PlotUtil.EMPTY_TUPLE_SEQUENCE;
        }
    }

    public TupleRunner getTupleRunner() {
        return runner_;
    }

    /**
     * Abstract superclass for tuple sequences dispatched by this factory.
     * It simply reads the user data every time and converts it to
     * storage format as the sequence iterates.
     */
    private static abstract class SimpleTupleSequence implements TupleSequence {

        final DataSpec spec_;
        final RowData rowdata_;
        final UserDataReader reader_;
        final List<Function<Object[],?>> inputStorages_;
        long irow_;
        boolean failed_;

        /**
         * Constructor.
         *
         * @param   spec  data specification
         * @param   rowdata  row data object from which current row data
         *                   is kept available
         */
        SimpleTupleSequence( DataSpec spec, RowData rowdata ) {
            spec_ = spec;
            rowdata_ = rowdata;
            reader_ = spec.createUserDataReader();
            inputStorages_ = new ArrayList<Function<Object[],?>>();
            for ( int ic = 0; ic < spec.getCoordCount(); ic++ ) {
                ValueInfo[] infos = spec.getUserCoordInfos( ic );
                DomainMapper[] dms = spec.getUserCoordMappers( ic );
                inputStorages_.add( spec.getCoord( ic )
                                        .inputStorage( infos, dms ) );
            }
            irow_ = -1;
        }

        public long getRowIndex() {
            return irow_;
        }

        public Object getObjectValue( int icol ) {
            try {
                Object[] userCoords =
                    reader_.getUserCoordValues( rowdata_, irow_, icol );
                Object value = inputStorages_.get( icol ).apply( userCoords );
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
        private final RowSequence rseq_;

        /**
         * Constructor.
         *
         * @param   spec  data specification
         * @param   rseq  row sequence from spec's source table
         */
        SequentialSimpleTupleSequence( DataSpec spec, RowSequence rseq ) {
            super( spec, rseq );
            rseq_ = rseq;
        }

        public boolean next() {
            try {
                while ( ! failed_ && rseq_.next() ) {
                    if ( reader_.getMaskFlag( rseq_, ++irow_ ) ) {
                        return true;
                    }
                }
            }
            catch ( IOException e ) {
                logError( e );
            }
            try {
                rseq_.close();
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
        private final RowAccess racc_;
        private long nrow_;

        /**
         * Constructor for external use.
         *
         * @param   spec  data specification
         * @param   table  source table
         */
        RandomSimpleTupleSequence( DataSpec spec, StarTable table )
                throws IOException {
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
                                   long irow, long nrow ) throws IOException {
            super( spec, table.getRowAccess() );
            table_ = table;
            racc_ = (RowAccess) rowdata_;
            irow_ = irow;
            nrow_ = nrow;
        }

        public boolean next() {
            try {
                while ( ! failed_ && irow_ < nrow_ - 1 ) {
                    if ( reader_.getMaskFlag( racc_, ++irow_ ) ) {
                        racc_.setRowIndex( irow_ );
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
                TupleSequence split;
                try {
                    split = new RandomSimpleTupleSequence( spec_, table_,
                                                           irow_, mid );
                }
                catch ( IOException e ) {
                    logger_.log( Level.WARNING, "Error re-reading plot data",
                                 e );
                    return null;
                }
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
