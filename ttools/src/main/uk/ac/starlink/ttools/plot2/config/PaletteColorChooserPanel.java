package uk.ac.starlink.ttools.plot2.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.plaf.basic.BasicArrowButton;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Custom chooser panel implementation that lets you choose from
 * a number of pre-configured palettes.
 *
 * @author   Mark Taylor
 * @since    23 Feb 2017
 */
public class PaletteColorChooserPanel extends AbstractColorChooserPanel {

    private final Map<String,Color[]> paletteMap_;
    private final JColorChooser chooser_;
    private PalettePanel ppanel_;
    private NavigateAction[] navActs_;

    /**
     * Constructor.
     *
     * @param  paletteMap   map of palette name-&gt;colour lists
     * @param  chooser   chooser on behalf of which his panel will work
     */
    public PaletteColorChooserPanel( Map<String,Color[]> paletteMap,
                                     JColorChooser chooser ) {
        paletteMap_ = paletteMap;
        chooser_ = chooser;
    }

    protected void buildChooser() {
        setLayout( new BorderLayout() );

        /* Add the main palette display/interaction panel. */
        ppanel_ = new PalettePanel( paletteMap_ );
        ppanel_.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) );
        add( ppanel_, BorderLayout.CENTER );

        /* Add a row of N/E/W/S navigation buttons below the palettes. */
        JComponent bbox = Box.createHorizontalBox();
        add( bbox, BorderLayout.SOUTH );
        bbox.add( Box.createHorizontalGlue() );
        navActs_ = new NavigateAction[] {
            new NavigateAction( SwingConstants.SOUTH ),
            new NavigateAction( SwingConstants.WEST ),
            new NavigateAction( SwingConstants.EAST ),
            new NavigateAction( SwingConstants.NORTH ),
        };

        /* Arrange for keyboard arrow keys to do navigation too. */
        InputMap inputMap = getInputMap( WHEN_IN_FOCUSED_WINDOW );
        for ( NavigateAction navAct : navActs_ ) {
            bbox.add( Box.createHorizontalStrut( 5 ) );
            bbox.add( new ShrinkWrapper( navAct.createButton() ) );
            String navKey = "Palette." + navAct.txt_;
            inputMap.put( navAct.keyStroke_, navKey );
            getActionMap().put( navKey, navAct );
        }

        /* Initialise selection. */
        ppanel_.setSelection( -1, -1 );
    }

    public String getDisplayName() {
        return "Palettes";
    }

    public int getMnemonic() {
        return KeyEvent.VK_P;
    }

    /**
     * Returns null.
     * At least up to java 8, these seem to be ignored anyway.
     */
    public Icon getSmallDisplayIcon() {
        return null;
    }

    /**
     * Returns null.
     * At least up to java 8, these seem to be ignored anyway.
     */
    public Icon getLargeDisplayIcon() {
        return null;
    }

    public void updateChooser() {
    }

    /**
     * Action that can move round the palette grid.
     */
    private class NavigateAction extends AbstractAction {
        private final int swingDirection_;
        private final String txt_;
        private final int dx_;
        private final int dy_;
        private final KeyStroke keyStroke_;

        /**
         * Constructor.
         *
         * @param   swingDirection
         *          one of SwingConstants.NORTH/SOUTH/EAST/WEST
         */
        NavigateAction( int swingDirection ) {
            swingDirection_ = swingDirection;
            final int keyEvent;
            switch ( swingDirection_ ) {
                case SwingConstants.NORTH:
                    txt_ = "Up";
                    dx_ = 0;
                    dy_ = -1;
                    keyEvent = KeyEvent.VK_UP;
                    break;
                case SwingConstants.SOUTH:
                    txt_ = "Down";
                    dx_ = 0;
                    dy_ = +1;
                    keyEvent = KeyEvent.VK_DOWN;
                    break;
                case SwingConstants.WEST:
                    txt_ = "Left";
                    dx_ = -1;
                    dy_ = 0;
                    keyEvent = KeyEvent.VK_LEFT;
                    break;
                case SwingConstants.EAST:
                    txt_ = "Right";
                    dx_ = +1;
                    dy_ = 0;
                    keyEvent = KeyEvent.VK_RIGHT;
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            keyStroke_ = KeyStroke.getKeyStroke( keyEvent, 0 );
            putValue( NAME, txt_ );
            putValue( SHORT_DESCRIPTION, "Move " + txt_ + " in palette grid" );
        }

        public void actionPerformed( ActionEvent evt ) {
            ppanel_.navigate( dx_, dy_ );
            ppanel_.repaint();
        }

        /**
         * Returns a button that will invoke this action.
         */
        public JButton createButton() {
            JButton button = new BasicArrowButton( swingDirection_ );
            button.setAction( this );
            return button;
        }
    }

    /**
     * Panel that displays the grid of palette colours for selection.
     */
    private class PalettePanel extends JComponent {

        private final int np_;
        private final String[] labels_;
        private final Color[][] colors_;
        private final int ncmax_;
        private final int xPatch_ = 12;
        private final int yPatch_ = 12;
        private final int xGap_ = 4;
        private final int yGap_ = 4;
        private final Font font_;
        private final int yPatchOff_;
        private final int txtWidth_;
        private final int lineHeight_;
        private final LineMetrics lineMetrics_;
        private final Color txtColor_;
        private final Color selectColor_;
        private final Color overColor_;
        private int[] xySel_;
        private int[] xyOver_;

        /**
         * Constructor.
         *
         * @param   paletteName-&gt;colourList map
         */
        PalettePanel( Map<String,Color[]> paletteMap ) {

            /* Set up constants etc for component painting. */
            np_ = paletteMap.size();
            labels_ = new String[ np_ ];
            colors_ = new Color[ np_ ][];
            int ip = 0;
            int ncmax = 0;
            for ( Map.Entry<String,Color[]> entry : paletteMap.entrySet() ) {
                labels_[ ip ] = entry.getKey() + ": ";
                colors_[ ip ] = entry.getValue();
                ncmax = Math.max( ncmax, colors_[ ip ].length );
                ip++;
            }
            ncmax_ = ncmax;
            font_ = UIManager.getFont( "Label.font" );
            xyOver_ = new int[] { -1, -1 };
            FontRenderContext frc =
                new FontRenderContext( new AffineTransform(), false, false );
            int txtWidth = 0;
            int txtHeight = 0;
            for ( String label : labels_ ) {
                Rectangle r = font_.getStringBounds( label, frc ).getBounds();
                txtWidth = Math.max( txtWidth, r.width );
                txtHeight = Math.max( txtHeight, r.height );
            }
            txtWidth_ = txtWidth;
            lineHeight_ = txtHeight;
            lineMetrics_ = font_.getLineMetrics( labels_[ 0 ], frc );
            yPatchOff_ = ( lineHeight_ - yPatch_ ) / 2;
            txtColor_ = UIManager.getColor( "ColorChooser.foreground" );
            overColor_ = UIManager.getColor( "ComboBox.selectionBackground" );
            selectColor_ = txtColor_;
            setSelection( -1, -1 );

            /* Mouse click will set selection. */
            final JComponent paletteComp = this;
            addMouseListener( new MouseAdapter() {
                @Override
                public void mouseClicked( MouseEvent evt ) {
                    int[] sel = getGridPosition( evt.getPoint() );
                    int ix = sel[ 0 ];
                    int iy = sel[ 1 ];
                    if ( ix >= 0 && iy >= 0 ) {
                        setSelection( ix, iy );
                    }
                    paletteComp.repaint();
                }
            } );

            /* Mouse rollover will highlight colour. */
            addMouseMotionListener( new MouseAdapter() {
                @Override
                public void mouseMoved( MouseEvent evt ) {
                    xyOver_ = getGridPosition( evt.getPoint() );
                    paletteComp.repaint();
                }
            } );
        }

        @Override
        protected void paintComponent( Graphics g ) {
            super.paintComponent( g );
            Color color0 = g.getColor();
            Font font0 = g.getFont();
            g.setFont( font_ );
            Insets insets = getInsets();
            int gy = insets.top;
            int yTxtOff = (int) lineMetrics_.getHeight();
            for ( int ip = 0; ip < np_; ip++ ) {
                int gx = insets.left;
                g.setColor( txtColor_ );
                g.drawString( labels_[ ip ], gx, gy + yTxtOff );
                gx += txtWidth_;
                Color[] colors = colors_[ ip ];
                for ( int ic = 0; ic < colors.length; ic++ ) {
                    gx += xGap_;
                    int x0 = gx;
                    int y0 = gy + yPatchOff_;
                    int w0 = xPatch_;
                    int h0 = yPatch_;
                    if ( ic == xySel_[ 0 ] && ip == xySel_[ 1 ] ) {
                        g.setColor( selectColor_ );
                        g.fillRect( x0 - 4, y0 - 4, w0 + 8, h0 + 8 );
                    }
                    else if ( ic == xyOver_[ 0 ] && ip == xyOver_[ 1 ] ) {
                        g.setColor( overColor_ );
                        g.fillRect( x0 - 3, y0 - 3, w0 + 6, h0 + 6 );
                    }
                    g.setColor( Color.GRAY );
                    g.fillRect( x0 - 1, y0 - 1, w0 + 2, h0 + 2 );
                    g.setColor( colors[ ic ] );
                    g.fillRect( x0, y0, w0, h0 );
                    gx += xPatch_ + xGap_;
                }
                gy += lineHeight_ + yGap_;
            }
            g.setColor( color0 );
            g.setFont( font0 );
        }

        @Override
        public Dimension getPreferredSize() {
            Insets insets = getInsets();
            return new Dimension( txtWidth_
                                + ncmax_ * ( xPatch_ + 2 * xGap_ )
                                + insets.left + insets.right,
                                  np_ * lineHeight_ + ( np_ - 1 ) * yGap_
                                + insets.top + insets.bottom );
        }

        /**
         * Returns the coordinates of the colour patch corresponding
         * to a graphics position.
         *
         * @param  p   position on this component
         */
        private int[] getGridPosition( Point p ) {
            Insets insets = getInsets();
            int px = p.x - insets.left - txtWidth_ - xGap_;
            int ix = px % ( xPatch_ + 2 * xGap_ ) <= xPatch_
                   ? px / ( xPatch_ + 2 * xGap_ )
                   : -1;
            int py = p.y - insets.top - yPatchOff_;
            int iy = py % ( lineHeight_  + yGap_ ) <= yPatch_
                   ? py / ( lineHeight_ + yGap_ )
                   : -1;
            return iy >= 0 && iy < np_ &&
                   ix >= 0 && ix < colors_[ iy ].length
                 ? new int[] { ix, iy }
                 : new int[] { -1, -1 };
        }

        /**
         * Make a relative X/Y change in the location of the currently
         * selected colour patch in the grid.
         * Wrap arounds will be applied.
         * One of <code>dx</code>/<code>dy</code> should be +/-1,
         * the other should be zero.
         *
         * @param  dx  grid X offset
         * @param  dy  grid Y offset
         */
        public void navigate( int dx, int dy ) {
            int ix = xySel_[ 0 ];
            int iy = xySel_[ 1 ];

            /* This code is not bulletproof, but it should work for the
             * dx/dy values we are expecting. */
            if ( ix >= 0 && iy >= 0 ) {
                int nc = colors_[ iy ].length;
                ix = ( ix + dx + nc ) % nc;
                iy = ( iy + dy + np_ ) % np_;
                nc = colors_[ iy ].length;
                ix = Math.min( ix, nc - 1 );
            }
            setSelection( ix, iy );
        }

        /**
         * Sets the currently selected colour patch on the grid.
         *
         * @param  ix  grid X coordinate
         * @param  iy  grid Y coordinate
         */
        private void setSelection( int ix, int iy ) {
            xySel_ = new int[] { ix, iy };
            boolean hasSel = ix >= 0 && iy >= 0;
            if ( hasSel ) {
                chooser_.getSelectionModel()
                        .setSelectedColor( colors_[ iy ][ ix ] );
            }
            if ( navActs_ != null ) {
                for ( Action act : navActs_ ) {
                    act.setEnabled( hasSel );
                }
            }
            repaint();
        }
    }
}
