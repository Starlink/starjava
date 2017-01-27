package uk.ac.starlink.ttools.plot2.config;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
    private final JButton button_;
    private final AWTEventListener mousey_;
    private final Toolkit toolkit_;
    private Color color_;
    private JDialog dialog_;

    /**
     * Constructs a specifier based on a given default colour.
     *
     * @param   dfltColor  initial colour
     */
    public ChooserColorSpecifier( Color dfltColor ) {
        this( new JColorChooser( dfltColor ) );
    }
    
    /**
     * Constructs a specifier based on a given JColorChooser.
     *
     * @param  choser  chooser component
     */
    public ChooserColorSpecifier( JColorChooser chooser ) {
        super( false );
        chooser_ = chooser;
        toolkit_ = chooser.getToolkit();
        button_ = new JButton();

        /* Set up a listener that will be installed on the AWT toolkit
         * when the chooser dialogue is visible.  It watches for clicks
         * outside of the dialogue window, and closes the dialogue if
         * that happens.  This roughly (though not exactly) mimics the
         * behaviour of a JComboBox; if you start to do something
         * elsewhere in the application the popop pops down again.
         * Without this I can imagine the dialogue getting in the way
         * or hidden or (if modal) locking up the GUI. */
        mousey_ = new AWTEventListener() {
            public void eventDispatched( AWTEvent evt ) {
                if ( evt instanceof MouseEvent ) {
                    MouseEvent mevt = (MouseEvent) evt;
                    if ( mevt.getClickCount() > 0 ) {
                        Point wp = dialog_.getLocationOnScreen();
                        Dimension ws = dialog_.getSize();
                        if ( ! new Rectangle( wp.x, wp.y, ws.width, ws.height )
                              .contains( mevt.getLocationOnScreen() ) ) {
                            disposeDialog();
                        }
                    }
                }
            }
        };
    }

    public Color getSpecifiedValue() {
        return color_;
    }

    protected JComponent createComponent() {

        /* Create and install a custom preview panel; the default
         * JColorChooser preview is not very appropriate here. */
        final JComponent previewPanel = new PreviewPanel();
        chooser_.getSelectionModel().addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                previewPanel.repaint();
            }
        } );
        chooser_.setPreviewPanel( previewPanel );

        /* Fix it so that colour the state of the chooser is reflected
         * in the state of this specifier. */
        chooser_.getSelectionModel()
                .addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                setSelectedColor( chooser_.getColor() );
            }
        } );

        /* Set up the button that pops up the dialogue with the chooser. */
        final boolean isModal = false;
        Action chooserAct =
                new AbstractAction( (String) null, ResourceIcon.COLOR_WHEEL ) {
            public void actionPerformed( ActionEvent evt ) {
                if ( dialog_ != null ) {
                    disposeDialog();
                }
                else {
                    dialog_ =
                        JColorChooser
                       .createDialog( button_, "Colour", isModal, chooser_,
                                      createCloseListener( null ),
                                      createCloseListener( color_ ) );
                    postDialog();
                }
            }
        };
        chooserAct.putValue( Action.SHORT_DESCRIPTION, "Free colour chooser" );
        button_.setAction( chooserAct );
        button_.setMargin( new Insets( 0, 0, 0, 0 ) );

        /* The button forms the visible part of this specifier.  Return it. */
        return button_;
    }

    public void submitReport( ReportMap report ) {
    }

    public void setSpecifiedValue( Color color ) {
        color_ = color;
    }

    /**
     * Invoked internally to change the selected colour of this specifier.
     *
     * @param  color  newly selected colour
     */
    private void setSelectedColor( Color color ) {
        setSpecifiedValue( color );
        fireAction();
    }

    /**
     * Displays this specifier's dialogue and installs associated handlers.
     */
    private void postDialog() {
        toolkit_.addAWTEventListener( mousey_, AWTEvent.MOUSE_EVENT_MASK );
        dialog_.setVisible( true );
    }

    /**
     * Closes this specifier's dialogue and uninstalls associated handlers.
     */
    private void disposeDialog() {
        toolkit_.removeAWTEventListener( mousey_ );
        dialog_.dispose();
        dialog_ = null;
    }

    /**
     * Returns an action that closes this specifier's dialogue.
     * It should only be invoked when the dialogue is currently open.
     *
     * @param  color  if non-null, the given colour will be set as this
     *                specifier's selected colour on close
     * @return  new listener whose action will close the dialogue
     */
    private ActionListener createCloseListener( final Color color ) {
        return new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                assert dialog_ != null;
                if ( dialog_ != null ) {
                    disposeDialog();
                }
                if ( color != null ) {
                    setSelectedColor( color );
                }
            }
        };
    }

    /**
     * Implements a custom preview panel for use with JColorChooser.
     * Note there are some version-dependent Swing bugs related to
     * the use of this component, so tread carefully (and preferably
     * test on multiple JRE versions).
     *
     * @see  javax.swing.JColorChooser#setPreviewPanel
     */
    private class PreviewPanel extends JPanel {
        PreviewPanel() {
            super( new BorderLayout() );
            add( new JLabel( new ColorIcon( 100, 12 ) ), BorderLayout.CENTER );
            setBackground( Color.WHITE );

            /* These hoops apparently required to work round java 6
             * Swing bugs. */
            setBorder( BorderFactory.createEmptyBorder( 0, 0, 1, 0 ) );
            setSize( getPreferredSize() );
        }
    }

    /**
     * Simple icon that displays a block of this specifier's current colour.
     */
    private class ColorIcon implements Icon {
        private final int width_;
        private final int height_;

        /**
         * Constructor.
         *
         * @param  width  icon width
         * @param  height  icon height
         */
        ColorIcon( int width, int height ) {
            width_ = width;
            height_ = height;
        }

        public int getIconWidth() {
            return width_;
        }

        public int getIconHeight() {
            return height_;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            Color color0 = g.getColor();
            g.setColor( c.isEnabled()
                      ? color_
                      : UIManager.getColor( "Label.disabledForeground" ) );
            g.fillRect( x, y, width_, height_ );
            g.setColor( color0 );
        }
    }
}
