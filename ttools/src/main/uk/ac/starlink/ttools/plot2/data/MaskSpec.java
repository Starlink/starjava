package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import uk.ac.starlink.table.RowData;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Characterises information about a data inclusion mask.
 * It aggregates a table and a maskId, and provides the capability of
 * reading the corresponding inclusion data from a RowData.
 *
 * @author   Mark Taylor
 * @since    6 Jan 2020
 */
@Equality
public class MaskSpec {

    private final UserDataReader dataReader_;
    private final StarTable table_;
    private final String maskId_;

    /**
     * Constructor.
     *
     * @param   dataSpec   specification from which the mask information
     *                     is taken
     */
    public MaskSpec( DataSpec dataSpec ) {
        dataReader_ = dataSpec.createUserDataReader();
        table_ = dataSpec.getSourceTable();
        maskId_ = dataSpec.getMaskId();
    }

    /**
     * Returns the table to which this mask belongs.
     *
     * @return  table
     */
    public StarTable getTable() {
        return table_;
    }

    /**
     * Returns the unique mask identifier string associated with this spec.
     *
     * @return  mask ID
     */
    public String getMaskId() {
        return maskId_;
    }

    /**
     * Reads inclusion flag from a row sequence.
     *
     * @param   rdata   row for this data spec's table
     * @param   irow   row index
     * @return  inclusion mask for current row
     */
    public boolean readFlag( RowData rdata, long irow ) throws IOException {
        return dataReader_.getMaskFlag( rdata, irow );
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof MaskSpec ) {
            MaskSpec other = (MaskSpec) o;
            return other.table_.equals( this.table_ )
                && other.maskId_.equals( this.maskId_ );
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.table_.hashCode() * 23 + maskId_.hashCode();
    }

    @Override
    public String toString() {
        return String.valueOf( maskId_ );
    }
}
