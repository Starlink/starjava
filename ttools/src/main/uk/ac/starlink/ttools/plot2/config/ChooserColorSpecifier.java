package uk.ac.starlink.ttools.plot2.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicArrowButton;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * SpecifierPanel subclass that uses a JColorChooser to specify a colour.
 * The color chooser is somewhat customised, including by installing a
 * custom Palettes chooser panel, and the whole thing is displayed
 * in a popup menu rather than a dialogue or frame, since that more
 * closely approximates what you get from other SpecifierPanel components
 * (such as a JComboBox).
 *
 * @author   Mark Taylor
 * @since    27 Jan 2017
 */
public class ChooserColorSpecifier extends SpecifierPanel<Color> {

    private final JColorChooser chooser_;
    private final Action chooserAct_;
    private final ChooseAction okAct_;
    private final ChooseAction resetAct_;
    private Color color_;
    private static final Map<String,Color[]> PALETTE_MAP = createPaletteMap();

    /**
     * Constructs a specifier based on a given default colour.
     *
     * @param   dfltColor  initial colour
     */
    public ChooserColorSpecifier( Color dfltColor ) {
        this( new JColorChooser( dfltColor ) );
        chooser_.addChooserPanel( new PaletteColorChooserPanel( PALETTE_MAP,
                                                                chooser_ ) );

        /* Get rid of the default JColorChooser preview panel.
         * The details of it are not very useful here. */
        chooser_.setPreviewPanel( new JPanel() );
    }
    
    /**
     * Constructs a specifier based on a given JColorChooser.
     *
     * @param  chooser  chooser component
     */
    public ChooserColorSpecifier( JColorChooser chooser ) {
        super( false );
        chooser_ = chooser;
        okAct_ = new ChooseAction( "OK" );
        resetAct_ = new ChooseAction( "Reset" );

        /* Set up a popup menu.  We are abusing the JPopupMenu here
         * by putting a whole JColorChooser component in as one of the
         * menu items.  The benefit of this is that it behaves in
         * a similar way to JComboBox, that is the component pops down
         * again if the mouse moves away from it.  I think this is
         * more suitable behaviour for this kind of selector than
         * posting a separate JDialog window, which is the obvious
         * alternative.  We also have two 'normal' menu items,
         * one to accept the current selection (this does nothing
         * except dismiss the menu, since selection is done continuously
         * as the chooser state changes), and one to reset the colour
         * to its value before the menu was popped up. */
        final JPopupMenu popup = new JPopupMenu( "JColorChooser" );
        popup.insert( chooser_, 0 );
        popup.add( okAct_.createMenuItem() );
        popup.add( resetAct_.createMenuItem() );

        /* Intialise the menu items when the menu is popped up. */
        popup.addPopupMenuListener( new PopupMenuListener() {
            public void popupMenuCanceled( PopupMenuEvent evt ) {
            }
            public void popupMenuWillBecomeInvisible( PopupMenuEvent evt ) {
            }
            public void popupMenuWillBecomeVisible( PopupMenuEvent evt ) {
                resetAct_.setActionColor( color_ );
                okAct_.setActionColor( chooser_.getColor() );
            }
        } );

        /* Fix it so that the state of the chooser is reflected
         * in the state of this specifier. */
        chooser_.getSelectionModel()
                .addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                Color color = chooser_.getColor();
                okAct_.setActionColor( color );
                setSpecifiedValue( color );
            }
        } );

        /* Set up the button that pops up the dialogue with the chooser. */
        chooserAct_ = new AbstractAction( "Color", ResourceIcon.COLOR_WHEEL ) {
            public void actionPerformed( ActionEvent evt ) {
                Object src = evt.getSource();
                Component c = src instanceof Component ? (Component) src : null;
                popup.show( c, 0, 0 );
            }
        };
        chooserAct_.putValue( Action.SHORT_DESCRIPTION, "Free colour chooser" );
    }

    protected JComponent createComponent() {
        JButton button = new JButton( chooserAct_ );
        button.setHideActionText( true );
        button.setMargin( new Insets( 0, 0, 0, 0 ) );
        return button;
    }

    public Color getSpecifiedValue() {
        return color_;
    }

    public void setSpecifiedValue( Color color ) {
        color_ = color;
        fireAction();
    }

    public void submitReport( ReportMap report ) {
    }

    /**
     * Returns this specifier's JColorChooser.
     *
     * @return   color chooser
     */
    public JColorChooser getColorChooser() {
        return chooser_;
    }

    /**
     * Creates some standard named colour lists.
     * The sequences of some of these have been somewhat adjusted
     * so that the colours align between different palettes.
     *
     * @return   paletteName-&gt;colourList map
     */
    public static Map<String,Color[]> createPaletteMap() {
        Map<String,Color[]> map = new LinkedHashMap<String,Color[]>();

        /* This is the one that's always been available in topcat. */
        map.put( "Classic", ColorConfigKey.getPlottingColors() );

        /* Copied from MATLAB
         * (http://uk.mathworks.com/help/matlab/graphics_transition/
         *         why-are-plot-lines-different-colors.html).
         * I'm not sure what the licencing situation is here.
         * Probably it's OK to copy a sequence of colours from a web page.
         * But I admit I haven't asked. */
        map.put( "Cycle1",
                 toColors( new int[] { 0xff0000, 0x0000ff, 0x008000, 0xc0c000, 
                                       0xc000c0, 0x00c0c0, 0x404040, } ) );
        map.put( "Cycle2",
                 new Color[] { new Color( 0.635f, 0.078f, 0.184f ),
                               new Color( 0.000f, 0.447f, 0.741f ),
                               new Color( 0.466f, 0.674f, 0.188f ),
                               new Color( 0.929f, 0.694f, 0.125f ),
                               new Color( 0.494f, 0.184f, 0.556f ),
                               new Color( 0.301f, 0.745f, 0.933f ),
                               new Color( 0.850f, 0.325f, 0.098f ), } );

        /* From Paul Tol's https://personal.sron.nl/~pault/, not in document. */
        map.put( "SRON-Bright",
                 toColors( new int[] { 0xee3333, 0x3366aa, 0x66aa55, 0xcccc55,
                                       0x992288, 0x11aa99, 0xee7722, } ) );

        /* From Fig 2 of Paul Tol's SRON/EPS/TN/09-002,
         * https://personal.sron.nl/~pault/colourschemes.pdf. */
        map.put( "SRON-Light",
                 toColors( new int[] { 0x77aadd, 0x77cccc, 0x88ccaa, 0xdddd77,
                                       0xddaa77, 0xdd7788, 0xcc99bb, } ) );
        map.put( "SRON-Mid",
                 toColors( new int[] { 0x4477aa, 0x44aaaa, 0x44aa77, 0xaaaa44,
                                       0xaa7744, 0xaa4455, 0xaa4488, } ) );
        map.put( "SRON-Dark",
                 toColors( new int[] { 0x114477, 0x117777, 0x117744, 0x777711,
                                       0x774411, 0x771122, 0x771155, } ) );
        return Collections.unmodifiableMap( map );
    }

    /**
     * Converts an array of RGB values to a corresponding array
     * of Color objects.
     *
     * @param  rgbs   RGB array
     * @return   colour array
     */
    private static Color[] toColors( int[] rgbs ) {
        int nc = rgbs.length;
        Color[] colors = new Color[ nc ];
        for ( int ic = 0; ic < nc; ic++ ) {
            colors[ ic ] = new Color( rgbs[ ic ], false );
        }
        return colors;
    }

    /**
     * Action that can select a colour and dismiss the popup.
     */
    private class ChooseAction extends AbstractAction {
        private static final String COLOR_PROP = "colorChoice";

        /**
         * Constructor.
         *
         * @param  name  action name
         */
        ChooseAction( String name ) {
            super( name );
            putValue( SMALL_ICON, new PreviewIcon( 12 ) {
                protected Color getPreviewColor() {
                    return ChooseAction.this.getActionColor();
                }
            } );
        }

        public void actionPerformed( ActionEvent evt ) {
            setSpecifiedValue( getActionColor() );
        }

        /**
         * Sets the colour which this action will set when invoked.
         *
         * @param  color   colour
         */
        void setActionColor( Color color ) {
            putValue( COLOR_PROP, color );
        }

        /**
         * Returns the colour which this action will set when invoked.
         *
         * @return colour
         */
        Color getActionColor() {
            Object color = getValue( COLOR_PROP );
            return color instanceof Color ? (Color) color : null;
        }

        /**
         * Constructs a menu item based on this action.
         * It will repaint itself appropriately when the colour changes.
         */
        JMenuItem createMenuItem() {
            final JMenuItem mItem = new JMenuItem( this );
            addPropertyChangeListener( new PropertyChangeListener() {
                public void propertyChange( PropertyChangeEvent evt ) {
                    if ( COLOR_PROP.equals( evt.getPropertyName() ) ) {
                        mItem.repaint();
                    }
                }
            } );
            return mItem;
        }
    }

    /**
     * Icon used for displaying the preview for a selected colour
     * in a JMenuItem.
     */
    private static abstract class PreviewIcon implements Icon {

        private final int iconHeight_;
        private final int iconWidth_;
        private final Color disabledColor_;
        private final Font hexFont_;
        private final int gap_;
        private final int blockWidth_;
        private final int shadeWidth_;
        private final Shader[] shaders_;
        private final int markWidth_;
        private final int[] sizes_;
        private final int txtWidth_;
        private final int padTxt_;
        private final int yTxt_;

        /**
         * Constructor.
         *
         * @param  height  icon height in pixels
         */
        PreviewIcon( int height ) {
            iconHeight_ = height;
            blockWidth_ = 24;
            gap_ = 10;
            shadeWidth_ = 20;
            shaders_ = new Shader[] {
                Shaders.invert( Shaders.FADE_BLACK ),
                Shaders.FADE_WHITE,
            };
            markWidth_ = 10;
            sizes_ = new int[] { 0, 1, 2, 3, 2, 1, 0, };
            Color dcol = UIManager.getColor( "Label.disabledForeground" );
            disabledColor_ = dcol == null ? Color.GRAY : dcol;
            hexFont_ = new Font( Font.MONOSPACED, Font.PLAIN, 12 );
            FontRenderContext frc =
                new FontRenderContext( new AffineTransform(), false, false );
            String hex0 = getHexString( Color.BLACK );
            txtWidth_ = hexFont_.getStringBounds( hex0, frc ).getBounds().width;
            LineMetrics lm = hexFont_.getLineMetrics( hex0, frc );
            yTxt_ = (int) ( lm.getHeight() + lm.getLeading() );
            padTxt_ = 4;
            iconWidth_ = blockWidth_ + gap_
                       + shadeWidth_ * shaders_.length + gap_
                       + markWidth_ * sizes_.length + gap_
                       + txtWidth_ + 2 * padTxt_ + gap_;
        }

        /**
         * Returns the colour selection to be previewed.
         *
         * @return   selection colour
         */
        protected abstract Color getPreviewColor();

        public int getIconWidth() {
            return iconWidth_;
        }

        public int getIconHeight() {
            return iconHeight_;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {

            /* Save graphics context. */
            Color color0 = g.getColor();
            Font font0 = g.getFont();

            /* Determine colour to preview. */
            Color color = c.isEnabled() ? getPreviewColor() : disabledColor_;

            /* Solid rectangle of colour. */
            g.setColor( color );
            g.fillRect( x, y, blockWidth_, iconHeight_ );
            x += blockWidth_ + gap_;

            /* Block showing fade black -> colour -> white. */
            for ( Shader sh : shaders_ ) {
                Shaders.createShaderIcon( Shaders.applyShader( sh, color,
                                                               shadeWidth_ ),
                                          true, shadeWidth_, iconHeight_, 0, 0 )
                       .paintIcon( c, g, x, y );
                x += shadeWidth_;
            }
            x += gap_;

            /* Plot some markers. */
            g.setColor( Color.WHITE );
            g.fillRect( x, y, sizes_.length * markWidth_, iconHeight_ );
            for ( int i = 0; i < sizes_.length; i++ ) {
                MarkShape.FILLED_CIRCLE.getStyle( color, sizes_[ i ] )
                                       .getIcon( markWidth_, iconHeight_ )
                                       .paintIcon( c, g, x, y );
                x += markWidth_;
            }
            x += gap_;

            /* Write rrggbb text representation. */
            g.setColor( Color.WHITE );
            g.fillRect( x, y, txtWidth_ + 2 * padTxt_, iconHeight_ );
            g.setFont( hexFont_ );
            g.setColor( color );
            g.drawString( getHexString( color ), x + padTxt_, yTxt_ );
            x += txtWidth_ + 2 * padTxt_;

            /* Restore graphics context. */
            g.setFont( font0 );
            g.setColor( color0 );
        }

        /**
         * Returns the RRGGBB hexadecimal representation of a given colour.
         *
         * @param  color  query colour
         * @return  6-digit string representation
         */
        private String getHexString( Color color ) {
            String hex = Integer.toString( color.getRGB() & 0xffffff, 16 );
            for ( int i = hex.length(); i < 6; i++ ) {
                hex = "0" + hex;
            }
            return hex;
        }
    }

    /**
     * Custom chooser panel implementation that lets you choose from
     * a number of pre-configured palettes.
     */
    private static class PaletteColorChooserPanel
                         extends AbstractColorChooserPanel {

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
                putValue( SHORT_DESCRIPTION,
                          "Move " + txt_ + " in palette grid" );
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
                for ( Map.Entry<String,Color[]> entry :
                      paletteMap.entrySet() ) {
                    labels_[ ip ] = entry.getKey() + ": ";
                    colors_[ ip ] = entry.getValue();
                    ncmax = Math.max( ncmax, colors_[ ip ].length );
                    ip++;
                }
                ncmax_ = ncmax;
                font_ = UIManager.getFont( "Label.font" );
                xyOver_ = new int[] { -1, -1 };
                FontRenderContext frc =
                    new FontRenderContext( new AffineTransform(),
                                           false, false );
                int txtWidth = 0;
                int txtHeight = 0;
                for ( String label : labels_ ) {
                    Rectangle r =
                        font_.getStringBounds( label, frc ).getBounds();
                    txtWidth = Math.max( txtWidth, r.width );
                    txtHeight = Math.max( txtHeight, r.height );
                }
                txtWidth_ = txtWidth;
                lineHeight_ = txtHeight;
                lineMetrics_ = font_.getLineMetrics( labels_[ 0 ], frc );
                yPatchOff_ = ( lineHeight_ - yPatch_ ) / 2;
                txtColor_ = UIManager.getColor( "ColorChooser.foreground" );
                overColor_ =
                    UIManager.getColor( "ComboBox.selectionBackground" );
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
}
