package uk.ac.starlink.topcat;

import java.awt.event.KeyEvent;
import javax.swing.JMenu;
import uk.ac.starlink.topcat.interop.Transmitter;
import uk.ac.starlink.vo.RegistryPanel;
import uk.ac.starlink.vo.RegistryServiceTableLoadDialog;

/**
 * Provides the necessary methods to customise one of the
 * RegistryServiceTableLoadDialog classes for use with TOPCAT.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2010
 */
public class RegistryDialogAdjuster {
    private final String resourceType_;
    private final RegistryServiceTableLoadDialog dalLoader_;

    /**
     * Constructor.
     *
     * @param  dalLoader  standard load dialogue
     * @param  resourceType   name of resource type the dialogue's resource
     *         list contains; must be MType subtype for
     *         voresource.loadlist.* message
     */
    public RegistryDialogAdjuster( RegistryServiceTableLoadDialog dalLoader,
                                   String resourceType ) {
        dalLoader_ = dalLoader;
        resourceType_ = resourceType;
    }

    /**
     * Returns an Interop menu suitable for use in this object's load dialogue.
     *
     * @return  interop menu
     */
    public JMenu createInteropMenu() {
        return createInteropMenu( dalLoader_.getRegistryPanel(),
                                  resourceType_ );
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
