package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import uk.ac.starlink.splat.data.SpecDataComp;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;

/**
 * PlotControlFrame provides a top-level wrapper for a PlotControl
 * object.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @since $Date$
 * @since 29-SEP-2000, original version
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 */
public class PlotControlFrame extends JFrame
{
    /**
     *  PlotControl object for displaying the spectra.
     */
    protected PlotControl plot;

    /**
     *  SpecDataComp object that contains all the displayed spectra.
     */
    protected SpecDataComp specDataComp;

    /**
     *  PlotConfig object for containing the complete Graphics
     *  configuration of the Plot (not for any AST spectral line properties).
     */
    protected PlotConfig config = new PlotConfig();

    /**
     * PlotConfigFrame window for changing the Plot configuration
     * values (created when required).
     */
    protected PlotConfigFrame configFrame = null;

    /**
     * PolyFitFrame window for fitting a polynomial to the spectrum
     * (created when required).
     */
    protected PolyFitFrame polyFitFrame = null;

    /**
     * LineFitFrame window for measuring to the spectral line properties.
     * (created when required).
     */
    protected LineFitFrame lineFitFrame = null;

    /**
     * The panner window.
     */
    protected PlotPannerFrame pannerFrame = null;

    /**
     * The cutter window.
     */
    protected SpecCutterFrame cutterFrame = null;

    /**
     *  The global list of spectra and plots.
     */
    protected GlobalSpecPlotList globalList =
        GlobalSpecPlotList.getReference();

    /**
     *  Main menubar and various menus.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu();
    protected JMenuItem closeFileMenu = new JMenuItem();
    protected JMenu helpMenu = new JMenu();
    protected JMenuItem windowHelp = new JMenuItem();
    protected JMenu analysisMenu = new JMenu();
    protected JMenuItem polyFitMenu = new JMenuItem();
    protected JMenuItem lineFitMenu = new JMenuItem();

    /**
     *  Toolbar and contents.
     */
    protected JToolBar toolBar = new JToolBar();
    protected JPanel toolBarContainer = new JPanel();
    protected JButton configButton = new JButton();
    protected JButton viewCutterButton = new JButton();
    protected JButton regionCutterButton = new JButton();
    protected JButton fitHeightButton = new JButton();
    protected JButton fitWidthButton = new JButton();
    protected JButton helpButton = new JButton();
    protected JButton lineFitButton = new JButton();
    protected JButton pannerButton = new JButton();
    protected JButton polyFitButton = new JButton();
    protected JButton printPostscriptButton = new JButton();
    protected JButton printJPEGButton = new JButton();

    /**
     *  Plot a spectrum.
     *
     *  @param spectrum  Active SpecDataComp reference.
     *
     */
    public PlotControlFrame( SpecDataComp specDataComp )
        throws SplatException
    {
        this( "PlotControlFrame", specDataComp );
    }

    /**
     *  Create the main window.
     */
    public PlotControlFrame( String title ) throws SplatException
    {
        this( title, (SpecDataComp) null );
    }

    /**
     *  Plot a spectrum.
     *
     *  @param specDataComp Active SpecDataComp reference.
     *
     */
    public PlotControlFrame( String title, SpecDataComp specDataComp )
        throws SplatException
    {
        if ( specDataComp == null ) {
            plot = new PlotControl();
        } else {
            plot = new PlotControl( specDataComp );
        }
        this.specDataComp = plot.getSpecDataComp();
        initUI( title );
        plot.setConfig( config );
    }

    /**
     *  Plot a spectrum.
     *
     *  @param file  name of file containing spectrum.
     *
     */
    public PlotControlFrame( String title, String file )
        throws SplatException
    {
        plot = new PlotControl( file );
        this.specDataComp = plot.getSpecDataComp();
        initUI( title );
        plot.setConfig( config );
    }

    /**
     *  Return a reference to the PlotControl object.
     */
    public PlotControl getPlot()
    {
        return plot;
    }

    /**
     *  Make the frame visible and set the default action for when we
     *  are closed.
     */
    protected void initUI( String title )
    {
        setTitle( Utilities.getTitle( title ) );
        setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        getContentPane().add( plot, BorderLayout.CENTER );
        getContentPane().add( toolBarContainer, BorderLayout.NORTH );
        configureMenus();
        pack();
        setVisible( true );
    }

    /**
     *  Configure the menu and toolbar.
     */
    protected void configureMenus()
    {
        //  Add the menuBar.
        this.setJMenuBar( menuBar );

        //  Add the toolbar to a container. Need extra component for
        //  sensible float behaviour.
        toolBarContainer.setLayout( new BorderLayout() );
        toolBarContainer.add( toolBar );

        //  Add the File menu.
        setupFileMenu();

        //  Set up the Analysis menu.
        setupAnalysisMenu();

        //  Set up the help menu.
        setupHelpMenu();
    }

    /**
     *  Configure the File menu.
     */
    protected void setupFileMenu()
    {
        fileMenu.setText( "File" );
        menuBar.add( fileMenu );

        ImageIcon printPostscriptImage = new ImageIcon(
            ImageHolder.class.getResource( "print.gif" ) );
        ImageIcon printJPEGImage = new ImageIcon(
            ImageHolder.class.getResource( "jpeg.gif" ) );
        ImageIcon closeImage = new ImageIcon(
            ImageHolder.class.getResource( "exit.gif" ) );
        ImageIcon fitWidthImage = new ImageIcon(
            ImageHolder.class.getResource( "fitwidth.gif" ) );
        ImageIcon fitHeightImage = new ImageIcon(
            ImageHolder.class.getResource( "fitheight.gif" ) );
        ImageIcon configImage = new ImageIcon(
            ImageHolder.class.getResource( "config.gif" ) );
        ImageIcon pannerImage = new ImageIcon(
            ImageHolder.class.getResource( "panner.gif" ) );

        //  Add action to print figure to postscript.
        PrintPostscriptAction printPostscriptAction  =
            new PrintPostscriptAction( "Print", printPostscriptImage );
        fileMenu.add( printPostscriptAction );
        printPostscriptButton = toolBar.add( printPostscriptAction );
        printPostscriptButton.setToolTipText
            ( "Print display to printer or file (postscript)" );

        //  Add action to print figure to postscript.
        PrintJPEGAction printJPEGAction  =
            new PrintJPEGAction( "Print to JPEG", printJPEGImage );
        fileMenu.add( printJPEGAction );
        printJPEGButton = toolBar.add( printJPEGAction );
        printJPEGButton.setToolTipText( "Print display to a JPEG file" );

        //  Add action to fit plot to window width.
        FitWidthAction fitWidthAction  = new FitWidthAction( "Fit width",
                                                             fitWidthImage );
        fileMenu.add( fitWidthAction );
        fitWidthButton = toolBar.add( fitWidthAction );
        fitWidthButton.setToolTipText( "Scale spectrum to fit visible width" );

        //  Add action to fit plot to window height.
        FitHeightAction fitHeightAction  = new FitHeightAction( "Fit height",
                                                                fitHeightImage );
        fileMenu.add( fitHeightAction );
        fitHeightButton = toolBar.add( fitHeightAction );
        fitHeightButton.setToolTipText(
                        "Scale spectrum to fit visible height" );

        //  Add action to enable the panner.
        PannerAction pannerAction  = new PannerAction( "Show panner",
                                                       pannerImage );
        fileMenu.add( pannerAction );
        pannerButton = toolBar.add( pannerAction );
        pannerButton.setToolTipText(
                     "Show panner window for controlling scroll position " );

        //  Add action to configure plot.
        ConfigAction configAction  = new ConfigAction( "Configure",
                                                       configImage );
        fileMenu.add( configAction );
        configButton = toolBar.add( configAction );
        configButton.setToolTipText(
                     "Configure plot presentation attributes" );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction );
    }

    /**
     * Configure the analysis menu.
     */
    protected void setupAnalysisMenu()
    {
        analysisMenu.setText( "Analysis" );
        menuBar.add( analysisMenu );

        ImageIcon backImage = new ImageIcon(
            ImageHolder.class.getResource( "fitback.gif" ) );
        ImageIcon lineImage = new ImageIcon(
            ImageHolder.class.getResource( "fitline.gif" ) );
        ImageIcon cutterImage = new ImageIcon(
            ImageHolder.class.getResource( "cutter.gif" ) );
        ImageIcon regionCutterImage = new ImageIcon(
            ImageHolder.class.getResource( "regioncutter.gif" ) );

        //  Add action to enable to cut out the current view of
        //  current spectrum.
        ViewCutterAction viewCutterAction =
            new ViewCutterAction( "Cut out view", cutterImage );
        analysisMenu.add( viewCutterAction );
        viewCutterButton = toolBar.add( viewCutterAction );
        viewCutterButton.setToolTipText(
                     "Cut out what you can see of the current spectrum" );

        //  Add action start the cutter tool.
        RegionCutterAction regionCutterAction =
            new RegionCutterAction( "Cut regions from spectrum",
                                    regionCutterImage );
        analysisMenu.add( regionCutterAction );
        regionCutterButton = toolBar.add( regionCutterAction );
        regionCutterButton.setToolTipText(
                     "Cut out selected regions of the current spectrum" );

        //  Add the fit polynomial to background item.
        PolyFitAction polyFitAction = new PolyFitAction( "Fit polynomial",
                                                         backImage );
        analysisMenu.add( polyFitAction );
        polyFitButton = toolBar.add( polyFitAction );
        polyFitButton.setToolTipText(
                      "Fit parts of spectrum using a polynomial" );

        //  Add the measure and fit spectral lines action.
        LineFitAction lineFitAction = new LineFitAction( "Fit lines",
                                                         lineImage );
        analysisMenu.add( lineFitAction );
        lineFitButton = toolBar.add( lineFitAction );
        lineFitButton.setToolTipText(
                      "Fit spectral lines using a variety of functions" );
    }

    /**
     * Configure the help menu.
     */
    protected void setupHelpMenu()
    {
        HelpFrame.createHelpMenu( "plot-window", "Help on window", 
                                  menuBar, toolBar );
    }

    /**
     *  Print the current display to a postscript (UNIX) file or printer.
     */
    protected void printPostscriptDisplay()
    {
        plot.print();
    }

    /**
     *  Print the current display to a JPEG file.
     */
    protected void printJPEGDisplay()
    {
        uk.ac.starlink.splat.util.JPEGUtilities.showJPEGChooser(plot.getPlot());
    }

    /**
     *  Make spectrum fit width.
     */
    public void fitToWidth()
    {
        plot.fitToWidth();
    }

    /**
     *  Make spectrum fit height.
     */
    public void fitToHeight()
    {
        plot.fitToHeight();
    }

    /**
     *  Activate the pop-up window for configuring the Plot.
     */
    public void configPlot()
    {
        if ( configFrame == null ) {
            configFrame = new PlotConfigFrame( config, plot );

            //  We'd like to know if the window is closed.
            configFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        configClosed();
                    }
                });
        }
        else {
            Utilities.raiseFrame( configFrame );
        }
    }

    /**
     *  Configuration window is closed.
     */
    protected void configClosed()
    {
        // Nullify if method for closing switches to dispose.
        // configFrame = null;
    }

    /**
     *  Close the config window.
     */
    protected void closeConfigFrame()
    {
        if ( configFrame != null ) {
            configFrame.dispose();
            configFrame = null;
        }
    }

    /**
     *  Activate the pop-up window for fitting a polynomial to the
     *  current spectrum.
     */
    public void polyFit()
    {
        if ( polyFitFrame == null ) {
            polyFitFrame = new PolyFitFrame( this );
            //  We'd like to know if the window is closed.
            polyFitFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        polyFitClosed();
                    }
                });
        }
        else {
            Utilities.raiseFrame( polyFitFrame );
        }
    }

    /**
     *  Polynomial fitting window is closed.
     */
    protected void polyFitClosed()
    {
        // Nullify if method for closing switches to dispose.
        // fitPolynomialWindow = null;
    }

    /**
     *  Close the polynomial fitting window.
     */
    protected void closePolyFitFrame()
    {
        if ( polyFitFrame != null ) {
            polyFitFrame.dispose();
            polyFitFrame = null;
        }
    }

    /**
     *  Activate the pop-up window for fitting lines of the
     *  current spectrum.
     */
    public void lineFit()
    {
        if ( lineFitFrame == null ) {
            lineFitFrame = new LineFitFrame( this );
            //  We'd like to know if the window is closed.
            lineFitFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        lineFitClosed();
                    }
                });
        } else {
            Utilities.raiseFrame( lineFitFrame );
        }
    }

    /**
     * Line fitting window is closed.
     */
    protected void lineFitClosed()
    {
        // Nullify if method for closing switches to dispose.
        // lineFitFrame = null;
    }

    /**
     *  Close the line fitting window.
     */
    protected void closeLineFitFrame()
    {
        if ( lineFitFrame != null ) {
            lineFitFrame.dispose();
            lineFitFrame = null;
        }
    }

    /**
     *  Activate the panner window for panning a zoomed spectrum.
     */
    public void showPanner()
    {
        if ( pannerFrame == null ) {
            pannerFrame = new PlotPannerFrame( this.getPlot() );
            //  We'd like to know if the window is closed.
            pannerFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        pannerClosed();
                    }
                });
        } else {
            Utilities.raiseFrame( pannerFrame );
        }
    }

    /**
     * Panner window is closed.
     */
    protected void pannerClosed()
    {
        // Nullify if method for closing switches to dispose.
        // lineFitWindow = null;
    }

    /**
     *  Close the panner window.
     */
    protected void closePanner()
    {
        if ( pannerFrame != null ) {
            pannerFrame.dispose();
            pannerFrame = null;
        }
    }

    /**
     *  Activate the cutter window.
     */
    public void showCutter()
    {
        if ( cutterFrame == null ) {
            cutterFrame = new SpecCutterFrame( this.getPlot() );
            //  We'd like to know if the window is closed.
            cutterFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        cutterClosed();
                    }
                });
        } else {
            Utilities.raiseFrame( cutterFrame );
        }
    }

    /**
     * Cutter window is closed.
     */
    protected void cutterClosed()
    {
        // Nullify if method for closing switches to dispose.
        // cutterFrame = null;
    }

    /**
     *  Close the cutter window.
     */
    protected void closeCutter()
    {
        if ( cutterFrame != null ) {
            cutterFrame.dispose();
            cutterFrame = null;
        }
    }

    /**
     * Cut out the current view of the current spectrum and add it to
     * the global list.
     */
    public void cutView()
    {
        SpecCutter.getReference().cutView( plot.getCurrentSpectrum(),
                                           getPlot() );
    }

    /**
     * Set the main cursor to indicate waiting for some action to
     * complete and lock the interface by trapping all mouse events.
     */
    protected void setWaitCursor()
    {
        Cursor wait = Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR );
        Component glassPane = getGlassPane();
        glassPane.setCursor( wait );
        glassPane.setVisible( true );
        glassPane.addMouseListener( new MouseAdapter() {} );
    }

    /**
     * Undo the action of the setWaitCursor method.
     */
    protected void resetWaitCursor()
    {
        getGlassPane().setCursor( null );
        getGlassPane().setVisible( false );
    }

    /**
     *  Close all associated tool windows.
     */
    public void closeToolWindows()
    {
        closeConfigFrame();
        closePolyFitFrame();
        closeLineFitFrame();
        closePanner();
        closeCutter();
    }

    /**
     *  Clear resources when finalized.
     */
    protected void finalize() throws Throwable
    {
        closeToolWindows();
        config = null;
        super.finalize();
    }

    /**
     *  When window is closed remove configuration window too.
     */
    protected void processWindowEvent( WindowEvent e )
    {
        super.processWindowEvent( e );
        if ( e.getID() == WindowEvent.WINDOW_CLOSING ) {
            closeToolWindows();
        }
    }

    /**
     *  Close the main window, removing all the tool windows too.
     */
    public void closeWindow()
    {
        this.dispose();
        closeToolWindows();
    }

//
//  Inner classes.
//
    /**
     *  Inner class defining Action for closing window.
     */
    protected class CloseAction extends AbstractAction
    {
        public CloseAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            closeWindow();
        }
    }

    /**
     *  Inner class defining Action for printing to postscript.
     */
    protected class PrintPostscriptAction extends AbstractAction
    {
        public PrintPostscriptAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            printPostscriptDisplay();
        }
    }

    /**
     *  Inner class defining Action for printing to a JPEG file.
     */
    protected class PrintJPEGAction extends AbstractAction
    {
        public PrintJPEGAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            printJPEGDisplay();
        }
    }

    /**
     *  Inner class defining Action for fitting spectrum to width.
     */
    protected class FitWidthAction extends AbstractAction
    {
        public FitWidthAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            fitToWidth();
        }
    }

    /**
     *  Inner class defining Action for fitting spectrum to height.
     */
    protected class FitHeightAction extends AbstractAction
    {
        public FitHeightAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            fitToHeight();
        }
    }

    /**
     *  Inner class defining Action for configuring plot.
     */
    protected class ConfigAction extends AbstractAction
    {
        public ConfigAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            configPlot();
        }
    }

    /**
     *  Inner class defining Action for showing panner.
     */
    protected class PannerAction extends AbstractAction
    {
        public PannerAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            showPanner();
        }
    }

    /**
     *  Inner class defining Action for cutting out view of spectrum.
     */
    protected class ViewCutterAction extends AbstractAction
    {
        public ViewCutterAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            cutView();
        }
    }

    /**
     *  Inner class defining Action for cutting out regions of spectrum.
     */
    protected class RegionCutterAction extends AbstractAction
    {
        public RegionCutterAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            showCutter();
        }
    }

    /**
     *  Inner class defining Action for fitting a polynomial to a spectrum.
     */
    protected class PolyFitAction extends AbstractAction
    {
        public PolyFitAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            polyFit();
        }
    }

    /**
     *  Inner class defining Action for fitting spectral lines.
     */
    protected class LineFitAction extends AbstractAction
    {
        public LineFitAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            lineFit();
        }
    }
}
