package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.util.Iterator;
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

    private static final int MIN_WIDTH = 50;

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
     * @param  rowSample  the number of rows of the tables to survey
     *                    for working out column widths.  If a number greater
     *                    than the number of rows in the table is given,
     *                    all rows will be surveyed
     */
    public static void configureColumnWidths( JTable table, int maxpix,
                                              int rowSample ) {
        int ncol = table.getColumnCount();

        /* Get minimum widths for each column. */
        int[] widths = new int[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            widths[ icol ] = getHeaderWidth( table, icol );
        }

        /* Take a sample of rows to see if the cells in each one are going
         * to require more width. */
        for ( Iterator it = sampleIterator( table.getRowCount(), rowSample );
              it.hasNext(); ) {
            int irow = ((Integer) it.next()).intValue();
            for ( int icol = 0; icol < ncol; icol++ ) {
                if ( widths[ icol ] < maxpix ) {
                    int w = getCellWidth( table, irow, icol );
                    if ( w > widths[ icol ] ) {
                        widths[ icol ] = Math.min( w, maxpix );
                    }
                }
            }
        }

        /* Configure the columns as calculated. */
        table.setAutoResizeMode( AUTO_RESIZE_OFF );
        TableColumnModel tcm = table.getColumnModel();
        for ( int icol = 0; icol < ncol; icol++ ) {
            int w = Math.max( widths[ icol ] + 8, MIN_WIDTH );
            tcm.getColumn( icol ).setPreferredWidth( w );
        }
    }

    /**
     * Sets the width of one column to match the width of its contents.
     * A heuristic * method is used; the widths of the headers and of 
     * the first few rows is got, and the width set to this value.
     * This method uses the cell renderers and table contents currently
     * in force, so should be called after internal configuration.
     *
     * @param   table  the JTable whose widths are to be set
     * @param   maxpix the maximum column width allowed (pixels)
     * @param rowSample the number of rows of the tables to survey
     *                  for working out column widths.  If a number greater
     *                  than the number of rows in the table is given,
     *                  all rows will be surveyed
     * @param  icol   the index of the column to be configured
     */
    public static void configureColumnWidth( JTable table, int maxpix,
                                             int rowSample, int icol ) {
        int width = Math.min( getColumnWidth( table, icol, rowSample ),
                              maxpix );
        table.getColumnModel().getColumn( icol ).setPreferredWidth( width );
    }

    /**
     * Gets the width that it looks like a given column should have.
     *
     * @param  table  table
     * @param  icol   column index
     * @param  rowSample  maximum number of rows to survey for working out
     *         the width
     */
    private static int getColumnWidth( JTable table, int icol, int rowSample ) {

        /* Get the width required for the header. */
        int width = getHeaderWidth( table, icol );

        /* Go through a sample of to see if any of them need more width. */
        for ( Iterator it = sampleIterator( table.getRowCount(), rowSample );
              it.hasNext(); ) {
            int irow = ((Integer) it.next()).intValue();
            int w = getCellWidth( table, irow, icol );
            width = Math.max( w, width );
        }

        /* Return the maximum cell width found plus a little bit of padding. */
        return Math.max( width + 10, MIN_WIDTH );
    }

    /**
     * Returns the width that a column must have in order to accommodate its
     * column header.  Even if there is no column header to render, this
     * will return some sensible minimum value so the column is not 
     * vanishingly small.
     *
     * @param   table  table whose column is to be measured
     * @param   icol   column index
     * @return  minimum size for column in pixels
     */
    private static int getHeaderWidth( JTable jtab, int icol ) {
        TableCellRenderer headRend = 
            jtab.getColumnModel().getColumn( icol ).getHeaderRenderer();
        if ( headRend == null ) {
            headRend = jtab.getTableHeader().getDefaultRenderer();
        }
        String headObj = jtab.getColumnName( icol );
        Component headComp = 
            headRend.getTableCellRendererComponent( jtab, headObj, false,
                                                    false, 0, icol );
        int width = headComp.getPreferredSize().width;
        return Math.max( MIN_WIDTH, width );
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

    /**
     * Returns an iterator over row indices representing a sample of 
     * rows in a table.  If <tt>nsample&gt;=nrow</tt> it will iterate
     * over all the rows, otherwise it try to cover a representative 
     * sample, broadly speaking the first few, last few, and some in the
     * middle.
     *
     * @param   number of rows in the table
     * @param   maximum number of rows to sample
     * @return  iterator over <tt>Integer</tt> objects each representing 
     *          a table row index 
     */
    private static Iterator sampleIterator( final int nrow,
                                            final int nsample ) {
        if ( nsample >= nrow ) {
            return new Iterator() {
                int irow = 0;
                public boolean hasNext() {
                    return irow < nrow;
                }
                public Object next() {
                    return new Integer( irow++ );
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        else {
            return new Iterator() {
                int irow = 0;
                int isamp = 0;
                int ns4 = nsample / 4;
                int ns2 = nsample / 2;
                public boolean hasNext() {
                    return isamp < nsample;
                }
                public Object next() {
                    int is = isamp++;
                    switch ( is * 4 / nsample ) {
                        case 0:
                            return new Integer( is );
                        case 1:
                        case 2:
                            double frac =
                                2.0 * ( ( is / (double) nsample ) - 0.25 );
                            int irow = ns4 + (int) ( frac * ( nrow - ns2 ) );
                            return new Integer( irow );
                        case 3:
                            return new Integer( nrow - ( nsample - is ) );
                        default:
                            throw new AssertionError();
                    }
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

 // public static void main( String[] args ) {
 //     int nrow = Integer.parseInt( args[ 0 ] );
 //     int nsamp = Integer.parseInt( args[ 1 ] );
 //     for ( Iterator it = sampleIterator( nrow, nsamp ); it.hasNext(); ) {
 //         System.out.println( (Integer) it.next() );
 //     }
 // }

}
