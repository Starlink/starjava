package uk.ac.starlink.topcat.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 * JComboBox for selecting line thickness.  Comes with its own renderer.
 *
 * @author   Mark Taylor
 * @since    12 Jan 2006
 */
public class ThicknessComboBox extends JComboBox implements ListCellRenderer {

    private final ListCellRenderer renderer_;
    private final int maxThick_;

    private static final int LINE_LENGTH = 48;

    /**
     * Constructs a new thickness selector up to a given maximum width.
     *
     * @param   maxThick   maximum line width
     */
    public ThicknessComboBox( int maxThick ) {
        maxThick_ = maxThick;
        Integer[] numbers = new Integer[ maxThick_ ];
        for ( int i = 0; i < maxThick_; i++ ) {
            numbers[ i ] = new Integer( i + 1 );
        }
        setModel( new DefaultComboBoxModel( numbers ) );
        renderer_ = new BasicComboBoxRenderer();
        setRenderer( this );
    }

    /**
     * Returns the currently selected thickness.
     *
     * @return    thickness (>=1)
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
        setSelectedIndex( Math.max( thick - 1, 1 ) );
    }

    public Component getListCellRendererComponent( JList list, Object value,
                                                   int index,
                                                   boolean isSelected,
                                                   boolean hasFocus ) {
        Component c =
            renderer_.getListCellRendererComponent( list, value, index,
                                                    isSelected, hasFocus );
        final int thick = ( index >= 0 ? index : getSelectedIndex() ) + 1;
        if ( c instanceof JLabel ) {
            ((JLabel) c).setText( Integer.toString( thick ) );
            ((JLabel) c).setIcon( new Icon() {
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
                                                   BasicStroke.JOIN_MITER, 10f,
                                                   null, 0f ) );
                    int ypos = y + thick / 2;
                    g2.drawLine( x + 4, ypos, x + 4 + LINE_LENGTH, ypos );
                    g2.setStroke( oldStroke );
                    g2.setColor( oldColor );
                }
            } );
        }
        return c;
    }
}
