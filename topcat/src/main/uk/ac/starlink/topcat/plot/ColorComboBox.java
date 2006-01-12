package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 * Combo box for selecting colours.  Comes with its own renderer.
 *
 * @author   Mark Taylor
 * @since    12 Jan 2006
 */
public class ColorComboBox extends JComboBox implements ListCellRenderer {

    private final ListCellRenderer renderer_;

    private static final int ICON_WIDTH = 24;
    private static final int ICON_HEIGHT = 12;

    /**
     * Constructs a colour selector with a default set of colours.
     */
    public ColorComboBox() {
        this( Styles.COLORS );
    }

    /**
     * Constructs a colour selector with a given set of colours.
     *
     * @param   colors  colour array
     */
    public ColorComboBox( Color[] colors ) {
        super( (Color[]) colors.clone() );
        renderer_ = new BasicComboBoxRenderer();
        setRenderer( this );
        setSelectedIndex( 0 );
    }

    /**
     * Sets the currently selected colour.
     *
     * @param  color  colour to select
     */
    public void setSelectedColor( Color color ) {
        setSelectedItem( color );
    }

    /**
     * Returns the currently selected colour.
     *
     * @return  selected colour
     */
    public Color getSelectedColor() {
        return (Color) getSelectedItem();
    }

    public Component getListCellRendererComponent( JList list, Object value,
                                                   int index,
                                                   boolean isSelected,
                                                   boolean hasFocus ) {
        Component c =
            renderer_.getListCellRendererComponent( list, value, index,
                                                    isSelected, hasFocus );
        if ( c instanceof JLabel ) {
            final Color color =
                (Color) getItemAt( index >= 0 ? index : getSelectedIndex() );
            ((JLabel) c).setText( null );
            ((JLabel) c).setIcon( new Icon() {
                public int getIconHeight() {
                    return ICON_HEIGHT;
                }
                public int getIconWidth() {
                    return ICON_WIDTH;
                }
                public void paintIcon( Component c, Graphics g, int x, int y ) {
                    Color oldColor = g.getColor();
                    g.setColor( color );
                    g.fillRect( x, y, ICON_WIDTH, ICON_HEIGHT );
                    g.setColor( oldColor );
                }
            } );
        }
        return c;
    }
}
