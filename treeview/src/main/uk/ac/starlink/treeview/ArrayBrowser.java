package uk.ac.starlink.treeview;

import java.awt.Color;
import java.awt.Component;
import java.io.IOException;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.OrderedNDShape;

class ArrayBrowser extends JTable {

    public static Object BAD_CELL = null;

    private TableCellRenderer cellRend;
    private TableCellRenderer headRend;
    private TableModel model;
    private int ndim;
    private static float colfact = (float) 1.0;

    public ArrayBrowser( NDArray nda ) throws IOException {
        if ( ! nda.isRandom() ) {
            throw new IllegalArgumentException( 
                "NDArray " + nda + " does not have random access" );
        }
        final BadHandler bh = nda.getBadHandler();
        final Object buf = nda.getType().newArray( 1 );
        final ArrayAccess acc = nda.getAccess();
        final CellGetter cg = new CellGetter() {
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

        final OrderedNDShape shape = nda.getShape();
        ndim = shape.getNumDims();

        /* Construct the component to be displayed for all bad cells. */
        Color goodColor = UIManager.getColor( "Table.foreground" );
        Color badColor = new Color( goodColor.getRed(), 
                                    goodColor.getGreen(), 
                                    goodColor.getBlue(), 
                                    goodColor.getAlpha() / 3 );
        DefaultTableCellRenderer badRend = new DefaultTableCellRenderer();
        badRend.setForeground( badColor );
        final Component badcell = 
            badRend.getTableCellRendererComponent( this, "BAD", false, false,
                                                   0, 0 );

        /* Construct the normal renderer for cells in the table body. */
        cellRend = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent( JTable table,
                                                            Object value,
                                                            boolean isSelected,
                                                            boolean hasFocus,
                                                            int row,
                                                            int column ) {
                return ( value == BAD_CELL ) 
                    ? badcell 
                    : super.getTableCellRendererComponent( table, value, 
                                                           isSelected, hasFocus,
                                                           row, column );
            }
        };

        /* Construct the renderer for cells in the table headings. */
        headRend = new DefaultTableCellRenderer();
        ((DefaultTableCellRenderer) headRend)
       .setBackground( UIManager.getColor( "TableHeader.background" ) );
        ((DefaultTableCellRenderer) headRend)
       .setForeground( UIManager.getColor( "TableHeader.foreground" ) );

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
        if ( ndim == 2 ) {
            setAutoResizeMode( AUTO_RESIZE_OFF );
            if ( colfact != 1.0 ) {
                int colwidth = (int) ( colfact * getColumnModel().getColumn( 0 )
                                                                 .getWidth() );
                int ncols = (int) shape.getDims()[ 1 ];
                for ( int i = 0; i < ncols; i++ ) {
                    TableColumn tcol = getColumnModel().getColumn( 1 + i );
                    tcol.setPreferredWidth( colwidth );
                    tcol.setMaxWidth( colwidth );
                    tcol.setMinWidth( colwidth );
                }
            }
        }
        else {
            long lastpix = shape.getNumPixels() - 1;
            String rep = NDShape.toString( shape.offsetToPosition( lastpix ) );
            int colwidth = getFont().getSize() * rep.length();
            TableColumn tcol = getColumnModel().getColumn( 0 );
            // tcol.setPreferredWidth( colwidth );
            tcol.setMaxWidth( colwidth );
            tcol.setMinWidth( colwidth );
        }

        /* Table configuration. */
        setColumnSelectionAllowed( false );
        setCellSelectionEnabled( false );
        setDragEnabled( false );
        setRowSelectionAllowed( false );
        setTableHeader( null );
        setPreferredScrollableViewportSize( new java.awt.Dimension( 1, 1 ) );
    }


    public TableCellRenderer getCellRenderer( int row, int col ) {
        boolean isHead = 
           ( ndim == 2 ) ? ( row == 0 || row == model.getRowCount() - 1 ||
                             col == 0 || col == model.getColumnCount() - 1 )
                         : ( col == 0 );
        return isHead ? headRend : cellRend;
    }


    private static interface CellGetter {
        Object getValueAt( int index );
    }


}
