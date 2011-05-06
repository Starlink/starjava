package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.net.MalformedURLException;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import uk.ac.starlink.table.gui.AbstractTableLoadDialog;

/**
 * Partial implementation of a table load dialogue which has two parts:
 * first a query on the registry to locate a set of suitable services,
 * and then a query to one of the services selected from that list.
 * Concrete subclasses should populate the control box 
 * {@link #getControlBox} with service-specific controls and implement
 * the abstract 
 * {@link uk.ac.starlink.table.gui.TableLoadDialog#createTableLoader}
 * method appropriately.
 *
 * @author   Mark Taylor (Starlink)
 * @since    6 Jan 2005
 */
public abstract class RegistryServiceTableLoadDialog 
                      extends AbstractTableLoadDialog {

    private final String protoName_;
    private final RegistryQueryFactory queryFactory_;
    private final boolean showCapabilities_;
    private JComponent controlBox_;
    private RegistryPanel regPanel_;
    private static final Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param  name  dialogue name
     * @param  protoName   short name (perhaps acronym) for protocol
     * @param  description  dialogue description
     * @param  queryFactory  source of registry query definition
     * @param  showCapabilities  true to display the capabilities JTable as
     *         well as the Resource one; sensible if resource:capabilities
     *         relationship may not be 1:1
     */
    public RegistryServiceTableLoadDialog( String name, String protoName,
                                           String description,
                                           RegistryQueryFactory queryFactory,
                                           boolean showCapabilities ) {
        super( name, description );
        protoName_ = protoName;
        queryFactory_ = queryFactory;
        showCapabilities_ = showCapabilities;
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
        if ( isComponentShowing() ) {
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
                getRegistryPanel().performQuery( query, msg ); 
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

    protected Component createQueryComponent() {
        JPanel queryPanel = new JPanel( new BorderLayout() ) {
            public void setEnabled( boolean enabled ) {
                super.setEnabled( enabled );
                regPanel_.setEnabled( enabled );
            }
        };

        /* Construct and configure a panel which knows how to query the
         * registry and display the result. */
        regPanel_ = new RegistryPanel( queryFactory_, showCapabilities_ ) {
            public RegCapabilityInterface[] getCapabilities( RegResource res ) {
                return RegistryServiceTableLoadDialog.this
                      .getCapabilities( res );
            }
        };
        regPanel_.getResourceSelectionModel()
                 .setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        queryPanel.add( regPanel_, BorderLayout.CENTER );

        /* Add controls for invoking the selected service. */
        controlBox_ = Box.createVerticalBox();
        queryPanel.add( controlBox_, BorderLayout.SOUTH );

        /* Initiate default query if appropriate. */
        if ( queryFactory_.getComponent() == null ) {
            String msg = "Searching registry for " + protoName_ + " services";
            regPanel_.performAutoQuery( msg );
        }

        /* Menus. */
        JMenu metaMenu = regPanel_.makeColumnVisibilityMenu( "Columns" );
        metaMenu.setMnemonic( KeyEvent.VK_C );
        setMenus( new JMenu[] { metaMenu } );

        /* Cosmetics. */
        Border lineBorder = BorderFactory.createLineBorder( Color.BLACK );
        Border gapBorder = BorderFactory.createEmptyBorder( 5, 5, 5, 5 );
        Border etchedBorder = BorderFactory.createEtchedBorder();
        regPanel_.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder( lineBorder, gapBorder ),
                "Available " + protoName_ + " Services" ) );
        controlBox_.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder( lineBorder, gapBorder ),
                protoName_ + " Parameters" ) );
        queryPanel.setBorder( gapBorder );
        Dimension prefSize = new Dimension( 600, 400 );
        if ( showCapabilities_ ) {
            prefSize.height += 70;
        }
        if ( queryFactory_.getComponent() != null ) {
            prefSize.height += 100;
        }
        queryPanel.setPreferredSize( prefSize );
        return queryPanel;
    }

    /**
     * Returns the component within which service-specific components should
     * be placed.
     * Will return null if called before {@link #createQueryComponent}.
     *
     * @return  control box
     */
    protected JComponent getControlBox() {
        return controlBox_;
    }

    /**
     * Returns the registry panel for this dialogue.
     * Will return null if called before {@link #createQueryComponent}.
     *
     * @return  registry panel
     */
    public RegistryPanel getRegistryPanel() {
        return regPanel_;
    }

    /**
     * Returns the capabilities associated with a given resource.
     * This determines those capabilities which will be displayed and
     * selecatable for each resource.  The default implementation is to
     * include all capabilities; this may however be overridded in a more
     * selective way by subclasses.
     *
     * @param  resource  registry resource
     * @return   relevant capabilities from that resource
     */
    public RegCapabilityInterface[] getCapabilities( RegResource resource ) {
        return resource.getCapabilities();
    }

    /**
     * Returns the query factory used by this dialogue.
     *
     * @return   query factory
     */
    public RegistryQueryFactory getQueryFactory() {
        return queryFactory_;
    }
}
