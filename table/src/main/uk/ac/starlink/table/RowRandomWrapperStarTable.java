package uk.ac.starlink.table;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * RandomWrapperStarTable which works by storing rows in an ArrayList.
 *
 * @author   Mark Taylor (Starlink)
 * @see   ColumnRandomWrapperStarTable
 */
public class RowRandomWrapperStarTable extends RandomWrapperStarTable {

    private final List<Object[]> rows_;

    public RowRandomWrapperStarTable( StarTable baseTable ) throws IOException {
        super( baseTable );
        rows_ = new ArrayList<Object[]>();
    }

    protected synchronized void storeNextRow( Object[] row ) {
        rows_.add( row );
    }

    protected Object[] retrieveStoredRow( long lrow ) {
        return rows_.get( Tables.checkedLongToInt( lrow ) );
    }
}
