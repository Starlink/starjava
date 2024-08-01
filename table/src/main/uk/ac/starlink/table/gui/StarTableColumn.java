package uk.ac.starlink.table.gui;

import javax.swing.table.TableColumn;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ValueInfo;

/**
 * A <code>TableColumn</code> representing a column
 * in a <code>StarJTable</code>.
 * This simple extension to <code>TableColumn</code> provides a constructor
 * and an accessor which reference the 
 * {@link uk.ac.starlink.table.ColumnInfo} object 
 * associated with a table column.  The renderers and column headings etc
 * used by this column are based by default on the characteristics of
 * the associated <code>ColumnInfo</code>.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StarTableColumn extends TableColumn {

    private ColumnInfo colinfo;

    /**
     * Constructs a <code>StarTableColumn</code> from a <code>ColumnInfo</code>
     * object with a given model index.
     *
     * @param   colinfo  the <code>ColumnInfo</code> object which supplies the
     *          characteristics of this column
     * @param   modelIndex the index of the column in the
     *          <code>TableModel</code> which will be displayed by this column
     */
    @SuppressWarnings("this-escape")
    public StarTableColumn( ColumnInfo colinfo, int modelIndex ) {
        super( modelIndex );
        this.colinfo = colinfo;
        setCellRenderer( createCellRenderer( colinfo ) );
        setCellEditor( createCellEditor( colinfo ) );
        setHeaderValue( colinfo.getName() );
    }

    /**
     * Constructs a <code>StarTableColumn</code> from a <code>ColumnInfo</code>
     * object using a default model index of 0.
     *
     * @param   colinfo  the <code>ColumnInfo</code> object which supplies the
     *          characteristics of this column
     */
    public StarTableColumn( ColumnInfo colinfo ) {
        this( colinfo, 0 );
    }

    /**
     * Returns the <code>ColumnInfo</code> object associated with this column.
     *
     * @return  the metadata for this column
     */
    public ColumnInfo getColumnInfo() {
        return colinfo;
    }

    /**
     * Utility method to create a table cell renderer suitable for a given
     * value info.  This is used in StarTableColumn's constructor.
     *
     * @param   info  metadata describing table cell contents
     * @return   cell renderer
     */
    public static TableCellRenderer createCellRenderer( ValueInfo info ) {
        Class<?> clazz = info.getContentClass();
        if ( Number.class.isAssignableFrom( clazz ) ) {
            return new NumericCellRenderer( clazz );
        }
        else if ( clazz.equals( Boolean.class ) ) {
            return BooleanCellRenderer.getInstance();
        }
        else {
            return new ValueInfoCellRenderer( info );
        }
    }

    /**
     * Utility method to create a table cell editor suitable for a given
     * value info.  This is used in StarTableColumn's constructor.
     *
     * @param   info  metadata describing table cell contents
     * @return   cell renderer
     */
    public static TableCellEditor createCellEditor( ValueInfo info ) {
        return ValueInfoCellEditor.makeEditor( info );
    }
}
