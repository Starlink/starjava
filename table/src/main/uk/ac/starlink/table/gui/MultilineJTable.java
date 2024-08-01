package uk.ac.starlink.table.gui;

import java.awt.Color;
import java.awt.Component;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 * JTable which can display multiple-line elements.
 * This class behaves like a JTable except that any of its cells which
 * contain String objects with newline characters ('\n'), or String[] arrays
 * are displayed on multiple table rows.  
 * <p>
 * The implementation may make some demands in consequence of this;
 * it may require that the data in the TableModel does not change while
 * this table is active, and it may need to read the whole TableModel
 * prior to startup.  If these demands exist, they may be relaxed 
 * by improved implemenatation in the future.
 * <p>
 * Because the number of rows is modified, it is unwise to make any
 * TableCellRenderers (or TableCellEditors?) associated with this table
 * sensitive to the index of the row with which they are dealing.
 *
 * @author   Mark Taylor (Starlink)
 */
public class MultilineJTable extends JTable {

    /** The TableModel which represents the actual data in the table. */
    private TableModel baseModel_;

    /** The TableModel which is used by the superclass. */
    private MultilineTableModel multiModel_;

    private Border firstBorder_;
    private Border otherBorder_;
 
    /**
     * Constructs a new table given a base TableModel.  
     * The model of 
     * this JTable is not actually the base model supplied, a new model
     * derived from that one (potentially with more rows) is used.
     *
     * @param  baseModel   a TableModel describing the data this table
     *                     will display
     */
    @SuppressWarnings("this-escape")
    public MultilineJTable( TableModel baseModel ) {
        this();
        setModel( baseModel );
    }

    /**
     * Constructs a MultilineJTable without any data. 
     * Its TableModel is initialised to <code>null</code>.
     */
    @SuppressWarnings("this-escape")
    public MultilineJTable() {
        setModel( getModel() );
        setGridColor( UIManager.getColor( "Table.gridColor" ) );
    }

    /**
     * Sets the model which this table should display.
     * The model of 
     * this JTable is not actually the base model supplied, a new model
     * derived from that one (potentially with more rows) is used.
     *
     * @param  baseModel   a TableModel describing the data this table
     *                     will display
     */
    public void setModel( TableModel baseModel ) {
        baseModel_ = baseModel;
        multiModel_ = new CachingMultilineTableModel( baseModel );
        super.setModel( multiModel_ );
    }

    /**
     * Returns the model which this table is using for data display.
     * Note that this will not be the same object as that specified in
     * the constructor or {@link #setModel} method, it will be 
     * some model derived from that one (potentially with more rows).
     *
     * @return  a TableModel used for display
     */
    public TableModel getModel() {
        return super.getModel();
    }

    public Component prepareRenderer( TableCellRenderer rend, int row, 
                                      int col ) {
        JComponent cell = 
            (JComponent) super.prepareRenderer( rend, row, col );
        boolean isFirst = multiModel_.isFirstRowOfGroup( row );
        cell.setBorder( isFirst ? firstBorder_ : otherBorder_ );
        return cell;
    }

    public void setGridColor( Color gridColor ) {
        firstBorder_ = BorderFactory.createMatteBorder( 1, 0, 0, 1, gridColor );
        otherBorder_ = BorderFactory.createMatteBorder( 0, 0, 0, 1, gridColor );
    }

    
    /**
     * The base table is the one that contains the original data.
     * The multi table is the one that has extra rows inserted to contain
     * continuation lines.
     */
    private static class MultilineTableModel extends AbstractTableModel {
 
        private int ncol;
        private int multiNrow;
        private int[] firstMultiRowOfBaseRow;
        private int[] baseRowOfMultiRow;
        private TableModel baseModel_;

        public MultilineTableModel( TableModel baseModel ) {
            baseModel_ = baseModel;
            int baseNrow = baseModel.getRowCount();
            ncol = baseModel.getColumnCount();

            /* Fill up forward lookup table. */
            firstMultiRowOfBaseRow = new int[ baseNrow + 1 ];
            multiNrow = 0;
            for ( int i = 0; i < baseNrow; i++ ) {
                firstMultiRowOfBaseRow[ i ] = multiNrow;
                int nlines = 1;
                for ( int j = 0; j < ncol; j++ ) {
                    Object val = baseModel.getValueAt( i, j );
                    Object[] lines = getLines( val );
                    if ( lines != null && lines.length > nlines ) {
                        nlines = lines.length;
                    }
                }
                multiNrow += nlines;
            }
            firstMultiRowOfBaseRow[ baseNrow ] = multiNrow;  // end marker

            /* Fill up reverse lookup table. */
            baseRowOfMultiRow = new int[ multiNrow ];
            for ( int i = 0; i < baseNrow; i++ ) {
                for ( int j = firstMultiRowOfBaseRow[ i ];
                          j < firstMultiRowOfBaseRow[ i + 1 ]; j++ ) {
                    baseRowOfMultiRow[ j ] = i;
                }
            }
        }

        public boolean isFirstRowOfGroup( int multiRow ) {
            return firstMultiRowOfBaseRow[ baseRowOfMultiRow[ multiRow ] ] 
                   == multiRow;
        }

        public int getRowCount() {
            return multiNrow;
        }

        public int getColumnCount() {
            return ncol;
        }

        public Object getValueAt( int multiRow, int col ) {
            int baseRow = baseRowOfMultiRow[ multiRow ];
            Object baseValue = baseModel_.getValueAt( baseRow, col );
            boolean singleRowGroup = firstMultiRowOfBaseRow[ baseRow + 1 ]
                                  == firstMultiRowOfBaseRow[ baseRow ] + 1;
            if ( singleRowGroup ) {
                Object val = baseValue;

                /* If it's a 1-element array, return the sole element. */
                if ( val != null &&
                     val.getClass().getComponentType() != null &&
                     Array.getLength( val ) == 1 ) {
                    val = Array.get( val, 0 );
                }
                return val;
            }
            else {
                int index = multiRow - firstMultiRowOfBaseRow[ baseRow ];
                return getLineFromGroup( index, baseValue );
            }
        }

        private Object getLineFromGroup( int index, Object val ) {
            Object[] lines = getLines( val );
            if ( lines == null ) {
                return ( index == 0 ) ? val : null;
            }
            else {
                return ( index < lines.length ) ? lines[ index ] : null;
            }
        }

        /**
         * Returns an array of lines for a given value.  In the case that
         * the value is not of a multiline type, it returns null.
         */
        private Object[] getLines( Object val ) {
            if ( val instanceof String[] ) {
                return (Object[]) val;
            }
            else if ( val instanceof String ) {
                return ((String) val).trim().split( "\n" );
            }
            else {
                return null;
            }
        }

        /* Other TableModel methods are delegated. */
        public Class<?> getColumnClass( int colIndex ) {
            return baseModel_.getColumnClass( colIndex );
        }
        public String getColumnName( int colIndex ) {
            return baseModel_.getColumnName( colIndex );
        }
    }

    private static class CachingMultilineTableModel 
            extends MultilineTableModel {
        private static Object BLANK = new Object();
        private final List<ArrayList<Object>> cellValues_;
        public CachingMultilineTableModel( TableModel baseModel ) {
            super( baseModel );
            int ncol = getColumnCount();
            cellValues_ = new ArrayList<ArrayList<Object>>();
            for ( int i = 0; i < ncol; i++ ) {
                cellValues_.add( new ArrayList<Object>( 16 ) );
            }
        }
        public Object getValueAt( int irow, int icol ) {
            ArrayList<Object> colValues = cellValues_.get( icol );
            if ( colValues.size() <= irow ) {
                colValues.ensureCapacity( (int) ( irow * 1.5 ) );
            }
            while ( colValues.size() <= irow ) {
                colValues.add( BLANK );
            }
            if ( colValues.get( irow ) == BLANK ) {
                colValues.set( irow, super.getValueAt( irow, icol ) );
            }
            return colValues.get( irow );
        }
    }

}
