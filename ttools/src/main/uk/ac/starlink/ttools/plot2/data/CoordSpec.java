package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import java.util.function.Function;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.RowData;
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

    private final DataSpec dataSpec_;
    private final int icoord_;
    private final StarTable table_;
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
        dataSpec_ = dataSpec;
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
     * Returns an object that can read the user value for this coordinate
     * from the current row of a supplied a row sequence.
     *
     * @param   rdata   row data for this coord spec's table
     * @return  value reader
     */
    public Reader valueReader( final RowData rdata ) {
        final UserDataReader dataReader = dataSpec_.createUserDataReader();
        return irow -> {
            Object[] userCoords =
                dataReader.getUserCoordValues( rdata, irow, icoord_ );
            Object value = inputStorage_.apply( userCoords );
            assert value != null;
            return value;
        };
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

    /**
     * Reads a data value object.
     */
    @FunctionalInterface
    public interface Reader {

        /**
         * Returns a particular value given this reader's current state.
         * Note that the supplied row index is additional information
         * (for instance may be relevant to JEL expression evaluation)
         * and it does <em>not</em> determine the row from which the
         * value is supplied.
         *
         * @param  irow   row index
         * @return  coordinate stored value
         */
        Object readValue( long irow ) throws IOException;
    }
}
