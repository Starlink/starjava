/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *    07-JUL-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.ast.gui.ScientificFormat;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.util.gui.GridBagLayouter;
import uk.ac.starlink.splat.util.GraphicFileUtilities;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;

/**
 * Provides controls for displaying a list of spectra in a plot. Each spectrum
 * is displayed and then removed in an animation like sequence. Control over
 * the speed of drawing, which plot is drawn into and how the axes scales are
 * determined are given.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see DivaPlot 
 * @see SplatBrowser
 */
public class SpecAnimatorFrame 
    extends JFrame 
    implements PlotListener
{
    /**
     * The SplatBrowser that is a parent to this (i.e.&nbsp;invokes
     * it). This is used to create new Plots.
     */
    protected SplatBrowser browser = null;

    /**
     * Reference to global list of spectra and plots.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * UI preferences.
     */
    protected static Preferences prefs = 
        Preferences.userNodeForPackage( SpecAnimatorFrame.class );

    /**
     * JList that displays the global list of spectra.
     */
    protected JList specList = new JList();

    /**
     * The target plot that we're animating into.
     */
    protected PlotControl animatePlot = null;

    /**
     * List of possible target plots for displaying into.
     */
    protected JComboBox plotList = null;

    /**
     * Field with the delay value entered (JSpinner when available?).
     */
    protected DecimalField delayField = null;

    /**
     * Whether to loop forever.
     */
    protected JCheckBox loopCheckBox = new JCheckBox();

    /**
     * Are we looping forever or just showing spectra once.
     */
    protected boolean loopForever = false;

    /** 
     * Type of graphics used for capture.
     */
    protected JCheckBox graphicTypeCheckBox = new JCheckBox();

    /**
     * Whether to capture animation.
     */
    protected JCheckBox captureCheckBox = new JCheckBox();

    /**
     * Are we capturing the animation (implies not looping forever).
     */
    protected boolean capturingAnimation = false;

    /**
     * Field for the graphics base names.
     */
    protected JTextField baseField = new JTextField();

    /**
     * Content pane of frame.
     */
    protected JPanel contentPane = null;

    /**
     * Control panels.
     */
    protected JPanel animationPanel = new JPanel();
    protected JPanel capturePanel = new JPanel();

    /**
     * Action buttons container.
     */
    protected JPanel actionBar = new JPanel();

    /**
     * Display for current spectrum.
     */
    protected JTextField spectrumName = new JTextField();

    /**
     * Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu();
    protected JMenuItem closeFileMenu = new JMenuItem();

    /**
     * Type of scaling that we're using.
     */
    protected final static int FREE = 0;
    protected final static int AUTO = 1;
    protected final static int FIXED = 2;
    protected int scaleType = AUTO;

    /**
     * Create an instance.
     *
     * @param browser used to create new plots.
     */
    public SpecAnimatorFrame( SplatBrowser browser )
    {
        this.browser = browser;
        contentPane = (JPanel) getContentPane();
        initUI();
        initFrame();
        globalList.addPlotListener( this );
    }

    /**
     * Free any locally allocated resources.
     *
     * @exception Throwable Description of the Exception
     */
    public void finalize()
        throws Throwable
    {
        globalList.removePlotListener( this );
        super.finalize();
    }

    /**
     * Initialise the main part of the user interface.
     */
    protected void initUI()
    {
        // The layout is a BorderLayout with the list of regions in
        // the center and the toolbox below this.
        contentPane.setLayout( new BorderLayout() );

        // Add the menuBar.
        setJMenuBar( menuBar );

        // Create the File menu.
        fileMenu.setText( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Create the Help menu.
        HelpFrame.createHelpMenu( "animation-window", "Help on window",
                                  menuBar, null );

        //  Display the global list of spectra in a scroll pane.
        JScrollPane scroller = new JScrollPane( specList );
        TitledBorder listTitle =
            BorderFactory.createTitledBorder( "Global list of spectra:" );
        scroller.setBorder( listTitle );
        contentPane.add( scroller, BorderLayout.CENTER );

        //  The JList model is the global list of spectra.
        specList.setModel( new SpecListModel( specList.getSelectionModel() ) );

        //  Initially all spectra are selected.
        specList.setSelectionInterval( 0, globalList.specCount() - 1 );

        // Action bar uses a BoxLayout and is placed at the south.
        JPanel controlsPanel = new JPanel( new BorderLayout() );
        contentPane.add( controlsPanel, BorderLayout.SOUTH );

        actionBar.setLayout( new BoxLayout( actionBar, BoxLayout.X_AXIS ) );
        actionBar.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
        controlsPanel.add( actionBar, BorderLayout.SOUTH );

        // Add an action to close the window (appears in File menu
        // and action bar).
        CloseAction closeAction = new CloseAction();
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

        JButton closeButton = new JButton( closeAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( closeButton );
        actionBar.add( Box.createGlue() );

        // The control panel holds the interesting components. These
        // are for the animation properties, and whether to capture to a
        // series of JPEGs/PNGs.
        animationPanel.setBorder
            ( BorderFactory.createTitledBorder( "Animation controls" ) );

        capturePanel.setBorder
            ( BorderFactory.createTitledBorder( "Capture controls" ) );

        controlsPanel.add( animationPanel, BorderLayout.NORTH );
        controlsPanel.add( capturePanel, BorderLayout.CENTER );

        initAnimationControls();
        initCaptureControls();
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Animate spectra" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        Utilities.setFrameLocation( this, null, prefs, "SpecAnimator" );
        setVisible( true );
    }

    /**
     * Add the animation controls.
     */
    protected void initAnimationControls()
    {
        GridBagLayouter layouter = 
            new GridBagLayouter( animationPanel, GridBagLayouter.SCHEME3 );

        // The delay (in seconds) between updates. Just a decimal
        // number.
        JLabel delayLabel = new JLabel( "Delay:" );

        ScientificFormat scientificFormat = new ScientificFormat();
        double delay = prefs.getDouble( "SpecAnimator_delay", 1.0 );
        delayField = new DecimalField( delay, 5, scientificFormat );
        delayField.setToolTipText( "Delay before displaying the next " +
                                   "spectrum (seconds, press return to" +
                                   " apply immediately)" );
        delayField.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    matchTimer();
                    prefs.putDouble( "SpecAnimator_delay", 
                                     delayField.getDoubleValue() );
                }
            } );
        matchTimer();

        layouter.add( delayLabel, false );
        layouter.add( delayField, true );

        // Whether to loop forever or not.
        JLabel loopLabel = new JLabel( "Loop forever:" );
        layouter.add( loopLabel, false );
        layouter.add( loopCheckBox, true );

        loopCheckBox.setToolTipText( "Loop animation until stop is pressed" );
        boolean state = prefs.getBoolean( "SpecAnimator_loopforever", false );
        loopCheckBox.setSelected( state );
        loopCheckBox.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    matchLoop();
                    prefs.putBoolean( "SpecAnimator_loopforever", 
                                      loopCheckBox.isSelected() );
                }
            } );
        matchLoop();

        // The target plot for displaying the spectra in. This is the
        // global list, but with special entry of "Create", which
        // means create a new plot.
        JLabel listLabel = new JLabel( "Plot:" );
        plotList = new JComboBox( new LocalPlotModel() );
        plotList.setSelectedIndex( 0 );
        plotList.setToolTipText( "Plot to display spectra, choose 'Create'" +
                                 " for a new plot." );

        layouter.add( listLabel, false );
        layouter.add( plotList, true );

        // Selection of the various scaling options.
        JLabel radioLabel = new JLabel( "Scaling option:" );

        ButtonGroup scaleGroup = new ButtonGroup();

        Action autoAction =
            new ScaleAction( AUTO, "Auto", "Make each spectrum fit the" +
                             " display area" );
        JRadioButton autoScale = new JRadioButton( autoAction );

        scaleGroup.add( autoScale );

        Action fixAction =
            new ScaleAction( FIXED, "Fix", "Keep the scale the same as" +
                             " shown at present (pre-display a spectrum)" );
        JRadioButton fixScale = new JRadioButton( fixAction );
        scaleGroup.add( fixScale );

        Action freeAction =
            new ScaleAction( FREE, "Free", "Scale each spectrum freely" +
                             " (same effect as just adding to a plot)" );
        JRadioButton freeScale = new JRadioButton( freeAction );
        scaleGroup.add( freeScale );

        layouter.add( radioLabel, false );
        layouter.add( autoScale, false );
        layouter.add( fixScale, false );
        layouter.add( freeScale, true );


        int iselect = prefs.getInt( "SpecAnimator_scaling_option", AUTO );
        if ( iselect == AUTO ) {
            autoScale.setSelected( true );
        }
        else if ( iselect == FIXED ) {
            fixScale.setSelected( true );
        }
        else {
            freeScale.setSelected( true );
        }

        // Add the field to display the spectrum name.
        JLabel nameLabel = new JLabel( "Current spectrum:" );

        spectrumName.setEditable( false );
        spectrumName.setToolTipText
            ( "Name of the spectrum currently being animated" );

        layouter.add( nameLabel, false );
        layouter.add( spectrumName, true );

        // Add an action to start the animation.
        JPanel startStopPanel = new JPanel();
        startStopPanel.setLayout( new BoxLayout( startStopPanel,
            BoxLayout.X_AXIS ) );

        StartAction startAction = new StartAction();
        JButton startButton = new JButton( startAction );
        startStopPanel.add( Box.createGlue() );
        startStopPanel.add( startButton );

        // Add an action to pause the animation.
        PauseAction pauseAction = new PauseAction();
        JButton pauseButton = new JButton( pauseAction );
        startStopPanel.add( Box.createGlue() );
        startStopPanel.add( pauseButton );

        // Add an action to stop the animation.
        StopAction stopAction = new StopAction();
        JButton stopButton = new JButton( stopAction );
        startStopPanel.add( Box.createGlue() );
        startStopPanel.add( stopButton );
        startStopPanel.add( Box.createGlue() );

        layouter.add( startStopPanel, true );
    }

    /**
     * Add the capture to JPEG/PNG controls.
     */
    protected void initCaptureControls()
    {
        GridBagLayouter layouter = 
            new GridBagLayouter( capturePanel, GridBagLayouter.SCHEME3 );

        // Whether to capture the animation.
        JLabel captureLabel = new JLabel( "Start capture:" );
        layouter.add( captureLabel, false );
        layouter.add( captureCheckBox, true );

        captureCheckBox.setToolTipText
            ( "Capture animation to a sequence of graphics images" );
        captureCheckBox.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    matchCapture();
                }
            } );

        // Type of graphic to capture. JPEGs or PNGs.
        JLabel graphicTypeLabel = 
            new JLabel( "Capture to JPEG (otherwise PNG):" );
        layouter.add( graphicTypeLabel, false );
        layouter.add( graphicTypeCheckBox, true );

        graphicTypeCheckBox.setToolTipText
            ( "Capture to JPEG images, otherwise try PNG (optional)" );
        graphicTypeCheckBox.setSelected( true );
        


        //  If we're capturing then we need a base name.
        JLabel baseLabel = new JLabel( "Basename for graphics files:" );
        layouter.add( baseLabel, false );
        layouter.add( baseField, true );

        baseField.setToolTipText( "Prefix basename for graphics files" );
        baseField.setText( "SPLAT" );

        // Initialize enabled/disabled states.
        captureCheckBox.setSelected( false );
    }

    /**
     * Close the window.
     */
    protected void closeWindowEvent()
    {
        stop();
        Utilities.saveFrameLocation( this, prefs, "SpecAnimator" );
        dispose();
    }

    /**
     * Start the animation to display all the currently selected spectra, or
     * all spectra if none are selected in the chosen plot.
     */
    public void start()
    {
        if ( timer != null && timer.isRunning() ) {
            stop();
        }
        indices = specList.getSelectedIndices();
        if ( indices.length == 0 ) {
            // Use all spectra.
            int count = specList.getModel().getSize();
            indices = new int[count];
            for ( int i = 0; i < count; i++ ) {
                indices[i] = i;
            }
        }

        // Get plot and display the first spectrum. Create a new plot
        // if needed.
        if ( animatePlot == null ||
            globalList.getPlotIndex( animatePlot ) < 0 ) {
            Object target = plotList.getSelectedItem();
            if ( target instanceof String ) {

                // Must be "Create" so make a new Plot. Need a spectrum
                // initially so use the first valid one and then remove it.

                PlotControlFrame pFrame = null;
                SpecData spec = null;
                for ( int i = 0; i < indices.length; i++ ) {
                    spec = globalList.getSpectrum( indices[i] );
                    pFrame = browser.displaySpectrum( spec );
                    if ( pFrame != null ) break;
                }
                if ( pFrame == null ) {
                    throw new RuntimeException("Cannot find a valid spectrum");
                }
                animatePlot = pFrame.getPlot();
                globalList.removeSpectrum( animatePlot, indices[0] );
            }
            else {
                // target is selected PlotControl.
                animatePlot = (PlotControl) target;
            }
        }
        animatePlot.setVisible( true );

        // Use a threaded method to display the rest.
        threadAnimateSpectra();
    }

    private Timer timer;
    // The current Timer.
    private boolean lastAutoRange;
    // Auto-range status of last spectrum
    private int[] indices = null;
    // Indices of spectra to animate
    private int lastIndex = -1;
    // Current index of indices.

    /**
     * Thread driven animator for displaying a list of spectra.
     */
    protected void threadAnimateSpectra()
    {
        // Create a timer thread that displays the spectra one after
        // the other, with the given interval.
        timer = new Timer( 1000,
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    int newIndex = lastIndex + 1;
                    if ( newIndex > globalList.specCount() ) {

                        //  Global list has mutated, reget the selection.
                        newIndex = 0;
                        indices = specList.getSelectedIndices();
                    }
                    else if ( newIndex >= indices.length ) {
                        if ( loopForever && ! capturingAnimation ) {
                            newIndex = 0;
                        }
                        else {
                            stop();
                            return;
                        }
                    }
                    SpecData spec = globalList.getSpectrum( indices[newIndex] );
                    if ( scaleType == FIXED ) {
                        lastAutoRange = spec.isUseInAutoRanging();
                        spec.setUseInAutoRanging( false );
                    }
                    try {
                        globalList.addSpectrum( animatePlot, spec );
                    }
                    catch (SplatException ignored) {
                        // Failed to display spectrum, make a simple
                        // report and pass on.
                        System.out.println( ignored.getMessage() );
                        removeLastSpectrum();
                        lastIndex = newIndex;
                        return;
                    }
                    spectrumName.setText( spec.getShortName() );
                    removeLastSpectrum();
                    if ( scaleType == AUTO ) {
                        animatePlot.fitToWidthAndHeight( false );
                    }

                    //  If we're capturing then do it.
                    if ( capturingAnimation ) {
                        int type = GraphicFileUtilities.PNG;
                        String ext = ".png";
                        if ( graphicTypeCheckBox.isSelected() ) {
                            type = GraphicFileUtilities.JPEG;
                            ext = ".jpg";
                        }
                        File outputFile =
                            new File( baseField.getText() + newIndex + ext );
                        DivaPlot plot = animatePlot.getPlot();
                        int width = plot.getWidth();
                        int height = plot.getHeight();
                        GraphicFileUtilities.printGraphics
                            ( type, outputFile, animatePlot.getPlot(),
                              width, height, false );
                    }
                    lastIndex = newIndex;
                }
            } );

        // Start loading spectra.
        matchTimer();
        timer.start();
    }

    /**
     * Pause the animation. Just stops the Timer.
     */
    public void pause()
    {
        if ( timer != null ) {
            timer.stop();
        }
    }

    /**
     * Stop the animation.
     */
    public void stop()
    {
        if ( timer != null ) {
            timer.stop();
            removeLastSpectrum();
            lastIndex = -1;
            timer = null;
            spectrumName.setText( " " );
            if ( indices != null ) {
                specList.setSelectedIndices( indices );
            }
        }
    }

    /**
     * Remove the spectrum that was displayed last.
     */
    protected void removeLastSpectrum()
    {
        //  Can become dangerous if global list is mutating.
        try {
            if ( lastIndex != -1 ) {
                SpecData spec = globalList.getSpectrum( indices[lastIndex] );
                globalList.removeSpectrum( animatePlot, spec );
                if ( scaleType == FIXED ) {
                    spec.setUseInAutoRanging( lastAutoRange );
                }
            }
        }
        catch ( Exception e ) {
            try {
                stop();
            }
            catch ( Exception ee ) {
                //  Do nothing.
            }
        }
    }

    /**
     * Set the Timer interval to match the entry field. Default under error
     * condition is 1000.
     */
    protected void matchTimer()
    {
        if ( timer != null ) {
            int millisec = 1000;
            try {
                millisec = (int) ( delayField.getDoubleValue() * 1000.0 );
            }
            catch ( Exception e ) {
                millisec = 1000;
            }
            timer.setDelay( millisec );
        }
    }

    /**
     * Set the loop forever value to match the current value.
     */
    protected void matchLoop()
    {
        loopForever = loopCheckBox.isSelected();
        captureCheckBox.setEnabled( ! loopForever );
    }

    /**
     * Set looping forever to match the current value. Implies that we're not
     * looping forever.
     */
    protected void matchCapture()
    {
        capturingAnimation = captureCheckBox.isSelected();
        loopCheckBox.setEnabled( ! capturingAnimation );
        baseField.setEnabled( capturingAnimation );
    }

    /**
     * Our plot may been closed, so we may need to create another.
     */
    protected void isPlotClosed()
    {
        if ( globalList.getPlotIndex( animatePlot ) < 0 ) {
            stop();
            animatePlot = null;
        }
    }

    /**
     * Set the type of display scaling that we're using.
     *
     * @param type Description of the Parameter
     */
    public void toggleScale( int type )
    {
        scaleType = type;
    }

    private final static ImageIcon closeImage =
        new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );

    /**
     * Inner class defining Action for closing window.
     */
    protected class CloseAction extends AbstractAction
    {
        public CloseAction()
        {
            super( "Close", closeImage );
            putValue( SHORT_DESCRIPTION, "Close window" );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control W" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            closeWindowEvent();
        }
    }

    private final static ImageIcon playImage =
        new ImageIcon( ImageHolder.class.getResource( "play.gif" ) );

    /**
     * Inner class defining Action for starting the animation.
     */
    protected class StartAction extends AbstractAction
    {
        public StartAction()
        {
            super( "Start", playImage );
            putValue( SHORT_DESCRIPTION, "Start animation" );
        }
        public void actionPerformed( ActionEvent ae )
        {
            start();
        }
    }

    private final static ImageIcon pauseImage =
        new ImageIcon( ImageHolder.class.getResource( "pause.gif" ) );

    /**
     * Inner class defining Action for pausing the animation.
     */
    protected class PauseAction extends AbstractAction
    {
        public PauseAction()
        {
            super( "Pause", pauseImage );
            putValue( SHORT_DESCRIPTION, "Pause animation" );
        }
        public void actionPerformed( ActionEvent ae )
        {
            pause();
        }
    }

    private final static ImageIcon stopImage =
        new ImageIcon( ImageHolder.class.getResource( "stop.gif" ) );

    /**
     * Inner class defining Action for stopping the animation.
     */
    protected class StopAction extends AbstractAction
    {
        public StopAction()
        {
            super( "Stop", stopImage );
            putValue( SHORT_DESCRIPTION, "Stop animation" );
        }
        public void actionPerformed( ActionEvent ae )
        {
            stop();
        }
    }

    /**
     * Inner class defining Action for dealing with a scale change.
     */
    protected class ScaleAction extends AbstractAction
    {
        protected int type;
        public ScaleAction( int type, String name, String shortHelp )
        {
            super( name, null );
            putValue( SHORT_DESCRIPTION, shortHelp );
            this.type = type;
        }
        public void actionPerformed( ActionEvent ae )
        {
            toggleScale( type );
            prefs.putInt( "SpecAnimator_scaling_option", type );
        }
    }

//
// Implement the PlotListener interface. We need to know if the
// plots that we're using are closed.
//
    /**
     * Sent when a plot is created.
     */
    public void plotCreated( PlotChangedEvent e )
    {
        //  Nothing to do.
    }

    /**
     * Sent when a plot is removed.
     */
    public void plotRemoved( PlotChangedEvent e )
    {
        //  Check if this is animatePlot and stop animation if so.
        if ( animatePlot != null ) {
            if ( globalList.getPlotIndex( animatePlot ) < 0 ) {
                stop();
                animatePlot = null;
            }
        }
    }

    /**
     * Sent when a plot property is changed (i.e.<!-- --> spectrum added/removed?).
     */
    public void plotChanged( PlotChangedEvent e )
    {
        // Nothing to do.
    }

//
// Model for the list of plots.
//
    protected class LocalPlotModel extends AbstractListModel
         implements PlotListener, ComboBoxModel
    {
        /**
         * Reference to the GlobalSpecPlotList object. This is an interface to
         * all information about plot and spectra availability.
         */
        GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

        /**
         * The selected item (a plot or the "Create" string).
         */
        protected Object plot = null;

        /**
         * Create an instance of this class.
         */
        public LocalPlotModel()
        {
            // Register ourselves as interested in plot changes.
            globalList.addPlotListener( this );
        }

        /**
         * Description of the Method
         */
        public void dispose()
        {
            globalList.removePlotListener( this );
        }

        //
        // Implement rest of ListModel interface (listeners are free from
        // AbstractListModel)
        //

        /**
         * Return the number of plots, plus our special "create" entry.
         */
        public int getSize()
        {
            return globalList.plotCount() + 1;
        }

        /**
         * Return the value of a given row, this is a plot reference.
         */
        public Object getElementAt( int row )
        {
            if ( row == 0 ) {
                return "Create";
            }
            else {
                return globalList.getPlot( row - 1 );
            }
        }

        //
        // Implement the ComboBoxModel interface.
        //
        /**
         * Gets the selectedItem attribute of the LocalPlotModel object
         */
        public Object getSelectedItem()
        {
            return plot;
        }

        /**
         * Sets the selectedItem attribute of the LocalPlotModel object
         */
        public void setSelectedItem( Object anItem )
        {
            plot = anItem;
        }

        //
        // Implement the PlotListener interface.
        //
        /**
         * React to a new plot being created.
         */
        public void plotCreated( PlotChangedEvent e )
        {
            int index = e.getIndex();
            fireIntervalAdded( this, index, index );
        }

        /**
         * React to a plot being removed.
         */
        public void plotRemoved( PlotChangedEvent e )
        {
            int index = e.getIndex();
            fireIntervalRemoved( this, index, index );
        }

        /**
         * React to a plot change, this requires no action.
         */
        public void plotChanged( PlotChangedEvent e )
        {
            // Do nothing.
        }
    }
}
