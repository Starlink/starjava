package uk.ac.starlink.util.gui;

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
public class CustomComboBoxRenderer<T> implements ListCellRenderer {

    private final Class<T> clazz_;
    private final String nullTxt_;

    /* Should I be getting this from the PLAF somehow? */
    private static ListCellRenderer baseRenderer_ = new BasicComboBoxRenderer();

    /**
     * Constructs a renderer for which nulls are represented as blank.
     *
     * @param  clazz  type of object to be rendered
     */
    public CustomComboBoxRenderer( Class<T> clazz ) {
        this( clazz, null );
    }

    /**
     * Constructs a renderer with a custom null representation.
     *
     * @param  clazz  type of object to be rendered
     * @param  nullTxt  text to be displayed for null values
     */
    public CustomComboBoxRenderer( Class<T> clazz, String nullTxt ) {
        clazz_ = clazz;
        nullTxt_ = nullTxt;
    }

    public Component getListCellRendererComponent( JList list, Object value,
                                                   int index,
                                                   boolean isSelected,
                                                   boolean hasFocus ) {
        final String txt;
        if ( value == null ) {
            txt = nullTxt_;
        }
        else if ( clazz_.isInstance( value ) ) {
            txt = mapValue( clazz_.cast( value ) );
        }
        else {
            txt = "??";
        }
        return baseRenderer_
              .getListCellRendererComponent( list, txt, index,
                                             isSelected, hasFocus );
    }

    /**
     * Turns a non-null object which might be found in the ComboBox itself
     * into a string to be displayed by a standard combobox renderer.
     * The default implementation just uses the toString method.
     *
     * <p>This method will only be invoked if <code>value</code> is not null.
     * In case of null, the <code>nullTxt</code> value supplied at construction
     * tim will be used instead.
     *
     * @param  value  non-null value to map
     * @return  display string
     */
    protected String mapValue( T value ) {
        return value.toString();
    }
}
