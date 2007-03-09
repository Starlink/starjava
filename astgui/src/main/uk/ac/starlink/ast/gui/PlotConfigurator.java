/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     25-SEP-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.w3c.dom.Element;

import uk.ac.starlink.ast.Plot;                   // For documentation
import uk.ac.starlink.util.images.ImageHolder;
import uk.ac.starlink.util.gui.StoreControlFrame;
import uk.ac.starlink.util.gui.StoreSource;

/**
 * PlotConfigurator creates a dialog window for controlling the
 * configuration of an {@link Plot}.
 * <p>
 * The configuration controls are presented as a serious of tabbed
 * panes. Each of these panes containing a set of "related" controls
 * (such as controls for setting the Plot title, this pane contains
 * a text area, plus font and colour selection).
 * <p>
 * Each pane of related controls has a data model that contains the
 * state. These can be used to save and restore the complete
 * configuration to some XML database.
 * <p>
 * When closed this window will be hidden, not disposed. If this
 * therefore necessary that the user disposes of it when it is really
 * no longer required.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PlotConfigurator
    extends JFrame
    implements StoreSource, ChangeListener
{
    /**
     * Content pane of frame.
     */
    protected JPanel contentPane = null;

    /**
     * Action buttons container.
     */
    protected JPanel actionBar = new JPanel();

    /**
     * Tabbed pane.
     */
    protected JTabbedPane tabbedPane = new JTabbedPane();

    /**
     *  Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();

    protected JMenu fileMenu = new JMenu();
    protected JMenuItem saveConfigFileMenu = new JMenuItem();
    protected JMenuItem drawFileMenu = new JMenuItem();
    protected JMenuItem closeFileMenu = new JMenuItem();

    protected JMenu optionsMenu = new JMenu();
    protected JMenuItem saveConfigOptionsMenu = new JMenuItem();
    protected JMenuItem autoDrawOptionsMenu =
        new JCheckBoxMenuItem( "Auto-update" );

    /**
     * Graphics configuration object.
     */
    protected PlotConfiguration config = null;

    /**
     * PlotController. Used to send control instructions to the Plot
     * we're configuring.
     */
    protected PlotController controller;

    /**
     * Graphics configuration store window.
     */
    protected StoreControlFrame storeControl = null;

    /**
     * The default configuration of all known elements.
     */
    protected Element defaultConfig = null;

    /**
     * Default title for window: "Configure AST graphics options".
     */
    protected static String defaultTitle = "Configure AST graphics options";

    /**
     * Application name for the store to use.
     */
    protected String applicationName = "astgui";

    /**
     * Name for the store file.
     */
    protected String storeName = "plot-configs.xml";

    /**
     * Title for window.
     */
    protected String title = defaultTitle;

    /**
     * List of PlotControls that have been added.
     */
    protected ArrayList controlsList = new ArrayList( 10 );

    /**
     * Create an instance. This has the default title and does not
     * attempt to control a Plot.
     */
    public PlotConfigurator()
    {
        this( null, null );
    }

    /**
     * Create an instance, setting the window title.
     */
    public PlotConfigurator( String title )
    {
        this( title, null );
    }

    /**
     * Create an instance that controls a Plot and has a given window
     * title.
     */
    public PlotConfigurator( String title, PlotController controller )
    {
        this( title, controller, null, null, null );
    }

    /**
     * Create an instance that controls a Plot, has a given window
     * title and uses a pre-defined configuration. Also change the
     * default names used for the backing store.
     */
    public PlotConfigurator( String title, PlotController controller,
                             PlotConfiguration config,
                             String applicationName, String storeName )
    {
        if ( title == null ) {
            this.title = defaultTitle;
        }
        else {
            this.title = title;
        }
        this.controller = controller;
        if ( config == null ) {
            this.config = new PlotConfiguration();
        }
        else {
            this.config = config;
        }
        if ( applicationName != null ) {
            this.applicationName = applicationName;
        }
        if ( storeName != null ) {
            this.storeName = storeName;
        }

        contentPane = (JPanel) getContentPane();
        initTabbedPane();
        initMenus();
        initFrame();
    }

    /**
     * Initialise the tabbed pane of controls.
     */
    protected void initTabbedPane()
    {
        //  Setup the tabbedPane.
        tabbedPane.setTabPlacement( JTabbedPane.TOP );
        tabbedPane.setToolTipText( "Ast graphics configurator window" );

        //  Add the default controls.
        addDefaultControls();
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( title );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        contentPane.add( tabbedPane, BorderLayout.CENTER );
        contentPane.add( actionBar, BorderLayout.SOUTH );
        setSize( new Dimension( 400, 750 ) );
        setVisible( true );
    }

    /**
     * Initialise the menu bar, action bar and related actions.
     */
    protected void initMenus()
    {
        //  Add the menuBar.
        setJMenuBar( menuBar );

        //  Action bar uses two BoxLayouts.
        JPanel topAction = new JPanel();
        JPanel botAction = new JPanel();
        topAction.setLayout( new BoxLayout( topAction, BoxLayout.X_AXIS ) );
        botAction.setLayout( new BoxLayout( botAction, BoxLayout.X_AXIS ) );
        topAction.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
        botAction.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
        actionBar.setLayout( new BorderLayout() );
        actionBar.add( topAction, BorderLayout.NORTH );
        actionBar.add( botAction, BorderLayout.SOUTH );
        actionBar.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );

        //  Get toolbar icons.
        ImageIcon drawImage = new ImageIcon(
            ImageHolder.class.getResource( "accept.gif" ) );
        ImageIcon resetImage = new ImageIcon(
            ImageHolder.class.getResource( "reset.gif" ) );
        ImageIcon closeImage = new ImageIcon(
            ImageHolder.class.getResource( "exit.gif" ) );
        ImageIcon configImage = new ImageIcon(
            ImageHolder.class.getResource( "config.gif" ) );

        //  Create the File menu.
        fileMenu.setText( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Create the Options menu.
        optionsMenu.setText( "Options" );
        optionsMenu.setMnemonic( KeyEvent.VK_O );
        menuBar.add( optionsMenu );

        //  Add action to draw the AST description.
        DrawAction drawAction = new DrawAction( "Draw", drawImage );
        fileMenu.add( drawAction ).setMnemonic( KeyEvent.VK_D );

        JButton drawButton = new JButton( drawAction );
        topAction.add( Box.createGlue() );
        topAction.add( drawButton );
        drawButton.setToolTipText
            ( "Re-draw graphics using the current configuration" );

        //  Add action to reset all values.
        ResetAction resetAction = new ResetAction( "Reset", resetImage );
        fileMenu.add( resetAction ).setMnemonic( KeyEvent.VK_R );

        JButton resetButton = new JButton( resetAction );
        topAction.add( Box.createGlue() );
        topAction.add( resetButton );
        resetButton.setToolTipText( "Reset all values to defaults" );

        //  Add action to create the configuration storage window.
        StoreAction storeAction = new StoreAction( "Store/restore",
                                                   configImage );
        optionsMenu.add( storeAction ).setMnemonic( KeyEvent.VK_S );

        JButton storeButton = new JButton( storeAction );
        topAction.add( Box.createGlue() );
        topAction.add( storeButton );
        storeButton.setToolTipText
            ( "Open window to store and restore configurations" );

        //  Add action to toggle auto-updating of the configuration.
        optionsMenu.add( autoDrawOptionsMenu );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

        JButton closeButton = new JButton( closeAction );
        botAction.add( Box.createGlue() );
        botAction.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        topAction.add( Box.createGlue() );
        botAction.add( Box.createGlue() );
    }

    /**
     * Get the complete AST description of all default components.
     */
    public String getAst()
    {
        return config.getAst();
    }

    /**
     * Get the PlotConfiguration object used to store the actual state.
     */
    public PlotConfiguration getConfiguration()
    {
        return config;
    }

    /**
     * Set whether any changes are applied immediately or only when "Draw" is
     * pressed.
     */
    public void setAutoDrawOption( boolean state )
    {
        if ( autoDrawOptionsMenu.isSelected() != state ) {
            autoDrawOptionsMenu.setSelected( state );
        }
    }

    /**
     * Get whether any changes are to be applied immediately or only when
     * "Draw" is pressed.
     */
    public boolean isAutoDrawOption()
    {
        return autoDrawOptionsMenu.isSelected();
    }

    /**
     * Add the default list of controls currently provided by the
     * PlotConfiguration.
     */
    protected void addDefaultControls()
    {
        addTitle();
        addAxisLabels();
        addNumberLabels();
        addAxes();
        addGrid();
        addBorder();
        addTicks();
        addText();
    }

    public void addTitle()
    {
        TitleControls titlePanel = new TitleControls
            ( config.getControlsModel
              ( TitleControls.getControlsModelClass() ) );
        addControls( titlePanel, true );
    }
    protected void addAxisLabels()
    {
        AxisLabelControls axisLabelsPanel = new AxisLabelControls
            ( config.getControlsModel
              ( AxisLabelControls.getControlsModelClass() ) );
        addControls( axisLabelsPanel, true );
    }
    protected void addNumberLabels()
    {
        AxisNumLabControls axisNumbersPanel = new AxisNumLabControls
            ( config.getControlsModel
              ( AxisNumLabControls.getControlsModelClass() ) );
        addControls( axisNumbersPanel, true );
    }
    protected void addGrid()
    {
        GridControls gridPanel = new GridControls
            ( config.getControlsModel(GridControls.getControlsModelClass()));
        addControls( gridPanel, true );
    }
    protected void addAxes()
    {
        AxesControls axesPanel = new AxesControls
            ( config.getControlsModel
              ( AxesControls.getControlsModelClass()), controller );
        addControls( axesPanel, true );
    }
    protected void addBorder()
    {
        BorderControls borderPanel = new BorderControls
            ( config.getControlsModel
              ( BorderControls.getControlsModelClass() ) );
        addControls( borderPanel, true );
    }
    protected void addTicks()
    {
        TickControls ticksPanel = new TickControls
            ( config.getControlsModel
              ( TickControls.getControlsModelClass() ), controller );
        addControls( ticksPanel, true );
    }
    public void addText()
    {
        StringsControls stringsPanel = new StringsControls
            ( config.getControlsModel
              ( StringsControls.getControlsModelClass() ) );
        addControls( stringsPanel, true );
    }

    /**
     * Add a set of extra controls to the tabbed pane.
     *
     * @param controls the controls (a JComponent that implements the
     *                 PlotControls interface).
     * @param append whether to append the controls to the tabbed pane
     *               list.
     */
    public void addExtraControls( PlotControls controls, boolean append )
    {
        addControls( controls, append );
    }

    /**
     * Add a "page" of controls to the tabbed pane.
     */
    protected void addControls( PlotControls controls, boolean append )
    {
        JComponent controlsUI = controls.getControlsComponent();
        controlsUI.setBorder( new TitledBorder(controls.getControlsTitle()) );
        if ( append ) {
            tabbedPane.add( controls.getControlsName(), controlsUI );
        }
        else {
            tabbedPane.add( controlsUI, controls.getControlsName(), 0 );
        }
        controls.getControlsModel().addChangeListener( this );
        controlsList.add( controls );
    }

    /**
     * Reveal a page of controls.
     */
    public void reveal( int page )
    {
        try {
            tabbedPane.setSelectedIndex( page );
        }
        catch (IndexOutOfBoundsException e) {
            // Don't care, so do nothing.
        }
    }

    /**
     *  Close the window.
     */
    protected void closeWindowEvent()
    {
        this.dispose();

        //  Also close the configuration store window.
        if ( storeControl != null ) {
            storeControl.dispose();
        }
    }

    /**
     * Force a send of the current configuration to the associated
     * plot, thus hopefully getting it to re-draw itself.
     */
    public void update()
    {
        controller.updatePlot();
    }

    /**
     * Reset everything to the default values.
     */
    public void reset()
    {
        for( int i = 0; i < controlsList.size(); i++ ) {
            ((PlotControls) controlsList.get( i )).reset();
        }
        if ( defaultConfig != null ) {
            config.decode( defaultConfig );
        }
    }

    //
    // StoreSource interface.
    //

    public void saveState( Element rootElement )
    {
        config.encode( rootElement );
    }

    public void restoreState( Element rootElement )
    {
        defaultConfig = rootElement;
        config.decode( rootElement );
    }

    public String getApplicationName()
    {
        return applicationName;
    }

    public String getStoreName()
    {
        return storeName;
    }

    public String getTagName()
    {
        return config.getTagName();
    }

    /**
     * Draw action. Applies the current configuration to the
     * associated plot.
     */
    protected class DrawAction extends AbstractAction
    {
        public DrawAction( String name, Icon icon ) {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control D" ) );
        }
        public void actionPerformed( ActionEvent ae ) {
            update();
        }
    }

    /**
     * Inner class defining Action for closing window.
     */
    protected class CloseAction extends AbstractAction
    {
        public CloseAction( String name, Icon icon ) {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control W" ) );
        }
        public void actionPerformed( ActionEvent ae ) {
            closeWindowEvent();
        }
    }

    /**
     * Inner class defining action for resetting all values.
     */
    protected class ResetAction extends AbstractAction
    {
        public ResetAction( String name, Icon icon ) {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control R" ) );
        }
        public void actionPerformed( ActionEvent ae ) {
            reset();
        }
    }

    /**
     * Inner class defining action for initiating the configuration
     * storage window.
     */
    protected class StoreAction extends AbstractAction
    {
        public StoreAction( String name, Icon icon ) {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control S" ) );
        }
        public void actionPerformed( ActionEvent ae ) {
            openStoreWindow();
        }
    }

    /**
     * Create or open the configuration storage window.
     */
    public void openStoreWindow()
    {
        if ( storeControl == null ) {
            storeControl = new StoreControlFrame( this );

            //  We'd like to know if the window is closed.
            storeControl.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        storeControlClosed();
                    }
                });
        }
        else {
            storeControl.setVisible( true );
        }
    }

    /**
     *  Configuration storage window is closed.
     */
    protected void storeControlClosed()
    {
        // Nullify if method for closing switches to dispose.
        // storeControl = null;
    }

    /**
     *  Close the config store window.
     */
    protected void closeStoreConfigFrame()
    {
        if ( storeControl != null ) {
            storeControl.dispose();
            storeControl = null;
        }
    }

    //
    // Implement the ChangeListener interface. Used to auto-draw any
    // changes to the plot.
    //
    public void stateChanged( ChangeEvent e )
    {
        if ( autoDrawOptionsMenu.isSelected() ) {
            update();
        }
    }
}
