package uk.ac.starlink.table;

import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.EmptyRowSequence;
import uk.ac.starlink.table.RowSequence;

/**
 * Utility StarTable implementation which contains only metadata, no data.
 * Suitable for passing to
 * {@link uk.ac.starlink.table.TableSink#acceptMetadata}.
 *
 * @author   Mark Taylor
 * @since    14 Oct 2011
 */
public class MetadataStarTable extends AbstractStarTable {

    private final ColumnInfo[] colInfos_;
    private final long rowCount_;

    /**
     * Constructs a metadata table with given column metadata
     * and an indeterminate number of rows.
     *
     * @param  colInfos  metadata items for each column
     */
    public MetadataStarTable( ColumnInfo[] colInfos ) {
        this( colInfos, -1 );
    }

    /**
     * Constructs a metadata table with given column metadata
     * and a given number of rows.
     *
     * @param  colInfos  metadata items for each column
     * @param  rowCount  row count, may be -1 to indicate unknown
     */
    public MetadataStarTable( ColumnInfo[] colInfos, long rowCount ) {
        colInfos_ = colInfos;
        rowCount_ = rowCount;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public int getColumnCount() {
        return colInfos_.length;
    }

    public RowSequence getRowSequence() {
        return EmptyRowSequence.getInstance();
    }

    public long getRowCount() {
        return rowCount_;
    }
}
