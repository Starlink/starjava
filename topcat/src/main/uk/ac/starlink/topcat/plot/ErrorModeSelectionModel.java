package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.event.ListDataListener;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.ErrorModeSelection;
import uk.ac.starlink.ttools.plot.ErrorRenderer;

/**
 * Model for selecting {@link ErrorMode} values.
 * Methods are provided for acquiring GUI controls which reflect this model.
 *
 * @author   Mark Taylor
 * @since    26 Feb 2007
 */
@SuppressWarnings("rawtypes")
public class ErrorModeSelectionModel
        implements ErrorModeSelection, ComboBoxModel, ActionListener {

    private final int iaxis_;
    private final String axisName_;
    private final ErrorMode[] options_;
    private final ActionForwarder actionForwarder_;
    private final ErrorRenderer errorRenderer_;
    private final List<JComponent> controlList_;
    private boolean enabled_;
    private int iOpt_;

    /**
     * Constructor.
     *
     * @param  iaxis  index of the axis for which this model selects ErrorModes
     * @param  axisName  name of the axis
     */
    @SuppressWarnings("this-escape")
    public ErrorModeSelectionModel( int iaxis, String axisName ) {
        iaxis_ = iaxis;
        axisName_ = axisName;
        options_ = ErrorMode.getOptions();
        actionForwarder_ = new ActionForwarder();
        errorRenderer_ = ErrorRenderer.EXAMPLE;
        enabled_ = true;
        controlList_ = new ArrayList<JComponent>();
        setMode( ErrorMode.NONE );
    }

    /**
     * Returns the currently selected mode.
     *
     * @return  error mode
     */
    public ErrorMode getErrorMode() {
        return options_[ iOpt_ ];
    }

    /**
     * Sets the selected mode.
     *
     * @param  mode  new error mode
     */
    public void setMode( ErrorMode mode ) {
        actionPerformed( new ActionEvent( this, 0, mode.toString() ) );
    }

    /**
     * Toggles enabled state of any controls based on this model.
     *
     * @param  enabled  true iff user should be able to change state
     */
    public void setEnabled( boolean enabled ) {
        for ( JComponent comp : controlList_ ) {
            comp.setEnabled( enabled );
        }
        enabled_ = enabled;
    }

    /**
     * Indicates enabledness of this model.
     *
     * @return   true  iff user should be able to change state
     */
    public boolean isEnabled() {
        return enabled_;
    }

    /**
     * Updates model state and informs listeners as required.
     * The command string of the provided ActionEvent must match the
     * name of one of the available ErrorModes.
     *
     * @param  evt  action whose command string names the new mode
     */
    public void actionPerformed( ActionEvent evt ) {
        String modeName = evt.getActionCommand();
        for ( int i = 0; i < options_.length; i++ ) {
            if ( options_[ i ].toString().equals( modeName ) ) {
                if ( iOpt_ != i ) {
                    iOpt_ = i;
                    actionForwarder_.actionPerformed( evt );
                }
                return;
            }
        }
        throw new IllegalArgumentException( "No such mode " + modeName );
    }

    /**
     * Returns a set of menu items which allow selection of the state for
     * this model.
     * Currently they are radio buttons, one for each known mode.
     *
     * @return   list of menu items for control
     */
    public JMenuItem[] createMenuItems() {
        int nmode = options_.length;
        JRadioButtonMenuItem[] items = new JRadioButtonMenuItem[ nmode ];
        ButtonGroup bGrp = new ButtonGroup();
        for ( int imode = 0; imode < nmode; imode++ ) {
            ErrorMode mode = options_[ imode ];
            String modeName = mode.toString();
            JRadioButtonMenuItem item =
                new JRadioButtonMenuItem( " " + axisName_ + " " + mode,
                                          getIcon( mode, 24, 24, 1, 1 ) );
            final int im = imode;
            item.setModel( new DefaultButtonModel() {
                public boolean isSelected() {
                    return im == iOpt_;
                }
            } );
            items[ imode ] = item;
            item.setActionCommand( mode.toString() );
            bGrp.add( item );
            item.addActionListener( this );
            controlList_.add( (JComponent) item );
        }
        return items;
    }

    /**
     * Returns a toolbar button which toggles the mode between no error bars
     * and symmetrical error bars.  This does not allow full control
     * (not all modes are available).
     *
     * @return  toolbar button for toggling error bar status
     */
    public AbstractButton createOnOffToolbarButton() {
        AbstractButton button = createOnOffButton();
        button.setText( null );
        return button;
    }

    /**
     * Returns a normal button which toggles the mode between no error bars
     * and symmetrical error bars.  This does not allow full control
     * (not all modes are available).
     *
     * @return  button for toggling error bar status
     */
    public AbstractButton createOnOffButton() {
        ButtonModel model = new DefaultButtonModel() {
            ErrorMode lastOnMode = ErrorMode.SYMMETRIC;
            public String getActionCommand() {
                ErrorMode currentMode = getErrorMode();
                if ( currentMode == ErrorMode.NONE ) {
                    return lastOnMode.toString();
                }
                else {
                    lastOnMode = currentMode;
                    return ErrorMode.NONE.toString();
                }
            }
        };
        final AbstractButton button =
            new JToggleButton( axisName_ + " Errors",
                               getIcon( ErrorMode.SYMMETRIC, 24, 24, 1, 1 ) );
        addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                button.setSelected( getErrorMode() != ErrorMode.NONE );
            }
        } );
        model.addActionListener( this );
        button.setModel( model );
        button.setToolTipText( "Toggle " + axisName_ + " error bars on/off" );
        controlList_.add( button );
        return button;
    }

    /*
     * ComboBoxModel implementation.
     */

    public Object getElementAt( int index ) {
        return options_[ index ];
    }

    public int getSize() {
        return options_.length;
    }

    public Object getSelectedItem() {
        return getErrorMode();
    }

    public void setSelectedItem( Object mode ) {
        setMode( (ErrorMode) mode );
    }

    /**
     * No-op - the list never changes.
     */
    public void addListDataListener( ListDataListener listener ) {
        // list never changes
    }

    /**
     * No-op - the list never changes.
     */
    public void removeListDataListener( ListDataListener listener ) {
        // list never changes
    }

    /**
     * Adds a listener which will be informed when the selection changes.
     *
     * @param  listener listener
     */
    public void addActionListener( ActionListener listener ) {
        actionForwarder_.addActionListener( listener );
    }

    /**
     * Removes a listener previously added by {@link #addActionListener}.
     *
     * @param  listener   listener
     */
    public void removeActionListener( ActionListener listener ) {
        actionForwarder_.addActionListener( listener );
    }

    /**
     * Returns an icon which can be used to represent a given error mode.
     *
     * @param   mode  error mode
     * @param   width  total width of icon
     * @param   height  total height of icon
     * @param   xpad  internal horizontal padding of icon
     * @param   ypad  internal vertical padding of icon
     * @return   icon
     */
    public Icon getIcon( ErrorMode mode, int width, int height,
                         int xpad, int ypad ) {
        ErrorMode[] modes = new ErrorMode[ iaxis_ + 1 ];
        for ( int i = 0; i < modes.length; i++ ) {
            modes[ i ] = i == iaxis_ ? mode : ErrorMode.NONE;
        }
        return markCentre( errorRenderer_
                          .getLegendIcon( modes, width, height, xpad, ypad ) );
    }

    /**
     * Returns an icon based on a given one but with a little circle
     * painted in the middle to represent the point around which the
     * error bars are displayed.  Suitable for using in menus etc.
     *
     * @param   icon  input icon representing error bars
     * @return  same as <code>icon</code> but with a centre mark
     */
    private static Icon markCentre( final Icon icon ) {
        return new Icon() {
            public int getIconHeight() {
                return icon.getIconHeight();
            }
            public int getIconWidth() {
                return icon.getIconWidth();
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                Color oldColor = g.getColor();
                g.setColor( Color.BLACK );
                icon.paintIcon( c, g, x, y );
                g.setColor( Color.WHITE );
                int radius = 2;
                g.drawOval( x + getIconWidth() / 2 - radius,
                            y + getIconHeight() / 2 - radius,
                            radius * 2, radius * 2 );
                g.setColor( oldColor );
            }
        };
    }
}
