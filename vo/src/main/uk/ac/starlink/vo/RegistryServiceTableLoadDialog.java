package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.table.gui.BasicTableLoadDialog;

/**
 * Partial implementation of a table load dialogue which has two parts:
 * first a query on the registry to locate a set of suitable services,
 * and then a query to one of the services selected from that list.
 * Concrete subclasses should populate the control box 
 * {@link #getControlBox} with service-specific controls and implement
 * the abstract 
 * {@link uk.ac.starlink.table.gui.BasicTableLoadDialog#getTableSupplier}
 * method appropriately.
 *
 * @author   Mark Taylor (Starlink)
 * @since    6 Jan 2005
 */
public abstract class RegistryServiceTableLoadDialog 
                      extends BasicTableLoadDialog {

    private final JComponent controlBox_;
    private final RegistryPanel regPanel_;
    private final String name_;
    private final RegistryQueryFactory queryFactory_;
    private boolean setup_;
    private static final Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param  name  dialogue name
     * @param  description  dialogue description
     * @param  queryFactory  source of registry query definition
     * @param  showCapabilities  true to display the capabilities JTable as
     *         well as the Resource one; sensible if resource:capabilities
     *         relationship may not be 1:1
     */
    public RegistryServiceTableLoadDialog( String name, String description,
                                           RegistryQueryFactory queryFactory,
                                           boolean showCapabilities ) {
        super( name, description );
        name_ = name;
        queryFactory_ = queryFactory;
        final Action okAction = getOkAction();
        okAction.setEnabled( false );
        setLayout( new BorderLayout() );

        /* Construct and configure a panel which knows how to query the
         * registry and display the result. */
        regPanel_ = new RegistryPanel( queryFactory, showCapabilities ) {
            public RegCapabilityInterface[] getCapabilities( RegResource res ) {
                return RegistryServiceTableLoadDialog.this
                      .getCapabilities( res );
            }
        };
        regPanel_.getResourceSelectionModel()
                 .setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        regPanel_.getResourceSelectionModel()
                 .addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                okAction.setEnabled( regPanel_.getSelectedResources()
                                              .length == 1 );
            }
        } );
        add( regPanel_, BorderLayout.CENTER );

        /* Add controls for invoking the selected service. */
        controlBox_ = Box.createVerticalBox();
        add( controlBox_, BorderLayout.SOUTH );

        /* Cosmetics. */
        Border lineBorder = BorderFactory.createLineBorder( Color.BLACK );
        Border gapBorder = BorderFactory.createEmptyBorder( 5, 5, 5, 5 );
        Border etchedBorder = BorderFactory.createEtchedBorder();
        regPanel_.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder( lineBorder, gapBorder ),
                "Available " + name + " Services" ) );
        controlBox_.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder( lineBorder, gapBorder ),
                name + " Parameters" ) );
        setBorder( gapBorder );
        setPreferredSize( new Dimension( 600, 450 ) );
    }

    public boolean isAvailable() {
        return true;
    }

    /**
     * Returns the component within which service-specific components should
     * be placed.
     *
     * @return  control box
     */
    protected JComponent getControlBox() {
        return controlBox_;
    }

    /**
     * Returns the registry panel for this dialogue.
     *
     * @return  registry panel
     */
    protected RegistryPanel getRegistryPanel() {
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

    protected JDialog createDialog( Component parent ) {

        /* Do one-time setup.  Doing it in the constructor is too early. */
        if ( ! setup_ ) {
            setup_ = true;
            if ( queryFactory_.getComponent() == null ) {
                String msg = "Searching registry for " + name_ + " services";
                regPanel_.performAutoQuery( msg );
            }
        }

        /* Embellish the dialogue with a menu allowing selection of which
         * columns are visible in the displayed registry table. */
        JDialog dia = super.createDialog( parent );
        if ( dia.getJMenuBar() == null ) {
            dia.setJMenuBar( new JMenuBar() );
        }
        JMenu metaMenu = regPanel_.makeColumnVisibilityMenu( "Columns" );
        metaMenu.setMnemonic( KeyEvent.VK_C );
        dia.getJMenuBar().add( metaMenu );
        return dia;
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        regPanel_.setEnabled( enabled );
    }
}
