package uk.ac.starlink.ttools.plot2.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
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
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.layer.MarkerShape;

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
    private static final Map<String,Color> COLOR_MAP =
        NamedColorSet.CSS_DARK.getMap();

    /**
     * Constructs a specifier based on a given default colour.
     *
     * @param   dfltColor  initial colour
     */
    public ChooserColorSpecifier( Color dfltColor ) {
        this( new JColorChooser( dfltColor ) );

        /* Add custom choosers. */
        AbstractColorChooserPanel[] extraChoosers = {
            new PaletteColorChooserPanel( PALETTE_MAP, chooser_ ),
            new NamedColorChooserPanel( COLOR_MAP, chooser_ ),
        };
        for ( AbstractColorChooserPanel c : extraChoosers ) {
            chooser_.addChooserPanel( c );
        }

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
                MarkerShape.FILLED_CIRCLE.getStyle( color, sizes_[ i ] )
                           .getLegendIcon( markWidth_, iconHeight_ )
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
}
