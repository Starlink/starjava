package uk.ac.starlink.treeview;

import java.awt.Component;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.array.ArrayArrayImpl;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.treeview.votable.Table;

public class VOTableViewer extends JTable {

    private static final int MAX_VALUES_IN_CELL = 4;

    private List rowList = new ArrayList();
    private Number[] badvals;
    private Table votable;

    public VOTableViewer( final Table votable ) {
        this.votable = votable;

        /* Find the number of rows and columns. */
        int nr = votable.getNumRows();
        if ( nr < 0 ) {
            nr = 0;
            while ( votable.hasNextRow() ) {
                getRow( nr++ );
            }
        }
        final int nrows = nr;
        final int ncols = votable.getNumColumns();

        /* Store the bad values for each column. */
        badvals = new Number[ ncols ];
        for ( int i = 0; i < ncols; i++ ) {
            badvals[ i ] = votable.getField( i ).getDatatype().getNull();
        }

        /* Construct and set the data model. */
        TableModel tmodel = new AbstractTableModel() {
            public int getRowCount() {
                return nrows;
            }
            public int getColumnCount() {
                return ncols + 1;
            }
            public Object getValueAt( int irow, int icol ) {
                if ( icol == 0 ) {
                    return new Integer( irow + 1 );
                }
                else {
                    return getRow( irow )[ icol - 1 ];
                }
            }
            public String getColumnName( int icol ) {
                if ( icol == 0 ) {
                    return "Row index";
                }
                else {
                    return votable.getField( icol - 1 ).getHandle();
                }
            }
        };
        setModel( tmodel );

        /* Do cosmetic setup and set the cell renderer. */
        setAutoResizeMode( AUTO_RESIZE_OFF );
        TableColumnModel tcm = getColumnModel();
        TableCellRenderer headRend = new DefaultTableCellRenderer();
        for ( int icol = 0; icol < tcm.getColumnCount(); icol++ ) {
            TableColumn tc = tcm.getColumn( icol );
            TableCellRenderer bodyRend = ( icol == 0 ) 
                                         ? new DefaultTableCellRenderer()
                                         : VOCellRenderer.getInstance();
            if ( nrows == 0 ) {
                bodyRend = null;
            }
            tc.setPreferredWidth( getColumnWidth( tmodel, bodyRend, 
                                                  headRend, icol ) );
            tc.setCellRenderer( bodyRend );
        }
    }

    /**
     * Gets a given row from the VOTable.
     */
    private Object[] getRow( int irow ) {

        /* If we haven't filled up our row list far enough yet, do it now. */
        while ( rowList.size() <= irow ) {
            Object[] nextRow = votable.nextRow();

            /* Doctor each field in this row as necessary. */
            for ( int i = 0; i < nextRow.length; i++ ) {
                Object item = nextRow[ i ];
                if ( item != null ) {

                    /* If it's a java array, it must represent a 1-d array. 
                     * Try to turn it into a 1-d NDArray unless it's short. */
                    Class ctype = item.getClass().getComponentType();
                    if ( Type.getType( ctype ) != null &&
                         Array.getLength( item ) > MAX_VALUES_IN_CELL ) {
                        long[] dims = new long[] { Array.getLength( item ) };
                        OrderedNDShape oshape = 
                            new OrderedNDShape( dims, Order.COLUMN_MAJOR );
                        ArrayImpl ai = 
                            new ArrayArrayImpl( item, oshape, badvals[ i ] );
                        nextRow[ i ] = new BridgeNDArray( ai );
                    }
                }
            }
            rowList.add( nextRow );
        }
        return (Object[]) rowList.get( irow );
    }

    private static int getColumnWidth( TableModel tmodel,
                                       TableCellRenderer bodyRend, 
                                       TableCellRenderer headRend, int icol ) {
        JTable dummyTable = new JTable();

        Object headObj = tmodel.getColumnName( icol );
        Component headComp =
            headRend.getTableCellRendererComponent( dummyTable, headObj,
                                                    false, false, 0, icol );
        int headWidth = headComp.getPreferredSize().width + 20;

        int bodyWidth;
        if ( bodyRend != null ) {
            Object bodyObj = tmodel.getValueAt( 0, icol );
            Component bodyComp = 
                bodyRend.getTableCellRendererComponent( dummyTable, bodyObj, 
                                                        false, false, 0, icol );
            bodyWidth = (int) (bodyComp.getPreferredSize().width * 1.2);
        }
        else {
            bodyWidth = 0;
        }
        return Math.max( 60, Math.max( headWidth, bodyWidth ) );
    }


    /**
     * Class used for rendering cells in the JTable.  This is a singleton.
     */
    private static class VOCellRenderer extends DefaultTableCellRenderer {
        private static VOCellRenderer instance = new VOCellRenderer();
        public static TableCellRenderer getInstance() {
            return instance;
        }
        public Component getTableCellRendererComponent( 
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column ) {
            super.getTableCellRendererComponent( table, value, isSelected,
                                                 hasFocus, row, column );
            setIcon( null );
            setText( null );
            if ( value instanceof NDArray ) {
                NDArray nda = (NDArray) value;
                setIcon( IconFactory.getInstance()
                        .getArrayIcon( nda.getShape().getNumDims() ) );
            }
            else {
                StringBuffer sb = new StringBuffer();
                if ( value instanceof String ) {
                    sb.append( value );
                }
                else if ( value instanceof String[] ) {
                    String[] array = (String[]) value;
                    for ( int i = 0; i < array.length; i++ ) {
                        if ( i > 0 ) sb.append( ", " );
                        sb.append( array[ i ] );
                    }
                }
                else if ( value instanceof byte[] ) {
                    byte[] array = (byte[]) value;
                    for ( int i = 0; i < array.length; i++ ) {
                        if ( i > 0 ) sb.append( ", " );
                        sb.append( array[ i ] );
                    }
                }
                else if ( value instanceof short[] ) {
                    short[] array = (short[]) value;
                    for ( int i = 0; i < array.length; i++ ) {
                        if ( i > 0 ) sb.append( ", " );
                        sb.append( array[ i ] );
                    }
                }
                else if ( value instanceof int[] ) {
                    int[] array = (int[]) value;
                    for ( int i = 0; i < array.length; i++ ) {
                        if ( i > 0 ) sb.append( ", " );
                        sb.append( array[ i ] );
                    }
                }
                else if ( value instanceof float[] ) {
                    float[] array = (float[]) value;
                    for ( int i = 0; i < array.length; i++ ) {
                        if ( i > 0 ) sb.append( ", " );
                        sb.append( array[ i ] );
                    }
                }
                else if ( value instanceof double[] ) {
                    double[] array = (double[]) value;
                    for ( int i = 0; i < array.length; i++ ) {
                        if ( i > 0 ) sb.append( ", " );
                        sb.append( array[ i ] );
                    }
                }
                setText( sb.toString() );
            }
            return this;
        }
    }
}
