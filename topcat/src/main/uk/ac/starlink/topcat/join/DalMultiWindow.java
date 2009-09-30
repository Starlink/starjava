package uk.ac.starlink.topcat.join;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JProgressBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.vo.Capability;
import uk.ac.starlink.vo.KeywordServiceQueryFactory;
import uk.ac.starlink.vo.RegCapabilityInterface;
import uk.ac.starlink.vo.RegResource;
import uk.ac.starlink.vo.RegistryPanel;

/**
 * Window for executing a multiple query type match between an input
 * table and a remote DAL service.
 *
 * @author   Mark Taylor
 * @since    29 Sep 2009
 */
public class DalMultiWindow extends AuxWindow {

    /**
     * Constructor.
     *
     * @param   parent  parent component
     * @param   service  describes the service type to perform queries on
     */
    public DalMultiWindow( Component parent, DalMultiService service ) {
        super( "Multiple " + service.getName(), parent );
        final Capability capability = service.getCapability();
        String servName = service.getName();

        /* Set up a registry query panel for selecting suitable services. */
        KeywordServiceQueryFactory qfact =
            new KeywordServiceQueryFactory( capability );
        final RegistryPanel regPanel = new RegistryPanel( qfact, true ) {
            public RegCapabilityInterface[] getCapabilities( RegResource res ) {
                RegCapabilityInterface[] caps = super.getCapabilities( res );
                List serviceCapList = new ArrayList();
                for ( int ic = 0; ic < caps.length; ic++ ) {
                    if ( capability.isInstance( caps[ ic ] ) ) {
                        serviceCapList.add( caps[ ic ] );
                    }
                }
                return (RegCapabilityInterface[])
                       serviceCapList
                      .toArray( new RegCapabilityInterface[ 0 ] );
            }
        };

        /* Set up the panel for entering the query parameters for a given
         * service. */
        final DalMultiPanel multiPanel =
            new DalMultiPanel( service, placeProgressBar() );

        /* Arrange for the multiquery panel to get updated when a service
         * is selected from the registry panel. */
        ListSelectionListener serviceSelListener = new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                RegCapabilityInterface[] caps =
                    regPanel.getSelectedCapabilities();
                multiPanel.setServiceUrl( caps.length == 1
                                              ? caps[ 0 ].getAccessUrl()
                                              : null );
            }
        };
        regPanel.getResourceSelectionModel()
                .setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        regPanel.getCapabilitySelectionModel()
                .setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        regPanel.getResourceSelectionModel()
                .addListSelectionListener( serviceSelListener );
        regPanel.getCapabilitySelectionModel()
                .addListSelectionListener( serviceSelListener );

        /* Cosmetics. */
        regPanel.setBorder(
            BorderFactory.createCompoundBorder(
                makeTitledBorder( "Available " + servName + " Services" ),
                BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
        multiPanel.setBorder(
            BorderFactory.createCompoundBorder(
                makeTitledBorder( "Multiple " + servName + " Parameters" ),
                BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
        regPanel.setPreferredSize( new Dimension( 600, 350 ) );

        /* Place components. */
        JComponent main = getMainArea();
        main.add( regPanel, BorderLayout.CENTER );
        main.add( multiPanel, BorderLayout.SOUTH );

        /* Place start and stop actions. */
        JComponent controls = getControlPanel();
        controls.add( new JButton( multiPanel.getStartAction() ) );
        controls.add( new JButton( multiPanel.getStopAction() ) );

        /* Place menu actions. */
        JMenu colMenu = regPanel.makeColumnVisibilityMenu( "Columns" );
        colMenu.setMnemonic( KeyEvent.VK_C );
        getJMenuBar().add( colMenu );
        JMenu regMenu = new JMenu( "Registry" );
        regMenu.add( qfact.getRegistrySelector().getRegistryUpdateAction() );
        regMenu.setMnemonic( KeyEvent.VK_R );
        getJMenuBar().add( regMenu );
    }
}
