package uk.ac.starlink.ttools.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import uk.ac.starlink.util.gui.RenderingComboBox;

/**
 * JComboBox for selecting line thickness.  Comes with its own renderer.
 *
 * @author   Mark Taylor
 * @since    12 Jan 2006
 */
public class ThicknessComboBox extends RenderingComboBox<Integer> {

    private static final int LINE_LENGTH = 48;

    /**
     * Constructs a new thickness selector up to a given maximum width.
     *
     * @param   maxThick   maximum line width
     */
    public ThicknessComboBox( int maxThick ) {
        Integer[] numbers = new Integer[ maxThick ];
        for ( int i = 0; i < maxThick; i++ ) {
            numbers[ i ] = Integer.valueOf( i + 1 );
        }
        setModel( new DefaultComboBoxModel<Integer>( numbers ) );
    }

    /**
     * Returns the currently selected thickness.
     *
     * @return    thickness (&gt;=1)
     */
    public int getSelectedThickness() {
        return getSelectedIndex() + 1;
    }

    /**
     * Sets the currently selected thickness
     *
     * @param   thick  selected thickness
     */
    public void setSelectedThickness( int thick ) {
        setSelectedIndex( Math.max( thick - 1, 0 ) );
    }

    public Icon getRendererIcon( Integer thickObj ) {
        final int thick = thickObj.intValue();
        return new Icon() {
            public int getIconHeight() {
                return thick;
            }
            public int getIconWidth() {
                return LINE_LENGTH + 4;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                Graphics2D g2 = (Graphics2D) g;
                Stroke oldStroke = g2.getStroke();
                Color oldColor = g2.getColor();
                g2.setColor( Color.BLACK );
                g2.setStroke( new BasicStroke( thick, BasicStroke.CAP_BUTT,
                                               BasicStroke.JOIN_MITER,
                                               10f, null, 0f ) );
                int ypos = y + thick / 2;
                g2.drawLine( x + 4, ypos, x + 4 + LINE_LENGTH, ypos );
                g2.setStroke( oldStroke );
                g2.setColor( oldColor );
            }
        };
    }
}
