package uk.ac.starlink.table.gui;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.StarTable;

/**
 * Provides a graphical viewer for StarTable objects.
 * This convenience class adapts a JTable and sets it components appropriately
 * for viewing a StarTable.  The main jobs it does are to set its 
 * model to a suitable StarTableModel and set up some suitable cell renderers.
 * It also provides the method {@link #configureColumnWidths} which
 * sets the column widths according to the contents of the first few
 * rows of the table.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StarJTable extends JTable {

    private static TableCellRenderer basicRenderer = 
        new StarTableCellRenderer();
    private static TableCellRenderer borderRenderer =
        new StarTableCellRenderer( true );

    private StarTableModel tmodel;

    /**
     * Constructs a StarJTable from a StarTable object.
     * The supplied StarTable must provide random access
     * (<tt>startable.isRandom()</tt> returns true).
     *
     * @param  startable  the StarTable to view
     */
    public StarJTable( StarTable startable ) {

        /* Construct a table model to contain the actual data. */
        super( new StarTableModel( startable ) );
        tmodel = (StarTableModel) getModel();

        /* Do some cosmetic setup. */
        setAutoResizeMode( AUTO_RESIZE_OFF );

        /* Set up default renderers for various classes.  The default 
         * renderer doesn't do numbers very well, it often truncates them. */
        setDefaultRenderer( Object.class, basicRenderer );
        setDefaultRenderer( Number.class, basicRenderer );
        setDefaultRenderer( Float.class, basicRenderer );
        setDefaultRenderer( Double.class, basicRenderer );
    }

    /**
     * Sets a new StarTableModel.
     *
     * @param  model  a new StarTableModel
     * @throws  IllegalArgumentException   if <tt>model</tt> is not a 
     *          StarTableModel
     */
    public void setModel( TableModel model ) {
        if ( model instanceof StarTableModel ) {
            this.tmodel = (StarTableModel) model;
            super.setModel( model );
        }
        else { 
            throw new IllegalArgumentException(
                "You can only set a StarJTable model to a StarTableModel" );
        }
    }

    public TableModel getModel() {
        return tmodel;
    }

    /**
     * Gets an appropriate cell renderer for a given cell.
     */
    public TableCellRenderer getCellRenderer( int irow, int icol ) {
        if ( irow < tmodel.getExtraRows() || icol < tmodel.getExtraColumns() ) {
            return borderRenderer;
        }
        else {
            return getDefaultRenderer( tmodel.getBodyColumnClass( icol ) );
        }
    }

    /**
     * Sets the width of each column heuristically from the contents of
     * the cells headers and cells.  Should be called after any 
     * default renderers have been set.
     *
     * @param  maxpix   the maximum column width allowed (pixels)
     * @param  nrows    the number of rows of the tables to survey 
     *                  for working out column widths.  If a number greater
     *                  than the number of rows in the table is given, 
     *                  all rows will be surveyed
     */
    public void configureColumnWidths( int maxpix, int nrows ) {
        configureColumnWidths( this, maxpix, nrows );
    }

    /**
     * Utility method provided to set the widths of the columns of a JTable
     * so that they match the widths of their contents.  A heuristic
     * method is used; the widths of the headers and of the first few
     * rows is got, and the width set to this value.
     * This method uses the cell renderers and table contents currently
     * in force, so should be called after internal configuration.
     *
     * @param   table  the JTable whose widths are to be set
     * @param   maxpix the maximum column width allowed (pixels)
     * @param  nrows    the number of rows of the tables to survey 
     *                  for working out column widths.  If a number greater
     *                  than the number of rows in the table is given, 
     *                  all rows will be surveyed
     */
    public static void configureColumnWidths( JTable table, int maxpix, 
                                              int nrows ) {
        TableColumnModel tcm = table.getColumnModel();
        int ncol = table.getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {

            /* See how wide the cells want to be, and set the graphical
             * cell width accordingly. */
            TableColumn tc = tcm.getColumn( icol );
            int width = Math.min( getColumnWidth( table, icol, nrows ),
                                  maxpix );
            tc.setPreferredWidth( width );
        }
    }

    /**
     * Gets the width that it looks like a given column should have.
     */
    private static int getColumnWidth( JTable table, int icol, int nrows ) {

        /* Get a renderer for the header of this column. */
        TableCellRenderer headRend = table.getColumnModel().getColumn( icol )
                                    .getHeaderRenderer();
        if ( headRend == null ) {
            headRend = table.getTableHeader().getDefaultRenderer();
        }

        /* Work out the width required by the header. */
        String headObj = table.getColumnName( icol );
        Component headComp =
            headRend.getTableCellRendererComponent( table, headObj, false,
                                                    false, 0, icol );
        int width = headComp.getPreferredSize().width;

        /* Go through the first few columns and see if any of them need
         * more width. */
        int nr = Math.min( table.getRowCount(), nrows );
        for ( int i = 0; i < nr; i++ ) {
             Object cellObj = table.getModel().getValueAt( i, icol );
             Component cellComp = 
                 table.getCellRenderer( i, icol )
                .getTableCellRendererComponent( table, cellObj, false, false,
                                                i, icol );
             int cellWidth = cellComp.getPreferredSize().width;
             if ( cellWidth > width ) {
                 width = cellWidth;
             }
        }

        /* Return the maximum cell width found plus a little bit of padding. */
        return Math.max( width + 10, 50 );
    }
}
