package uk.ac.starlink.table;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Table cell renderer used for boolean values.
 * This does the same as the default Swing (JTable) one, except that it
 * renders an empty cell for a null value (or in fact any non-boolean one).
 * It's rather questionable allowing null values for a boolean column,
 * but this is explicitly permitted by the FITS and VOTable standards,
 * so we support it here.
 *
 * <p>This is a singleton class.
 *
 * @author   Mark Taylor
 * @since    9 Nov 2005
 */
class BooleanCellRenderer implements TableCellRenderer {

    private static BooleanCellRenderer instance_;
    private final TableCellRenderer nonNullBase_;
    private final TableCellRenderer nullBase_;

    /**
     * Private sole constructor.
     */
    private BooleanCellRenderer() {
        JTable jt = new JTable();
        nonNullBase_ = jt.getDefaultRenderer( Boolean.class );
        nullBase_ = jt.getDefaultRenderer( String.class );
    }

    public Component getTableCellRendererComponent( JTable table, Object value,
                                                    boolean isSelected,
                                                    boolean hasFocus,
                                                    int irow, int icol ) {
        if ( value instanceof Boolean ) {
            return nonNullBase_
                  .getTableCellRendererComponent( table, value, isSelected,
                                                  hasFocus, irow, icol );
        }
        else {
            return nullBase_
                  .getTableCellRendererComponent( table, "", isSelected,
                                                  hasFocus, irow, icol );
        }
    }

    /**
     * Returns the singleton instance of this class.
     *
     * @return   instance
     */
    public static BooleanCellRenderer getInstance() {
        if ( instance_ == null ) {
            instance_ = new BooleanCellRenderer();
        }
        return instance_;
    }
}
