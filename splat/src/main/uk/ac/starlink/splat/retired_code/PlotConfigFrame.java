package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.Utilities;

/**
 * PlotConfigFrame creates a dialog window for setting all the known
 * Plot configuration items. Each type of item is defined by an either
 * an AST or bespoke model that populates a pane of the main tabbed
 * area. Methods are provided for creating this window, removing it
 * and accessing the current state as an AST description for
 * configuring the main plot.
 * <p>
 * When closed this window will be hidden, not disposed. If this
 * therefore necessary that the user disposes of it when it is really
 * no longer required.
 *
 * @since $Date$
 * @since 06-NOV-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 */
public class PlotConfigFrame
    extends JFrame
    implements ChangeListener
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
    protected JMenuItem applyFileMenu = new JMenuItem();
    protected JMenuItem closeFileMenu = new JMenuItem();

    protected JMenu optionsMenu = new JMenu();
    protected JMenuItem saveConfigOptionsMenu = new JMenuItem();
    protected JMenuItem autoApplyOptionsMenu = 
        new JCheckBoxMenuItem( "Auto-update" );

    protected JMenu helpMenu = new JMenu();
    protected JMenuItem helpMenuAbout = new JMenuItem();

    /**
     * Graphics configuration object.
     */
    protected PlotConfig config = null;

    /**
     * Graphics configuration store window.
     */
    protected PlotConfigStoreFrame configStore = null;

    /**
     * The Plot that we're configuring.
     */
    protected PlotControl plot = null;

    // Control panels.
    protected ComponentColourControls colourPanel = null;
    protected DataLimitControls limitsPanel = null;
    protected TitleControls titlePanel = null;
    protected AxisLabelControls axisLabelsPanel = null;
    protected AxisNumLabControls axisNumbersPanel = null;
    protected GridControls gridPanel = null;
    protected BorderControls borderPanel = null;
    protected TickControls ticksPanel = null;
    protected GraphicsHintsControls hintsPanel = null;
    protected GraphicsEdgesControls edgesPanel = null;

    /**
     * Create an instance.
     */
    public PlotConfigFrame( PlotConfig config, PlotControl plot )
    {
        this.config = config;
        this.plot = plot;
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
        tabbedPane.setToolTipText( "Plot configurator window" );

        //  Add data limits controls.
        addDataLimits();

        //  Add title controls.
        addTitle();

        //  Add axes labels controls.
        addAxisLabels();

        //  Add number labels controls.
        addNumberLabels();

        //  Add grid controls.
        addGrid();

        //  Add border controls.
        addBorder();

        //  Add tick controls.
        addTicks();

        //  Add graphics rendering hints controls.
        addGraphicsHints();

        //  Add graphics edges controls.
        addGraphicsEdges();

        //  Add controls for setting the background colour.
        addBackgroundColour();
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Configure Plot Options" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        contentPane.add( tabbedPane, BorderLayout.CENTER );
        contentPane.add( actionBar, BorderLayout.SOUTH );
        setSize( new Dimension( 400, 700 ) );
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
        ImageIcon applyImage = new ImageIcon(
            ImageHolder.class.getResource( "accept.gif" ) );
        ImageIcon resetImage = new ImageIcon(
            ImageHolder.class.getResource( "reset.gif" ) );
        ImageIcon closeImage = new ImageIcon(
            ImageHolder.class.getResource( "exit.gif" ) );
        ImageIcon helpImage = new ImageIcon(
            ImageHolder.class.getResource( "help.gif" ) );
        ImageIcon configImage = new ImageIcon(
            ImageHolder.class.getResource( "config.gif" ) );

        //  Create the File menu.
        fileMenu.setText( "File" );
        menuBar.add( fileMenu );

        //  Create the Options menu.
        optionsMenu.setText( "Options" );
        menuBar.add( optionsMenu );

        //  Add action to apply the AST description.
        ApplyAction applyAction = new ApplyAction( "Apply", applyImage );
        fileMenu.add( applyAction );
        JButton applyButton = new JButton( applyAction );
        topAction.add( Box.createGlue() );
        topAction.add( applyButton );
        applyButton.setToolTipText( "Apply the current setup to the plot" );

        //  Add action to reset all values.
        ResetAction resetAction = new ResetAction( "Reset", resetImage );
        fileMenu.add( resetAction );
        JButton resetButton = new JButton( resetAction );
        topAction.add( Box.createGlue() );
        topAction.add( resetButton );
        resetButton.setToolTipText( "Reset all values to defaults" );

        //  Add action to create the configuration storage window.
        StoreAction storeAction = new StoreAction( "(Re)Store",
                                                   configImage );
        optionsMenu.add( storeAction );
        JButton storeButton = new JButton( storeAction );
        topAction.add( Box.createGlue() );
        topAction.add( storeButton );
        storeButton.setToolTipText
            ( "Open window to store and restore configurations" );

        //  Add action to toggle auto-updating of the configuration.
        optionsMenu.add( autoApplyOptionsMenu );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction );
        JButton closeButton = new JButton( closeAction );
        botAction.add( Box.createGlue() );
        botAction.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        topAction.add( Box.createGlue() );
        botAction.add( Box.createGlue() );

        //  Add the help menu.
	HelpFrame.createHelpMenu( "config-window", "Help on window",
                                  menuBar, null );
    }

    /**
     * Get the complete AST description of all components.
     */
    public String getAst()
    {
        return config.getAst();
    }

    /**
     * Get the PlotConfig object used to store the actual state.
     */
    public PlotConfig getConfig()
    {
        return config;
    }

    /**
     * Add the data limits display page.
     */
    protected void addDataLimits()
    {
        limitsPanel = new DataLimitControls( config.getDataLimits(), plot );
        limitsPanel.setBorder( new TitledBorder( "Axis Data Limits:" ) );
        tabbedPane.add( limitsPanel, "Limits" );

        //  Get informed of any changes that we may want to apply
        //  automatically.
        config.getDataLimits().addChangeListener( this );
   }

    /**
     * Add the title display page.
     */
    protected void addTitle()
    {
        titlePanel = new TitleControls( config.getAstTitle() );
        titlePanel.setBorder( new TitledBorder( "Title Properties:" ) );
        tabbedPane.add( titlePanel, "Title" );

        //  Get informed of any changes that we may want to apply
        //  automatically.
        config.getAstTitle().addChangeListener( this );
    }

    /**
     * Add the axis label display page.
     */
    protected void addAxisLabels()
    {
        axisLabelsPanel = new AxisLabelControls( config.getAstAxisLabels() );
        axisLabelsPanel.setBorder(
            new TitledBorder( "Axis Labels Properties:" ) );
        tabbedPane.add( axisLabelsPanel, "Axis Labels" );

        //  Get informed of any changes that we may want to apply
        //  automatically.
        config.getAstAxisLabels().addChangeListener( this );
    }

    /**
     * Add the axis number labels display page.
     */
    protected void addNumberLabels()
    {
        axisNumbersPanel =
            new AxisNumLabControls( config.getAstNumberLabels() );
        axisNumbersPanel.setBorder(
            new TitledBorder( "Axis Number Label Properties:" ) );
        tabbedPane.add( axisNumbersPanel, "Axis Numbers" );

        //  Get informed of any changes that we may want to apply
        //  automatically.
        config.getAstNumberLabels().addChangeListener( this );
    }

    /**
     * Add the grid properties display page.
     */
    protected void addGrid()
    {
        gridPanel = new GridControls( config.getAstGrid() );
        gridPanel.setBorder( new TitledBorder( "Grid Line Properties:" ) );
        tabbedPane.add( gridPanel, "Grid Lines" );

        //  Get informed of any changes that we may want to apply
        //  automatically.
        config.getAstGrid().addChangeListener( this );
    }

    /**
     * Add the border properties display page.
     */
    protected void addBorder()
    {
        borderPanel = new BorderControls( config.getAstBorder() );
        borderPanel.setBorder( new TitledBorder( "Border Line Properties:" ) );
        tabbedPane.add( borderPanel, "Border Lines" );

        //  Get informed of any changes that we may want to apply
        //  automatically.
        config.getAstBorder().addChangeListener( this );
    }

    /**
     * Add the tick properties display page.
     */
    protected void addTicks()
    {
        ticksPanel = new TickControls( config.getAstTicks() );
        ticksPanel.setBorder( new TitledBorder( "Tick Properties:" ) );
        tabbedPane.add( ticksPanel, "Ticks" );

        //  Get informed of any changes that we may want to apply
        //  automatically.
        config.getAstTicks().addChangeListener( this );
    }

    /**
     * Add the graphics rendering hints page.
     */
    protected void addGraphicsHints()
    {
        hintsPanel = new GraphicsHintsControls( config.getGraphicsHints() );
        hintsPanel.setBorder( new TitledBorder( "Graphics Rendering Hints:" ) );
        tabbedPane.add( hintsPanel, "Rendering" );

        //  Get informed of any changes that we may want to apply
        //  automatically.
        config.getGraphicsHints().addChangeListener( this );
    }

    /**
     * Add the graphics edge drawing page.
     */
    protected void addGraphicsEdges()
    {
        edgesPanel = new GraphicsEdgesControls( config.getGraphicsEdges() );
        edgesPanel.setBorder( new TitledBorder( "Edge Drawing Properties:" ) );
        tabbedPane.add( edgesPanel, "Edges" );

        //  Get informed of any changes that we may want to apply
        //  automatically.
        config.getGraphicsEdges().addChangeListener( this );
    }

    /**
     * Add controls for setting the background colour.
     */
    protected void addBackgroundColour()
    {
        colourPanel =
            new ComponentColourControls( plot.getPlot(),
                                         config.getBackgroundColour(),
                                         "Background", "Colour:" );
        colourPanel.setBorder
            ( new TitledBorder( "Plot background:" ) );
        tabbedPane.add( colourPanel, "Background" );

        //  Get informed of any changes that we may want to apply
        //  automatically.
        config.getBackgroundColour().addChangeListener( this );
    }

    /**
     * Apply the current configuration to the associated plot.
     */
    public void applyConfiguration()
    {
        plot.updatePlot();
    }

    /**
     *  Close the window.
     */
    protected void closeWindowEvent()
    {
        this.dispose();

        //  Also close the configuration store window.
        if ( configStore != null ) {
            configStore.dispose();
        }
    }

    /**
     * Reset everything to the default values.
     */
    protected void resetActionEvent()
    {
        limitsPanel.reset();
        titlePanel.reset();
        axisLabelsPanel.reset();
        axisNumbersPanel.reset();
        gridPanel.reset();
        borderPanel.reset();
        ticksPanel.reset();
        hintsPanel.reset();
        edgesPanel.reset();
        colourPanel.reset();
    }

    /**
     * Apply action. Applies the current configuration to the
     * associated plot.
     */
    protected class ApplyAction extends AbstractAction
    {
        public ApplyAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            applyConfiguration();
        }
    }

    /**
     * Inner class defining Action for closing window.
     */
    protected class CloseAction extends AbstractAction
    {
        public CloseAction( String name, Icon icon ) {
            super( name, icon );
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
        }
        public void actionPerformed( ActionEvent ae ) {
            resetActionEvent();
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
        if ( configStore == null ) {
            configStore = new PlotConfigStoreFrame( this );

            //  We'd like to know if the window is closed.
            configStore.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        configStoreClosed();
                    }
                });
        } else {
            configStore.setVisible( true );
        }
    }

    /**
     *  Configuration storage window is closed.
     */
    protected void configStoreClosed()
    {
        // Nullify if method for closing switches to dispose.
        // configStore = null;
    }

    /**
     *  Close the config store window.
     */
    protected void closeConfigStoreFrame()
    {
        if ( configStore != null ) {
            configStore.dispose();
            configStore = null;
        }
    }

//
// Implement the ChangeListener interface. Used to auto-apply any
// changes to the plot.
//
    public void stateChanged( ChangeEvent e )
    {
        if ( autoApplyOptionsMenu.isSelected() ) {
            applyConfiguration();
        } 
    }
}
