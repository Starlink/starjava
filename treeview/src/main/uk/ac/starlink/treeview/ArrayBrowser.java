package uk.ac.starlink.treeview;

import java.awt.*;
import java.io.IOException;
import java.nio.*;
import javax.swing.*;
import javax.swing.table.*;
import uk.ac.starlink.hdx.array.*;

class ArrayBrowser extends JTable {

    public static Object BAD_CELL = null;

    private TableCellRenderer cellRend;
    private TableCellRenderer headRend;
    private TableModel model;
    private int ndim;
    private float colfact = (float) 1.0;

    public ArrayBrowser( final Buffer niobuf, final Number bad,
                         Cartesian origin, Cartesian shape ) {

        CellGetter cg;
        if ( niobuf instanceof ByteBuffer ) {
            cg = new CellGetter() {
                ByteBuffer buf = (ByteBuffer) niobuf;
                public Object getValueAt( int index ) {
                    Byte val = new Byte( buf.get( index ) );
                    return val.equals( bad ) ? BAD_CELL : val;
                }
            };
        }
        else if ( niobuf instanceof ShortBuffer ) {
            cg = new CellGetter() {
                ShortBuffer buf = (ShortBuffer) niobuf;
                public Object getValueAt( int index ) {
                    Short val = new Short( buf.get( index ) );
                    return val.equals( bad ) ? BAD_CELL : val;
                }
            };
        }
        else if ( niobuf instanceof IntBuffer ) {
            cg = new CellGetter() {
                IntBuffer buf = (IntBuffer) niobuf;
                public Object getValueAt( int index ) {
                    Integer val = new Integer( buf.get( index ) );
                    return val.equals( bad ) ? BAD_CELL : val;
                }
            };
        }
        else if ( niobuf instanceof FloatBuffer ) {
            cg = new CellGetter() {
                FloatBuffer buf = (FloatBuffer) niobuf;
                public Object getValueAt( int index ) {
                    Float val = new Float( buf.get( index ) );
                    return ( val.equals( bad ) || val.isNaN() ) ? BAD_CELL 
                                                                : val;
                }
            };
            colfact = (float) 1.4;
        }
        else if ( niobuf instanceof DoubleBuffer ) {
            cg = new CellGetter() {
                DoubleBuffer buf = (DoubleBuffer) niobuf;
                public Object getValueAt( int index ) {
                    Double val = new Double( buf.get( index ) );
                    return ( val.equals( bad ) || val.isNaN() ) ? BAD_CELL
                                                                : val;
                }
            };
            colfact = (float) 2.0;
        }
        else {
            // assert false;
            throw new AssertionError();
        }

        initTable( cg, origin, shape );
    }
 

    public ArrayBrowser( final NDArray nda ) {
        if ( ! nda.isRandom() ) {
            throw new IllegalArgumentException( 
                "NDArray " + nda + " does not have random access" );
        }
        final BadHandler bh = nda.getBadHandler();
        final Object buf = nda.getType().newArray( 1 );
        CellGetter cg = new CellGetter() {
            public Object getValueAt( int index ) {
                try {
                    nda.setOffset( (long) index );
                    nda.read( buf, 0, 1 );
                    Number val = bh.makeNumber( buf, 0 );
                    return ( val == null ) ? BAD_CELL : val;
                }
                catch ( IOException e ) {
                    e.printStackTrace();
                    return BAD_CELL;
                }
            }
        };
        NDShape oshape = nda.getShape();
        initTable( cg, new Cartesian( oshape.getOrigin() ),
                       new Cartesian( oshape.getDims() ) );
    }
         

    private void initTable( final CellGetter cg, 
                            Cartesian origin, Cartesian shape ) {

        ndim = shape.getNdim();
        if ( origin == null ) {
            origin = new Cartesian( ndim );
            for ( int i = 0; i < ndim; i++ ) {
                origin.setCoord( i, 1L );
            }
        }

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
            final int rows = (int) shape.getCoord( 0 );
            final int cols = (int) shape.getCoord( 1 );
            final long o0 = origin.getCoord( 0 );
            final long o1 = origin.getCoord( 1 );
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
                        int index = c * rows + r;
                        return cg.getValueAt( index );
                    }
                    else {
                        if ( ( r < 0 || r >= rows ) &&
                             ( c >= 0 && c < cols ) ) {
                            return Long.toString( (long) c + o1 );
                        }
                        else if ( ( c < 0 || c >= cols ) &&
                                  ( r >= 0 && r < rows ) ) {
                            return Long.toString( (long) r + o0 );
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
            final int rows = (int) shape.numCells();
            final long[] dims = shape.getCoords();
            final long[] org = origin.getCoords();

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
                        long[] pos = (long[]) org.clone();
                        for ( int i = 0; i < ndim; i++ ) {
                            pos[ i ] += index % dims[ i ];
                            index /= dims[ i ];
                        }
                        return Cartesian.toString( pos );
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
                for ( int i = 0; i < shape.getCoord( 1 ); i++ ) {
                    TableColumn tcol = getColumnModel().getColumn( 1 + i );
                    tcol.setPreferredWidth( colwidth );
                    tcol.setMaxWidth( colwidth );
                    tcol.setMinWidth( colwidth );
                }
            }
        }
        else {
            String rep = shape.toString();
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


    private interface CellGetter {
        Object getValueAt( int index );
    }


}
