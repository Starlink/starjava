package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import uk.ac.starlink.ttools.plot2.Gang;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Manaages control of controller objects that work with multiple plotting
 * zones.  A controller (the parameterised type <code>&lt;C&gt;</code>)
 * in this context is an object that manages one or more fixed
 * {@link Control}s in the non-layer part of the control stack.
 *
 * @param  <C>   object that manages fixed {@link Control}s in the stack
 *
 * @author   Mark Taylor
 * @since    12 Feb 2016
 */
public class MultiController<C> {

    private final ControllerFactory<C> cfact_;
    private final MultiConfigger configger_;
    private final Map<ZoneId,C> zoneControllers_;
    private final List<MultiControl> controls_;
    private final ActionForwarder multiForwarder_;
    private ZoneId[] zones_;
    private Gang gang_;

    /**
     * Constructor.
     *
     * @param  cfact   encapsulates management with a particular type of
     *                 controller object <code>C</code>
     * @param  zfact     zone id factory
     * @param  configger   manages global and per-zone axis config items
     */
    @SuppressWarnings("this-escape")
    public MultiController( ControllerFactory<C> cfact, ZoneFactory zfact,
                            MultiConfigger configger ) {
        cfact_ = cfact;
        configger_ = configger;
        zoneControllers_ = new TreeMap<ZoneId,C>( zfact.getComparator() );
        zones_ = new ZoneId[ 0 ];
        multiForwarder_ = new ActionForwarder();

        /* Prepare the control objects. */
        controls_ = new ArrayList<MultiControl>();
        int nc = cfact_.getControlCount();
        for ( int ic = 0; ic < nc; ic++ ) {
            controls_.add( new MultiControl( ic ) );
        }
    }

    /**
     * Returns the fixed controls managed by this object that can
     * be inserted into the control stack.
     * The return value does not change over the lifetime of this object.
     *
     * @return  control array
     */
    public Control[] getStackControls() {
        return controls_.toArray( new Control[ 0 ] );
    }

    /**
     * Returns the controller associated with a particular zone.
     * This should be for a zone that has previously been submitted
     * with a call to {@link #setZones setZones}.
     *
     * @param  zid  zone id
     * @return  controller for zone
     */
    public C getController( ZoneId zid ) {
        return zoneControllers_.get( zid );
    }

    /**
     * Returns this controller's config manager.
     *
     * @return  configger   manages global and per-zone axis config items
     */
    public MultiConfigger getConfigger() {
        return configger_;
    }

    /**
     * Sets the list of zone obects that are to be visible in the current
     * state of the GUI.
     *
     * @param   zones   ordered list of zoneIds whose configuration will
     *                  be accessible from the GUI
     * @param   gang    gang to which the zones belong;
     *                  the sequence of the zones array must match that
     *                  of the gang elements
     */
    public void setZones( ZoneId[] zones, Gang gang ) {
        gang_ = gang;

        /* Update the current state to mach the supplied zones,
         * in particular the zoneId->Controller map. */
        List<ZoneId> oldZoneList = Arrays.asList( zones_ );
        List<ZoneId> newZoneList = Arrays.asList( zones );
        List<ZoneId> removables = new ArrayList<ZoneId>( oldZoneList );
        removables.removeAll( newZoneList );
        List<ZoneId> addables = new ArrayList<ZoneId>( newZoneList );
        addables.removeAll( oldZoneList );
        zones_ = zones;
        for ( ZoneId zid : removables ) {
            C controller = zoneControllers_.get( zid );
            for ( Control c : cfact_.getControls( controller ) ) {
                c.removeActionListener( multiForwarder_ );
            }
        }
        for ( ZoneId zid : addables ) {
            if ( ! zoneControllers_.containsKey( zid ) ) {
                C controller = cfact_.createController();
                zoneControllers_.put( zid, controller );
                Configger configger = cfact_.getConfigger( controller );
                configger_.addZoneConfigger( zid, configger );
            }
            C controller = zoneControllers_.get( zid );
            for ( Control c : cfact_.getControls( controller ) ) {
                c.addActionListener( multiForwarder_ );
            }
        }
        for ( MultiControl control : controls_ ) {
            control.updateZones();
            control.updatePanel();
        }
    }

    /**
     * Returns the currently available list of zones.
     *
     * @return  most recently submitted zone list
     */
    public ZoneId[] getZones() {
        return zones_;
    }

    /**
     * Returns a map of zone ids to controller objects.
     *
     * @return   controller map
     */
    protected Map<ZoneId,C> getControllerMap() {
        return zoneControllers_;
    }

    /**
     * Creates a renderer suitable for zone ids.
     *
     * @return  renderer
     */
    private final ListCellRenderer<Object> createZoneRenderer() {
        final Dimension size = new Dimension( 24, 16 );
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent( JList<?> list,
                                                           Object value,
                                                           int index,
                                                           boolean isSel,
                                                           boolean hasFocus ) {
                final Icon icon;
                if ( value instanceof ZoneId && gang_ != null ) {
                    int iz = Arrays.asList( zones_ ).indexOf( (ZoneId) value );
                    icon = ZoneIcon.createZoneIcon( size, 1, gang_, iz );
                }
                else {
                    icon = null;
                }
                Component c =
                    super.getListCellRendererComponent( list, value, index,
                                                        isSel, hasFocus );
                setIcon( icon );
                return c;
            }
        };
    }

    /**
     * Control that manages a single control from this class's controller
     * (C) type, by allowing the user to work with multiple zones.
     */
    private class MultiControl implements Control {
        final int iControl_;
        final JComponent panel_;
        final JComponent zoneLine_;
        final JComboBox<ZoneId> zoneSelector_;
        Control zoneControl_;

        /**
         * Constructor.
         *
         * @param  iControl  stack control index within this controller type
         */
        MultiControl( int iControl ) {
            iControl_ = iControl;
            panel_ = new JPanel( new BorderLayout() );
            zoneSelector_ = new JComboBox<ZoneId>();
            zoneSelector_.setRenderer( createZoneRenderer() );
            zoneSelector_.addItemListener( new ItemListener() {
                public void itemStateChanged( ItemEvent evt ) {
                    updatePanel();
                }
            } );
            zoneLine_ = Box.createHorizontalBox();
            zoneLine_.add( new JLabel( "Zone: " ) );
            zoneLine_.add( new ShrinkWrapper( zoneSelector_ ) );
            zoneLine_.add( Box.createHorizontalStrut( 5 ) );
            zoneLine_.add( new ComboBoxBumper( zoneSelector_ ) );
            zoneLine_.add( Box.createHorizontalGlue() );
            zoneLine_.setBorder( BorderFactory
                                .createEmptyBorder( 2, 2, 2, 2 ) );
            updatePanel();
        }

        public Icon getControlIcon() {
            return zoneControl_.getControlIcon();
        }

        public String getControlLabel() {
            return zoneControl_.getControlLabel();
        }

        public void addActionListener( ActionListener l ) {
            multiForwarder_.addActionListener( l );
        }

        public void removeActionListener( ActionListener l ) {
            multiForwarder_.removeActionListener( l );
        }

        public JComponent getPanel() {
            return panel_;
        }

        /**
         * Resets the model of the zone selector, ensuring that the
         * current selection is maintained if possible.
         */
        public void updateZones() {
            ZoneId selection =
                zoneSelector_.getItemAt( zoneSelector_.getSelectedIndex() );
            if ( ! Arrays.asList( zones_ ).contains( selection ) ) {
                selection = zones_.length > 0 ? zones_[ 0 ] : null;
            }
            zoneSelector_.setModel( new DefaultComboBoxModel<ZoneId>( zones_ ));
            zoneSelector_.setSelectedItem( selection );
        }

        /**
         * Ensures the main GUI panel for the multi-zone axis control is
         * in a state that reflects the current state of this object.
         */
        public void updatePanel() {
            panel_.removeAll();

            /* Display the zone selector only if there is more than one zone
             * to select from. */
            if ( zoneSelector_.getItemCount() > 1 ) {
                panel_.add( zoneLine_, BorderLayout.NORTH );
            }

            /* Display the axis control corresponding to the selected zone. */
            ZoneId zid = (ZoneId) zoneSelector_.getSelectedItem();
            C controller = getController( zid );
            if ( controller != null ) {
                Control zc = cfact_.getControls( controller )[ iControl_ ];

                /* If this involves a change of control, try to fix it so that
                 * the newly selected one appears in a similar state to the
                 * old one (e.g. same tab visible). */
                if ( zc != null && zoneControl_ != null &&
                     zc != zoneControl_ ) {
                    ControlStackPanel.configureLike( zoneControl_, zc );
                }
                panel_.add( zc.getPanel(), BorderLayout.CENTER );
                zoneControl_ = zc;
            }
            panel_.revalidate();
            panel_.repaint();
        }
    }

    /**
     * Defines how to interact with members of this class's controller type.
     */
    public static interface ControllerFactory<C> {

        /**
         * Creates an instance of the controller.
         *
         * @return  new controller
         */
        C createController();

        /**
         * Returns the number of stack controls this controller manages.
         *
         * @return   length of array returned by <code>getControls</code>
         */
        int getControlCount();

        /**
         * Returns the stack controls associated with a controller instance.
         *
         * @param  controller  controller object
         */
        Control[] getControls( C controller );

        /**
         * Returns an object capable of extraction configuration information
         * from a controller instance.
         *
         * @param  controller  controller object
         */
        Configger getConfigger( C controller );
    }
}
