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
public class CustomComboBoxRenderer implements ListCellRenderer {

    /* Should I be getting this from the PLAF somehow? */
    private static ListCellRenderer baseRenderer_ = new BasicComboBoxRenderer();
    private Object nullRep_;

    public Component getListCellRendererComponent( JList list, Object value,
                                                   int index,
                                                   boolean isSelected,
                                                   boolean hasFocus ) {
        Object rep;
        Object nullRep = getNullRepresentation();
        if ( value == null && nullRep != null ) {
            rep = nullRep;
        }
        else {
            rep = mapValue( value );
        }
        return baseRenderer_
              .getListCellRendererComponent( list, rep, index,
                                             isSelected, hasFocus );
    }

    /**
     * Sets the representation for the <tt>null</tt> value.
     * If set to a non-null value, this will be used to render a 
     * null; otherwise, {@link #mapValue} will be called as usual
     * (which may itself do something with the null).
     *
     * @param  nullRep  null representation
     */
    public void setNullRepresentation( Object nullRep ) {
        nullRep_ = nullRep;
    }

    /**
     * Returns the representation for the <tt>null</tt> value.
     *
     * @return  null representation
     */
    public Object getNullRepresentation() {
        return nullRep_;
    }

    /**
     * Turns an object which might be found in the ComboBox itself into
     * an object that can be rendered by a standard combobox renderer.
     * Typically the return value of this method would be a String 
     * more suitable than the result of <tt>value</tt>'s <tt>toString</tt>
     * method.
     *
     * <p>The default implementation just returns the value itself
     *
     * @param  value  value to map
     * @return  value to map it into (probably a string)
     */
    protected Object mapValue( Object value ) {
        return value;
    }
}
