package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.RowSequence;
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
        try {
            RowSequence rseq = spec.getSourceTable().getRowSequence();
            return new SimpleTupleSequence( spec, rseq );
        }
        catch ( IOException e ) {
            logger_.log( Level.WARNING, "Error reading plot data", e );
            return PlotUtil.EMPTY_TUPLE_SEQUENCE;
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
     * TupleSequence implementation for use with SimpleDataStoreFactory.
     * It simply reads the user data every time and converts it to
     * storage format as the sequence iterates.
     */
    private static class SimpleTupleSequence implements TupleSequence {
        private final DataSpec spec_;
        private final UserDataReader reader_;
        private final RowSequence baseSeq_;
        private final DomainMapper[][] mappers_;
        private long irow_ = -1;
        private boolean failed_;

        /**
         * Constructor.
         *
         * @param   spec  data specification
         * @param   rseq  row sequence from spec's source table
         */
        SimpleTupleSequence( DataSpec spec, RowSequence rseq ) {
            spec_ = spec;
            reader_ = spec.createUserDataReader();
            baseSeq_ = rseq;
            int nc = spec.getCoordCount();
            mappers_ = new DomainMapper[ nc ][];
            for ( int ic = 0; ic < nc; ic++ ) {
                mappers_[ ic ] = getUserCoordMappers( spec, ic );
            }
        }

        public boolean next() {
            try {
                while ( ! failed_ && baseSeq_.next() ) {
                    if ( reader_.getMaskFlag( baseSeq_, ++irow_ ) ) {
                        return true;
                    }
                }
            }
            catch ( IOException e ) {
                logError( e );
            }
            try {
                baseSeq_.close();
            }
            catch ( IOException e ) {
                // ignore
            }
            return false;
        }

        public long getRowIndex() {
            return irow_;
        }

        public Object getObjectValue( int icol ) {
            try {
                Object[] userCoords =
                    reader_.getUserCoordValues( baseSeq_, irow_, icol );
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

        public boolean getBooleanValue( int icol ) {
            return Boolean.TRUE.equals( getObjectValue( icol ) );
        }

        private void logError( IOException err ) {
            failed_ = true;
            logger_.log( Level.WARNING,
                         "Error reading plot data - truncating sequence", err );
        }
    }
}
