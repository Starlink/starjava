package uk.ac.starlink.topcat;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JMenu;
import uk.ac.starlink.topcat.interop.Transmitter;
import uk.ac.starlink.vo.RegistryPanel;
import uk.ac.starlink.vo.RegistryServiceTableLoadDialog2;

/**
 * Provides the necessary methods to customise one of the
 * RegistryServiceTableLoadDialog classes for use with TOPCAT.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2010
 */
public class RegistryDialogAdjuster {
    private final String resourceType_;
    private final RegistryServiceTableLoadDialog2 dalLoader_;

    /**
     * Constructor.
     *
     * @param  dalLoader  standard load dialogue
     * @param  resourceType   name of resource type the dialogue's resource
     *         list contains; must be MType subtype for
     *         voresource.loadlist.* message
     */
    public RegistryDialogAdjuster( RegistryServiceTableLoadDialog2 dalLoader,
                                   String resourceType ) {
        dalLoader_ = dalLoader;
        resourceType_ = resourceType;
    }

    /**
     * Adds a suitable Interop menu to this object's load dialogue menu list.
     */
    public void addInteropMenu() {
        List<JMenu> menuList =
            new ArrayList<JMenu>( Arrays.asList( dalLoader_.getMenus() ) );
        menuList.add( createInteropMenu( dalLoader_.getRegistryPanel(),
                                         resourceType_ ) );
        dalLoader_.setMenus( menuList.toArray( new JMenu[ 0 ] ) );
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
}
