package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.topcat.CheckBoxList;
import uk.ac.starlink.topcat.ResourceIcon;

/**
 * List component that contains Control objects.
 * It is a CheckBoxList so they can be dragged up and down and
 * selected on and off.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 * @see    ControlStackModel
 * @see    ControlStackPanel
 */
public class ControlStack extends CheckBoxList<Control> {

    private final ControlStackModel stackModel_;

    /**
     * Constructs a stack with a default (empty) model.
     */
    public ControlStack() {
        this( new ControlStackModel() );
    }

    /**
     * Constructs a stack with a given model.
     *
     * @param  stackModel   stack model
     */
    public ControlStack( ControlStackModel stackModel ) {
        super( stackModel, true, new Rendering<Control,JLabel>() {
            public JLabel createRendererComponent() {
                return new JLabel();
            }
            public void configureRendererComponent( JLabel label,
                                                    Control control, int ix ) {
                label.setText( control.getControlLabel() );
                label.setIcon( toStandardSize( control.getControlIcon() ) );
            }
        } );
        stackModel_ = stackModel;
    }

    /**
     * Returns the list model used for this control stack.
     *
     * @return  stack model
     */
    public ControlStackModel getStackModel() {
        return stackModel_;
    }

    @Override
    public boolean isChecked( Control control ) {
        return stackModel_.isControlActive( control );
    }

    @Override
    public void setChecked( Control control, boolean active ) {
        stackModel_.setControlActive( control, active );
    }

    @Override
    public void moveItem( int ifrom, int ito ) {
        stackModel_.moveControl( ifrom, ito );
    }

    /**
     * Returns the currently selected control.
     *
     * @return   current control, or null
     */
    public Control getCurrentControl() {
        return getSelectedValue();
    }

    /**
     * Adds a control to this stack.
     *
     * @param  control   new control
     */
    public void addControl( Control control ) {
        stackModel_.addControl( control );
        setSelectedValue( control, true );
    }

    /**
     * Removes a given control from this stack.
     *
     * @param  control  control to remove
     */
    public void removeControl( Control control ) {
        int isel = -1;
        if ( control == getSelectedValue() ) {
            isel = getSelectedIndex();
        }
        stackModel_.removeControl( control );

        /* Try to make sure this doesn't leave the stack with no
         * selected control. */
        isel = Math.min( isel, stackModel_.getSize() - 1 );
        setSelectedIndex( isel );
    }

    /**
     * Returns an action which deletes the currently selected control from
     * the stack.
     *
     * @param  name  action name
     * @param  description   action short description (tooltip)
     * @return   new action
     */
    public Action createRemoveAction( String name, String description ) {
        final Action action = new AbstractAction( name, ResourceIcon.DELETE ) {
            public void actionPerformed( ActionEvent evt ) {
                Control control = getCurrentControl();
                if ( control != null ) {
                    removeControl( control );
                }
            }
        };
        action.putValue( Action.SHORT_DESCRIPTION, description );
        ListSelectionListener selListener = new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                action.setEnabled( getCurrentControl() != null );
            }
        };
        selListener.valueChanged( null );
        addListSelectionListener( selListener );
        return action;
    }

    /**
     * Redeclares an icon's dimensions to a standard size, and translates it
     * so that it will appear centred in its new shape.
     *
     * @param  icon  input icon
     * @return   icon of fixed standard size
     */
    private static Icon toStandardSize( final Icon icon ) {
        final int sw = 24;
        final int sh = 26;
        final int iw = icon.getIconWidth();
        final int ih = icon.getIconHeight();
        if ( iw == sw && ih == sh ) {
            return icon;
        }
        else {
            return new Icon() {
                public int getIconWidth() {
                    return sw;
                }
                public int getIconHeight() {
                    return sh;
                }
                public void paintIcon( Component c, Graphics g,
                                       int x, int y ) {
                    icon.paintIcon( c, g,
                                    ( sw - iw ) / 2, ( sh - ih ) / 2 );
                }
            };
        }
    }
}
