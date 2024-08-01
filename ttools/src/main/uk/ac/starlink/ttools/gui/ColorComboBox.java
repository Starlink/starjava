package uk.ac.starlink.ttools.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;
import javax.swing.JComboBox;
import uk.ac.starlink.ttools.plot.Styles;
import uk.ac.starlink.util.gui.RenderingComboBox;

/**
 * Combo box for selecting colours.  Comes with its own renderer.
 *
 * @author   Mark Taylor
 * @since    12 Jan 2006
 */
public class ColorComboBox extends RenderingComboBox<Color> {

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
    @SuppressWarnings("this-escape")
    public ColorComboBox( Color[] colors ) {
        super( colors.clone() );
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

    protected String getRendererText( Color obj ) {
        return null;
    }

    protected Icon getRendererIcon( Color obj ) {
        final Color color = ColorComboBox.this.isEnabled() ? obj
                                                           : Color.LIGHT_GRAY;
        return new Icon() {
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
        };
    }
}
