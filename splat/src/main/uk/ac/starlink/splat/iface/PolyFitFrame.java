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
import javax.swing.border.TitledBorder;

import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.PolynomialFitter;
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
    protected JPanel botActionBar = new JPanel();

    /**
     *  Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu();
    protected JMenuItem closeFileMenu = new JMenuItem();
    protected JMenu helpMenu = new JMenu();
    protected JMenuItem helpMenuAbout = new JMenuItem();

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
    protected GlobalSpecPlotList globalList =
        GlobalSpecPlotList.getReference();

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
           "Degree of the polynomial (1=constant, 2=straight-line etc.)" );

        //  Set the possible polynomial degrees.
        for ( int i = 1; i < 15; i++ ) {
            degreeBox.addItem( new Integer( i ) );
        }

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
        subtractNothing.setSelected( true );

        //  Decide if we should generate and display a divided
        //  version of the spectrum.
        divideSpectrum.setToolTipText( "Divide spectrum by fit" );
        layouter.add( new JLabel( "Divide spectrum by fit:" ), false );
        layouter.add( divideSpectrum, true );

        //  List of regions of spectrum to fit.
        rangeList = new XGraphicsRangesView( plot.getPlot().getPlot() );
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
        setTitle( Utilities.getTitle( "Fit Polynomial to a Spectrum" ));
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        contentPane.add( actionBarContainer, BorderLayout.SOUTH );
        setSize( new Dimension( 550, 500 ) );
        setVisible( true );
    }

    /**
     * Return the current polynomial degree.
     */
    public int getDegree()
    {
        Integer degree = (Integer) degreeBox.getSelectedItem();
        return degree.intValue();
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
        ImageIcon helpImage = new ImageIcon(
            ImageHolder.class.getResource( "help.gif" ) );

        //  Create the File menu.
        fileMenu.setText( "File" );
        menuBar.add( fileMenu );

        //  Add action to do read a list of ranges from disk file.
        Action readAction = rangeList.getReadAction( "Read ranges",
                                                     readImage );
        fileMenu.add( readAction );

        //  Add action to save the extents to disk file.
        Action saveAction = rangeList.getWriteAction( "Save ranges",
                                                      saveImage );
        fileMenu.add( saveAction );

        //  Add action to do the fit.
        FitAction fitAction = new FitAction( "Fit", fitImage );
        fileMenu.add( fitAction );
        JButton fitButton = new JButton( fitAction );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( fitButton );
        fitButton.setToolTipText( "Fit the polynomial" );

        //  Add action to do the fit to selected.
        FitSelectedAction fitSelectedAction =
            new FitSelectedAction( "Fit selected", fitImage );
        fileMenu.add( fitSelectedAction );
        JButton fitSelectedButton = new JButton( fitSelectedAction );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( fitSelectedButton );
        fitSelectedButton.setToolTipText
            ( "Fit polynomial to selected ranges" );

        //  Add action to reset all values.
        ResetAction resetAction = new ResetAction( "Reset", resetImage );
        fileMenu.add( resetAction );
        JButton resetButton = new JButton( resetAction );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( resetButton );
        resetButton.setToolTipText
            ( "Reset all values and clear all generated fits" );

        //  Add action to just delete any fits.
        DeleteFitsAction deleteFitsAction =
            new DeleteFitsAction( "Delete fits", deleteImage );
        fileMenu.add( deleteFitsAction );
        JButton deleteFitsButton = new JButton( deleteFitsAction );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( deleteFitsButton );
        deleteFitsButton.setToolTipText( "Delete all generated fits" );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction );
        JButton closeButton = new JButton( closeAction );
        botActionBar.add( Box.createGlue() );
        botActionBar.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        topActionBar.add( Box.createGlue() );
        botActionBar.add( Box.createGlue() );
        actionBarContainer.setLayout( new BorderLayout() );
        actionBarContainer.add( topActionBar, BorderLayout.NORTH );
        actionBarContainer.add( botActionBar, BorderLayout.SOUTH );

        //  Create the Help menu.
        HelpFrame.createHelpMenu( "polynomial-fit-window", "Help on window",
                                  menuBar, null );


    }

    /**
     *  Perform the fit, to either all the ranges or just the selected
     *  ones.
     *
     *  @param selected true if we should just fit to selected ranges.
     */
    public void fitPoly( boolean selected )
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

        int[] ranges = extractRanges( selected, oldX );
        if ( ranges.length == 0 ) {
            return; // No ranges, so nothing to do.
        }

        //  Test for presence of BAD values in the data. These are
        //  left out of selected ranges.
        int n = 0;
        for ( int i = 0; i < ranges.length; i += 2 ) {
            int low = ranges[i];
            int high = Math.min( ranges[i+1], oldX.length );
            for ( int j = low; j < high; j++ ) {
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
                for ( int j = ranges[i];
                  j < Math.min(ranges[i+1], oldX.length); j++ ) {
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
                for ( int j = ranges[i];
                  j < Math.min(ranges[i+1], oldX.length); j++ ) {
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
            fitter = new PolynomialFitter( getDegree(), newX, newY, weights );
        }

        //  Create the positions and display them as a spectrum in the
        //  plot.
        double[] fitY = fitter.evalArray( oldX );
        String name = "Polynomial Fit: " + (++fitCounter);
        displayFit( name, currentSpectrum, fitY, null );

        //  Also create and display the subtracted form of the
        //  spectrum.
        subtractAndDisplayFit( currentSpectrum, name, fitY );

        //  And the normalized form.
        divideAndDisplayFit( currentSpectrum, name, fitY );

        //  Make a report of the fit results.
        reportResults( name, fitter, newX, newY, ( weights != null ),
                       currentSpectrum  );
    }

    /**
     * Create and display a new spectrum with the fit data values.
     */
    protected void displayFit( String name, SpecData spectrum, 
                               double[] data, double[] errors )
    {
        SpecData newSpec = createNewSpectrum( name, spectrum, data, errors );
        if ( newSpec != null ) {
            try {
                newSpec.setType( SpecData.POLYNOMIAL );
                newSpec.setUseInAutoRanging( false );
                globalList.addSpectrum( plot.getPlot(), newSpec );

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
                SpecDataFactory.getReference().createEditable( name );
            if ( errors == null ) {
                newSpec.setData( spectrum.getFrameSet(), data );
            }
            else {
                newSpec.setData( spectrum.getFrameSet(), data, errors );
            }
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
     * plot. The subtracted spectrum is created as a memory spectrum
     * The default line colour is yellow.
     */
    protected void subtractAndDisplayFit( SpecData spectrum, String polyName,
                                          double[] fitData )
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
     * requested.  The new spectrum is created as a memory spectrum
     * The default line colour is cyan.
     */
    protected void divideAndDisplayFit( SpecData spectrum, String polyName,
                                        double[] fit )
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
        for ( int i = 0; i < coeffs.length; i++ ) {
            fitResults.append( "\t: " + coeffs[i] + "\n" );
        }
    }

    /**
     * Return an array of indices that map the ranges of the fit
     * regions into indices of the spectrum.
     *
     * @param selected whether to just return the ranges selected.
     * @param oldX coordinates of the spectrum.
     * @return pairs of indices in oldX that cover the graphics
     *         ranges. Returned as null if none exist.
     */
    protected int[] extractRanges( boolean selected, double[] oldX )
    {
        double[] worldRanges = rangeList.getRanges( selected );
        if ( worldRanges != null ) {
            int[] arrayRanges = new int[worldRanges.length];
            for ( int i = 0; i < worldRanges.length; i++ ) {
                arrayRanges[i] = lookup( worldRanges[i], oldX );
            }

            //  Check ordering, these can be reversed.
            int temp;
            for ( int i = 0; i < worldRanges.length; i+=2 ) {
                if ( arrayRanges[i] > arrayRanges[i+1] ) {
                    temp = arrayRanges[i];
                    arrayRanges[i] = arrayRanges[i+1];
                    arrayRanges[i+1] = temp;
                }
            }
            return arrayRanges;
        }
        return null;
    }

    /**
     * Lookup an array index that most closely presents a given
     * value. The array of values should be sorted.
     *
     * @param value the value to lookup.
     * @param array the available values to interpolate.
     *
     * @return index of the closest value.
     */
    protected int lookup( double value, double[] array )
    {
        //  Look for the two data values that are nearest in the
        //  array. Use a binary search as values are sorted.
        int low = 0;
        int high = array.length - 1;
        int mid = 0;
        if ( array[0] < array[high] ) {
            while ( low < high - 1 ) {
                mid = ( low + high ) / 2;
                if ( value < array[mid] ) {
                    high = mid;
                }
                else if ( value > array[mid] ) {
                    low = mid;
                }
                else {
                    //  Exact match.
                    low = high = mid;
                    break;
                }
            }
        }
        else {
            while ( low < high - 1 ) {
                mid = ( low + high ) / 2;
                if ( value > array[mid] ) {
                    high = mid;
                }
                else if ( value < array[mid] ) {
                    low = mid;
                }
                else {
                    //  Exact match.
                    low = high = mid;
                    break;
                }
            }
        }

        //  Find which position is nearest in reality.
        int index = 0;
        if ( ( value - array[low] ) < ( array[high] - value ) ) {
            index = low;
        }
        else {
            index = high;
        }
        return index;
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
     * Reset all controls and dispose of all associated fits.
     */
    protected void resetActionEvent()
    {
        //  Remove any spectra.
        deleteFits();

        //  Clear results description.
        fitResults.selectAll();
        fitResults.cut();

        //  Remove any graphics and ranges.
        rangeList.deleteAllRanges();

        //  Set polynomial degree to 1.
        degreeBox.setSelectedIndex( 0 );
    }

    /**
     * Fit action. Performs fit to all ranges.
     */
    protected class FitAction extends AbstractAction
    {
        public FitAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            fitPoly( false );
        }
    }

    /**
     * Fit selected action. Performs fit to the selected ranges.
     */
    protected class FitSelectedAction extends AbstractAction
    {
        public FitSelectedAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            fitPoly( true );
        }
    }

    /**
     * Inner class defining Action for closing window and keeping fit.
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
     * Inner class defining action for deleting associated fits.
     */
    protected class DeleteFitsAction extends AbstractAction
    {
        public DeleteFitsAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            deleteFits();
        }
    }
}
