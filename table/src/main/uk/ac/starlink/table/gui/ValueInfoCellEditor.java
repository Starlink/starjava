package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.awt.Toolkit;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import uk.ac.starlink.table.ValueInfo;

/**
 * Can make a TableCellEditor suitable for a ValueInfo.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ValueInfoCellEditor {

    /**
     * Returns a TableCellEditor that can be used for editing the values
     * described by this <code>ValueInfo</code>.
     *
     * @param  vinfo  the ValueInfo object describing the cell to be edited
     * @return  a TableCellEditor
     */
    public static TableCellEditor makeEditor( final ValueInfo vinfo ) {
        Class<?> clazz = vinfo.getContentClass();
        if ( clazz == Boolean.class ) {
            return new DefaultCellEditor( new JCheckBox() ) {
                public boolean stopCellEditing() {
                    try {
                        return super.stopCellEditing();
                    }
                    catch ( Exception e ) {
                        copeWithException( e );
                        cancelCellEditing();
                        return true;
                    }
                }
            };
        }
        else {
            final JTextField tfield = new JTextField();
            return new DefaultCellEditor( tfield ) {
                public Object getCellEditorValue() {
                    return vinfo.unformatString( tfield.getText() );
                }
                public boolean stopCellEditing() {
                    try {
                        return super.stopCellEditing();
                    }
                    catch ( Exception e ) {
                        copeWithException( e );
                        cancelCellEditing();
                        return true;
                    }
                }
                public Component 
                        getTableCellEditorComponent( JTable table, Object value,
                                                     boolean isSelected,
                                                     int irow, int icol ) {
                    String renderedValue = vinfo.formatValue( value, 10240 );
                    return super.getTableCellEditorComponent( table, 
                                                              renderedValue,
                                                              isSelected, 
                                                              irow, icol );
                }
            };
        }
    }

    private static void copeWithException( Exception e ) {
        Toolkit.getDefaultToolkit().beep();
    }

}
