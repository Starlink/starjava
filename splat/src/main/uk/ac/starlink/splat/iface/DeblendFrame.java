/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     23-MAR-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
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
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.GridBagLayouter;
import uk.ac.starlink.ast.gui.ScientificFormat;

/**
 * Provides a toolbox for fitting blends of spectral lines using a variety of
 * different profile models and displaying the results of the fits.
 * <p>
 * The components of the blend are identified as profiles that can be created
 * interactively. The range of the blend (i.e. the region that is actually
 * fitted over) is either the whole spectrum or any regions that are shown as
 * rectangles.
 * <p>
 * The results are shown as a series of parameters, plus a model fit spectrum
 * the components are also updated to show the fit.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see PlotControlFrame
 */
public class DeblendFrame
    extends JFrame
    implements PlotListener, ActionListener
{
    /**
     * The list of all the spectra that we've created.
     */
    protected SpecSubList localList = new SpecSubList();

    /**
     * The constant background spectrum if created.
     */
    protected EditableSpecData constantSpectrum = null;

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
     * Chooser for the background values, either constant or
     * spectrum/polynomial.
     */
    protected JLabel backgroundSourceLabel = new JLabel();
    protected JRadioButton consBackgroundSource = new JRadioButton();
    protected JRadioButton polyBackgroundSource = new JRadioButton();

    /**
     * The background value, if a constant.
     */
    protected JComboBox backgroundValue = new JComboBox();
    protected JLabel backgroundValueLabel = new JLabel();

    /**
     * JComboBox that displays the list of available spectrum to use
     * as the background.
     */
    protected JComboBox backgroundSpectra = new JComboBox();
    protected JLabel backgroundSpectraLabel = new JLabel();
    protected boolean backgroundWarningIssued = false;

    /**
     * Reference to GlobalSpecPlotList object.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * Lines that are to be fitted.
     */
    protected ModelLineView linesView = null;

    /**
     *  Ranges of data that are to be fitted.
     */
    protected XGraphicsRangesView rangeList = null;

    /**
     * Control for saying whether to use errors as weights for the fits.
     */
    protected JCheckBox errors = new JCheckBox();
    protected JLabel errorLabel = new JLabel();

    /**
     * Number of fits done so far (used as unique identifier for
     * generating shortnames for any spectra created).
     */
    protected static int fitCounter = 0;

    /**
     * Whether any polynomials were detected on start up.
     */
    protected boolean havePolynomials = true;

    /**
     * Create an instance.
     */
    public DeblendFrame( PlotControlFrame plot )
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
        linesView = new ModelLineView( plot.getPlot().getPlot() );

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
        JPanel panel = new JPanel();
        GridBagLayouter layouter =
            new GridBagLayouter( panel, GridBagLayouter.SCHEME4 );
        layouter.setInsets( new java.awt.Insets( 5, 5, 5, 5 ) );

        // The type of background, use a constant or other spectrum.
        backgroundSourceLabel.setText( "Background type:" );
        consBackgroundSource.setText( "Constant" );
        consBackgroundSource.setToolTipText
            ( "Use a constant value as background" );
        consBackgroundSource.addActionListener( this );

        polyBackgroundSource.setText( "Polynomial" );
        polyBackgroundSource.setToolTipText
            ( "Use a polynomial fit (or other spectrum) as background" );
        polyBackgroundSource.addActionListener( this );

        ButtonGroup sourceGroup = new ButtonGroup();
        sourceGroup.add( consBackgroundSource );
        sourceGroup.add( polyBackgroundSource );

        layouter.add( backgroundSourceLabel, false );
        layouter.add( consBackgroundSource, false );
        layouter.add( polyBackgroundSource, true );

        if ( havePolynomials ) {
            polyBackgroundSource.setSelected( true );
        }
        else {
            consBackgroundSource.setSelected( true );
        }
        toggleBackgroundSource();

        //  Add controls for choosing the background spectrum.
        backgroundSpectraLabel.setText( "Background fit:" );
        backgroundSpectraLabel.setForeground( Color.red );
        layouter.add( backgroundSpectraLabel, false );
        String tipText = "Select a spectrum to use as background, "+
            "should normally be a fit (*)";
        backgroundSpectraLabel.setToolTipText( tipText );

        backgroundSpectra.setRenderer( new LineRenderer( backgroundSpectra ) );
        layouter.add( backgroundSpectra, true );
        backgroundSpectra.setToolTipText( tipText );

        //  Add controls for setting the background value. Should be decimal.
        backgroundValueLabel.setText( "Background value:" );
        backgroundValue.addItem( new Integer( 1 ) );
        backgroundValue.addItem( new Integer( 0 ) );
        ScientificFormat format = new ScientificFormat();
        DecimalComboBoxEditor editor = new DecimalComboBoxEditor( format );
        backgroundValue.setEditor( editor );
        backgroundValue.setEditable( true );

        layouter.add( backgroundValueLabel, false );
        layouter.add( backgroundValue, true );

        //  Add controls for using any errors as weights.
        errorLabel.setText( "Use errors as weights:" );
        layouter.add( errorLabel, false );
        layouter.add( errors, true );
        errors.setToolTipText
            ( "Use errors as weights when fitting (if available)" );

        // Add the XGraphicsRangesView that displays the spectral line
        // ranges.
        rangeList = new XGraphicsRangesView( plot.getPlot().getPlot(), null );
        layouter.add( rangeList, true );


        // And the view of the properties of the lines themselves.
        linesView = new ModelLineView( plot.getPlot().getPlot(),
                                       Color.yellow );
        layouter.add( linesView, true );

        //  Add the LineView that displays and creates the line properties
        //  objects.
        contentPane.add( panel, BorderLayout.CENTER );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Deblend spectral lines" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        contentPane.add( actionBar, BorderLayout.SOUTH );
        setSize( new Dimension( 500, 800 ) );
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
        ImageIcon closeImage =
            new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );
        ImageIcon resetImage =
            new ImageIcon( ImageHolder.class.getResource( "reset.gif" ) );
        ImageIcon deleteImage =
            new ImageIcon( ImageHolder.class.getResource( "delete.gif" ) );
        ImageIcon deblendImage =
            new ImageIcon( ImageHolder.class.getResource( "deblend.gif" ) );

        //  Create the File menu.
        fileMenu.setText( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Add action to do read a list of extents for fitting the lines.
        Action readAction = linesView.getReadAction( "Read line fit ranges" );
        fileMenu.add( readAction );

        //  Add action to save the extents to disk file.
        Action saveAction = linesView.getWriteAction( "Save line fit ranges" );
        fileMenu.add( saveAction );

        //  Add action to save the fits results to disk file.
        Action fitSaveAction = linesView.getWriteAction( "Save fit" );
        fileMenu.add( fitSaveAction );

        //  Add action to do the fit.
        FitAction fitAction = new FitAction( "Fit", deblendImage );
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
        deleteFitsButton.setToolTipText( "Delete all fit products" );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );
        JButton closeButton = new JButton( closeAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        actionBar.add( Box.createGlue() );

        //  Add a help menu and the topic about this window.
        HelpFrame.createHelpMenu( "deblend-window", "Help on window",
                                  menuBar, null );
    }

    /**
     *  Close the window. Delete any related graphics.
     */
    protected void closeWindowEvent()
    {
        linesView.deleteAll();
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

        //  Clear lines?
        linesView.deleteAll();

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
            havePolynomials = false;
            for ( int i = 0; i < globalList.specCount(); i++ ) {
                if ( globalList.getSpectrum( i ).getType() ==
                     SpecData.POLYNOMIAL ) {
                    havePolynomials = true;
                    model.setSelectedItem( globalList.getSpectrum( i ) );
                    break;
                }
            }
            if ( ! havePolynomials && ! backgroundWarningIssued ) {
                JOptionPane.showMessageDialog
                    ( this,
                      "You do not seem to have any fits to a local \n"+
                      "or global background for any of your spectra. \n"+
                      "Use the polynomial fitting tool or generate one \n" +
                      "from an interpolated line (unless you have an \n" +
                      "existing spectrum that defines the background \n" +
                      "for your lines, or you have a background subtracted \n"+
                      "or normalized spectrum). ",
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
     * Get the background value.
     */
    protected double getBackgroundValue()
    {
        double value = SpecData.BAD;
        try {
            Object target = backgroundValue.getEditor().getItem();
            if ( target instanceof Number ) {
                value = ((Number) target).doubleValue();
            }
            else if ( target instanceof String ) {
                value = Double.parseDouble( (String) target );
            }
            else {
                // Should never happen.
                JOptionPane.showMessageDialog
                    ( this, "background value object has a unknown type",
                      "Unknown background type",
                      JOptionPane.INFORMATION_MESSAGE );
            }
        }
        catch (NumberFormatException e) {
            ErrorDialog.showError( this, e );
        }
        return value;
    }

    /**
     *  Do the deblend fit.
     */
    public void fitLines()
    {

        // Extract all ranges, obtain current and background spectra, set up
        // various solvers and do it. Add line fit as a new spectrum and
        // display.
        SpecData currentSpectrum = plot.getPlot().getCurrentSpectrum();
        double[] specXData = currentSpectrum.getXData();
        double[] specYData = currentSpectrum.getYData();
        double[] specYDataErrors = null;
        if ( errors.isSelected() ) {
            specYDataErrors = currentSpectrum.getYDataErrors();
        }

        // If no ranges are defined, then we use the whole spectrum.
        int[] ranges = rangeList.extractRanges( false, false, specXData );
        if ( ranges.length == 0 ) {
            ranges = new int[2];
            ranges[0] = 0;
            ranges[1] = specXData.length - 1;
        }

        // Determine the background to be used. This can be another spectrum
        // or a constant.
        boolean polySource = polyBackgroundSource.isSelected();
        SpecData backgroundSpectrum = null;
        double backgroundValue = SpecData.BAD;
        if ( polySource ) {
            backgroundSpectrum = getBackgroundSpectrum( false );
        }
        else {
            backgroundValue = getBackgroundValue();
            if ( backgroundValue == SpecData.BAD ) {
                return;
            }
        }

        double[] backYData = null;
        if ( polySource && backgroundSpectrum != null ) {

            // Get background values that match the positions of the spectrum
            // we're going to fit.
            backYData = backgroundSpectrum.evalYDataArray( specXData );
        }
        else if ( polySource && backgroundSpectrum == null ) {
            JOptionPane.showMessageDialog( this,
                                           "Cannot subtract a spectrum " +
                                           " from itself. Using a zero" +
                                           "background",
                                           "background null",
                                           JOptionPane.INFORMATION_MESSAGE );
            backgroundValue = 0.0;
        }

        //  Extract any valid data associated with the line.
        Object[] extracts = new Object[4];
        int n = extractLineData( ranges,
                                 specXData, specYData,
                                 specYDataErrors,
                                 backYData,
                                 backgroundValue,
                                 extracts );
        if ( n == 0 ) {
            JOptionPane.showMessageDialog( this,
                                           "Line contains no valid data",
                                           "all bad",
                                           JOptionPane.INFORMATION_MESSAGE );
            return;
        }
        double[] XSpecXData = (double[]) extracts[0];
        double[] XSpecYData = (double[]) extracts[1];
        double[] XSpecYDataWeights = (double[]) extracts[2];
        double[] XBackYData = (double[]) extracts[3];

        //  Increment Fit counter (so all names look associated).
        fitCounter++;

        //  Do the fit...
        doFit( XSpecXData, XSpecYData, XSpecYDataWeights,
               specXData, backYData, backgroundValue );

        // If using a constant background create a spectrum to
        // show this.
        if ( XBackYData == null ) {
            updateConstantSpectrum( currentSpectrum, backgroundValue,
                                    XSpecXData[0],
                                    XSpecXData[XSpecXData.length-1] );
        }
    }

    /**
     * Extract the data associated with set of ranges. Returns the number of
     * elements extracted (which will be zero if all elements are BAD).
     *
     * @param ranges pairs of indices indicating the sections of the data that
     *               are required.
     * @param upper finishing index for extraction range.
     * @param specXData the coordinates of the complete spectrum.
     * @param specYData the data value of the complete spectrum.
     * @param specYDataErrors the errors of the data values, null for none.
     * @param backYData data values of the background, these should
     *                  match the data values of the spectrum (i.e. be
     *                  at same coordinates), null for none.
     * @param backgroundValue if backYData is null then this value
     *                        defines the background.
     * @param extracts Object array for containing references to the
     *                 four extracted double arrays. These are the
     *                 extracted coordinates, the extracted background
     *                 subtracted (if appropriate) data values, the
     *                 extracted data errors converted to weights, if
     *                 any, and the extracted background data, if
     *                 any. Missing arrays are set to null.
     * @return the number of position extracted.
     */
    protected int extractLineData( int[] ranges,
                                   double[] specXData,
                                   double[] specYData,
                                   double[] specYDataErrors,
                                   double[] backYData,
                                   double backgroundValue,
                                   Object[] extracts )
    {
        int n = 0;
        int low = 0;
        int high = specXData.length;
        double[] XSpecXData = null;
        double[] XSpecYData = null;
        double[] XSpecYDataWeights = null;
        double[] XBackYData = null;
        if ( backYData != null && specYDataErrors != null ) {

            //  Have errors and background data. Use first pass to count how
            //  many good values we have.
            n = 0;
            for ( int i = 0; i < ranges.length; i += 2 ) {
                low = ranges[i];
                high = Math.min( ranges[i+1], specXData.length - 1 );
                for ( int j = low; j <= high; j++ ) {
                    if ( specXData[j] != SpecData.BAD &&
                         specYData[j] != SpecData.BAD &&
                         backYData[j] != SpecData.BAD ) {
                        n++;
                    }
                }
            }
            if ( n != 0 ) {
                XSpecXData = new double[n];
                XSpecYData = new double[n];
                XSpecYDataWeights = new double[n];
                XBackYData = new double[n];
                n = 0;

                for ( int i = 0; i < ranges.length; i += 2 ) {
                    low = ranges[i];
                    high = Math.min( ranges[i+1], specXData.length - 1 );
                    for ( int j = low; j <= high; j++ ) {
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
            }
        }
        else if ( backYData != null ) {
            //  Have background data and no errors.
            n = 0;
            for ( int i = 0; i < ranges.length; i += 2 ) {
                low = ranges[i];
                high = Math.min( ranges[i+1], specXData.length - 1 );
                for ( int j = low; j <= high; j++ ) {
                    if ( specXData[j] != SpecData.BAD &&
                         specYData[j] != SpecData.BAD &&
                         backYData[j] != SpecData.BAD ) {
                        n++;
                    }
                }
            }
            if ( n != 0 ) {
                XSpecXData = new double[n];
                XSpecYData = new double[n];
                XBackYData = new double[n];
                n = 0;
                for ( int i = 0; i < ranges.length; i += 2 ) {
                    low = ranges[i];
                    high = Math.min( ranges[i+1], specXData.length - 1 );
                    for ( int j = low; j <= high; j++ ) {
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
            }
        }
        else if ( specYDataErrors != null ) {

            //  Have errors but no background spectrum. Will need to
            //  subtract a constant.
            n = 0;
            for ( int i = 0; i < ranges.length; i += 2 ) {
                low = ranges[i];
                high = Math.min( ranges[i+1], specXData.length - 1 );
                for ( int j = low; j <= high; j++ ) {
                    if ( specXData[j] != SpecData.BAD &&
                         specYData[j] != SpecData.BAD ) {
                        n++;
                    }
                }
            }
            if ( n != 0 ) {
                XSpecXData = new double[n];
                XSpecYData = new double[n];
                XSpecYDataWeights = new double[n];
                n = 0;
                for ( int i = 0; i < ranges.length; i += 2 ) {
                    low = ranges[i];
                    high = Math.min( ranges[i+1], specXData.length - 1 );
                    for ( int j = low; j <= high; j++ ) {
                        if ( specXData[j] != SpecData.BAD &&
                             specYData[j] != SpecData.BAD ) {

                            XSpecXData[n] = specXData[j];
                            XSpecYData[n] = specYData[j] - backgroundValue;
                            XSpecYDataWeights[n] = 1.0 /
                                ( specYDataErrors[j] * specYDataErrors[j] );
                            n++;
                        }
                    }
                }
            }
        }
        else {
            // Have no errors or background spectrum, will need to subtract
            // a constant.
            n = 0;
            for ( int i = 0; i < ranges.length; i += 2 ) {
                low = ranges[i];
                high = Math.min( ranges[i+1], specXData.length - 1 );
                for ( int j = low; j <= high; j++ ) {
                    if ( specXData[j] != SpecData.BAD &&
                         specYData[j] != SpecData.BAD ) {
                        n++;
                    }
                }
            }
            if ( n != 0 ) {
                XSpecXData = new double[n];
                XSpecYData = new double[n];
                n = 0;
                for ( int i = 0; i < ranges.length; i += 2 ) {
                    low = ranges[i];
                    high = Math.min( ranges[i+1], specXData.length - 1 );
                    for ( int j = low; j <= high; j++ ) {
                        if ( specXData[j] != SpecData.BAD &&
                             specYData[j] != SpecData.BAD ) {

                            XSpecXData[n] = specXData[j];
                            XSpecYData[n] = specYData[j] - backgroundValue;
                            n++;
                        }
                    }
                }
            }
        }
        extracts[0] = XSpecXData;
        extracts[1] = XSpecYData;
        extracts[2] = XSpecYDataWeights;
        extracts[3] = XBackYData;
        return n;
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
     *                      in genCoords (this is added to the fit),
     *                      null for none.
     * @param genBackgroundValue a single background value used if
     *                           genBackground is null.
     */
    protected void doFit( double[] fitCoords, double[] fitData, 
                          double[] fitWeights, double[] genCoords,
                          double[] genBackground, double genBackgroundValue )
    {
        //  Do some stuff.
    }

    /**
     * Update or create the constant value background spectrum.
     */
    protected void updateConstantSpectrum( SpecData currentSpectrum,
                                           double value, double high, 
                                           double low )
    {
        try {
            boolean created = false;
            if ( constantSpectrum == null ) {
                constantSpectrum =
                    SpecDataFactory.getInstance().createEditable( "dummy" );
                created = true;
            }

            // Update values -- edit spectrum
            double[] coords = new double[2];
            coords[0] = low * 1.1;
            coords[1] = high * 1.1;
            double[] values = new double[2];
            values[0] = value;
            values[1] = value;

            FrameSet frameSet = 
                ASTJ.get1DFrameSet( currentSpectrum.getAst().getRef(), 1 );
            constantSpectrum.setSimpleUnitData
                ( frameSet, coords, currentSpectrum.getCurrentDataUnits(), 
                  values );
            constantSpectrum.setType( SpecData.POLYNOMIAL );
            constantSpectrum.setUseInAutoRanging( false );
            constantSpectrum.setShortName( "Constant " + value );

            if ( created ) {
                globalList.add( constantSpectrum );
                globalList.addSpectrum( plot.getPlot(), constantSpectrum );
            }
        }
        catch (Exception e) {
            // Not important.
            e.printStackTrace();
        }
    }

    /**
     *  Display a line fit as a spectrum.
     */
    protected void displayFit( SpecData currentSpectrum,
                               String name, double[] coords,
                               double[] data, int colourScheme )
    {
        //  Create a memory spectrum to contain the fit.
        try {
            EditableSpecData lineSpec = SpecDataFactory.getInstance()
                .createEditable( name );


            FrameSet frameSet = 
                ASTJ.get1DFrameSet( currentSpectrum.getAst().getRef(), 1 );
            lineSpec.setSimpleUnitData( frameSet, coords, 
                                        currentSpectrum.getCurrentDataUnits(),
                                        data );
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
     * Toggle the interface to reflect the source of the background.
     */
    protected void toggleBackgroundSource()
    {
        boolean polySource = polyBackgroundSource.isSelected();
        if ( polySource ) {
            backgroundSpectra.setEnabled( true );
            backgroundValue.setEnabled( false );
        }
        else {
            backgroundSpectra.setEnabled( false );
            backgroundValue.setEnabled( true );
        }
    }

    /**
     * Add an interpolated gaussian to the model.
     */
    protected void addGaussian()
    {
        //  Get the interaction started...

    }

    /**
     * Fit action. Perform a fit of all lines.
     */
    protected class FitAction extends AbstractAction
    {
        public FitAction( String name, Icon icon ) 
        {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) 
        {
            fitLines();
        }
    }

    /**
     * Inner class defining action for adding a Gaussian component.
     */
    protected class GaussianAction extends AbstractAction
    {
        public GaussianAction() 
        {
            super( "Gaussian" );
        }
        public void actionPerformed( ActionEvent ae ) 
        {
            addGaussian();
        }
    }

    /**
     * Inner class defining action for adding a Lorentz component.
     */
    protected class LorentzAction extends AbstractAction
    {
        public LorentzAction() 
        {
            super( "Lorentzian" );
        }
        public void actionPerformed( ActionEvent ae ) 
        {
            //addLorentz();
        }
    }

    /**
     * Inner class defining action for add a Voigt component.
     */
    protected class VoigtAction extends AbstractAction
    {
        public VoigtAction() 
        {
            super( "Voigt" );
        }
        public void actionPerformed( ActionEvent ae ) 
        {
            //addVoigt();
        }
    }


    /**
     * Inner class defining Action for closing window and keeping fit.
     */
    protected class CloseAction extends AbstractAction
    {
        public CloseAction( String name, Icon icon ) 
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control W" ) );
        }
        public void actionPerformed( ActionEvent ae ) 
        {
            closeWindowEvent();
        }
    }

    /**
     * Inner class defining action for resetting all values.
     */
    protected class ResetAction extends AbstractAction
    {
        public ResetAction( String name, Icon icon ) 
        {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) 
        {
            resetActionEvent();
        }
    }

    /**
     * Inner class defining action for deleting all fits.
     */
    protected class DeleteFitsAction extends AbstractAction
    {
        public DeleteFitsAction( String name, Icon icon ) 
        {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) 
        {
            deleteFits();
        }
    }

//
// ActionListener interface.
//
    public void actionPerformed( ActionEvent e )
    {
        toggleBackgroundSource();
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
