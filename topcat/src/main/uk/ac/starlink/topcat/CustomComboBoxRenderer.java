package uk.ac.starlink.topcat;

import java.awt.Component;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 * Utility class which does the job of rendering items into a JComboBox
 * when you just want to provide a different stringification of them
 * than the one provided by the toString method.
 * <p>
 * You would use this class by providing an implementation of the 
 * {@link #mapValue} method and class by calling 
 * {@link javax.swing.JComboBox#setRenderer} on an instance of the resulting
 * subclass.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class CustomComboBoxRenderer implements ListCellRenderer {

    /* Should I be getting this from the PLAF somehow? */
    private static ListCellRenderer baseRenderer = new BasicComboBoxRenderer();

    public Component getListCellRendererComponent( JList list, Object value,
                                                   int index,
                                                   boolean isSelected,
                                                   boolean hasFocus ) {
        return baseRenderer
              .getListCellRendererComponent( list, mapValue( value ), index,
                                             isSelected, hasFocus );
    }

    /**
     * Turns an object which might be found in the ComboBox itself into
     * an object that can be rendered by a standard combobox renderer.
     * Typically the return value of this method would be a String 
     * more suitable than the result of <tt>value</tt>'s <tt>toString</tt>
     * method.
     *
     * @param  value  value to map
     * @return  value to map it into (probably a string)
     */
    protected abstract Object mapValue( Object value );
}
