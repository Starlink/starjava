/*
 * Copyright (C) 2001-2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     19-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.GaussianFitter;
import uk.ac.starlink.splat.util.GridBagLayouter;
import uk.ac.starlink.splat.util.LorentzFitter;
import uk.ac.starlink.splat.util.QuickLineFitter;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.util.VoigtFitter;

/**
 * Provides a toolbox for fitting spectral lines using a variety of
 * different models and displaying the results of the fits.
 * <p>
 * Spectral lines are identified as a range of X values in the current
 * spectrum using a rectangle figure. The data in this region is
 * initially fitted using an "abline" algorithm (known as Quick) that
 * makes robust estimates of the centre, width and height. Further
 * fits can then be made using Gaussian, Lorentzian and Voigt shaped
 * profiles.
 * <p>
 * The results are shown as a series of tabulated values in a tabbed
 * pane, with a pane for each of the fit types selected, Quick is
 * always available. The ranges of the fits are shown and controlled
 * by a separate table. These ranges can be saved and restored to
 * disk-file. The measurements can only be saved.
 * <p>
 * When closed this window will be hidden, not disposed. If this
 * therefore necessary that the user disposes of it when it is really
 * no longer required.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see PlotControlFrame
 */
public class LineFitFrame 
    extends JFrame 
    implements PlotListener
{
    /**
     * The list of all the spectra that we've created.
     */
    protected SpecSubList localList = new SpecSubList();

    /**
     * Content pane of frame.
     */
    protected JPanel contentPane = null;

    /**
     * Action buttons container.
     */
    protected JPanel actionBar = new JPanel();

    /**
     * Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu();
    protected JMenuItem closeFileMenu = new JMenuItem();

    /**
     * The PlotControlFrame that specifies the current spectrum.
     */
    protected PlotControlFrame plot = null;

    /**
     * JComboBox that displays the list of available spectrum to use
     * as the background.
     */
    protected JComboBox backgroundSpectra = new JComboBox();
    protected JLabel backgroundLabel = new JLabel();
    protected boolean backgroundWarningIssued = false;

    /**
     * What types of measurements to perform: Gaussian, Lorentzian or Voigt.
     */
    protected JLabel fitWhat = new JLabel( "Types of fit:" );
    protected JCheckBox fitGaussians = new JCheckBox();
    protected JCheckBox fitLorentzians = new JCheckBox();
    protected JCheckBox fitVoigts = new JCheckBox();

    /**
     *  Reference to GlobalSpecPlotList object.
     */
    protected GlobalSpecPlotList globalList =
        GlobalSpecPlotList.getReference();

    /**
     *  Ranges of data that are to be fitted.
     */
    protected XGraphicsRangesView rangeList = null;

    /**
     * Reference to LineView for displaying the line measurements.
     */
    protected LineFitView lineView = null;

    /**
     * Control for saying whether to use errors as weights for the fits.
     */
    protected JCheckBox errors = new JCheckBox();
    protected JLabel errorLabel = new JLabel();

    /**
     *  Number of fits done so far (used as unique identifier for
     * generating shortnames for the spectra created).
     */
    protected static int fitCounter = 0;

    /**
     * Create an instance.
     */
    public LineFitFrame( PlotControlFrame plot )
    {
        contentPane = (JPanel) getContentPane();
        setPlot( plot );
        initUI();
        initMenus();
        initFrame();
    }

    /**
     * Get the PlotControlFrame that is displaying the spectrum we're
     * to fit.
     *
     * @return reference to the PlotControlFrame.
     */
    public PlotControlFrame getPlot()
    {
        return plot;
    }

    /**
     * Set the PlotControlFrame that is displaying the spectrum that
     * we're to fit.
     *
     * @param plot the PlotControlFrame reference.
     */
    public void setPlot( PlotControlFrame plot )
    {
        this.plot = plot;
        lineView = new LineFitView( plot.getPlot().getPlot() );

        //  We are listening to plot changes.
        globalList.addPlotListener( this );
        updateNames();
    }

    /**
     * Free any locally allocated resources.
     */
    public void finalize() throws Throwable
    {
        globalList.removePlotListener( this );
        super.finalize();
    }

    /**
     * Initialise the main part of the user interface.
     */
    protected void initUI()
    {
        //  This part of UI goes into a JPanel in the centre, which is
        //  split vertically into two equal parts, one for the table
        //  of ranges and the tabbed pane of measurements.
        JPanel centre = new JPanel( new GridLayout( 2, 1 ) );
        JPanel top = new JPanel();
        JPanel bottom = new JPanel( new BorderLayout() );
        contentPane.add( centre, BorderLayout.CENTER );
        centre.add( top );
        centre.add( bottom );

        GridBagLayouter layouter =
            new GridBagLayouter( top, GridBagLayouter.SCHEME4 );
        layouter.setInsets( new java.awt.Insets( 5, 5, 5, 5 ) );

        //  Setup the choices of fits we offer.
        layouter.add( fitWhat, false );

        GaussianAction gaussAction = new GaussianAction();
        fitGaussians.setAction( gaussAction );
        fitGaussians.setToolTipText( "Select to fit Gaussians" );
        layouter.add( fitGaussians, false );
        fitGaussians.setSelected( true );
        changeGaussianFitsEvent();

        LorentzAction lorentzAction = new LorentzAction();
        fitLorentzians.setAction( lorentzAction );
        fitLorentzians.setToolTipText( "Select to fit Lorentzians" );
        layouter.add( fitLorentzians, false );
        fitLorentzians.setSelected( false );
        changeLorentzFitsEvent();

        VoigtAction voigtAction = new VoigtAction();
        fitVoigts.setAction( voigtAction );
        fitVoigts.setToolTipText( "Select to fit Voigt profiles" );
        layouter.add( fitVoigts, false );
        changeVoigtFitsEvent();
        layouter.add( Box.createHorizontalBox(), true );

        //  Add controls for choosing the background spectrum.
        backgroundLabel.setText( "Background fit:" );
        backgroundLabel.setForeground( Color.red );
        layouter.add( backgroundLabel, false );
        String tipText = "Select a spectrum to use as background, "+
            "should normally be a fit (*)";
        backgroundLabel.setToolTipText( tipText );

        backgroundSpectra.setRenderer( new LineRenderer() );
        layouter.add( backgroundSpectra, true );
        backgroundSpectra.setToolTipText( tipText );

        //  Add controls for using any errors as weights.
        errorLabel.setText( "Use errors as weights:" );
        layouter.add( errorLabel, false );
        layouter.add( errors, true );
        errors.setToolTipText
            ( "Use errors as weights when fitting (if available)" );

        //  Add the XGraphicsRangesView that displays the spectral
        //  line ranages.
        rangeList = new XGraphicsRangesView( plot.getPlot().getPlot(),
                                             Color.yellow, false );
        layouter.add( rangeList, true );

        //  Add the LineView that displays and creates the line
        //  properties objects.
        bottom.add( lineView , BorderLayout.CENTER );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Measure Spectral Lines" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        contentPane.add( actionBar, BorderLayout.SOUTH );
        setSize( new Dimension( 500, 600 ) );
        setVisible( true );
    }

    /**
     * Initialise the menu bar, action bar and related actions.
     */
    protected void initMenus()
    {
        //  Add the menuBar.
        setJMenuBar( menuBar );

        //  Action bar uses a BoxLayout.
        actionBar.setLayout( new BoxLayout( actionBar, BoxLayout.X_AXIS ) );
        actionBar.setBorder( BorderFactory.createEmptyBorder( 10, 5, 5, 10 ) );

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

        //  Add action to do read a list of extents for fitting the lines.
        Action readAction = rangeList.getReadAction( "Read line extents",
                                                     readImage );
        fileMenu.add( readAction );

        //  Add action to save the extents to disk file.
        Action saveAction = rangeList.getWriteAction( "Save line extents",
                                                      saveImage );
        fileMenu.add( saveAction );

        //  Add action to save the fits results to disk file.
        Action fitSaveAction = lineView.getWriteAction( "Save line fits",
                                                        saveImage );
        fileMenu.add( fitSaveAction );

        //  Add action to do the fit.
        FitAction fitAction = new FitAction( "Fit", fitImage );
        fileMenu.add( fitAction );
        JButton fitButton = new JButton( fitAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( fitButton );
        fitButton.setToolTipText( "Fit spectral lines" );

        //  Add action to reset all values.
        ResetAction resetAction = new ResetAction( "Reset", resetImage );
        fileMenu.add( resetAction );
        JButton resetButton = new JButton( resetAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( resetButton );
        resetButton.setToolTipText( "Reset, clearing all associated fits" );

        //  Add action to just clear the line fits.
        DeleteFitsAction deleteFitsAction =
            new DeleteFitsAction( "Delete fits", deleteImage );
        fileMenu.add( deleteFitsAction );
        JButton deleteFitsButton = new JButton( deleteFitsAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( deleteFitsButton );
        deleteFitsButton.setToolTipText( "Delete all associated fits" );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction );
        JButton closeButton = new JButton( closeAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        actionBar.add( Box.createGlue() );

        //  Add a help menu and the topic about this window.
        HelpFrame.createHelpMenu( "line-fit-window", "Help on window",
                                  menuBar, null );
    }

    /**
     *  Close the window. Delete any related graphics.
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
     * Reset everything to the default values.
     */
    protected void resetActionEvent()
    {
        //  Remove any spectra.
        deleteFits();

        //  Clear results description.
        lineView.clear();

        //  Remove any graphics and ranges.
        rangeList.deleteAllRanges();
    }

    /**
     *  Update the spectra available for the background.
     */
    protected void updateNames()
    {
        //  Keep reference to current background spectrum.
        SpecData currentSpectrum = getBackgroundSpectrum( true );

        //  Re-create a vector/model for this. Faster as avoids
        //  listener updates also gets resize right.
        Vector v = new Vector( globalList.specCount() );
        for ( int i = 0; i < globalList.specCount(); i++ ) {
            v.add( globalList.getSpectrum( i ) );
        }
        ComboBoxModel model = new DefaultComboBoxModel( v );
        backgroundSpectra.setModel( model );

        //  Restore selected spectrum.
        if ( currentSpectrum != null && globalList.specCount() > 1 ) {
            model.setSelectedItem( currentSpectrum );
        }
        else {
            //  Select the first polynomial fit that we can discover.
            boolean located = false;
            for ( int i = 0; i < globalList.specCount(); i++ ) {
                if ( globalList.getSpectrum( i ).getType() ==
                     SpecData.POLYNOMIAL ) {
                    located = true;
                    model.setSelectedItem( globalList.getSpectrum( i ) );
                    break;
                }
            }
            if ( ! located && ! backgroundWarningIssued ) {
                JOptionPane.showMessageDialog
                    ( this,
                      "You do not seem to have any fits to a local \n"+
                      "or global background for any of your spectra. \n"+
                      "Use the polynomial fitting tool to perform \n"+
                      "this task first (unless you have an existing \n"+
                      "spectrum that defines the background for your \n"+
                      "lines, or you have a background subtracted \n"+
                      "spectrum). ",
                      "No known background",
                      JOptionPane.INFORMATION_MESSAGE );

                //  After window is going suppress warning which should only
                //  be seen once.
                backgroundWarningIssued = true;
            }
        }
    }

    /**
     * Access the selected background spectrum. If selfOK is false
     * then returns null if this is the same as the current spectrum.
     *
     * @param selfOK true if OK for background to be same as spectrum
     *               to fit.
     * @return reference to the background spectrum, null for none.
     */
    protected SpecData getBackgroundSpectrum( boolean selfOK )
    {
        SpecData backgroundSpectrum =
            (SpecData) backgroundSpectra.getSelectedItem();
        if ( backgroundSpectrum == null ) {
            return null;
        }
        SpecData currentSpectrum = plot.getPlot().getCurrentSpectrum();
        if ( ! selfOK && currentSpectrum.equals( backgroundSpectrum ) ) {
            return null;
        }
        return backgroundSpectrum;
    }

    /**
     *  Fit all lines using the currently selected fitting types.
     */
    public void fitLines()
    {
        //  Extract all ranges, obtain current and background spectra,
        //  set up gaussian solver and do it. Add line fit as a new
        //  spectrum and display.
        SpecData currentSpectrum = plot.getPlot().getCurrentSpectrum();
        double[] specXData = currentSpectrum.getXData();
        double[] specYData = currentSpectrum.getYData();
        double[] specYDataErrors = null;
        if ( errors.isSelected() ) {
            specYDataErrors = currentSpectrum.getYDataErrors();
        }

        //  If no ranges are defined, then nothing to do.
        int[] ranges = extractLineRanges( specXData );
        if ( ranges.length == 0 ) {
            JOptionPane.showMessageDialog( this,
                                           "You have not selected any lines",
                                           "No lines",
                                           JOptionPane.INFORMATION_MESSAGE );
            return;
        }
        SpecData backgroundSpectrum = getBackgroundSpectrum( false );
        double[] backYData = null;
        if ( backgroundSpectrum != null ) {
            //  Get background values that match the positions of the
            //  spectrum we're going to fit. TODO: record associated
            //  spectrum with background, that would usually avoid
            //  this calculation? Need full evaluation so that we can
            //  add background to fits.
            backYData = backgroundSpectrum.evalYDataArray( specXData );
        } else {
            JOptionPane.showMessageDialog( this,
                                           "Using a zero background",
                                           "background null",
                                           JOptionPane.INFORMATION_MESSAGE );
        }

        // Loop over each line.
        for ( int i = 0, nline = 0; i < ranges.length; i +=2, nline++ ) {

            //  Extract data associated with the line. Note use
            //  extraction, rather than just passing ranging integers
            //  as data points could be missing.
            ArrayList extracts = new ArrayList( 4 );
            int n = extractLineData( ranges[i], ranges[i+1],
                                     specXData, specYData,
                                     specYDataErrors,
                                     backYData, extracts );
            if ( n == 0 ) {
                JOptionPane.showMessageDialog( this,
                                               "Line contains no valid data",
                                               "all bad",
                                               JOptionPane.INFORMATION_MESSAGE );
                return;
            }
            double[] XSpecXData = (double[]) extracts.get( 0 );
            double[] XSpecYData = (double[]) extracts.get( 1 );
            double[] XSpecYDataWeights = (double[]) extracts.get( 2 );
            double[] XBackYData = (double[]) extracts.get( 3 );

            //  Increment Fit counter (so all names look associated).
            fitCounter++;

            //  Get some useful line parameters that we can as guesses
            //  for the minimisation.
            double[] guess = quickFit( nline, XSpecXData, XSpecYData,
                                       XBackYData );

            //  Fit the line.
            if ( fitGaussians.isSelected() ) {
                doFitGaussian( nline, XSpecXData, XSpecYData,
                               XSpecYDataWeights, guess[1], guess[2],
                               guess[3], specXData, backYData );

            }
            if ( fitLorentzians.isSelected() ) {
                doFitLorentzian( nline, XSpecXData, XSpecYData,
                                 XSpecYDataWeights, guess[1],
                                 guess[2], guess[3], specXData, backYData );
            }
            if ( fitVoigts.isSelected() ) {
                doFitVoigts( nline, XSpecXData, XSpecYData,
                             XSpecYDataWeights, guess[1], guess[2],
                             guess[3], specXData, backYData );
            }
        }
    }

    /**
     * Extract the data associated with a line range. Returns the
     * number of elements extracted (which will be zero if all
     * elements are BAD).
     *
     * @param lower starting index for extraction range.
     * @param upper finishing index for extraction range.
     * @param specXData the coordinates of the complete spectrum.
     * @param specYData the data value of the complete spectrum.
     * @param specYDataErrors the errors of the data values, null for none.
     * @param backYData data values of the background, these should
     *                  match the data values of the spectrum (i.e. be
     *                  at same coordinates), null for none.
     * @param extracts ArrayList for containing references to the
     *                 four extracted double arrays. These are the
     *                 extracted coordinates, the extracted background
     *                 subtracted (if appropriate) data values, the
     *                 extracted data errors converted to weights, if
     *                 any, and the extracted background data, if
     *                 any. Missing arrays are set to null.
     * @return the number of position extracted.
     */
    protected int extractLineData( int lower, int upper,
                                   double[] specXData,
                                   double[] specYData,
                                   double[] specYDataErrors,
                                   double[] backYData,
                                   ArrayList extracts )
    {
        int n = 0;
        int low = lower;
        int high = Math.min( upper, specXData.length );
        double[] XSpecXData = null;
        double[] XSpecYData = null;
        double[] XSpecYDataWeights = null;
        double[] XBackYData = null;
        if ( backYData != null && specYDataErrors != null ) {

            //  Have errors and background data. Use first pass to
            //  count how many good values we have.
            n = 0;
            for ( int j = low; j < high; j++ ) {
                if ( specXData[j] != SpecData.BAD &&
                     specYData[j] != SpecData.BAD &&
                     backYData[j] != SpecData.BAD ) {
                    n++;
                }
            }
            if ( n != 0 ) {
                XSpecXData = new double[n];
                XSpecYData = new double[n];
                XSpecYDataWeights = new double[n];
                XBackYData = new double[n];
                n = 0;
                for ( int j = low; j < high; j++ ) {
                    if ( specXData[j] != SpecData.BAD &&
                         specYData[j] != SpecData.BAD &&
                         backYData[j] != SpecData.BAD ) {

                        XSpecXData[n] = specXData[j];
                        XSpecYData[n] = specYData[j] - backYData[j];
                        XBackYData[n] = backYData[j];
                        XSpecYDataWeights[n] = 1.0 /
                            ( specYDataErrors[j] * specYDataErrors[j] );
                        n++;
                    }
                }
            }
        } else if ( backYData != null ) {

            //  Have background data and no errors.
            n = 0;
            for ( int j = low; j < high; j++ ) {
                if ( specXData[j] != SpecData.BAD &&
                     specYData[j] != SpecData.BAD &&
                     backYData[j] != SpecData.BAD ) {
                    n++;
                }
            }
            if ( n != 0 ) {
                XSpecXData = new double[n];
                XSpecYData = new double[n];
                XBackYData = new double[n];
                n = 0;
                for ( int j = low; j < high; j++ ) {
                    if ( specXData[j] != SpecData.BAD &&
                         specYData[j] != SpecData.BAD &&
                         backYData[j] != SpecData.BAD ) {

                        XSpecXData[n] = specXData[j];
                        XSpecYData[n] = specYData[j] - backYData[j];
                        XBackYData[n] = backYData[j];
                        n++;
                    }
                }
            }
        } else if ( specYDataErrors != null ) {

            //  Have errors but no background data.
            n = 0;
            for ( int j = low; j < high; j++ ) {
                if ( specXData[j] != SpecData.BAD &&
                     specYData[j] != SpecData.BAD ) {
                    n++;
                }
            }
            if ( n != 0 ) {
                XSpecXData = new double[n];
                XSpecYData = new double[n];
                XSpecYDataWeights = new double[n];
                n = 0;
                for ( int j = low; j < high; j++ ) {
                    if ( specXData[j] != SpecData.BAD &&
                         specYData[j] != SpecData.BAD ) {

                        XSpecXData[n] = specXData[j];
                        XSpecYData[n] = specYData[j];
                        XSpecYDataWeights[n] = 1.0 /
                            ( specYDataErrors[j] * specYDataErrors[j] );
                        n++;
                    }
                }
            }
        } else {

            //  Have no errors or background data.
            n = 0;
            for ( int j = low; j < high; j++ ) {
                if ( specXData[j] != SpecData.BAD &&
                     specYData[j] != SpecData.BAD ) {
                    n++;
                }
            }
            if ( n != 0 ) {
                XSpecXData = new double[n];
                XSpecYData = new double[n];
                n = 0;
                for ( int j = low; j < high; j++ ) {
                    if ( specXData[j] != SpecData.BAD &&
                         specYData[j] != SpecData.BAD ) {

                        XSpecXData[n] = specXData[j];
                        XSpecYData[n] = specYData[j];
                        n++;
                    }
                }
            }
        }
        extracts.add( 0, XSpecXData );
        extracts.add( 1, XSpecYData );
        extracts.add( 2, XSpecYDataWeights );
        extracts.add( 3, XBackYData );
        return n;
    }

    /**
     * Perform a "quick" fit of a line. All data areas are considered
     * and should be extracted. Based on the ABLINE algorithm from
     * Figaro. The background can be null (and is considered to be
     * zero everywhere and hence has infinite equivalent width == 0).
     *
     * The results are the guess for peak, centre and width.
     */
    public double[] quickFit( int lineIndex, double[] coords,
                              double[] data, double[] background )
    {
        QuickLineFitter fitter = new QuickLineFitter( coords, data,
                                                      background );
        double[] results = new double[6];
        results[0] = lineIndex;
        results[1] = fitter.getPeak();
        results[2] = fitter.getCentre();
        results[3] = 0.5 * fitter.getWidth();
        results[4] = fitter.getEquivalentWidth();
        results[5] = fitter.getAsymmetry();

        boolean abs = fitter.isAbsorption();
        if ( abs ) {
            results[1] = -results[1];
        }

        //  Create a false spectrum that represents these results.
        double[] fitY = new double[3];
        double[] fitX = new double[3];

        fitX[0] = results[2] - results[3];
        fitX[1] = results[2];
        fitX[2] = results[2] + results[3];
        if ( background != null ) {
            int left = lookup( fitX[0], coords );
            int centre = lookup( fitX[1], coords );
            int right = lookup( fitX[2], coords );
            fitY[0] = results[1] * 0.5 + background[left];
            fitY[1] = results[1]       + background[centre];
            fitY[2] = results[1] * 0.5 + background[right];
        } else {
            fitY[0] = results[1] * 0.5;
            fitY[1] = results[1];
            fitY[2] = results[1] * 0.5;
        }
        displayFit( "Quick Fit: " + fitCounter, fitX, fitY, 3 );

        //  Make these viewable.
        lineView.addOrUpdateLine( LineProperties.QUICK, results );
        return results;
    }

    /**
     * Fit a gaussian to given extracted spectral data. The results
     * are added to the global list of spectra, displayed in the
     * current plot and added to the LineView.
     *
     * @param fitCoords the spectral coordinates to be fitted.
     * @param fitData the spectral data values, one per fitCoords.
     * @param fitWeights the spectral data value weights, if any.
     * @param peak initial guess for gaussian peak.
     * @param centre initial guess for gaussian centre.
     * @param width initial guess for gaussian width.
     * @param genCoords the spectral coordinates at which the fit is
     *                  to be evaluated (usually larger than region to
     *                  be fit).
     * @param genBackground the background values for each coordinate
     *                      in genCoords (this is added to the fit).
     */
    protected void doFitGaussian( int lineIndex, double[] fitCoords,
                                  double[] fitData, double[] fitWeights,
                                  double peak, double centre,
                                  double width, double[] genCoords,
                                  double[] genBackground )
    {
        GaussianFitter fitter = null;
        if ( fitWeights == null ) {
            fitter = new GaussianFitter( fitCoords, fitData, peak,
                                         centre, width );
        } else {
            fitter = new GaussianFitter( fitCoords, fitData,
                                         fitWeights, peak, centre, width );
        }
        double[] fitY = fitter.evalArray( genCoords );

        //  Add the background to fit.
        if ( genBackground != null ) {
            for ( int l = 0; l < fitY.length; l++ ) {
                fitY[l] += genBackground[l];
            }
        }
        displayFit( "Gaussian Fit: " + fitCounter, genCoords, fitY, 0 );

        //  Add results to view.
        double[] results = new double[6];
        results[0] = lineIndex;
        results[1] = fitter.getScale();
        results[2] = fitter.getCentre();
        results[3] = fitter.getSigma();
        results[4] = fitter.getFlux();
        results[5] = fitter.calcRms( fitCoords, fitData );
        lineView.addOrUpdateLine( LineProperties.GAUSS, results );
    }

    /**
     * Fit a lorentzian to given extracted spectral data. The results
     * are added to the global list of spectra, displayed in the
     * current plot and added to the LineView.
     *
     * @param fitCoords the spectral coordinates to be fitted.
     * @param fitData the spectral data values, one per fitCoords.
     * @param fitWeights the spectral data value weights, if any.
     * @param peak initial guess for lorentzian peak.
     * @param centre initial guess for lorentzian centre.
     * @param width initial guess for lorentzian width.
     * @param genCoords the spectral coordinates at which the fit is
     *                  to be evaluated (usually larger than region to
     *                  be fit).
     * @param genBackground the background values for each coordinate
     *                      in genCoords (this is added to the fit).
     */
    protected void doFitLorentzian( int lineIndex, double[] fitCoords,
                                    double[] fitData, double[] fitWeights,
                                    double peak, double centre,
                                    double width, double[] genCoords,
                                    double[] genBackground )
    {
        LorentzFitter fitter = null;
        if ( fitWeights == null ) {
            fitter = new LorentzFitter( fitCoords, fitData, peak,
                                        centre, width );
        } else {
            fitter = new LorentzFitter( fitCoords, fitData,
                                        fitWeights, peak, centre, width );
        }
        double[] fitY = fitter.evalArray( genCoords );

        //  Add the background to fit.
        if ( genBackground != null ) {
            for ( int l = 0; l < fitY.length; l++ ) {
                fitY[l] += genBackground[l];
            }
        }
        displayFit( "Lorentzian Fit: " + fitCounter, genCoords, fitY, 1 );

        //  Add results to view.
        double[] results = new double[6];
        results[0] = lineIndex;
        results[1] = fitter.getScale();
        results[2] = fitter.getCentre();
        results[3] = fitter.getWidth();
        results[4] = fitter.getFlux();
        results[5] = fitter.calcRms( fitCoords, fitData );
        lineView.addOrUpdateLine( LineProperties.LORENTZ, results );
    }

    /**
     * Fit a voigt profile to given extracted spectral data. The results
     * are added to the global list of spectra, displayed in the
     * current plot and added to the LineView.
     *
     * @param fitCoords the spectral coordinates to be fitted.
     * @param fitData the spectral data values, one per fitCoords.
     * @param fitWeights the spectral data value weights, if any.
     * @param peak initial guess for voigt peak.
     * @param centre initial guess for voigt centre.
     * @param width initial guess for total width.
     * @param genCoords the spectral coordinates at which the fit is
     *                  to be evaluated (usually larger than region to
     *                  be fit).
     * @param genBackground the background values for each coordinate
     *                      in genCoords (this is added to the fit).
     */
    protected void doFitVoigts( int lineIndex, double[] fitCoords,
                                double[] fitData, double[] fitWeights,
                                double peak, double centre,
                                double width, double[] genCoords,
                                double[] genBackground )
    {
        VoigtFitter fitter = null;
        if ( fitWeights == null ) {
            fitter = new VoigtFitter( fitCoords, fitData, peak,
                                      centre, width * 0.25, width *.75 );
        } else {
            fitter = new VoigtFitter( fitCoords, fitData, fitWeights,
                                      peak, centre, width * 0.25, width *.75 );
        }
        double[] fitY = fitter.evalArray( genCoords );

        //  Add the background to fit.
        if ( genBackground != null ) {
            for ( int l = 0; l < fitY.length; l++ ) {
                fitY[l] += genBackground[l];
            }
        }
        displayFit( "Voigt Fit: " + fitCounter, genCoords, fitY, 2 );

        //  Add results to view.
        double[] results = new double[7];
        results[0] = lineIndex;
        results[1] = fitter.getScale();
        results[2] = fitter.getCentre();
        results[3] = fitter.getGWidth();
        results[4] = fitter.getLWidth();
        results[5] = fitter.getFlux();
        results[6] = fitter.calcRms( fitCoords, fitData );
        lineView.addOrUpdateLine( LineProperties.VOIGT, results );
    }

    /**
     *  Display a line fit as a spectrum.
     */
    protected void displayFit( String name, double[] coords,
                               double[] data, int colourScheme )
    {
        //  Create a memory spectrum to contain the fit.
        try {
            EditableSpecData lineSpec = SpecDataFactory.getReference().
                createEditable( name );
            lineSpec.setData( coords, data );
            lineSpec.setType( SpecData.LINEFIT );
            lineSpec.setUseInAutoRanging( false );
            globalList.add( lineSpec );
            globalList.addSpectrum( plot.getPlot(), lineSpec );
            localList.add( lineSpec );

            if ( colourScheme == 0 ) {
                // line is magenta and solid.
                globalList.setKnownNumberProperty( lineSpec,
                                                   SpecData.LINE_COLOUR,
                                                   new Integer( Color.magenta.getRGB() ) );
                globalList.setKnownNumberProperty( lineSpec,
                                                   SpecData.LINE_STYLE,
                                                   new Integer( 1 ) );
            }
            else if ( colourScheme == 1 ) {
                // line is cyan and solid.
                globalList.setKnownNumberProperty( lineSpec,
                                                   SpecData.LINE_COLOUR,
                                                   new Integer( Color.cyan.getRGB() ) );
                globalList.setKnownNumberProperty( lineSpec,
                                                   SpecData.LINE_STYLE,
                                                   new Integer( 1 ) );
            }
            else if ( colourScheme == 2 ) {
                // line is grey and solid.
                globalList.setKnownNumberProperty( lineSpec,
                                                   SpecData.LINE_COLOUR,
                                                   new Integer( Color.gray.getRGB() ) );
                globalList.setKnownNumberProperty( lineSpec,
                                                   SpecData.LINE_STYLE,
                                                   new Integer( 1 ) );
            }
            else {
                // line is black and dashed.
                globalList.setKnownNumberProperty( lineSpec,
                                                   SpecData.LINE_COLOUR,
                                                   new Integer( Color.black.getRGB() ) );
                globalList.setKnownNumberProperty( lineSpec,
                                                   SpecData.LINE_STYLE,
                                                   new Integer( 5 ) );
            }
        }
        catch ( Exception e ) {
            //  Do nothing.
            e.printStackTrace();
        }
    }

    /**
     * Return an array of indices that map the ranges of the fit
     * regions into indices of the spectrum
     */
    protected int[] extractLineRanges( double[] oldX )
    {
        double[] worldLines = rangeList.getRanges( false );
        int[] arrayLines = new int[worldLines.length];
        for ( int i = 0, j = 0; i < arrayLines.length; i += 2 ) {
            arrayLines[i] = lookup( worldLines[i], oldX );
            arrayLines[i+1] = lookup( worldLines[i+1], oldX );

            //  Check ordering, these can be reversed (sky coordinates).
            int temp;
            if ( arrayLines[i] > arrayLines[i+1] ) {
                temp = arrayLines[i];
                arrayLines[i] = arrayLines[i+1];
                arrayLines[i+1] = temp;
            }
        }
        return arrayLines;
    }

    /**
     * Lookup an array index that most closely presents a given
     * value. The array of values should be sorted.
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
                } else if ( value > array[mid] ) {
                    low = mid;
                } else {
                    low = mid;
                    break;
                }
            }
        } else {
            while ( low < high - 1 ) {
                mid = ( low + high ) / 2;
                if ( value > array[mid] ) {
                    high = mid;
                } else if ( value < array[mid] ) {
                    low = mid;
                } else {
                    low = mid;
                    break;
                }
            }
        }

        //  Find which position is nearest in reality.
        int index = 0;
        if ( ( value - array[low] ) < ( array[high] - value ) ) {
            index = low;
        } else {
            index = high;
        }
        return index;
    }

    /**
     * Make necessary changes when fitting Gaussian profiles.
     */
    protected void changeGaussianFitsEvent()
    {
        if ( fitGaussians.isSelected() ) {
            lineView.addView( LineProperties.GAUSS );
        } else {
            lineView.removeView( LineProperties.GAUSS );
        }
    }

    /**
     * Make necessary changes when fitting Lorentz profiles.
     */
    protected void changeLorentzFitsEvent()
    {
        if ( fitLorentzians.isSelected() ) {
            lineView.addView( LineProperties.LORENTZ );
        } else {
            lineView.removeView( LineProperties.LORENTZ );
        }
    }

    /**
     * Make necessary changes when fitting Voigt profiles.
     */
    protected void changeVoigtFitsEvent()
    {
        if ( fitVoigts.isSelected() ) {
            lineView.addView( LineProperties.VOIGT );
        } else {
            lineView.removeView( LineProperties.VOIGT );
        }
    }


    /**
     * Fit action. Perform a fit of all lines.
     */
    protected class FitAction extends AbstractAction
    {
        public FitAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            fitLines();
        }
    }

    /**
     * Inner class defining action for fitting gaussians.
     */
    protected class GaussianAction extends AbstractAction
    {
        public GaussianAction() {
            super( "Gaussian" );
        }
        public void actionPerformed( ActionEvent ae ) {
            changeGaussianFitsEvent();
        }
    }

    /**
     * Inner class defining action for fitting lorentzian.
     */
    protected class LorentzAction extends AbstractAction
    {
        public LorentzAction() {
            super( "Lorentzian" );
        }
        public void actionPerformed( ActionEvent ae ) {
            changeLorentzFitsEvent();
        }
    }

    /**
     * Inner class defining action for fitting voigt.
     */
    protected class VoigtAction extends AbstractAction
    {
        public VoigtAction() {
            super( "Voigt" );
        }
        public void actionPerformed( ActionEvent ae ) {
            changeVoigtFitsEvent();
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
     * Inner class defining action for deleting all fits.
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

//
// Implement the PlotListener interface. Do this so that we can keep
// the list of background spectra up to date.
//

    /**
     *  Sent when a plot is created.
     */
    public void plotCreated( PlotChangedEvent e )
    {
        //  Do nothing.
    }

    /**
     *  Sent when a plot is removed.
     */
    public void plotRemoved( PlotChangedEvent e )
    {
        // Do nothing.
    }

    /**
     *  Send when a plot property is changed, assume this means that a
     *  spectrum has been added or removed, so update the list.
     */
    public void plotChanged( PlotChangedEvent e )
    {
        updateNames();
    }
}
