package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import uk.ac.starlink.topcat.ToggleButtonModel;

/**
 * ListModel for the control stack.
 * All the entries are {@link Control} objects.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class ControlStackModel extends AbstractListModel<Control> {

    private final List<Control> list_;
    private final Map<Control,ToggleButtonModel> activeMap_;
    private final Map<Control,ControlListener> controlListenerMap_;
    private final List<ActionListener> plotListenerList_;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public ControlStackModel() {
        list_ = new ArrayList<Control>();
        activeMap_ = new HashMap<Control,ToggleButtonModel>();
        controlListenerMap_ = new HashMap<Control,ControlListener>();
        plotListenerList_ = new ArrayList<ActionListener>();
        addListDataListener( new ListDataListener() {
            public void contentsChanged( ListDataEvent evt ) {
                plotChange();
            }
            public void intervalAdded( ListDataEvent evt ) {
                plotChange();
            }
            public void intervalRemoved( ListDataEvent evt ) {
                plotChange();
            }
            private void plotChange() {
                ActionEvent evt =
                    new ActionEvent( ControlStackModel.this, 0, "replot" );
                for ( ActionListener listener : plotListenerList_ ) {
                    listener.actionPerformed( evt );
                }
            }
        } );
    }

    public Control getElementAt( int ix ) {
        return getControlAt( ix );
    }

    public int getSize() {
        return list_.size();
    }

    /**
     * Returns the control at a given index in this list.
     *
     * @param   ix  list index
     */
    public Control getControlAt( int ix ) {
        return ix >= 0 && ix < list_.size() ? list_.get( ix ) : null;
    }

    /**
     * Indicates whether a given control is marked as active in this stack.
     *
     * @param  control  control
     * @return  true iff active
     */
    public boolean isControlActive( Control control ) {
        ToggleButtonModel activeModel = activeMap_.get( control );
        return activeModel != null && activeModel.isSelected();
    }

    /**
     * Sets the activeness of a control in this stack.
     *
     * @param  control   control
     * @param  isActive  true iff active
     */
    public void setControlActive( Control control, boolean isActive ) {
        ToggleButtonModel activeModel = activeMap_.get( control );
        if ( activeModel != null ) {
            activeModel.setSelected( isActive );
        }
    }

    /**
     * Returns a list of the controls which can contribute layers to the plot,
     * that is LayerControls.
     * If the <code>activeOnly</code> parameter is set, it is restricted
     * further to controls which are currently marked as active and
     * which are known to contribute at least one layer.
     *
     * @param   activeOnly   if true, return only controls contributing layers;
     *                       if false, return all
     * @return   layer controls that would contribute to a plot
     */
    public LayerControl[] getLayerControls( boolean activeOnly ) {
        List<LayerControl> list = new ArrayList<LayerControl>();
        for ( int ic = 0; ic < getSize(); ic++ ) {
            Control control = getControlAt( ic );
            if ( control instanceof LayerControl ) {
                LayerControl lc = (LayerControl) control;
                if ( ! activeOnly ||
                     ( isControlActive( lc ) && lc.hasLayers() ) ) {
                    list.add( lc );
                }
            }
        }
        return list.toArray( new LayerControl[ 0 ] );
    }

    /**
     * Adds a control to this model.
     *
     * @param  control  new control
     */
    public void addControl( Control control ) {
        list_.add( control );
        ControlListener controlListener = new ControlListener( control );
        controlListenerMap_.put( control, controlListener );
        ToggleButtonModel activeModel =
            new ToggleButtonModel( null, null, "Layer visibility" );
        control.addActionListener( controlListener );
        activeModel.addChangeListener( controlListener );
        activeModel.setSelected( true );
        activeMap_.put( control, activeModel );
        int ix = list_.size();
        fireIntervalAdded( this, ix, ix );
    }

    /**
     * Removes a control from this model.
     *
     * @param   control  previously added control
     */
    public void removeControl( Control control ) {
        activeMap_.remove( control );
        control.removeActionListener( controlListenerMap_.remove( control ) );
        int ix = list_.indexOf( control );
        if ( ix >= 0 ) {
            list_.remove( control );
            fireIntervalRemoved( this, ix, ix );
        }
    }

    /**
     * Relocates a control in this list.
     *
     * @param   iFrom  source list index
     * @param   iTo   destination list index
     */
    public void moveControl( int iFrom, int iTo ) {
        Control control = list_.remove( iFrom );
        list_.add( iTo, control );
        fireContentsChanged( this, iFrom, iTo );
    }

    /**
     * Adds a listener which will be notified if this stack's state changes
     * in such a way that the plot might be affected.
     * That includes changes in the state of any of the controls
     * contained in this stack.
     *
     * @param  listener   listener to add
     */
    public void addPlotActionListener( ActionListener listener ) {
        plotListenerList_.add( listener );
    }

    /**
     * Removes a listener previously added.
     *
     * @param  listener  listener to remove
     */
    public void removePlotActionListener( ActionListener listener ) {
        plotListenerList_.remove( listener );
    }

    /**
     * Listens to things that happen to a Control, and passes these on
     * to listeners to this stack model.
     */
    private class ControlListener implements ActionListener, ChangeListener {

        private final Control control_;

        /**
         * Constructor.
         *
         * @param   control  control to monitor
         */
        ControlListener( Control control ) {
            control_ = control;
        }

        /** Implements ActionListener. */
        public void actionPerformed( ActionEvent evt ) {
            change();
        }

        /** Implements ChangeListener. */
        public void stateChanged( ChangeEvent evt ) {
            change();
        }

        /**
         * Invoked in case that this listener is notified of an action
         * or change.
         * It is passed on as a ContentsChanged ListDataEvent on the
         * approppriate row of this model to any listeners to this model.
         */
        private void change() {
            int ix = list_.indexOf( control_ );
            assert ix >= 0;
            fireContentsChanged( this, ix, ix );
        }
    }
}
