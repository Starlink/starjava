package uk.ac.starlink.ttools.plot2.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.ReportMap;

/**
 * SpecifierPanel subclass that uses a JColorChooser to specify a colour.
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

    /**
     * Constructs a specifier based on a given default colour.
     *
     * @param   dfltColor  initial colour
     */
    public ChooserColorSpecifier( Color dfltColor ) {
        this( new JColorChooser( dfltColor ) );

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
     * Action that can select a colour.
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
            final int iconWidth = 24;
            final int iconHeight = 12;
            Color dcol = UIManager.getColor( "Label.disabledForeground" );
            final Color disabledColor = dcol == null ? Color.GRAY : dcol;
            putValue( SMALL_ICON, new Icon() {
                public int getIconWidth() {
                    return iconWidth;
                }
                public int getIconHeight() {
                    return iconHeight;
                }
                public void paintIcon( Component c, Graphics g, int x, int y ) {
                    Color color0 = g.getColor();
                    g.setColor( c.isEnabled()
                              ? ChooseAction.this.getActionColor()
                              : disabledColor );
                    g.fillRect( x, y, iconWidth, iconHeight );
                    g.setColor( color0 );
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
}
