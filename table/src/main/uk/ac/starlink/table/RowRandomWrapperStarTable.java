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

    private List rows = new ArrayList();

    public RowRandomWrapperStarTable( StarTable baseTable ) throws IOException {
        super( baseTable );
    }

    protected synchronized void storeNextRow( Object[] row ) {
        rows.add( row );
    }

    protected Object[] retrieveStoredRow( long lrow ) {
        return (Object[]) rows.get( (int) lrow );
    }
}
