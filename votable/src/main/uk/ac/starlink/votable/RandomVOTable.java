package uk.ac.starlink.votable;

import java.util.ArrayList;
import java.util.List;

/**
 * A table model based on a VOTable Table element but which can do 
 * random access on its cells.  This may involve reading the whole table
 * in, so is not suitable for a table which
 * has an arbitrarily large number of rows accessed by streaming.
 * It is intended as a basis for VOTable adaptors to classes representing
 * tables which need to know what their dimensions are up front.
 *
 * @author   Mark Taylor (Starlink)
 */
public class RandomVOTable {

    private Table votable;
    private List rowList = new ArrayList();
    private int nrow;
    private int ncol;
    private Object[] badvals;

    /**
     * Construct a RandomVOTable from a VOTable <tt>Table</tt> object.
     *
     * @param  votable  the table object
     */
    public RandomVOTable( Table votable ) {
        this.votable = votable;

        /* Get the number of rows. */
        nrow = votable.getRowCount();
        if ( nrow < 0 ) {
            nrow = 0;
            while ( votable.hasNextRow() ) {
                getRow( nrow++ );
            }
        }

        /* Get the number of columns. */
        ncol = votable.getColumnCount();

        /* Set up bad values. */
        badvals = new Object[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            badvals[ i ] = votable.getField( i ).getDecoder().getNull();
        }
    }

    /**
     * Returns the number of columns in the table.
     *
     * @return  the number of columns
     */
    public int getColumnCount() {
        return ncol;
    }

    /**
     * Returns the number of rows in the table.
     *
     * @return  the number of rows
     */
    public int getRowCount() {
        return nrow;
    }

    /**
     * Returns the content of a given table cell.
     *
     * @param  irow  the row index
     * @param  icol  the column index
     * @return  the contents of the cell.  If it matches the relevant Field's
     *          'null' value, the intention is that the java <tt>null</tt> 
     *          value will be returned (though I'm not sure it always 
     *          works at the moment)
     */
    public Object getValueAt( int irow, int icol ) {
        Object cell = getRow( irow )[ icol ];
        if ( cell.equals( badvals[ icol ] ) ) {
            cell = null;
        }
        return cell;
    }

    /**
     * Returns the whole row at a given index as an array of objects.
     *
     * @param  irow  the row index
     * @return the row as an array of <tt>getColumnCount</tt> Objects
     */
    public Object[] getRow( int irow ) {

        /* If we haven't filled up our row list far enough yet, do it now. */
        while ( rowList.size() <= irow ) {
            Object[] nextRow = votable.nextRow();
            rowList.add( nextRow );
        }
        return (Object[]) rowList.get( irow );
    }
}
