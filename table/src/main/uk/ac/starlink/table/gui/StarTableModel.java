package uk.ac.starlink.table.gui;

import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.table.AbstractTableModel;
import uk.ac.starlink.table.StarTable;

/**
 * Adapts a <code>StarTable</code> into a <code>TableModel</code>.
 * The base StarTable must provide random access (its <code>isRandom</code>
 * method must return <code>true</code>); to make a StarTableModel if your
 * StarTable is not random you will have to make a random one using
 * for instance {@link uk.ac.starlink.table.Tables#randomTable}.
 * <p>
 * One extra bit of functionality is enabled, namely that an extra column
 * containing row indices may be provided.
 * <p>
 * As well as providing the data model for a <code>JTable</code>, this 
 * class can be used as a general wrapper for <code>StarTable</code> objects
 * when the event handling mechanism it supplies is required.
 * 
 * @author   Mark Taylor (Starlink)
 * @see      javax.swing.JTable
 */
public class StarTableModel extends AbstractTableModel {

    protected StarTable startable;
    private boolean rowHeader;
    private int extraCols;

    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.table.gui" );

    /**
     * Constructs a <code>StarTableModel</code> from a <code>StarTable</code>,
     * without row index column.
     * The supplied <code>StarTable</code> must provide random access.
     *
     * @param   startable  the <code>StarTable</code> object
     * @throws  IllegalArgumentException  if <code>startable.isRandom</code>
     *          returns <code>false</code>
     * @see     uk.ac.starlink.table.Tables#randomTable
     */
    public StarTableModel( StarTable startable ) {
        this( startable, false );
    }

    /**
     * Constructs a <code>StarTableModel</code> from a <code>StarTable</code>,
     * optionally with a row index column.
     * The supplied <code>StarTable</code> must provide random access.
     *
     * @param   startable  the <code>StarTable</code> object
     * @param   rowHeader  whether to add an extra column at the start
     *          containing the row index
     * @throws  IllegalArgumentException  if <code>startable.isRandom</code>
     *          returns <code>false</code>
     * @see     uk.ac.starlink.table.Tables#randomTable
     */
    public StarTableModel( StarTable startable, boolean rowHeader ) {
        super();
        this.startable = startable;
        this.rowHeader = rowHeader;
        extraCols = rowHeader ? 1 : 0;

        /* Ensure that we have a random access table to use, and that it
         * is not unfeasibly large. */
        if ( ! startable.isRandom() ) {
            throw new IllegalArgumentException( 
                "Table " + startable + " does not have random access" );
        }
        if ( startable.getRowCount() > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException(
                "Table has too many rows (" + startable.getRowCount() +
                " > Integer.MAX_VALUE)" );
        }
    }

    /**
     * Indicates whether the first column in this table is an artificial
     * one containing just the index of the row.
     *
     * @return  <code>true</code> iff column 0 is a row index
     */
    public boolean hasRowHeader() {
        return rowHeader;
    }

    /**
     * Gets the <code>StarTable</code> underlying this model.
     *
     * @return  the <code>StarTable</code> object
     */
    public StarTable getStarTable() {
        return startable;
    }

    public int getRowCount() {
        return (int) startable.getRowCount();
    }

    public int getColumnCount() {
        return startable.getColumnCount() + extraCols;
    }

    public Object getValueAt( int irow, int icol ) {
        if ( rowHeader && icol == 0 ) {
            return new Integer( irow + 1 );
        }
        else {
            try {
                return startable.getCell( (long) irow, icol - extraCols );
            }
            catch ( IOException e ) {
                logger.warning( "IOException for table cell " +
                                irow + ", " + icol );
                return e.getMessage();
            }
        }
    }

    public String getColumnName( int icol ) {
        if ( rowHeader && icol == 0 ) {
            return "";
        }
        else {
            return startable.getColumnInfo( icol - extraCols ).getName();
        }
    }

    public Class<?> getColumnClass( int icol ) {
        if ( rowHeader && icol == 0 ) {
            return Integer.class;
        }
        else {
            return startable.getColumnInfo( icol - extraCols )
                            .getContentClass();
        }
    }
}
