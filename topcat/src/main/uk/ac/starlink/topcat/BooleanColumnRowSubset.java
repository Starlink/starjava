package uk.ac.starlink.topcat;

import java.io.IOException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;

/**
 * A RowSubset implementation based on a boolean column of a StarTable.
 */
public class BooleanColumnRowSubset extends RowSubset {

    private final StarTable startab_;
    private final int icol_;

    /**
     * Constructs a row subset from a given StarTable and column index.
     *
     * @param  startab  the table
     * @param  icol  the index of the column in that table
     * @throws IllegalArgumentException  if the content class of the 
     *         table column is not Boolean
     */
    public BooleanColumnRowSubset( StarTable startab, int icol ) {
        super( startab.getColumnInfo( icol ).getName() );
        startab_ = startab;
        icol_ = icol;
        ColumnInfo colinfo = startab.getColumnInfo( icol );
        if ( colinfo.getContentClass() != Boolean.class ) {
            throw new IllegalArgumentException( "Column " + colinfo
                                              + " is not boolean" );
        }
    }

    public boolean isIncluded( long lrow ) {
        try {
            Object cellValue = startab_.getCell( lrow, icol_ );
            return Boolean.TRUE.equals( cellValue );
        }
        catch ( IOException e ) {
            return false;
        }
    }

    /**
     * Returns the table from whose column this subset is based.
     *
     * @return  table
     */
    public StarTable getTable() {
        return startab_;
    }

    /**
     * Returns the index of the column in the table on which this
     * subset's contents are based.
     *
     * @return  column index
     */
    public int getColumnIndex() {
        return icol_;
    }
}
