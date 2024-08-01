package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.swing.JLabel;
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
 * Extends the <code>JTable</code> for use with <code>StarTable</code> objects.
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

    private boolean rowHeader_;
    private StarTable startable_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.gui" );

    /** Minimum width in pixels for a column. */
    private static final int MIN_WIDTH = 50;

    /** Maximum time that <code>configureColumnWidths</code> will run for. */
    private static final long MAX_CONFIG_TIME = 8000;

    /**
     * Constructs a new <code>StarJTable</code>, optionally with a dummy
     * first column displaying the row number.
     *
     * @param  rowHeader  whether column 0 should contain row indices
     */
    public StarJTable( boolean rowHeader ) {
        super();
        rowHeader_ = rowHeader;
    }

    /**
     * Construsts a new <code>StarJTable</code> to display a given 
     * <code>StarTable</code>, 
     * optionally with a dummy first column displaying the row number.
     *
     * @param  startable  the <code>StarTable</code> to display
     * @param  rowHeader  whether column 0 should contain row indices
     * @throws  IllegalArgumentException  if <code>startable.isRandom</code>
     *          returns <code>false</code>
     * @see     uk.ac.starlink.table.Tables#randomTable
     */
    @SuppressWarnings("this-escape")
    public StarJTable( StarTable startable, boolean rowHeader ) {
        this( rowHeader );
        setStarTable( startable, rowHeader );
    }

    /**
     * Indicates whether the first column of this table is a dummy column
     * displaying the row index.
     *
     * @return  <code>true</code> iff column 0 displays row index
     */
    public boolean hasRowHeader() {
        return rowHeader_;
    }

    /**
     * Sets this <code>StarJTable</code> up to display a given 
     * <code>StarTable</code> object,
     * optionally with a dummy first column displaying the row number.
     * This table's model will be set to a {@link StarTableModel},
     * and the colum model will be set to one of which all the columns
     * are {@link StarTableColumn}s.
     *
     * @param  startable  the <code>StarTable</code> to display
     * @param  rowHeader  whether column 0 should contain row indices
     * @throws  IllegalArgumentException  if <code>startable.isRandom</code>
     *          returns <code>false</code>
     * @see     uk.ac.starlink.table.Tables#randomTable
     */
    public void setStarTable( StarTable startable, boolean rowHeader ) {
        setModel( new StarTableModel( startable, rowHeader ) );
        startable_ = startable;

        /* Set up the column and column model. */
        TableColumnModel tcm = new DefaultTableColumnModel();
        int jcol = 0;

        /* Construct a dummy column for the index entries if required. */
        if ( rowHeader_ ) {
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
     * Return a reference to the {@link StarTable} being used.
     * 
     * @return reference to the {@link StarTable}, null if none has been set.
     */
    public StarTable getStarTable()
    {
        return startable_;
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
        if ( rowHeader_ ) {
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
        for ( Iterator<Integer> it =
                  sampleIterator( table.getRowCount(), rowSample,
                                  MAX_CONFIG_TIME );
              it.hasNext(); ) {
            int irow = it.next().intValue();
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
        TableColumnModel tcm = table.getColumnModel();
        int wtot = 0;
        for ( int icol = 0; icol < ncol; icol++ ) {
            int w = Math.max( widths[ icol ] + 8, MIN_WIDTH );
            wtot += w;
            tcm.getColumn( icol ).setPreferredWidth( w );
        }

        /* Switch resizing on or off according to whether the total width
         * of the columns is less or greater than the width of the 
         * table's display component. */
        Component holder = table.getParent();
        table.setAutoResizeMode( ( holder != null &&
                                   wtot <= holder.getSize().width )
                                 ? JTable.AUTO_RESIZE_ALL_COLUMNS
                                 : JTable.AUTO_RESIZE_OFF );
    }

    /**
     * Sets up numeric cell renderers for the columns of a JTable.
     *
     * @param  jtable  table to configure; does not have to be a StarJTable
     */
    public static void configureDefaultRenderers( JTable jtable ) {
        for ( Class<?> clazz : new Class<?>[] {
                  Byte.class, Short.class, Integer.class, Long.class,
                  Float.class, Double.class,
              } ) {
            jtable.setDefaultRenderer( clazz,
                                       new NumericCellRenderer( clazz ) );
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
     * Utility method that tries to arrange for the column headers to be
     * left-aligned rather than, as seems to be the default, center-aligned.
     *
     * @param  jtable  table to affect
     */
    public static void alignHeadersLeft( JTable jtable ) {
        TableCellRenderer hdrRend =
            jtable.getTableHeader().getDefaultRenderer();
        if ( hdrRend instanceof JLabel ) {
            ((JLabel) hdrRend).setHorizontalAlignment( SwingConstants.LEFT );
        }
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
        for ( Iterator<Integer> it =
                  sampleIterator( table.getRowCount(), rowSample,
                                  MAX_CONFIG_TIME );
              it.hasNext(); ) {
            int irow = it.next().intValue();
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
        TableColumn tcol = jtab.getColumnModel().getColumn( icol );
        TableCellRenderer headRend = tcol.getHeaderRenderer();
        if ( headRend == null ) {
            headRend = jtab.getTableHeader().getDefaultRenderer();
        }
        Object headObj = tcol.getHeaderValue();
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
     * rows in a table.  If <code>nsample&gt;=nrow</code> it will iterate
     * over all the rows, otherwise it try to cover a representative 
     * sample, broadly speaking the first few, last few, and some in the
     * middle.  The iterator will not continue to iterate for an 
     * elapsed time of (much) more than {@link #MAX_CONFIG_TIME}.
     *
     * @param   nrow   number of rows in the table
     * @param   nsample   maximum number of rows to sample
     * @param   maxTime  maximum elapsed time (approx) for iterator to live
     *                   in milliseconds
     * @return  iterator over <code>Integer</code> objects each representing 
     *          a table row index 
     */
    private static Iterator<Integer> sampleIterator( final int nrow,
                                                     final int nsample,
                                                     final long maxTime ) {
        if ( nsample >= nrow ) {
            return new Iterator<Integer>() {
                int irow = 0;
                public boolean hasNext() {
                    return irow < nrow;
                }
                public Integer next() {
                    return Integer.valueOf( irow++ );
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        else {
            return new Iterator<Integer>() {
                final int NS4 = nsample / 4;
                final int NS2 = nsample / 2;
                final int NR4 = nrow / 4;
                int iseg_ = 0;
                int isamp4_ = 0;
                long segStart_ = System.currentTimeMillis();
                int next_ = nextInt();
                public boolean hasNext() {
                    return next_ >= 0;
                }
                public Integer next() {
                    if ( next_ >= 0 ) {
                        Integer nobj = Integer.valueOf( next_ );
                        next_ = nextInt();
                        return nobj;
                    }
                    else {
                        throw new IllegalStateException();
                    }
                }
                private int nextInt() {
                    long now = System.currentTimeMillis();
                    if ( isamp4_ >= NS4 || now - segStart_ > maxTime / 4 ) {
                        segStart_ = now;
                        isamp4_ = 0;
                        iseg_++;
                    }
                    int is4 = isamp4_++;
                    switch ( iseg_ ) {
                        case 0:
                            return is4;
                        case 1:
                            return ( ( is4 / NS4 ) * NR4 ) + NR4;
                        case 2:
                            return ( ( is4 / NS4 ) * NR4 ) + ( NR4 * 2 );
                        case 3:
                            return nrow - 1 - is4;
                        default:
                            return -1;
                    }
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
