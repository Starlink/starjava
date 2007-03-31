/*
 * Copyright (C) 2001-2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     06-JAN-2001 (Peter W. Draper):
 *        Original version.
 *     26-JUN-2003 (Peter W. Draper):
 *        Added divide spectrum option (needed to normalize fluxes,
 *        especially for absorption lines).
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.PolynomialFitter;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * PolyFitFrame display a series of controls that allow a polynomial
 * to be fitted to selected parts of a spectrum. The spectrum chosen
 * is the currently selected one in a specified PlotControlFrame.
 * <p>
 * The result of a fit is displayed as a spectrum in its own right, and
 * (optionally) as a spectrum with the polynomial subtracted.
 * <p>
 * When closed this window will be hidden, not disposed. If this
 * therefore necessary that the user disposes of it when it is really
 * no longer required.
 * <p>
 * The fit can optionally be subtracted from the spectrum, or divided
 * into the spectrum.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see PolynomialFitter
 */
public class PolyFitFrame
    extends JFrame
{
    /** UI preferences. */
    protected static Preferences prefs =
        Preferences.userNodeForPackage( PolyFitFrame.class );

    /**
     * List of spectra that we have created.
     */
    protected SpecSubList localList = new SpecSubList();

    /**
     * Content pane of frame.
     */
    protected JPanel contentPane = null;

    /**
     * Action buttons container.
     */
    protected JPanel actionBarContainer = new JPanel();
    protected JPanel topActionBar = new JPanel();
    protected JPanel midActionBar = new JPanel();
    protected JPanel botActionBar = new JPanel();

    /**
     *  Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu();
    protected JMenu rangeMenu = new JMenu();
    protected JMenuItem closeFileMenu = new JMenuItem();

    /**
     *  The PlotControlFrame that specifies the current spectrum.
     */
    protected PlotControlFrame plot = null;

    /**
     *  Number of fits done so far (used as unique identifier)
     */
    protected static int fitCounter = 0;

    /**
     *  Degree of polynomial combobox.
     */
    protected JComboBox degreeBox = new JComboBox();

    /**
     *  Whether to use errors as weights during the fit.
     */
    protected JCheckBox errorsBox = new JCheckBox();

    /**
     *  Whether to create a subtracted spectrum and it's sense.
     */
    protected JRadioButton subtractNothing = new JRadioButton();
    protected JRadioButton subtractFromAbove = new JRadioButton();
    protected JRadioButton subtractFromBelow = new JRadioButton();

    /**
     *  Whether to create a normalized spectrum.
     */
    protected JCheckBox divideSpectrum = new JCheckBox();

    /**
     *  Ranges of data that are to be fitted.
     */
    protected XGraphicsRangesView rangeList = null;

    /**
     *  View for showing the results of a fit.
     */
    protected JTextArea fitResults = new JTextArea();

    /**
     *  ScrollPane for display results of a fit.
     */
    protected JScrollPane fitResultsPane = new JScrollPane();

    /**
     *  Label for results area.
     */
    protected TitledBorder fitResultsTitle =
        BorderFactory.createTitledBorder( "Fit status:" );

    /**
     *  Reference to GlobalSpecPlotList object.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     *  The current spectrum last removed, if done.
     */
    protected SpecData removedCurrentSpectrum = null;

    /**
     * Create an instance.
     */
    public PolyFitFrame( PlotControlFrame plot )
    {
        contentPane = (JPanel) getContentPane();
        setPlot( plot );
        initUI();
        initMenus();
        initFrame();
    }

    /**
     * Get the PlotControlFrame that we are using.
     *
     * @return the PlotControlFrame
     */
    public PlotControlFrame getPlot()
    {
        return plot;
    }

    /**
     * Set the PlotControlFrame that has the spectrum that we are to
     * fit.
     *
     * @param plot the PlotControlFrame reference.
     */
    public void setPlot( PlotControlFrame  plot )
    {
        this.plot = plot;
    }

    /**
     * Initialise the main part of the user interface.
     */
    protected void initUI()
    {
        //  This all goes in a JPanel in the center that has a
        //  GridBagLayout.
        JPanel centre = new JPanel();
        GridBagLayouter layouter =
            new GridBagLayouter( centre, GridBagLayouter.SCHEME4 );
        contentPane.add( centre, BorderLayout.CENTER );

        //  Add degree control.
        layouter.add( new JLabel( "Degree of polynomial:" ), false );
        layouter.add( degreeBox, false );
        layouter.add( Box.createHorizontalBox(), true );
        degreeBox.setToolTipText(
           "Degree of the polynomial (0=constant, 1=straight-line etc.)" );

        //  Set the possible polynomial degrees.
        for ( int i = 0; i < 15; i++ ) {
            degreeBox.addItem( new Integer( i ) );
        }

        //  Restore the old degree value, if set.
        int degree = prefs.getInt( "PolyFitFrame_degree", 0 );
        degreeBox.setSelectedItem( new Integer( degree ) );

        //  Decide if we should use errors (if available) during fit.
        layouter.add( new JLabel( "Use errors as weights:" ), false );
        layouter.add( errorsBox, true );
        errorsBox.setToolTipText( "Use errors to weight fit, if available" );

        //  Decide if we should generate and display a subtracted
        //  version of the spectrum.  There are three options, do not,
        //  subtract background from spectrum and subtract spectrum
        //  from background (emission-v-absorption).
        layouter.add( new JLabel( "Subtract fit from spectrum:" ), false );

        subtractNothing.setText( "No" );
        subtractNothing.setToolTipText( "Do not subtract fit from spectrum" );
        layouter.add( subtractNothing, false );

        subtractFromBelow.setText( "As base line" );
        subtractFromBelow.setToolTipText( "Subtract fit from spectrum" );
        layouter.add( subtractFromBelow, false );

        subtractFromAbove.setText( "As ceiling" );
        subtractFromAbove.setToolTipText( "Subtract spectrum from fit" );
        layouter.add( subtractFromAbove, false );
        layouter.eatLine();

        ButtonGroup subtractGroup = new ButtonGroup();
        subtractGroup.add( subtractNothing );
        subtractGroup.add( subtractFromBelow );
        subtractGroup.add( subtractFromAbove );

        //  Retrieve user prefs for this group.
        int which = prefs.getInt( "PolyFitFrame_subtract", 1 );
        if ( which == 2 ) {
            subtractFromBelow.setSelected( true );
        }
        else if ( which == 3 ) {
            subtractFromAbove.setSelected( true );
        }
        else {
            subtractNothing.setSelected( true );
        }

        //  Record any new preferences.
        subtractNothing.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    if ( subtractNothing.isSelected() ) {
                        prefs.putInt( "PolyFitFrame_subtract", 1 );
                    }
                }
            });

        subtractFromBelow.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    if ( subtractFromBelow.isSelected() ) {
                        prefs.putInt( "PolyFitFrame_subtract", 2 );
                    }
                }
            });

        subtractFromAbove.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    if ( subtractFromAbove.isSelected() ) {
                        prefs.putInt( "PolyFitFrame_subtract", 3 );
                    }
                }
            });

        //  Decide if we should generate and display a divided
        //  version of the spectrum.
        divideSpectrum.setToolTipText( "Divide spectrum by fit" );
        layouter.add( new JLabel( "Divide spectrum by fit:" ), false );
        layouter.add( divideSpectrum, true );

        //  Retrieve user prefs.
        boolean state = prefs.getBoolean( "PolyFitFrame_divide", false );
        divideSpectrum.setSelected( state );
        divideSpectrum.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    boolean state = divideSpectrum.isSelected();
                    prefs.putBoolean( "PolyFitFrame_divide", state );
                }
            });

        //  List of regions of spectrum to fit.
        rangeList = new XGraphicsRangesView( plot.getPlot().getPlot(), 
                                             rangeMenu );
        layouter.add( rangeList, true );

        //  Add an area to show the results of the fit (coefficients
        //  and quality of fit).
        JPanel fitContainer = new JPanel( new BorderLayout() );
        fitContainer.setBorder( fitResultsTitle );
        fitResultsPane.getViewport().add( fitResults );
        fitContainer.add( fitResultsPane, BorderLayout.CENTER );

        layouter.add( fitContainer, true );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Fit polynomial to a spectrum" ));
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        contentPane.add( actionBarContainer, BorderLayout.SOUTH );
        setSize( new Dimension( 600, 500 ) );
        setVisible( true );
    }

    /**
     * Return the current polynomial degree.
     */
    public int getDegree()
    {
        Integer degree = (Integer) degreeBox.getSelectedItem();
        int value = degree.intValue();
        prefs.putInt( "PolyFitFrame_degree", value );
        return value;
    }

    /**
     * Initialise the menu bar, action bar and related actions.
     */
    protected void initMenus()
    {
        //  Add the menuBar.
        setJMenuBar( menuBar );

        //  Action bar uses a BoxLayout.
        topActionBar.setLayout( new BoxLayout( topActionBar,
                                               BoxLayout.X_AXIS ) );
        topActionBar.setBorder( BorderFactory.
                                createEmptyBorder( 3, 3, 3, 3 ) );

        midActionBar.setLayout( new BoxLayout( midActionBar,
                                               BoxLayout.X_AXIS ) );
        midActionBar.setBorder( BorderFactory.
                                createEmptyBorder( 3, 3, 3, 3 ) );

        botActionBar.setLayout( new BoxLayout( botActionBar,
                                               BoxLayout.X_AXIS ) );
        botActionBar.setBorder( BorderFactory.
                                createEmptyBorder( 3, 3, 3, 3 ) );

        //  Get icons.
        ImageIcon closeImage = new ImageIcon(
            ImageHolder.class.getResource( "close.gif" ) );
        ImageIcon readImage = new ImageIcon(
            ImageHolder.class.getResource( "read.gif" ) );
        ImageIcon saveImage = new ImageIcon(
            ImageHolder.class.getResource( "save.gif" ) );
        ImageIcon resetImage = new ImageIcon(
            ImageHolder.class.getResource( "reset.gif" ) );
        ImageIcon deleteImage = new ImageIcon(
            ImageHolder.class.getResource( "delete.gif" ) );
        ImageIcon fitImage = new ImageIcon(
            ImageHolder.class.getResource( "fitback.gif" ) );

        //  Create the File menu.
        fileMenu.setText( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Add action to do read a list of ranges from disk file.
        Action readAction = rangeList.getReadAction( "Read ranges",
                                                     readImage );
        fileMenu.add( readAction );

        //  Add action to save the extents to disk file.
        Action saveAction = rangeList.getWriteAction( "Save ranges",
                                                      saveImage );
        fileMenu.add( saveAction );

        //  Add actions to do the fit to all data.
        FitAction fitAction = new FitAction( "Fit", fitImage );
        fileMenu.add( fitAction ).setMnemonic( KeyEvent.VK_F );

        JButton fitButton = new JButton( fitAction );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( fitButton );
        fitButton.setToolTipText( "Fit the polynomial" );

        FitReplaceAction fitReplaceAction =
            new FitReplaceAction( "Fit (Replace)", fitImage );
        fileMenu.add( fitReplaceAction ).setMnemonic( KeyEvent.VK_L );

        JButton fitReplaceButton = new JButton( fitReplaceAction );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( fitReplaceButton );
        fitReplaceButton.setToolTipText
            ( "Fit the polynomial and replace current spectrum" );

        //  Add actions to do the fit to selected.
        FitSelectedAction fitSelectedAction =
            new FitSelectedAction( "Fit selected", fitImage );
        fileMenu.add( fitSelectedAction ).setMnemonic( KeyEvent.VK_S );

        JButton fitSelectedButton = new JButton( fitSelectedAction );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( fitSelectedButton );
        fitSelectedButton.setToolTipText
            ( "Fit polynomial to selected ranges" );

        FitReplaceSelectedAction fitReplaceSelectedAction =
            new FitReplaceSelectedAction( "Fit selected (Replace)", fitImage );
        fileMenu.add( fitReplaceSelectedAction ).setMnemonic( KeyEvent.VK_T );

        JButton fitReplaceSelectedButton = 
            new JButton( fitReplaceSelectedAction );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( fitReplaceSelectedButton );
        fitReplaceSelectedButton.setToolTipText
            ("Fit polynomial to selected ranges and replace current spectrum");

        //  Add action to reset after a replace.
        ResetReplaceAction resetReplaceAction = 
            new ResetReplaceAction( "Reset (Replace)", resetImage );
        fileMenu.add( resetReplaceAction ).setMnemonic( KeyEvent.VK_P );

        JButton resetReplaceButton = new JButton( resetReplaceAction );
        midActionBar.add( Box.createGlue() );
        midActionBar.add( resetReplaceButton );
        resetReplaceButton.setToolTipText( "Reset after a Fit (Replace)" );

        //  Add action to reset all values.
        ResetAction resetAction = new ResetAction( "Reset", resetImage );
        fileMenu.add( resetAction ).setMnemonic( KeyEvent.VK_R );

        JButton resetButton = new JButton( resetAction );
        midActionBar.add( Box.createGlue() );
        midActionBar.add( resetButton );
        resetButton.setToolTipText
            ( "Reset all values and clear all generated fits" );

        //  Add action to just delete any fits.
        DeleteFitsAction deleteFitsAction =
            new DeleteFitsAction( "Delete fits", deleteImage );
        fileMenu.add( deleteFitsAction ).setMnemonic( KeyEvent.VK_I );

        JButton deleteFitsButton = new JButton( deleteFitsAction );
        midActionBar.add( Box.createGlue() );
        midActionBar.add( deleteFitsButton );
        deleteFitsButton.setToolTipText( "Delete all generated fits" );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

        JButton closeButton = new JButton( closeAction );
        botActionBar.add( Box.createGlue() );
        botActionBar.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        topActionBar.add( Box.createGlue() );
        midActionBar.add( Box.createGlue() );
        botActionBar.add( Box.createGlue() );
        actionBarContainer.setLayout( new BorderLayout() );
        actionBarContainer.add( topActionBar, BorderLayout.NORTH );
        actionBarContainer.add( midActionBar, BorderLayout.CENTER );
        actionBarContainer.add( botActionBar, BorderLayout.SOUTH );

        // Now add the Ranges menu.
        rangeMenu.setText( "Ranges" );
        rangeMenu.setMnemonic( KeyEvent.VK_R );
        menuBar.add( rangeMenu );

        //  Create the Help menu.
        HelpFrame.createHelpMenu( "polynomial-fit-window", "Help on window",
                                  menuBar, null );
    }

    /**
     *  Perform the fit, to either all the ranges or just the selected
     *  ones.
     *
     *  @param selected true if we should just fit to selected ranges.
     *  @param replace if fitted spectrum should replace the current
     *                 spectrum (which will be removed from the plot).
     */
    public void fitPoly( boolean selected, boolean replace )
    {
        //  Extract all ranges, obtain current spectrum, set up solver
        //  and do it. Add fit as a new spectrum and display.
        SpecData currentSpectrum = plot.getPlot().getCurrentSpectrum();
        if ( currentSpectrum == null ) {
            return;
        }
        double[] oldX = currentSpectrum.getXData();
        double[] oldY = currentSpectrum.getYData();
        double[] oldErr = currentSpectrum.getYDataErrors();

        int[] ranges = rangeList.extractRanges( selected, true, oldX );
        if ( ranges == null || ranges.length == 0 ) {
            return; // No ranges, so nothing to do.
        }

        //  Test for presence of BAD values in the data. These are
        //  left out of selected ranges.
        int n = 0;
        for ( int i = 0; i < ranges.length; i += 2 ) {
            int low = ranges[i];
            int high = Math.min( ranges[i+1], oldX.length - 1 );
            for ( int j = low; j <= high; j++ ) {
                if ( oldX[j] != SpecData.BAD && oldY[j] != SpecData.BAD ) {
                    n++;
                }
            }
        }

        //  Now allocate the necessary memory and copy in the data.
        double[] newX = new double[n];
        double[] newY = new double[n];
        double[] weights = null;
        if ( oldErr != null && errorsBox.isSelected() ) {
            weights = new double[n];
        }
        n = 0;
        if ( weights == null ) {
            for ( int i = 0; i < ranges.length; i += 2 ) {
                int low = ranges[i];
                int high = Math.min( ranges[i+1], oldX.length - 1 );
                for ( int j = low; j <= high; j++ ) {
                    if ( oldX[j] != SpecData.BAD && oldY[j] != SpecData.BAD ) {
                        newX[n] = oldX[j];
                        newY[n] = oldY[j];
                        n++;
                    }
                }
            }
        }
        else {
            for ( int i = 0; i < ranges.length; i += 2 ) {
               int low = ranges[i];
               int high = Math.min( ranges[i+1], oldX.length - 1 );
               for ( int j = low; j <= high; j++ ) {
                    if ( oldX[j] != SpecData.BAD && oldY[j] != SpecData.BAD ) {
                        newX[n] = oldX[j];
                        newY[n] = oldY[j];
                        weights[n] = 1.0 / ( oldErr[j] * oldErr[j] );
                        n++;
                    }
                }
            }
        }

        //  Fit the polynomial.
        PolynomialFitter fitter = null;
        if ( weights == null ) {
            fitter = new PolynomialFitter( getDegree(), newX, newY );
        }
        else {
            fitter = new PolynomialFitter( getDegree(), newX, newY,
                                           weights );
        }

        //  Create the positions.
        double[] fitY = fitter.evalYDataArray( oldX );

        //  Check for extreme outliers. These happen when the fit isn't
        //  realistic. We arbitrarily make these some multiple of the
        //  limits of the spectrum we're fitting.
        clipYData( fitY, currentSpectrum.getRange() );

        //  Decide what we're going to display. If replace has been selected,
        //  that's a cue to display one spectrum, if sensible. Displaying the
        //  polynomial by itself make's little sense, so when that's true we
        //  ignore replace.
        boolean displayFit = true;
        boolean displaySubtract = ( ! subtractNothing.isSelected() );
        boolean displayDivide = divideSpectrum.isSelected();
        if ( replace && ( displaySubtract || displayDivide ) ) {
            displayFit = false;
        }

        //  Display the polynomial fit, unless we're replacing the current
        //  spectrum and either subtracting or dividing.
        String name = "Polynomial Fit: " + (++fitCounter);
        displayFit( name, currentSpectrum, fitY, displayFit );

        //  Create and display the subtracted form of the spectrum. This
        //  replaces current spectrum, if we're replacing and subtracting.
        boolean replaceCurrent = ( replace && displaySubtract );
        if ( displaySubtract ) {
            subtractAndDisplayFit( currentSpectrum, name, fitY,
                                   replaceCurrent );
        }

        //  Create and display the normalised form of the spectrum. This
        //  replaces current spectrum, if we're replacing and not subtracting.
        if ( displayDivide ) {
            replaceCurrent = ( ( ! replaceCurrent ) && displayDivide );
            divideAndDisplayFit( currentSpectrum, name, fitY, replaceCurrent );
        }

        //  Make a report of the fit results.
        reportResults( name, fitter, newX, newY, ( weights != null ),
                       currentSpectrum  );
    }

    /**
     * Create and optionally display a new spectrum with the fit data values.
     */
    protected void displayFit( String name, SpecData spectrum,
                               double[] data, boolean display )
    {
        SpecData newSpec = createNewSpectrum( name, spectrum, data, null );
        if ( newSpec != null ) {
            try {
                newSpec.setType( SpecData.POLYNOMIAL );
                newSpec.setUseInAutoRanging( false );
                if ( display ) {
                    globalList.addSpectrum( plot.getPlot(), newSpec );
                }

                //  Default line is red and dot-dash.
                globalList.setKnownNumberProperty
                    ( newSpec, SpecData.LINE_COLOUR,
                      new Integer( Color.red.getRGB() ) );
                globalList.setKnownNumberProperty( newSpec,
                                                   SpecData.LINE_STYLE,
                                                   new Integer( 6 ) );
            }
            catch (Exception e) {
                // Do nothing.
                e.printStackTrace();
            }
        }
    }

    /**
     * Create a new spectrum with the given data and errors.
     *
     * @param name the short name for the new spectrum
     * @param spectrum a spectrum that this new spectrum is related
     *                 (i.e. has same coordinates).
     * @param data the new data values.
     * @param errors the new errors (if any, null for none).
     */
    protected SpecData createNewSpectrum( String name, SpecData spectrum,
                                          double[] data, double[] errors )
    {
        try {
            EditableSpecData newSpec =
                SpecDataFactory.getInstance().createEditable( name, spectrum );
            FrameSet frameSet = 
                ASTJ.get1DFrameSet( spectrum.getAst().getRef(), 1 );
            newSpec.setFullData( frameSet, spectrum.getCurrentDataUnits(),
                                 data, errors );
            globalList.add( newSpec );

            //  Keep a local reference so we can delete it, if asked
            //  to reset.
            localList.add( newSpec );

            return newSpec;
        }
        catch ( Exception e ) {
            //  Do nothing.
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Display the current spectrum, minus the fit in the current
     * plot. Replaces the current spectrum if requesed.
     *
     * The subtracted spectrum is created as a memory spectrum.
     * The default line colour is yellow.
     */
    protected void subtractAndDisplayFit( SpecData spectrum, String polyName,
                                          double[] fitData, boolean replace )
    {
        if ( subtractNothing.isSelected() ) return;

        //  Subtract the polynomial from the spectrum. Note this is
        //  noiseless so we can preserve any error information.
        double[] data = spectrum.getYData();
        String name;
        String specName = spectrum.getShortName();
        double[] newData;
        if ( subtractFromBelow.isSelected() ) {
            name = "Diff: (" + specName + ") - (" + polyName + ") ";
            newData = subtractData( data, fitData );
        }
        else {
            name = "Diff: (" + polyName + ") - (" + specName + ") ";
            newData = subtractData( fitData, data );
        }
        SpecData newSpec = createNewSpectrum( name, spectrum, newData,
                                              spectrum.getYDataErrors() );
        if ( newSpec != null ) {
            try {
                globalList.addSpectrum( plot.getPlot(), newSpec );
                if ( replace ) {
                    if ( removedCurrentSpectrum == null ) {
                        //  Only changed once until a reset.
                        removedCurrentSpectrum = spectrum;
                    }
                    plot.getPlot().removeSpectrum( spectrum );
                }

                //  Default line is gray.
                globalList.setKnownNumberProperty
                    ( newSpec, SpecData.LINE_COLOUR,
                      new Integer( Color.darkGray.getRGB() ) );

                //  Get the plot to scale itself to fit in Y.
                plot.getPlot().fitToHeight();
            }
            catch (Exception e) {
                // Do nothing.
                e.printStackTrace();
            }
        }
    }

    /**
     * Clip a data values array for extreme outliers. This is a bit arbitrary
     * and just looks for values that are 10 times the range. Outliers like
     * these result from a very bad fit, especially when extrapolating.
     */
    private void clipYData( double[] data, double[] limits )
    {
        int size = data.length;
        double range = 10.0 * Math.abs( limits[3] - limits[2] );
        double low = limits[2] - range;
        double high = limits[3] + range;

        for ( int i = 0; i < size; i++ ) {
            if ( data[i] < low || data[i] > high ) {
                data[i] = SpecData.BAD;
            }
        }
    }

    /**
     *  Subtract two data arrays that may contain BAD data.
     */
    protected double[] subtractData( double[] one, double[] two )
    {
        double[] result = new double[one.length];
        for ( int i = 0; i < one.length; i++ ) {
            if ( one[i] != SpecData.BAD && two[i] != SpecData.BAD ) {
                result[i] = one[i] - two[i];
            }
            else {
                result[i] = SpecData.BAD;
            }
        }
        return result;
    }

    /**
     * Display the current spectrum divided by the current fit if
     * requested. Replaces the current spectrum if requesed.
     *
     * The new spectrum is created as a memory spectrum.
     * The default line colour is cyan.
     */
    protected void divideAndDisplayFit( SpecData spectrum, String polyName,
                                        double[] fit, boolean replace )
    {
        if ( ! divideSpectrum.isSelected() ) return;

        String specName = spectrum.getShortName();
        double[] data = spectrum.getYData();
        double[] errors = spectrum.getYDataErrors();
        double[] newData = new double[data.length];
        double[] newErrors = null;
        if ( errors != null ) {
            newErrors = new double[data.length];
        }
        String name = "Ratio: (" + specName + ") by (" + polyName + ") ";
        divideData( data, errors, fit, newData, newErrors );
        SpecData newSpec = createNewSpectrum( name, spectrum, newData,
                                              newErrors );
        if ( newSpec != null ) {
            try {
                globalList.addSpectrum( plot.getPlot(), newSpec );
                if ( replace ) {
                    plot.getPlot().removeSpectrum( spectrum );
                    if ( removedCurrentSpectrum == null ) {
                        //  Only changed once until a reset.
                        removedCurrentSpectrum = spectrum;
                    }
                }

                //  Default line is cyan.
                globalList.setKnownNumberProperty
                    ( newSpec, SpecData.LINE_COLOUR,
                      new Integer( Color.cyan.getRGB() ) );

                //  Get the plot to scale itself to fit in Y.
                plot.getPlot().fitToHeight();
            }
            catch (Exception e) {
                // Do nothing
                e.printStackTrace();
            }
        }
    }

    /**
     *  Divide two data arrays that may contain BAD data and have
     *  associated errors.
     */
    protected void divideData( double[] inData, double[] inErrors,
                               double[] divData,
                               double[] outData, double[] outErrors )
    {
        if ( inErrors != null && outErrors != null ) {
            for ( int i = 0; i < inData.length; i++ ) {
                if ( inData[i] != SpecData.BAD &&
                     divData[i] != SpecData.BAD && divData[i] != 0.0 &&
                     inErrors[i] != SpecData.BAD
                   ) {
                    outData[i] = inData[i] / divData[i];
                    outErrors[i] = inErrors[i] / divData[i];
                }
                else {
                    outData[i] = SpecData.BAD;
                    outErrors[i] = SpecData.BAD;
                }
            }
        }
        else {
            for ( int i = 0; i < inData.length; i++ ) {
                if ( inData[i] != SpecData.BAD &&
                     divData[i] != SpecData.BAD && divData[i] != 0.0
                   ) {
                    outData[i] = inData[i] / divData[i];
                }
                else {
                    outData[i] = SpecData.BAD;
                }
            }
        }
    }

    /**
     * Write a simple report of the details of a fit to the results area.
     *
     * @param name the short name of the spectrum produced.
     * @param fitter the PolynomialFitter object used.
     * @param newX coordinates of the points used in fit.
     * @param newY values of the points used in fit.
     * @param usedWeights whether fit used weights. If true then
     *                    chi-square estimates are produced.
     * @param spectrum reference to the spectrum that has been fitted
     */
    protected void reportResults( String name, PolynomialFitter fitter,
                                  double[] newX, double[] newY,
                                  boolean usedWeights,
                                  SpecData spectrum  )
    {
        fitResults.append( "\n" );
        fitResults.append( "Results of fit\t: \"" +
                           spectrum.getShortName() + "\"\n");
        fitResults.append( "Fit short name\t \": " + name + "\"\n" );
        fitResults.append( "Degree\t: " + getDegree() + "\n" );
        if ( usedWeights ) {
            double[] chi = fitter.getChi();
            fitResults.append( "Chi square\t: " + chi[0] + "\n" );
            fitResults.append( "Chi probability\t: " + chi[1] + "\n" );
        }
        fitResults.append( "RMS\t: " +
                           fitter.calcRms( newX, newY ) + "\n" );
        fitResults.append( "Coefficients\n" );
        double[] coeffs = fitter.getCoeffs();
        if ( coeffs != null ) {
            for ( int i = 0; i < coeffs.length; i++ ) {
                fitResults.append( "\t: " + coeffs[i] + "\n" );
            }
        }
    }

    /**
     *  Close the window. Delete any ranges that are shown.
     */
    protected void closeWindowEvent()
    {
        rangeList.deleteAllRanges();
        this.dispose();
    }

    /**
     *  Delete all knowns spectral fits.
     */
    protected void deleteFits()
    {
        localList.deleteAll();
    }

    /**
     * Reset after a Replace fits. Disposes of all associated fits and
     * restores the current spectrum.
     */
    protected void resetReplaceActionEvent()
    {
        //  Remove any fitted spectra.
        deleteFits();

        //  Clear results description.
        fitResults.selectAll();
        fitResults.cut();

        //  Restore the current spectrum, if needed.
        if ( removedCurrentSpectrum != null ) {
            if ( ! plot.getPlot().isDisplayed( removedCurrentSpectrum )  ) {
                try {
                    plot.getPlot().addSpectrum( removedCurrentSpectrum );
                    removedCurrentSpectrum = null;
                }
                catch (SplatException e) {
                    //  Do nothing, not important.
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Reset all controls and dispose of all associated fits. Restore the
     * current spectrum, if recorded and not present.
     */
    protected void resetActionEvent()
    {
        resetReplaceActionEvent();

        //  Remove any graphics and ranges.
        rangeList.deleteAllRanges();

        //  Set polynomial degree to default.
        int degree = prefs.getInt( "PolyFitFrame_degree", 0 );
        degreeBox.setSelectedItem( new Integer( degree ) );
    }

    /**
     * Fit action. Performs fit to all ranges.
     */
    protected class FitAction extends AbstractAction
    {
        public FitAction( String name, Icon icon ) {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control F" ) );
        }
        public void actionPerformed( ActionEvent ae ) {
            fitPoly( false, false );
        }
    }

    /**
     * Fit action. Performs fit to all ranges and replaces current spectrum.
     */
    protected class FitReplaceAction extends AbstractAction
    {
        public FitReplaceAction( String name, Icon icon ) {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control L" ) );
        }
        public void actionPerformed( ActionEvent ae ) {
            fitPoly( false, true );
        }
    }

    /**
     * Fit selected action. Performs fit to the selected ranges.
     */
    protected class FitSelectedAction extends AbstractAction
    {
        public FitSelectedAction( String name, Icon icon ) {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control S" ) );
        }
        public void actionPerformed( ActionEvent ae ) {
            fitPoly( true, false );
        }
    }

    /**
     * Fit selected action. Performs fit to the selected ranges and
     * replaces current spectrum.
     */
    protected class FitReplaceSelectedAction extends AbstractAction
    {
        public FitReplaceSelectedAction( String name, Icon icon ) {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control T" ) );
        }
        public void actionPerformed( ActionEvent ae ) {
            fitPoly( true, true );
        }
    }

    /**
     * Inner class defining Action for closing window and keeping fit.
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
     * Inner class defining action for resetting after a replace.
     */
    protected class ResetReplaceAction extends AbstractAction
    {
        public ResetReplaceAction( String name, Icon icon ) {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control P" ) );
        }
        public void actionPerformed( ActionEvent ae ) {
            resetReplaceActionEvent();
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
            resetActionEvent();
        }
    }

    /**
     * Inner class defining action for deleting associated fits.
     */
    protected class DeleteFitsAction extends AbstractAction
    {
        public DeleteFitsAction( String name, Icon icon ) {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control I" ) );
        }
        public void actionPerformed( ActionEvent ae ) {
            deleteFits();
        }
    }
}
