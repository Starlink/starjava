package uk.ac.starlink.topcat;

import uk.ac.starlink.table.gui.StarTableColumn;

/**
 * List cell renderer which will render StarTableColumn objects sensibly.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Feb 2004
 */
public class ColumnCellRenderer extends CustomComboBoxRenderer {
    public Object mapValue( Object value ) {
        return value instanceof StarTableColumn 
             ? ((StarTableColumn) value).getColumnInfo().getName()
             : value;
    }
}
