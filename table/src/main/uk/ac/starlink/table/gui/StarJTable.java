package uk.ac.starlink.table.gui;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;

/**
 * Extends the <tt>JTable</tt> for use with <tt>StarTable</tt> objects.
 * This convenience class adapts a JTable and sets its components appropriately
 * for viewing a StarTable.  The main jobs it does are to set its
 * model to a suitable StarTableModel and make sure the cell renderers
 * are set up suitably.
 * It also provides {@link #configureColumnWidths} and related methods 
 * which sets the column widths according to the contents of the first few
 * rows of the table.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StarJTable extends JTable {

    private boolean rowHeader;
    private StarTable startable;

    /**
     * Constructs a new <tt>StarJTable</tt>, optionally with a dummy
     * first column displaying the row number.
     *
     * @param  rowHeader  whether column 0 should contain row indices
     */
    public StarJTable( boolean rowHeader ) {
        super();
        this.rowHeader = rowHeader;
    }

    /**
     * Construsts a new <tt>StarJTable</tt> to display a given 
     * <tt>StarTable</tt>, 
     * optionally with a dummy first column displaying the row number.
     *
     * @param  startable  the <tt>StarTable</tt> to display
     * @param  rowHeader  whether column 0 should contain row indices
     * @throws  IllegalArgumentException  if <tt>startable.isRandom</tt>
     *          returns <tt>false</tt>
     * @see     uk.ac.starlink.table.Tables#randomTable
     */
    public StarJTable( StarTable startable, boolean rowHeader ) {
        this( rowHeader );
        setStarTable( startable, rowHeader );
    }

    /**
     * Indicates whether the first column of this table is a dummy column
     * displaying the row index.
     *
     * @return  <tt>true</tt> iff column 0 displays row index
     */
    public boolean hasRowHeader() {
        return rowHeader;
    }

    /**
     * Sets this <tt>StarJTable</tt> up to display a given 
     * <tt>StarTable</tt> object,
     * optionally with a dummy first column displaying the row number.
     * This table's model will be set to a {@link StarTableModel},
     * and the colum model will be set to one of which all the columns
     * are {@link StarTableColumn}s.
     *
     * @param  startable  the <tt>StarTable</tt> to display
     * @param  rowHeader  whether column 0 should contain row indices
     * @throws  IllegalArgumentException  if <tt>startable.isRandom</tt>
     *          returns <tt>false</tt>
     * @see     uk.ac.starlink.table.Tables#randomTable
     */
    public void setStarTable( StarTable startable, boolean rowHeader ) {
        setModel( new StarTableModel( startable, rowHeader ) );
        this.startable = startable;

        /* Set up the column and column model. */
        TableColumnModel tcm = new DefaultTableColumnModel();
        int jcol = 0;

        /* Construct a dummy column for the index entries if required. */
        if ( rowHeader ) {
            ColumnInfo rhColInfo = new ColumnInfo( new DefaultValueInfo(
                "Index", Integer.class, "Row index" ) );
            TableColumn rhcol = new StarTableColumn( rhColInfo, jcol++ );
            rhcol.setCellRenderer( getRowHeaderRenderer() );
            tcm.addColumn( rhcol );
        }

        /* Construct proper columns for the entries from the StarTable. */
        for ( int icol = 0; icol < startable.getColumnCount(); icol++ ) {
            ColumnInfo cinfo = startable.getColumnInfo( icol );
            TableColumn tcol = new StarTableColumn( cinfo, jcol++ );
            tcm.addColumn( tcol );
        }

        /* Set the column model to the one we have constructed. */
        setColumnModel( tcm );
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
        if ( rowHeader ) {
            int hwidth = Math.max( getCellWidth( this, 0, 0 ),
                                   getCellWidth( this, getRowCount() - 1, 0 ) )
                       + 8;
            getColumnModel().getColumn( 0 ).setPreferredWidth( hwidth );
        }
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
        table.setAutoResizeMode( AUTO_RESIZE_OFF );
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
            width = Math.max( width, getCellWidth( table, i, icol ) );
        }

        /* Return the maximum cell width found plus a little bit of padding. */
        return Math.max( width + 10, 50 );
    }

    /**
     * Returns the preferred width in pixels of a given cell in a JTable.
     * The table should be configured with its proper renderers and model
     * before this is called.  It is assumed that focus and selection 
     * does not affect the size.
     *
     * @param  jtab  the table
     */
    public static int getCellWidth( JTable jtab, int irow, int icol ) {
        TableCellRenderer rend = jtab.getCellRenderer( irow, icol );
        Object value = jtab.getValueAt( irow, icol );
        Component comp = 
            rend.getTableCellRendererComponent( jtab, value, false, false, 
                                                irow, icol );
        return comp.getPreferredSize().width;
    }

    /**
     * Returns a renderer suitable for heading-like content.
     * 
     * @param  a renderer
     */
    private static TableCellRenderer getRowHeaderRenderer() {
        DefaultTableCellRenderer rend = new DefaultTableCellRenderer();

        /* Where are these property names documented?  Don't know, but
         * you can find them in the source code of
         * javax.swing.plaf.basic.BasicLookAndFeel,
         * javax.swing.plaf.metal.MetalLookAndFeel. */
        rend.setFont( UIManager.getFont( "TableHeader.font" ) );
        rend.setBackground( UIManager.getColor( "TableHeader.background" ) );
        rend.setForeground( UIManager.getColor( "TableHeader.foreground" ) );
        rend.setHorizontalAlignment( SwingConstants.RIGHT );
        return rend;
    }

}
