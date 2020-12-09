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

    private final DataSpec dataSpec_;
    private final StarTable table_;
    private final String maskId_;

    /**
     * Constructor.
     *
     * @param   dataSpec   specification from which the mask information
     *                     is taken
     */
    public MaskSpec( DataSpec dataSpec ) {
        dataSpec_ = dataSpec;
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
     * Returns an object that can read the flag value for this mask
     * from the current row of a supplied row sequence.
     *
     * @param   rdata   row for this mask spec's table
     * @return  inclusion mask reader
     */
    public Reader flagReader( final RowData rdata ) {
        final UserDataReader dataReader = dataSpec_.createUserDataReader();
        return irow -> dataReader.getMaskFlag( rdata, irow );
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

    /**
     * Reads a flag.
     */
    @FunctionalInterface
    public interface Reader {

        /**
         * Returns a particular mask given this reader's current state.
         * Note that the supplied row index is additional information
         * (for instance may be relevant to JEL expression evaluation)
         * and it does <em>not</em> determine the row from which the
         * value is supplied.
         *
         * @param  irow   row index
         * @return  inclusion flag
         */
        boolean readFlag( long irow ) throws IOException;
    }
}
