package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JMenu;
import uk.ac.starlink.topcat.interop.Transmitter;
import uk.ac.starlink.vo.DalTableLoadDialog;
import uk.ac.starlink.vo.RegistryPanel;
import uk.ac.starlink.vo.SkyDalTableLoadDialog;
import uk.ac.starlink.vo.SkyPositionEntry;

/**
 * Provides the necessary methods to customise one of the
 * DalTableLoadDialog classes for use with TOPCAT.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2010
 */
public class RegistryDialogAdjuster {
    private final DalTableLoadDialog dalLoader_;
    private final SkyDalTableLoadDialog skyDalLoader_;
    private final String resourceType_;
    private final ToggleButtonModel acceptResourceModel_;
    private final ToggleButtonModel acceptPositionModel_;

    /**
     * Constructor.
     *
     * @param  dalLoader  standard load dialogue
     * @param  resourceType   name of resource type the dialogue's resource
     *         list contains; must be MType subtype for
     *         voresource.loadlist.* message
     * @param  isSky  true if the dialogue should be capable of receiving
     *         skyPositions
     */
    public RegistryDialogAdjuster( DalTableLoadDialog dalLoader,
                                   String resourceType, boolean isSky ) {
        dalLoader_ = dalLoader;
        resourceType_ = resourceType;
        skyDalLoader_ = isSky ? (SkyDalTableLoadDialog) dalLoader
                              : null;
        acceptResourceModel_ = createAcceptResourceIdListModel();
        acceptPositionModel_ =
            new ToggleButtonModel( "Accept Sky Positions", ResourceIcon.LISTEN,
                                   "Accept incoming SAMP/PLASTIC sky position "
                                 + "messages to update search coordinates" );
        acceptPositionModel_.setSelected( skyDalLoader_ != null );
    }

    /**
     * Performs various adjustments to this dialogue's query component
     * to make it work better in a TOPCAT window.
     */
    public void adjustComponent() {

        /* Add a new interop menu. */
        List<JMenu> menuList =
            new ArrayList<JMenu>( Arrays.asList( dalLoader_.getMenus() ) );
        JMenu interopMenu = createInteropMenu( dalLoader_.getRegistryPanel(),
                                               resourceType_ );
        interopMenu.addSeparator();
        interopMenu.add( acceptResourceModel_.createMenuItem() );
        if ( skyDalLoader_ != null ) {
            interopMenu.add( acceptPositionModel_.createMenuItem() );
        }
        menuList.add( interopMenu );
        dalLoader_.setMenus( menuList.toArray( new JMenu[ 0 ] ) );

        /* Add acceptance buttons at suitable places in the GUI. */
        adjustRegistryPanel( dalLoader_.getRegistryPanel(),
                             acceptResourceModel_ );
        if ( skyDalLoader_ != null ) {
            adjustSkyEntry( skyDalLoader_.getSkyEntry(), acceptPositionModel_ );
        }
    }

    /**
     * Indicates whether incoming resource lists are currently being accepted.
     *
     * @return   true  iff resource lists should be used
     */
    public boolean acceptResourceIdLists() {
        return acceptResourceModel_.isSelected();
    }

    /**
     * Indicates whether incoming sky positions are currently being accepted.
     *
     * @return  true  iff sky positions should be used
     */
    public boolean acceptSkyPositions() {
        return acceptPositionModel_.isSelected();
    }

    /**
     * Returns a toggle model for acceping resource lists.
     *
     * @return  new toggle button model
     */
    public static ToggleButtonModel createAcceptResourceIdListModel() {
        ToggleButtonModel model =
            new ToggleButtonModel( "Accept Resource Lists", ResourceIcon.LISTEN,
                                   "Accept incoming SAMP voresource.loadlist* "
                                 + "messages to update resource list" );
        model.setSelected( true );
        return model;
    }

    /**
     * Returns an Interop menu suitable for use with a registry panel.
     *
     * @param   regPanel  registry panel
     * @param  resourceType   name of resource type the panel's resource
     *         list contains; must be MType subtype for
     *         voresource.loadlist.* message
     */
    public static JMenu createInteropMenu( RegistryPanel regPanel,
                                           String resourceType ) {
        JMenu interopMenu = new JMenu( "Interop" );
        interopMenu.setMnemonic( KeyEvent.VK_I );
        Transmitter transmitter =
            ControlWindow.getInstance().getCommunicator()
           .createResourceListTransmitter( regPanel, resourceType );
        interopMenu.add( transmitter.getBroadcastAction() );
        interopMenu.add( transmitter.createSendMenu() );
        return interopMenu;
    }

    /**
     * Adjusts a registry panel for use with TOPCAT.
     *
     * @param  regPanel  registry panel to adjust
     * @param  acceptResourceModel  toggler for resource list acceptance
     */
    public static void adjustRegistryPanel(
            RegistryPanel regPanel, ToggleButtonModel acceptResourceModel ) {
        regPanel.getControlBox().add( acceptResourceModel.createCheckBox() );
    }

    /**
     * Adjusts a sky entry panel for use with TOPCAT.
     *
     * @param  skyEntry  sky entry panel to adjust
     * @param  acceptPositionModel  toggler for sky position acceptance
     */
    public static void adjustSkyEntry( SkyPositionEntry skyEntry,
                                       ToggleButtonModel acceptPositionModel ) {
        JComponent vbox = Box.createVerticalBox();
        vbox.add( acceptPositionModel.createCheckBox() );
        vbox.add( Box.createVerticalGlue() );
        skyEntry.add( vbox, BorderLayout.EAST );
    }
}
