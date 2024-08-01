package uk.ac.starlink.topcat.join;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JProgressBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.RegistryDialogAdjuster;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.vo.Capability;
import uk.ac.starlink.vo.KeywordServiceQueryFactory;
import uk.ac.starlink.vo.RegCapabilityInterface;
import uk.ac.starlink.vo.RegResource;
import uk.ac.starlink.vo.RegistryPanel;
import uk.ac.starlink.vo.RegistryProtocol;
import uk.ac.starlink.vo.RegistryQuery;

/**
 * Window for executing a multiple query type match between an input
 * table and a remote DAL service.
 *
 * @author   Mark Taylor
 * @since    29 Sep 2009
 */
public class DalMultiWindow extends AuxWindow {

    private final KeywordServiceQueryFactory queryFactory_;
    private final RegistryPanel regPanel_;
    private final ToggleButtonModel acceptResourceModel_;
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructor.
     *
     * @param   parent  parent component
     * @param   service  describes the service type to perform queries on
     * @param   autoQuery  whether to populate the service table with a
     *          full registry query on initial display
     */
    @SuppressWarnings("this-escape")
    public DalMultiWindow( Component parent, DalMultiService service,
                           boolean autoQuery ) {
        super( "Multiple " + service.getName(), parent );
        final Capability capability = service.getCapability();
        String servName = service.getName();

        /* Set up a registry query panel for selecting suitable services. */
        queryFactory_ = new KeywordServiceQueryFactory( capability );
        regPanel_ = new RegistryPanel( queryFactory_, true ) {
            public RegCapabilityInterface[] getCapabilities( RegResource res ) {
                RegistryProtocol regProto = queryFactory_.getRegistrySelector()
                                           .getModel().getProtocol();
                RegCapabilityInterface[] caps = super.getCapabilities( res );
                List<RegCapabilityInterface> serviceCapList =
                    new ArrayList<RegCapabilityInterface>();
                for ( int ic = 0; ic < caps.length; ic++ ) {
                    if ( regProto.hasCapability( capability, caps[ ic ] ) ) {
                        serviceCapList.add( caps[ ic ] );
                    }
                }
                return serviceCapList
                      .toArray( new RegCapabilityInterface[ 0 ] );
            }
        };
        acceptResourceModel_ =
            RegistryDialogAdjuster.createAcceptResourceIdListModel();
        RegistryDialogAdjuster.adjustRegistryPanel( regPanel_,
                                                    acceptResourceModel_ );

        /* Set up the panel for entering the query parameters for a given
         * service. */
        final DalMultiPanel multiPanel =
            new DalMultiPanel( service, placeProgressBar() );
        JComponent versionBox = service.getVersionComponent();
        if ( versionBox != null ) {
            JComponent urlBox = multiPanel.getServiceUrlBox();
            urlBox.add( Box.createHorizontalStrut( 10 ) );
            urlBox.add( versionBox );
        }

        /* Arrange for the multiquery panel to get updated when a service
         * is selected from the registry panel. */
        ListSelectionListener serviceSelListener = new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                RegCapabilityInterface[] caps =
                    regPanel_.getSelectedCapabilities();
                multiPanel.setServiceUrl( caps.length == 1
                                              ? caps[ 0 ].getAccessUrl()
                                              : null );
            }
        };
        regPanel_.getResourceSelectionModel()
                 .setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        regPanel_.getCapabilitySelectionModel()
                 .setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        regPanel_.getResourceSelectionModel()
                 .addListSelectionListener( serviceSelListener );
        regPanel_.getCapabilitySelectionModel()
                 .addListSelectionListener( serviceSelListener );
        service.init( regPanel_ );

        /* Cosmetics. */
        regPanel_.setBorder(
            BorderFactory.createCompoundBorder(
                makeTitledBorder( "Available " + servName + " Services" ),
                BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
        multiPanel.setBorder(
            BorderFactory.createCompoundBorder(
                makeTitledBorder( "Multiple " + servName + " Parameters" ),
                BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
        regPanel_.setPreferredSize( new Dimension( 600, 350 ) );

        /* Place components. */
        JComponent main = getMainArea();
        main.add( regPanel_, BorderLayout.CENTER );
        main.add( multiPanel, BorderLayout.SOUTH );

        /* Place start and stop actions. */
        JComponent controls = getControlPanel();
        controls.add( new JButton( multiPanel.getStartAction() ) );
        controls.add( new JButton( multiPanel.getStopAction() ) );

        /* Place menu actions. */
        JMenu colMenu = regPanel_.makeColumnVisibilityMenu( "Columns" );
        colMenu.setMnemonic( KeyEvent.VK_C );
        getJMenuBar().add( colMenu );
        if ( service.hasCoverages() ) {
            JMenu searchMenu = new JMenu( "Search" );
            searchMenu.setMnemonic( KeyEvent.VK_S );
            searchMenu.add( multiPanel.getCoverageModel().createMenuItem() );
            getJMenuBar().add( searchMenu );
        }
        
        JMenu regMenu = new JMenu( "Registry" );
        regMenu.add( queryFactory_.getRegistrySelector()
                                  .getRegistryUpdateAction() );
        regMenu.setMnemonic( KeyEvent.VK_R );
        getJMenuBar().add( regMenu );
        JMenu interopMenu =
            RegistryDialogAdjuster
           .createInteropMenu( regPanel_, service.getResourceListType() );
        interopMenu.addSeparator();
        interopMenu.add( acceptResourceModel_.createMenuItem() );
        getJMenuBar().add( interopMenu );

        /* Place toolbar buttons. */
        if ( service.hasCoverages() ) {
            getToolBar().add( multiPanel.getCoverageModel()
                                        .createToolbarButton() );
            getToolBar().addSeparator();
        }

        /* Display something in the registry result table.  Either start
         * a query for all services of the right type, or show a message
         * describing how to use the registry query. */
        if ( autoQuery ) {
            regPanel_.performAutoQuery( "Searching registry for all known "
                                     + servName + " services" );
        }
        else {
            regPanel_.displayAdviceMessage( new String[] {
                "Query registry for " + servName + " services:",
                "enter keywords like \"2mass qso\" and click "
                + regPanel_.getSubmitQueryAction().getValue( Action.NAME )
                + ".",
                " ",
                "Alternatively, enter " + servName + " URL in field below.",
            } );
        }
    }

    /**
     * Takes a list of resource ID values and may load them or a subset
     * into this object's dialogue as appropriate.
     *
     * @param  ivoids  ivo:-type identifier strings
     * @param  msg   text of user-directed message to explain where the
     *         IDs came from
     * @return  true iff at least some of the resources were, or may be,
     *          loaded into this window
     */ 
    public boolean acceptResourceIdList( String[] ivoids, String msg ) {
        if ( acceptResourceModel_.isSelected() && isShowing() ) {
            RegistryQuery query;
            try {
                query = queryFactory_.getIdListQuery( ivoids );
            }
            catch ( MalformedURLException e ) {
                logger_.warning( "Resource ID list not accepted: "
                               + "bad registry endpoint " + e );
                return false;
            }
            if ( query != null ) {
                regPanel_.performQuery( query, msg );
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }
}
