package uk.ac.starlink.topcat.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Styles;
import uk.ac.starlink.util.gui.RenderingComboBox;

/**
 * Combo box for selecting dash patterns.
 *
 * @author   Mark Taylor
 * @since    12 Jan 2006
 * @see   java.awt.BasicStroke
 * @see   uk.ac.starlink.ttools.plot.DefaultStyle
 */
public class DashComboBox extends RenderingComboBox {

    private static final int LINE_LENGTH = 48;
    private static final int LINE_THICKNESS = 2;
    private static final float[] SOLID = new float[] { 1.0f, 0.0f, };

    /**
     * Constructs a dash selector with a default range of dash patterns.
     */
    public DashComboBox() {
        this( Styles.DASHES );
    }

    /**
     * Constructs a dash selector with a given set of dash patterns.
     *
     * @param  dashes  dash patterns for selection; 
     *                 null is OK for a solid line.
     */
    public DashComboBox( float[][] dashes ) {
        dashes = (float[][]) dashes.clone();
        for ( int i = 0; i < dashes.length; i++ ) {
            if ( dashes[ i ] == null ) {
                dashes[ i ] = SOLID;
            }
        }
        setModel( new DefaultComboBoxModel( dashes ) );
    }

    /**
     * Returns the currently selected dash pattern.
     *
     * @return   selected dash array
     */
    public float[] getSelectedDash() {
        return (float[]) getSelectedItem();
    }

    /**
     * Sets the selected dash array.
     * You should use this and not set the dash directly using methods
     * on JComboBox or its model.
     *
     * @param  dash  new selected dash.  Null is ok for solid
     */
    public void setSelectedDash( float[] dash ) {
        if ( dash == null ) {
            dash = SOLID;
        }
        int nitem = getItemCount();
        for ( int i = 0; i < nitem; i++ ) {
            if ( Arrays.equals( dash, (float[]) getItemAt( i ) ) ) {
                setSelectedIndex( i );
                return;
            }
        }
        super.setSelectedItem( dash );
    }

    protected String getRendererText( Object d ) {
        return null;
    }

    protected Icon getRendererIcon( Object d ) {
        final float[] dash = (float[]) d;
        return new Icon() {
            public int getIconHeight() {
                return LINE_THICKNESS;
            }
            public int getIconWidth() {
                return LINE_LENGTH + 4;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                Graphics2D g2 = (Graphics2D) g;
                Stroke oldStroke = g2.getStroke();
                Color oldColor = g2.getColor();
                g2.setColor( Color.BLACK );
                g2.setStroke( new BasicStroke( LINE_THICKNESS,
                                               BasicStroke.CAP_BUTT,
                                               BasicStroke.JOIN_MITER,
                                               10f, dash, 0f ) );
                int ypos = y + LINE_THICKNESS / 2;
                g2.drawLine( x + 4, ypos, y + x + LINE_LENGTH, ypos );
                g2.setStroke( oldStroke );
                g2.setColor( oldColor );
            }
        };
    }
}
