package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import java.util.function.Function;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Characterises information about a coordinate value.
 * It aggregates a table and a coordId, and provides the capability of
 * reading the corresponding value data from a row sequence.
 *
 * @author   Mark Taylor
 * @since    6 Jan 2020
 */
@Equality
public class CoordSpec {

    private final UserDataReader dataReader_;
    private final StarTable table_;
    private final int icoord_;
    private final String coordId_;
    private final StorageType storageType_;
    private final Function<Object[],?> inputStorage_;

    /**
     * Constructor.
     *
     * @param  dataSpec  data specification
     * @param  icoord  coordinate index within dataSpec
     */
    public CoordSpec( DataSpec dataSpec, int icoord ) {
        dataReader_ = dataSpec.createUserDataReader();
        icoord_ = icoord;
        table_ = dataSpec.getSourceTable();
        coordId_ = dataSpec.getCoordId( icoord );
        Coord coord = dataSpec.getCoord( icoord );
        storageType_ = coord.getStorageType();
        ValueInfo[] infos = dataSpec.getUserCoordInfos( icoord );
        DomainMapper[] dms = dataSpec.getUserCoordMappers( icoord );
        inputStorage_ = coord.inputStorage( infos, dms );
    }

    /**
     * Returns the table to which this coord belongs.
     *
     * @return  table
     */
    public StarTable getTable() {
        return table_;
    }

    /**
     * Returns the storage type for this column.
     *
     * @return  storage type
     */
    public StorageType getStorageType() {
        return storageType_;
    }

    /**
     * Returns the unique coordinate identifier for this spec.
     *
     * @return   coord ID
     */
    public String getCoordId() {
        return coordId_;
    }

    /**
     * Reads the user value for this coordinate from a row sequence.
     *
     * @param   rseq   row sequence of this data spec's table
     * @param   irow   row index
     * @return   coordinate stored value for this column at current row
     */
    public Object readValue( RowSequence rseq, long irow ) throws IOException {
        Object[] userCoords =
            dataReader_.getUserCoordValues( rseq, irow, icoord_ );
        Object value = inputStorage_.apply( userCoords );
        assert value != null;
        return value;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof CoordSpec ) {
            CoordSpec other = (CoordSpec) o;
            return other.table_.equals( this.table_ )
                && other.coordId_.equals( this.coordId_ );
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.table_.hashCode() * 37 + coordId_.hashCode();
    }

    @Override
    public String toString() {
        return String.valueOf( coordId_ );
    }
}
