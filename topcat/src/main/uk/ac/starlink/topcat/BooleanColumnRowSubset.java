package uk.ac.starlink.topcat;

import java.io.IOException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;

/**
 * A RowSubset implementation based on a boolean column of a StarTable.
 */
public class BooleanColumnRowSubset extends RowSubset {

    private StarTable startab;
    private int icol;
    private ColumnInfo colinfo;

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
        this.startab = startab;
        this.icol = icol;
        this.colinfo = startab.getColumnInfo( icol );
        if ( colinfo.getContentClass() != Boolean.class ) {
            throw new IllegalArgumentException( "Column " + colinfo 
                                              + " is not boolean" );
        }
    }

    public boolean isIncluded( long lrow ) {
        try {
            Object cellValue = startab.getCell( lrow, icol );
            return Boolean.TRUE.equals( cellValue );
        }
        catch ( IOException e ) {
            return false;
        }
    }

    public ColumnInfo getColumnInfo() {
        return colinfo;
    }
}
