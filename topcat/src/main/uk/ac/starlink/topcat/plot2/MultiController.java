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
import uk.ac.starlink.ttools.plot2.Gang;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Manages control of GUI components that work with multiple plotting zones.
 *
 * @author   Mark Taylor
 * @since    11 Feb 2016
 */
public class MultiController<P,A> {

    private final PlotTypeGui<P,A> plotType_;
    private final MultiConfigger configger_;
    private final Map<ZoneId,AxisController<P,A>> axisControllers_;
    private final ActionForwarder axisForwarder_;
    private final Control axisStackControl_;
    private final JComboBox zoneSelector_;
    private final JComponent zoneLine_;
    private final JComponent panel_;
    private ZoneId[] zones_;
    private Control control_;
    private Gang gang_;

    /**
     * Constructor.
     *
     * @param  plotType   plot type
     * @param  zfact     zone id factory
     * @param  configger   manages global and per-zone axis config items
     */
    public MultiController( PlotTypeGui<P,A> plotType, ZoneFactory zfact,
                            MultiConfigger configger ) {
        plotType_ = plotType;
        configger_ = configger;
        axisControllers_ =
            new TreeMap<ZoneId,AxisController<P,A>>( zfact.getComparator() );
        zones_ = new ZoneId[ 0 ];

        /* Prepare the GUI components. */
        panel_ = new JPanel( new BorderLayout() );
        zoneSelector_ = new JComboBox();
        zoneSelector_.setRenderer( new DefaultListCellRenderer() {
            @Override
            public Component
                    getListCellRendererComponent( JList list, Object value,
                                                  int index, boolean isSel,
                                                  boolean hasFocus ) {
                Component c =
                    super.getListCellRendererComponent( list, value, index,
                                                        isSel, hasFocus );
                setIcon( value instanceof ZoneId ? getZoneIcon( (ZoneId) value )
                                                 : null );
                return c;
            }
        } );
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
        zoneLine_.setBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) );

        /* Prepare the stack control that represents this object. */
        axisForwarder_ = new ActionForwarder();
        axisStackControl_ = new Control() {
            public Icon getControlIcon() {
                return control_ == null ? null : control_.getControlIcon();
            }
            public String getControlLabel() {
                return control_ == null ? null : control_.getControlLabel();
            }
            public void addActionListener( ActionListener l ) {
                axisForwarder_.addActionListener( l );
            }
            public void removeActionListener( ActionListener l ) {
                axisForwarder_.removeActionListener( l );
            }
            public JComponent getPanel() {
                return panel_;
            }
        };

        /* Initialise. */
        updatePanel();
    }

    /**
     * Returns the stack controls that constitute this controller's user
     * interface.  The return value is fixed over the lifetime of this object.
     *
     * @return   stack control array
     */
    public Control[] getStackControls() {
        return new Control[] {
            axisStackControl_,
        };
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
         * in particular the zoneId->AxisController map. */
        List<ZoneId> oldZoneList = Arrays.asList( zones_ );
        List<ZoneId> newZoneList = Arrays.asList( zones );
        List<ZoneId> removables = new ArrayList<ZoneId>( oldZoneList );
        removables.removeAll( newZoneList );
        List<ZoneId> addables = new ArrayList<ZoneId>( newZoneList );
        addables.removeAll( oldZoneList );
        zones_ = zones;
        for ( ZoneId zid : removables ) {
            for ( Control c : axisControllers_.get( zid ).getControls() ) {
                c.removeActionListener( axisForwarder_ );
            }
        }
        for ( ZoneId zid : addables ) {
            if ( ! axisControllers_.containsKey( zid ) ) {
                AxisController<P,A> ac = plotType_.createAxisController();
                axisControllers_.put( zid, ac );
                configger_.addZoneConfigger( zid, ac );
            }
            for ( Control c : axisControllers_.get( zid ).getControls() ) {
                c.addActionListener( axisForwarder_ );
            }
        }

        /* Update the zone selector GUI, taking care to retain the
         * current selection if possible. */
        ZoneId selection = (ZoneId) zoneSelector_.getSelectedItem();
        if ( ! newZoneList.contains( selection ) ) {
            selection = zones.length > 0 ? zones[ 0 ] : null;
        }
        zoneSelector_.setModel( new DefaultComboBoxModel( zones ) );
        zoneSelector_.setSelectedItem( selection );

        /* Ensure that the panel GUI is in a suitable state. */
        updatePanel();
    }

    /**
     * Sets the surface aspect to use for a given zone.
     *
     * @param   ganger   object that defines multi-zone positioning
     * @param   zid    zone whose aspect is to be updated;
     *                 can, but probably shouldn't, be null
     * @param   aspect  new aspect
     */
    public void setAspect( Ganger<A> ganger, ZoneId zid, A aspect ) {

        /* Assemble an array of existing aspects. */
        int nz = zones_.length;
        AxisController<P,A>[] axControllers =
            (AxisController<P,A>[]) new AxisController[ nz ];
        A[] aspects = (A[]) new Object[ nz ];
        int iz0 = -1;
        for ( int iz = 0; iz < nz; iz++ ) {
            ZoneId zid1 = zones_[ iz ];
            axControllers[ iz ] = axisControllers_.get( zid1 );
            aspects[ iz ] = axControllers[ iz ].getAspect();
            if ( zid != null && zid.equals( zid1 ) ) {
                iz0 = iz;
            }
        }

        /* Update the requested one. */
        if ( iz0 >= 0 ) {
            aspects[ iz0 ] = aspect;
        }

        /* Ensure aspects of all zones are consistent. */
        aspects = ganger.adjustAspects( aspects, iz0 );

        /* Write the updated aspects to their controller objects. */
        for ( int iz = 0; iz < nz; iz++ ) {
            axControllers[ iz ].setAspect( aspects[ iz ] );
        }
    }

    /**
     * Resets aspects of all the current per-zone controllers.
     */
    public void resetAspects() {
        for ( AxisController<P,A> ac : axisControllers_.values() ) {
            ac.setAspect( null );
            ac.setRanges( null );
            ac.clearAspect();
        }
    }

    /**
     * Returns the axis controller for a given zone.
     * This should be one included in the most recent call to
     * {@link #setZones setZones}
     *
     * @param   zid  known zone id
     * @return  axis controller for zone
     */
    public AxisController<P,A> getAxisController( ZoneId zid ) {
        return axisControllers_.get( zid );
    }

    /**
     * Returns a combo-box-friendly icon suitable for a given zone id.
     *
     * @param  zid  zone id
     * @return  little icon
     */
    private Icon getZoneIcon( ZoneId zid ) {
        return gang_ == null
             ? null
             : ZoneIcon
              .createZoneIcon( new Dimension( 24, 16 ), 1, gang_,
                               Arrays.asList( zones_ ).indexOf( zid ) );
    }

    /**
     * Ensures the main GUI panel for the multi-zone axis control is
     * in a state that reflects the current state of this object.
     */
    private void updatePanel() {
        panel_.removeAll();

        /* Display the zone selector only if there is more than one zone
         * to select from. */
        if ( zoneSelector_.getItemCount() > 1 ) {
            panel_.add( zoneLine_, BorderLayout.NORTH );
        }

        /* Display the axis control corresponding to the selected zone. */
        ZoneId zid = (ZoneId) zoneSelector_.getSelectedItem();
        AxisController ac = getAxisController( zid );
        if ( ac != null ) {
            Control control = ac.getMainControl();

            /* If this involves a change of control, try to fix it so that
             * the newly selected one appears in a similar state to the
             * old one (e.g. same tab visible). */
            if ( control != null && control_ != null && control != control_ ) {
                ControlStackPanel.configureLike( control_, control );
            }
            panel_.add( control.getPanel(), BorderLayout.CENTER );
            control_ = control;
        }
        panel_.revalidate();
        panel_.repaint();
    }
}
