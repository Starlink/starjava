package uk.ac.starlink.topcat.join;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.MalformedURLException;
import java.net.URL;
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
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.AngleColumnConverter;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.RegistryDialogAdjuster;
import uk.ac.starlink.topcat.StiltsAction;
import uk.ac.starlink.topcat.StiltsReporter;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatJELUtils;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatTableNamer;
import uk.ac.starlink.ttools.cone.ConeSearchConer;
import uk.ac.starlink.ttools.task.FilterParameter;
import uk.ac.starlink.ttools.task.InputTableParameter;
import uk.ac.starlink.ttools.task.MultiCone;
import uk.ac.starlink.ttools.task.Setting;
import uk.ac.starlink.ttools.task.SettingGroup;
import uk.ac.starlink.ttools.task.StiltsCommand;
import uk.ac.starlink.vo.Capability;
import uk.ac.starlink.vo.ConeVerbosity;
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
public class DalMultiWindow extends AuxWindow implements StiltsReporter {

    private final DalMultiService service_;
    private final DalMultiPanel multiPanel_;
    private final KeywordServiceQueryFactory queryFactory_;
    private final RegistryPanel regPanel_;
    private final ToggleButtonModel acceptResourceModel_;
    private final ActionForwarder forwarder_;
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
        service_ = service;
        forwarder_ = new ActionForwarder();
        service_.addActionListener( forwarder_ );
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
        multiPanel_ = new DalMultiPanel( service, placeProgressBar() );
        multiPanel_.addActionListener( forwarder_ );
        JComponent versionBox = service.getVersionComponent();
        if ( versionBox != null ) {
            JComponent urlBox = multiPanel_.getServiceUrlBox();
            urlBox.add( Box.createHorizontalStrut( 10 ) );
            urlBox.add( versionBox );
        }

        /* Arrange for the multiquery panel to get updated when a service
         * is selected from the registry panel. */
        ListSelectionListener serviceSelListener = new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                RegCapabilityInterface[] caps =
                    regPanel_.getSelectedCapabilities();
                multiPanel_.setServiceUrl( caps.length == 1
                                               ? caps[ 0 ].getAccessUrl()
                                               : null );
                forwarder_.actionPerformed( new ActionEvent( evt.getSource(),
                                                              0, "service" ) );
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
        multiPanel_.setBorder(
            BorderFactory.createCompoundBorder(
                makeTitledBorder( "Multiple " + servName + " Parameters" ),
                BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
        regPanel_.setPreferredSize( new Dimension( 600, 350 ) );

        /* Place components. */
        JComponent main = getMainArea();
        main.add( regPanel_, BorderLayout.CENTER );
        main.add( multiPanel_, BorderLayout.SOUTH );

        /* Place start and stop actions. */
        JComponent controls = getControlPanel();
        controls.add( new JButton( multiPanel_.getStartAction() ) );
        controls.add( new JButton( multiPanel_.getStopAction() ) );

        /* Place menu actions. */
        JMenu colMenu = regPanel_.makeColumnVisibilityMenu( "Columns" );
        colMenu.setMnemonic( KeyEvent.VK_C );
        getJMenuBar().add( colMenu );
        if ( service.hasCoverages() ) {
            JMenu searchMenu = new JMenu( "Search" );
            searchMenu.setMnemonic( KeyEvent.VK_S );
            searchMenu.add( multiPanel_.getCoverageModel().createMenuItem() );
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
            getToolBar().add( multiPanel_.getCoverageModel()
                                         .createToolbarButton() );
        }
        getToolBar().add( new StiltsAction( this, () -> this ) );
        getToolBar().addSeparator();

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

    public void addStiltsListener( ActionListener listener ) {
        forwarder_.addActionListener( listener );
    }

    public void removeStiltsListener( ActionListener listener ) {
        forwarder_.removeActionListener( listener );
    }

    public StiltsCommand createStiltsCommand( TopcatTableNamer tnamer ) {
        MultiCone task = new MultiCone();
        List<SettingGroup> groups = new ArrayList<>();
        TopcatModel tcModel = multiPanel_.getTopcatModel();
        URL serviceUrl = multiPanel_.getServiceUrl();
        ConeSearchConer coner = task.getConer();
        if ( tcModel == null || serviceUrl == null ) {
            return null;
        }
        List<Setting> tableSettings = new ArrayList<>();
        InputTableParameter inTableParam = task.getInputTableParameter();
        FilterParameter inFilterParam = task.getInputFilterParameter();
        tableSettings.addAll(
            tnamer.createInputTableSettings( inTableParam, inFilterParam,
                                             tcModel )
        );
        tableSettings.add( StiltsCommand
                          .createProgressSetting( inFilterParam ) );
        groups.add( new SettingGroup( 1, tableSettings
                                        .toArray( new Setting[ 0 ] ) ) );
        Setting[] serviceSettings = new Setting[] {
            pset( coner.getServiceTypeParameter(), service_.getServiceType() ),
            pset( coner.getServiceUrlParameter(), serviceUrl ),
        };
        groups.add( new SettingGroup( 1, serviceSettings ) );
        AngleColumnConverter.Unit degreeUnit = AngleColumnConverter.Unit.DEGREE;
        Setting[] posSettings = new Setting[] {
            pset( task.getRaParameter(),
                  TopcatJELUtils
                 .getAngleExpression( tcModel, multiPanel_.getRaSelector(),
                                      degreeUnit ) ),
            pset( task.getDecParameter(),
                  TopcatJELUtils
                 .getAngleExpression( tcModel, multiPanel_.getDecSelector(),
                                      degreeUnit ) ),
            pset( task.getRadiusDegreeParameter(),
                  TopcatJELUtils
                 .getAngleExpression( tcModel, multiPanel_.getRadiusSelector(),
                                      degreeUnit ) ),
        };
        groups.add( new SettingGroup( 1, posSettings ) );
        List<Setting> optSettings = new ArrayList<Setting>();
        optSettings.add( pset( task.getFindModeParameter(),
                               multiPanel_.getConeFindMode() ) );
        optSettings.add( pset( task.getConeErrorPolicyParameter(),
                               multiPanel_.getConeErrorPolicy() ) );
        ConeVerbosity verb = service_.getVerbosity();
        if ( verb != null ) {
            optSettings.add( pset( coner.getVerbosityParameter(),
                                   Integer.toString( verb.getLevel() ) ) );
        }
        if ( service_.hasCoverages() ) {
            optSettings.add( pset( task.getUseCoverageParameter(),
                                   Boolean.valueOf( multiPanel_
                                                   .getCoverageModel()
                                                   .isSelected() ) ) );
        }
        optSettings.add( pset( task.getParallelParameter(),
                               Integer.valueOf( multiPanel_
                                               .getParallelism() ) ) );
        groups.add( new SettingGroup( 1, optSettings
                                        .toArray( new Setting[ 0 ] ) ) );
        return StiltsCommand
              .createCommand( task, groups.toArray( new SettingGroup[ 0 ] ) );
    }
}
