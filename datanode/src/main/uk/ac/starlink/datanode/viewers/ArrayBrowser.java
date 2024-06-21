package uk.ac.starlink.datanode.viewers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.io.IOException;
import java.lang.reflect.Array;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.hds.ArrayStructure;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.HDSType;
import uk.ac.starlink.table.gui.NumericCellRenderer;
import uk.ac.starlink.table.gui.StarJTable;

/**
 * Displays an array of primitives in a JTable.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ArrayBrowser extends JPanel {

    private static final Object BAD_CELL = new Object();

    /**
     * Constructs an ArrayBrowser from an NDArray.
     *
     * @param   nda  the NDArray to browse
     * @throws  IOException if there is some trouble doing random access
     *          reads on <code>nda</code>
     */
    public ArrayBrowser( NDArray nda ) throws IOException {
        this( makeCellGetter( nda ), nda.getShape() );
    }

    /**
     * Constructs an ArrayBrowser from an ArrayStructure.
     *
     * @param   ary  the ArrayStructure to browse
     * @throws  HDSException if there is some trouble reading <code>ary</code>
     */
    public ArrayBrowser( ArrayStructure ary ) throws HDSException {
        this( makeCellGetter( ary ), ary.getShape() );
    }

    /**
     * Makes an ArrayBrowser from a CellGetter object and an associated
     * pixel sequence (ordered shape).
     */
    private ArrayBrowser( final CellGetter cg, final OrderedNDShape shape ) {

        /* Get array shape. */
        int ndim = shape.getNumDims();
        final long[] dims = shape.getDims();
        final long[] origin = shape.getOrigin();

        /* Get a renderer suitable for headers. */
        DefaultTableCellRenderer hrend = new DefaultTableCellRenderer();
        hrend.setFont( UIManager.getFont( "TableHeader.font" ) );
        hrend.setBackground( UIManager.getColor( "TableHeader.background" ) );
        hrend.setForeground( UIManager.getColor( "TableHeader.foreground" ) );
        hrend.setHorizontalAlignment( SwingConstants.RIGHT );

        /* Get a renderer suitable for cells. */
        final NumericCellRenderer crend = 
            new NumericCellRenderer( cg.getContentClass() );
        crend.setBadValue( BAD_CELL );

        /* At present the only string arrays which this browser is likely
         * to be rendering will be the contents of HDS _CHAR arrays.
         * On the whole it is more sensible to render these in a fixed
         * width font (e.g. FITS cards).  Numeric data should be 
         * fixed-width in any case. */
        crend.setCellFont( new Font( "Monospaced", Font.PLAIN, 
                                     crend.getFont().getSize() ) );

        /* Place a scrollpane to hold the array browser. */
        JScrollPane scroller = new JScrollPane() {
            public Dimension getPreferredSize() {
                Dimension size = ArrayBrowser.this.getSize();
                Insets insets1 = ArrayBrowser.this.getInsets();
                Insets insets2 = this.getInsets();
                int x = size.width - insets1.left - insets1.right
                                   - insets2.left - insets2.right;
                int y = size.height - insets1.top - insets1.bottom
                                    - insets2.top - insets2.bottom;

                // I don't know why this -1 is necessary!
                return new Dimension( x, y - 1 );
            }
        };
        scroller.setAlignmentX( 0.0f );
        scroller.setAlignmentY( 0.0f );
        add( scroller );

        /* Handle the two-dimensional case by making a normal 2d table. */
        if ( ndim == 2 ) {

            /* Construct a model for the table data. */
            TableModel dataModel;
            final int ncol = (int) dims[ 0 ];
            final int nrow = (int) dims[ 1 ];
            final long o0 = origin[ 0 ];
            final long o1 = origin[ 1 ];
            dataModel = new AbstractTableModel() {
                public int getRowCount() {
                    return nrow;
                }
                public int getColumnCount() {
                    return ncol;
                }
                public Object getValueAt( int irow, int icol ) {
                    long p0 = o0 + ( (long) icol );
                    long p1 = o1 + ( (long) nrow - 1 - irow );
                    long[] pos = new long[] { p0, p1 };
                    int index = (int) shape.positionToOffset( pos );
                    return cg.getValueAt( index );
                }
            };

            /* Construct models for the label components. */
            TableModel rowModel = new AbstractTableModel() {
                public int getRowCount() {
                    return nrow;
                }
                public int getColumnCount() {
                    return 1;
                }
                public Object getValueAt( int irow, int icol ) {
                    return new Long( o1 + (long) nrow - 1 - irow ) + "  ";
                }
            };
            TableModel colModel = new AbstractTableModel() {
                public int getRowCount() {
                    return 1;
                }
                public int getColumnCount() {
                    return ncol; 
                }
                public Object getValueAt( int irow, int icol ) {
                    return new Long( o0 + (long) icol  ) + "  ";
                }
            };

            /* Construct the tables which will form the labels. */
            JTable rowHead = new JTable( rowModel );
            JTable colHead = new JTable( colModel );

            /* Construct the table which will form the data view.  
             * The business about the enclosing scroll pane is necessary since
             * otherwise JTable takes control of the columnheader of the
             * scrollpane itself, and on this occasion we want to handle it. */
            JTable dataTab = new JTable( dataModel ) {
                protected void configureEnclosingScrollPane() {
                }
                public TableCellRenderer getCellRenderer( int irow, int icol ) {
                    return crend;
                }
            };

            /* Add them to the correct parts of this frame. */
            scroller.setViewportView( dataTab );
            scroller.setRowHeaderView( rowHead );
            scroller.setColumnHeaderView( colHead );

            /* Sort out some borders for the headers. */
            Color bcol = Color.BLACK;
            Border cb = BorderFactory.createMatteBorder( 0, 0, 2, 0, bcol );
            colHead.setBorder( cb );
            Border rb = BorderFactory.createMatteBorder( 0, 0, 0, 2, bcol );
            rowHead.setBorder( rb );

            /* Put a corner in. */
            JPanel box = new JPanel();
            box.setBorder( BorderFactory
                          .createMatteBorder( 0, 0, 2, 2, bcol ) );
            scroller.setCorner( JScrollPane.UPPER_LEFT_CORNER, box );

            /* Configure the header components. */
            rowHead.setDefaultRenderer( Object.class, hrend );
            colHead.setDefaultRenderer( Object.class, hrend );
            JTable[] tables = new JTable[] { dataTab, rowHead, colHead };
            for ( int i = 0; i < tables.length; i++ ) {
                JTable table = tables[ i ];
                table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
                table.setTableHeader( null );
                table.setPreferredScrollableViewportSize( table
                                                         .getPreferredSize() );
                table.setColumnSelectionAllowed( false );
                table.setRowSelectionAllowed( false );
            }

            /* Set the column widths. */
            int cwidth = crend.getCellWidth();
            TableColumnModel dataTcm = dataTab.getColumnModel();
            TableColumnModel colTcm = colHead.getColumnModel();
            for ( int i = 0; i < ncol; i++ ) {
                dataTcm.getColumn( i ).setPreferredWidth( cwidth );
                colTcm.getColumn( i ).setPreferredWidth( cwidth );
            }
            int rwidth = 8 +
                Math.max( StarJTable.getCellWidth( rowHead, 0, 0 ),
                          StarJTable.getCellWidth( rowHead, nrow - 1, 0 ) );
            rowHead.getColumnModel().getColumn( 0 ).setPreferredWidth( rwidth );
            rowHead.getPreferredScrollableViewportSize().width = rwidth;
        }

        /* If it's not 2-d, treat it as 1-d. */
        else {

            /* Set up the model. */
            final int nrow = (int) shape.getNumPixels();
            final boolean alignRight = 
                ( crend.getHorizontalAlignment() == SwingConstants.RIGHT );
            TableModel dataModel = new AbstractTableModel() {
                public int getRowCount() {
                    return nrow;
                }
                public int getColumnCount() {
                    return 2 + ( alignRight ? 1 : 0 );
                }
                public Object getValueAt( int irow, int icol ) {
                    if ( icol == 0 ) {
                        long[] pos = shape.offsetToPosition( (long) irow );
                        return NDShape.toString( pos ) + "  ";
                    }
                    else if ( icol == 1 ) {
                        return cg.getValueAt( irow );
                    }
                    else {
                        return null;
                    }
                }
            };

            /* Construct the table itself. */
            JTable tab = new JTable( dataModel );
            scroller.setViewportView( tab );

            /* Configure the rendering. */
            tab.setTableHeader( null );
            tab.setShowVerticalLines( false );
            TableColumnModel tcm = tab.getColumnModel();
            TableColumn tcol0 = tcm.getColumn( 0 );
            TableColumn tcol1 = tcm.getColumn( 1 );
            tcol0.setCellRenderer( hrend );
            tcol1.setCellRenderer( crend );

            /* Sort out column widths. */
            int w0 = Math.max( StarJTable.getCellWidth( tab, 0, 0 ),
                               StarJTable.getCellWidth( tab, nrow - 1, 0 ) )
                   + 8;
            tcol0.setMinWidth( w0 );
            tcol0.setMaxWidth( w0 );
            tcol0.setPreferredWidth( w0 );
            if ( alignRight ) {
                int w1 = crend.getCellWidth() + 20;
                tcol1.setMinWidth( w1 );
                tcol1.setMaxWidth( w1 );
            }
        }
    }

    /**
     * Interface which defines how to get the contents of a cell at a given
     * offset into an array.  Mapping the offset to an array position is
     * defined by the OrderedNDShape implicitly associated with this
     * CellGetter.
     */
    private static interface CellGetter {

        /**
         * Returns the object at the cell indicated by a given offset.
         */
        Object getValueAt( int offset );

        /**
         * Returns the class of data we expect to obtain as cells.
         */
        Class getContentClass();
    }

    private static CellGetter makeCellGetter( NDArray nda ) throws IOException {
        if ( ! nda.isRandom() ) {
            throw new IOException( "NDArray " + nda +
                                   " does not have random access" );
        }
        final BadHandler bh = nda.getBadHandler();
        final Object buf = nda.getType().newArray( 1 );
        final ArrayAccess acc = nda.getAccess();
        final Class clazz = getWrapperClass( nda.getType() );
        CellGetter cg = new CellGetter() {
            public Object getValueAt( int index ) {
                try {
                    acc.setOffset( (long) index );
                    acc.read( buf, 0, 1 );
                    Number val = bh.makeNumber( buf, 0 );
                    return ( val == null ) ? BAD_CELL : val;
                }
                catch ( IOException e ) {
                    e.printStackTrace();
                    return BAD_CELL;
                }
            }
            public Class getContentClass() {
                return clazz;
            }
        };
        return cg;
    }

    private static CellGetter makeCellGetter( ArrayStructure ary )
            throws HDSException {
        final HDSObject datobj = ary.getData();
        final OrderedNDShape dshape =
            new OrderedNDShape( datobj.datShape(), Order.COLUMN_MAJOR );
        HDSType htype = ary.getType();
        final Class clazz = ( htype == null )
                          ? Object.class 
                          : getWrapperClass( ary.getType().getJavaType() );
        CellGetter cg = new CellGetter() {
            public Object getValueAt( int index ) {
                try {
                    long[] pos = dshape.offsetToPosition( (long) index );
                    HDSObject cellobj = datobj.datCell( pos );
                    return cellobj.datGet0c();
                }
                catch ( HDSException e ) {
                    e.printStackTrace();
                    return BAD_CELL;
                }
            }
            public Class getContentClass() {
                return clazz;
            }
        };
        return cg;
    }

    private static Class getWrapperClass( Type type ) {
        return Array.get( type.newArray( 1 ), 0 ).getClass();
    }

}
