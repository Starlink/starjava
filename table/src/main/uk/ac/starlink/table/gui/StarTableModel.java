package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import uk.ac.starlink.table.ColumnHeader;
import uk.ac.starlink.table.StarTable;

/**
 * An implementation of TableModel which can be constructed from a StarTable.
 * It is somewhat specialised in that it can contain additional non-data
 * rows/columns at the top and to the left of the table.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StarTableModel extends AbstractTableModel {

    private final StarTable startable;
    private int extraRows;
    private int extraCols;
    private List headRowList;
    private Map[] headMetas;
    private Class[] colClasses;

    public StarTableModel( StarTable startable ) {
        super();
        extraCols = 1;
        this.startable = startable;
        SortedSet headRowSet = new TreeSet( new HeadingComparator() );
        headMetas = new HashMap[ startable.getColumnCount() ];
        colClasses = new Class[ startable.getColumnCount() + extraCols ];
        colClasses[ 0 ] = Object.class;
        for ( int i = 0; i < startable.getColumnCount(); i++ ) {
            ColumnHeader head = startable.getHeader( i );
            colClasses[ i + 1 ] = head.getContentClass();
            headMetas[ i ] = new HashMap();

            String units = head.getUnitString();
            if ( units != null ) {
                String key = "Units";
                headRowSet.add( key );
                headMetas[ i ].put( key, units );
            }

            String ucd = head.getUCD();
            if ( ucd != null ) {
                String key = "UCD";
                headRowSet.add( key );
                headMetas[ i ].put( key, ucd );
            }

            for ( Iterator it = head.getMetadata().entrySet().iterator();
                  it.hasNext(); ) {
                Map.Entry miscItem = (Map.Entry) it.next();
                String key = miscItem.getKey().toString();
                Object value = miscItem.getValue();
                headRowSet.add( key );
                headMetas[ i ].put( key, value );
            }
        }

        /* I think I will do without these auxiliary columns for the moment.
         * In Treeview the information is available from another panel.
         * But to see them, reinstate the commented out line. */
        headRowList = new ArrayList();
        // headRowList.addAll( headRowSet );
        extraRows = headRowList.size();
    }

    public int getColumnCount() {
        return startable.getColumnCount() + extraCols;
    }

    public int getRowCount() {
        return startable.getRowCount() + extraRows;
    }

    public Object getValueAt( int irow, int icol ) {
        Object cell;
        if ( irow < extraRows ) {
            String name = (String) headRowList.get( irow );
            if ( icol == 0 ) {
                cell = name;
            }
            else {
                cell = headMetas[ icol - extraCols ].get( name );
            }
        }
        else {
            if ( icol == 0 ) {
                cell = new Integer( irow - extraRows + 1 );
            }
            else {
                cell = startable.getValueAt( irow - extraRows, 
                                             icol - extraCols );
            }
        }
        return cell;
    }

    public String getColumnName( int icol ) {
        if ( icol == 0 ) {
            return "";
        }
        ColumnHeader head = startable.getHeader( icol - extraCols );
        return head.getName();
    }

    /**
     * Returns the number of non-data rows inserted at the top of the table.
     * These typically contain additional header-type information and will
     * not in general have the same content class as that returned by the
     * appropriate <tt>getBodyClass</tt> call.
     *
     * @return  the numer of non-body rows at the top
     */
    public int getExtraRows() {
        return extraRows;
    }

    /**
     * Returns the number of non-data columns inserted at the left of the
     * table.  These typically contain row indices.
     */
    public int getExtraColumns() {
        return extraCols;
    }

    /**
     * Indicates the kind of data to be found in a given column. 
     * Note this excludes any cells in the 'extra' rows.
     * Subclasses should override this method, rather than 
     * {@link #getColumnClass}, if they want to indicate what type of data a
     * column provides in this way.
     *
     * @param  icol  the column index (including the effect of any extra rows)
     * @return  the class of all objects in that column
     */
    public Class getBodyColumnClass( int icol ) {
        return colClasses[ icol ];
    }

    public Class getColumnClass( int icol ) {
        return Object.class;
    }

    /**
     * Private comparator class used for ordering headings by name.
     */
    private static class HeadingComparator implements Comparator {
        /* This is the reverse of the order in which we want headings 
         * to come out.  Strings not in the list will be last. */
        private static List headings = 
            Arrays.asList( new String[] { "UCD", "Units" } );
        public int compare( Object o1, Object o2 ) {
            return headings.indexOf( o2 ) - headings.indexOf( o1 );
        }
    }

}
