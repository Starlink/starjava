package uk.ac.starlink.treeview;

import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
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
import uk.ac.starlink.hds.ArrayStructure;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.StarTableCellRenderer;

/**
 * Displays an array of primitives in a JTable.
 * 
 * @author   Mark Taylor (Starlink)
 */
class ArrayBrowser extends JTable {

    private static TableCellRenderer bodyRenderer;
    private static TableCellRenderer borderRenderer;
    private static final Object BAD_CELL = null;

    private int ndim;
    private TableModel model;

    /**
     * Constructs an ArrayBrowser from an NDArray.
     *
     * @param   nda  the NDArray to browse
     * @throws  IOException if there is some trouble doing random access
     *          reads on <tt>nda</tt>
     */
    public ArrayBrowser( NDArray nda ) throws IOException {
        this( makeCellGetter( nda ), nda.getShape() );
    }

    /**
     * Constructs an ArrayBrowser from an ArrayStructure.
     *
     * @param   ary  the ArrayStructure to browse
     * @throws  HDSException if there is some trouble reading <tt>ary</tt>
     */
    public ArrayBrowser( ArrayStructure ary ) throws HDSException {
        this( makeCellGetter( ary ), ary.getShape() );
    }

    /**
     * Makes an ArrayBrowser from a CellGetter object and an associated
     * pixel sequence (ordered shape).
     */
    private ArrayBrowser( final CellGetter cg, final OrderedNDShape shape ) {
        ndim = shape.getNumDims();
        borderRenderer = new StarTableCellRenderer( true );
        bodyRenderer = new StarTableCellRenderer();
        ((StarTableCellRenderer) bodyRenderer).setBadValue( BAD_CELL );

        /* Construct a model. */
        if ( ndim == 2 ) {
            final int cols = (int) shape.getDims()[ 0 ];
            final int rows = (int) shape.getDims()[ 1 ];
            final long o0 = shape.getOrigin()[ 0 ];
            final long o1 = shape.getOrigin()[ 1 ];
            model = new AbstractTableModel() {
                public int getRowCount() {
                    return rows + 2;
                }
                public int getColumnCount() {
                    return cols + 2;
                }
                public Object getValueAt( int row, int col ) {
                    int r = row - 1;
                    int c = col - 1; 
                    if ( r >= 0 && r < rows && c >= 0 && c < cols ) {
                        long p0 = o0 + ( (long) c );
                        long p1 = o1 + ( (long) rows - 1 - r );
                        long[] pos = new long[] { p0, p1 };
                        int index = (int) shape.positionToOffset( pos );
                        return cg.getValueAt( index );
                    }
                    else {
                        if ( ( r < 0 || r >= rows ) &&
                             ( c >= 0 && c < cols ) ) {
                            long p0 = o0 + ( (long) c );
                            return Long.toString( p0 );
                        }
                        else if ( ( c < 0 || c >= cols ) &&
                                  ( r >= 0 && r < rows ) ) {
                            long p1 = o1 + ( (long) rows - 1 - r );
                            return Long.toString( p1 );
                        }
                        else {
                            return null;
                        }
                    }
                }
            };
            setAutoResizeMode( AUTO_RESIZE_OFF );
        }
        else {
            final int rows = (int) shape.getNumPixels();
            final long[] dims = shape.getDims();
            final long[] org = shape.getOrigin();

            model = new AbstractTableModel() {
                public int getRowCount() {
                    return rows;
                }
                public int getColumnCount() {
                    return 2;
                }
                public Object getValueAt( int row, int column ) {
                    int index = row;
                    if ( column == 1 ) {
                        return cg.getValueAt( index );
                    }
                    else {
                        return NDShape.toString(
                                 shape.offsetToPosition( (long) index ) );
                    }
                }
            };
        }

        /* Set the table data model. */
        setModel( model );

        /* Sort out column widths. */
        TableColumnModel tcm = getColumnModel();
        if ( ndim == 2 ) {
            int ncol = model.getColumnCount();

            /* Get width of first and last columns. */
            long lastpix = shape.getDims()[ 1 ] - 1;
            Object borderObj = Long.toString( lastpix ); 
            Component borderCell = 
                borderRenderer
               .getTableCellRendererComponent( this, borderObj, false, false,
                                               (int) lastpix, 0 );
            int borderColwidth = borderCell.getPreferredSize().width + 10;
            tcm.getColumn( 0 ).setPreferredWidth( borderColwidth );
            tcm.getColumn( ncol - 1 ).setPreferredWidth( borderColwidth );

            /* Get width of body columns. */
            Object bodyObj = new Double( Math.PI * 1e-99 );
            Component bodyCell =
                bodyRenderer
               .getTableCellRendererComponent( this, bodyObj, false, false, 
                                               1, 1 );
            int bodyColwidth = bodyCell.getPreferredSize().width + 10;
            for ( int i = 1; i < ncol - 1; i++ ) {
                tcm.getColumn( i ).setPreferredWidth( bodyColwidth + 10 );
            }
        }
        else {
            long lastpix = shape.getNumPixels() - 1;
            String rep = NDShape.toString( shape.offsetToPosition( lastpix ) );
            int colwidth = getFont().getSize() * rep.length();
            TableColumn tcol = getColumnModel().getColumn( 0 );
            tcol.setMaxWidth( colwidth );
            tcol.setMinWidth( colwidth );
        }

        /* Table configuration. */
        setColumnSelectionAllowed( false );
        setCellSelectionEnabled( false );
        setDragEnabled( false );
        setRowSelectionAllowed( false );
        setTableHeader( null );
    }

    public TableCellRenderer getCellRenderer( int row, int col ) {
        boolean isBorder =
            ( ndim == 2 ) ? ( row == 0 || row == model.getRowCount() - 1 ||
                              col == 0 || col == model.getColumnCount() - 1 )
                          : ( col == 0 );
        return isBorder ? borderRenderer : bodyRenderer;
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
    }

    private static CellGetter makeCellGetter( NDArray nda ) throws IOException {
        if ( ! nda.isRandom() ) {
            throw new IOException( "NDArray " + nda + 
                                   " does not have random access" );
        }
        final BadHandler bh = nda.getBadHandler();
        final Object buf = nda.getType().newArray( 1 );
        final ArrayAccess acc = nda.getAccess();
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
        };
        return cg;
    }

    private static CellGetter makeCellGetter( ArrayStructure ary )
            throws HDSException {
        final HDSObject datobj = ary.getData();
        final OrderedNDShape dshape = 
            new OrderedNDShape( datobj.datShape(), Order.COLUMN_MAJOR );
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
        };
        return cg;
    }
}
