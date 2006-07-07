package uk.ac.starlink.util.gui;

import java.awt.Component;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 * JComboBox with custom renderer.
 * This convenience class allows you to do your own rendering without having
 * to provide an actual implementation of {@link javax.swing.ListCellRenderer},
 * which is mildly fiddly.
 * Just override {@link #getRendererLabel} and/or {@link #getRendererIcon}.
 *
 * @author   Mark Taylor
 * @since    12 Jan 2006
 */
public class RenderingComboBox extends JComboBox implements ListCellRenderer {

    private final ListCellRenderer renderer_;

    /**
     * Constructs a new combo box with a given model.
     *
     * @param  model   data model
     */
    protected RenderingComboBox( ComboBoxModel model ) {
        super( model );
        renderer_ = new BasicComboBoxRenderer();
        setRenderer( this );
    }

    /**
     * Constructs a new combo box containing some supplied items.
     *
     * @param  items  initial selection of items
     */
    protected RenderingComboBox( Object[] items ) {
        this( new DefaultComboBoxModel( items ) );
    }

    /**
     * Constructs a new combo box with a default data model.
     */
    protected RenderingComboBox() {
        this( new DefaultComboBoxModel() );
    }

    /**
     * Returns the text label to use to represent a given item.
     * The default implementation just uses <code>toString</code>.
     *
     * @param  item  item
     * @return  textual label for item
     */
    protected String getRendererText( Object item ) {
        return item == null ? null : item.toString();
    }

    /**
     * Returns an icon to use to represent a given item.
     * The default implementation returns null.
     *
     * @param   item   item
     * @param   graphic label for item
     */
    protected Icon getRendererIcon( Object item ) {
        return null;
    }

    /**
     * Implements ListCellRenderer.
     */
    public Component getListCellRendererComponent( JList list, Object value,
                                                   int index,
                                                   boolean isSelected,
                                                   boolean hasFocus ) {
        Component c =
            renderer_.getListCellRendererComponent( list, value, index,
                                                    isSelected, hasFocus );
        if ( c instanceof JLabel ) {
            ((JLabel) c).setText( getRendererText( value ) );
            ((JLabel) c).setIcon( getRendererIcon( value ) );
        }
        return c;
    }
}
