package uk.ac.starlink.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for wrapping a sequential table as a random one.
 * It does it by reading all lines up to the last one requested,
 * and remembering all the earlier ones.  Since it needs to 
 * know its total number of rows, if the underlying table does
 * not know this (<tt>getRowCount()&lt;0</tt>) then it has to
 * read all the rows in straight away.
 */
public class RowScratchTable extends RandomStarTable {

    private StarTable basetab;
    private int ncol;
    private long nrow;
    private List rows = new ArrayList();

    public RowScratchTable( StarTable basetab ) throws IOException {
        this.basetab = basetab;
        ncol = basetab.getColumnCount();

        /* Check we don't have an unfeasible number of rows. */
        nrow = basetab.getRowCount();
        if ( nrow > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException( 
                "Table " + basetab + " has too many rows (" + 
                nrow + " > Integer.MAX_VALUE)" );
        }

        /* If we don't know the number of rows, we have to load all the
         * data in now so we do. */
        else if ( nrow < 0 ) {
            long irow;
            for ( irow = 0; basetab.hasNext(); irow++ ) {
                basetab.next();
                rows.add( basetab.getRow() );
                if ( irow > Integer.MAX_VALUE ) {
                    throw new IllegalArgumentException( 
                        "Table " + basetab + " has too many rows (" + 
                        " > Integer.MAX_VALUE)" );
                }
            }
            nrow = irow;
        }
    }

    public int getColumnCount() {
        return ncol;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return basetab.getColumnInfo( icol );
    }

    public long getRowCount() {
        return nrow;
    }

    protected Object[] doGetRow( long lrow ) throws IOException {
        assert (int) nrow == nrow;
        int irow = (int) lrow;

        /* If we haven't got this far in the base table yet, read rows
         * from it until we have. */
        for ( long toRead = irow - rows.size(); toRead >= 0; toRead-- ) {
            basetab.next();
            rows.add( basetab.getRow() );
        }

        /* Return the row that we have now definitely read from our
         * interal row store. */
        assert irow < rows.size();
        return (Object[]) rows.get( irow );
    }

    public List getColumnAuxDataInfos() {
        return basetab.getColumnAuxDataInfos();
    }

    public List getParameters() {
        return basetab.getParameters();
    }

    public DescribedValue getParameterByName( String name ) {
        return basetab.getParameterByName( name );
    }

}
