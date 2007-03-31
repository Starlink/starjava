/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     18-MAR-2004 (Peter W. Draper):
 *        Original version.
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
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.diva.DrawActions;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.util.gui.GridBagLayouter;

import uk.ac.starlink.diva.DrawGraphicsPane;
import uk.ac.starlink.diva.InterpolatedCurveFigure;
import uk.ac.starlink.diva.geom.InterpolatedCurve2D;
import uk.ac.starlink.diva.interp.Interpolator;


/**
 * Toolbox for generating a spectrum from an {@link InterpolatedCurveFigure}.
 * This spectrum can then be subtracted or divided into a current spectrum.
 * The current spectrum is the one that is current in a specified
 * {@link PlotControlFrame}.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class GenerateFromInterpFrame
    extends JFrame
{
    /** UI preferences. */
    protected static Preferences prefs =
        Preferences.userNodeForPackage( GenerateFromInterpFrame.class );

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
    protected JMenuItem closeFileMenu = new JMenuItem();

    /**
     *  The PlotControlFrame that specifies the current spectrum.
     */
    protected PlotControlFrame plot = null;

    /**
     *  Number of spectra generated so far (used as unique identifier)
     */
    protected static int generateCounter = 0;

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
    public GenerateFromInterpFrame( PlotControlFrame plot )
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
     * Set the PlotControlFrame that should have the drawn graphics.
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
        JPanel centre = new JPanel();
        GridBagLayouter layouter =
            new GridBagLayouter( centre, GridBagLayouter.SCHEME4 );
        contentPane.add( centre, BorderLayout.CENTER );

        //  Decide if we should generate and display a subtracted
        //  version of the spectrum.  There are three options, do not,
        //  subtract background from spectrum and subtract spectrum
        //  from background (emission-v-absorption).
        layouter.add( new JLabel( "Subtract line from spectrum:" ), false );

        subtractNothing.setText( "No" );
        subtractNothing.setToolTipText( "Do not subtract line from spectrum" );
        layouter.add( subtractNothing, false );

        subtractFromBelow.setText( "As base line" );
        subtractFromBelow.setToolTipText( "Subtract line from spectrum" );
        layouter.add( subtractFromBelow, false );

        subtractFromAbove.setText( "As ceiling" );
        subtractFromAbove.setToolTipText( "Subtract spectrum from line" );
        layouter.add( subtractFromAbove, false );
        layouter.eatLine();

        ButtonGroup subtractGroup = new ButtonGroup();
        subtractGroup.add( subtractNothing );
        subtractGroup.add( subtractFromBelow );
        subtractGroup.add( subtractFromAbove );
        subtractNothing.setSelected( true );

        int which = prefs.getInt( "GenerateFromInterpFrame_subtract", 1 );
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
                        prefs.putInt( "GenerateFromInterpFrame_subtract", 1 );
                    }
                }
            });

        subtractFromBelow.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    if ( subtractFromBelow.isSelected() ) {
                        prefs.putInt( "GenerateFromInterpFrame_subtract", 2 );
                    }
                }
            });

        subtractFromAbove.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    if ( subtractFromAbove.isSelected() ) {
                        prefs.putInt( "GenerateFromInterpFrame_subtract", 3 );
                    }
                }
            });


        //  Decide if we should generate and display a divided
        //  version of the spectrum.
        divideSpectrum.setToolTipText( "Divide spectrum by line" );
        layouter.add( new JLabel( "Divide spectrum by line:" ), false );
        layouter.add( divideSpectrum, true );

        boolean state = prefs.getBoolean( "GenerateFromInterpFrame_divide",
                                          false );
        divideSpectrum.setSelected( state );
        divideSpectrum.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    boolean state = divideSpectrum.isSelected();
                    prefs.putBoolean("GenerateFromInterpFrame_divide", state);
                }
            });

    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle(Utilities.getTitle
                 ( "Generate spectra from interpolated lines" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        contentPane.add( actionBarContainer, BorderLayout.SOUTH );
        setSize( new Dimension( 450, 250 ) );
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
        ImageIcon interpolateImage = new ImageIcon(
            ImageHolder.class.getResource( "interpolate.gif" ) );
        ImageIcon resetImage = new ImageIcon(
            ImageHolder.class.getResource( "reset.gif" ) );
        ImageIcon deleteImage = new ImageIcon(
            ImageHolder.class.getResource( "delete.gif" ) );
        ImageIcon curveImage = new ImageIcon(
            ImageHolder.class.getResource( "curve.gif" ) );

        //  Create the File menu.
        fileMenu.setText( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Action to start a drawing interaction.
        DrawAction drawAction = new DrawAction( "Draw curve", curveImage );
        JButton drawButton = new JButton( drawAction );
        fileMenu.add( drawAction ).setMnemonic( KeyEvent.VK_D );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( drawButton );
        drawButton.setToolTipText
            ("Start (optional) interaction so that a new curve can be drawn");

        //  Add action to do the generation of a spectrum from a line.
        GenerateAction generateAction = new GenerateAction( "Generate",
                                                            interpolateImage );
        fileMenu.add( generateAction ).setMnemonic( KeyEvent.VK_G );
        JButton generateButton = new JButton( generateAction );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( generateButton );
        generateButton.setToolTipText
            ( "Generate a spectrum from a graphics interpolated line" );

        //  Add action to do the generation of a spectrum from a line and
        //  replace the current one, if subtracting or dividing.
        GenerateReplaceAction generateReplaceAction =
            new GenerateReplaceAction( "Generate (Replace)",interpolateImage );
        fileMenu.add( generateReplaceAction ).setMnemonic( KeyEvent.VK_T );
        JButton generateReplaceButton = new JButton( generateReplaceAction );
        topActionBar.add( Box.createGlue() );
        topActionBar.add( generateReplaceButton );
        generateReplaceButton.setToolTipText
            ( "Generate a spectrum from a graphics interpolated line and " +
              "replace the current spectrum with the subtracted/divided result" );

        //  Add action to reset all values.
        ResetAction resetAction = new ResetAction( "Reset", resetImage );
        fileMenu.add( resetAction ).setMnemonic( KeyEvent.VK_R );
        JButton resetButton = new JButton( resetAction );
        midActionBar.add( Box.createGlue() );
        midActionBar.add( resetButton );
        resetButton.setToolTipText
            ( "Reset all values and clear all generated spectra" );

        //  Add action to reset a generate replace operation.
        ResetReplaceAction resetReplaceAction =
            new ResetReplaceAction( "Reset (Replace)", resetImage );
        fileMenu.add( resetReplaceAction ).setMnemonic( KeyEvent.VK_P );
        JButton resetReplaceButton = new JButton( resetReplaceAction );
        midActionBar.add( Box.createGlue() );
        midActionBar.add( resetReplaceButton );
        resetReplaceButton.setToolTipText
            ( "Reset after a Generate (Replace)" );

        //  Add action to just delete any spectra
        DeleteAction deleteAction =
            new DeleteAction( "Delete spectra", deleteImage );
        fileMenu.add( deleteAction ).setMnemonic( KeyEvent.VK_E );
        JButton deleteButton = new JButton( deleteAction );
        midActionBar.add( Box.createGlue() );
        midActionBar.add( deleteButton );
        deleteButton.setToolTipText( "Delete all generated spectra" );

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

        //  Add menu to choose the type of curve to draw. Need to keep this
        //  synchronized with the "Graphics->Curve type" menu in the plot
        //  window, so a lot of work to do.
        JMenuBar plotMenuBar = plot.getJMenuBar();

        //  Find "Graphics" menu.
        JMenu plotGraphicsMenu = null;
        int n = plotMenuBar.getMenuCount();
        for ( int i = 0; i < n; i++ ) {
            plotGraphicsMenu = plotMenuBar.getMenu( i );
            if ( plotGraphicsMenu.getText().equals( "Graphics" ) ) {
                break;
            }
            plotGraphicsMenu = null;
        }
        if ( plotGraphicsMenu == null ) {
            System.err.println
                ("Need to update GenerateFromInterpFrame (no Graphics menu?)");
        }
        else {
            //  Find "Curve type" menu in "Graphics".
            JMenu plotCurveMenu = null;
            n = plotGraphicsMenu.getItemCount();
            for ( int i = 0; i < n; i++ ) {
                plotCurveMenu = (JMenu) plotGraphicsMenu.getItem( i );
                if ( plotCurveMenu.getText().equals( "Curve type" ) ) {
                    break;
                }
                plotCurveMenu = null;
            }
            if ( plotCurveMenu == null ) {
                System.err.println( "Need to update GenerateFromInterpFrame " +
                                    "(no Curve type menu?)");
            }
            else {

                //  Create a menu that looks like the graphics menu
                //  version. Note that the radio button pairs share a
                //  single ButtonModel so that they also share state.
                JMenu curveMenu = new JMenu( "Curve type" );
                curveMenu.setMnemonic( KeyEvent.VK_C );
                ButtonGroup group = new ButtonGroup();

                n = plotCurveMenu.getItemCount();
                JRadioButtonMenuItem plotMenuItem = null;
                JRadioButtonMenuItem menuItem = null;

                for ( int i = 0; i < n; i++ ) {
                    plotMenuItem = (JRadioButtonMenuItem)
                        plotCurveMenu.getItem( i );
                    menuItem =
                        new JRadioButtonMenuItem( plotMenuItem.getText() );

                    //  Share underlying model, hence state.
                    menuItem.setModel( plotMenuItem.getModel() );

                    curveMenu.add( menuItem );
                    group.add( menuItem );
                }
                menuBar.add( curveMenu );
            }
        }

        //  Create the Help menu.
        HelpFrame.createHelpMenu( "interpolation-window", "Help on window",
                                  menuBar, null );
    }

    /**
     *  Perform the conversion. This grabs the current
     *  InterpolatedCurveFigure. If none is grabbed an error dialog is shown.
     *
     *  @param replace if true and a subtracted or divided spectrum is being
     *                 created then remove the current spectrum.
     *
     */
    public void generate( boolean replace )
    {
        //  Grab the current figures from the plot and look for an
        //  InterpolatedCurveFigure instance.
        DrawGraphicsPane drawPane = plot.getPlot().getPlot().getGraphicsPane();
        Object[] selection = drawPane.getSelectionAsArray();
        if ( selection == null ) {
            JOptionPane.showMessageDialog
                ( this, "There are no figures selected. Create and" +
                  " select an interpolated curve",
                  "No selected figures",
                  JOptionPane.ERROR_MESSAGE );
            return;
        }

        //  Look for an InterpolatedCurveFigure.
        InterpolatedCurveFigure figure = null;
        for ( int i = 0; i < selection.length; i++ ) {
            if ( selection[i] instanceof InterpolatedCurveFigure ) {
                figure = (InterpolatedCurveFigure) selection[i];
                break;
            }
        }
        if ( figure == null ) {
            JOptionPane.showMessageDialog
                ( this, "There are no interpolated curves selected." +
                  " Create and select one.",
                  "No interpolated curves selected",
                  JOptionPane.ERROR_MESSAGE );
            return;
        }

        //  Obtain the current spectrum on the plot.
        SpecData currentSpectrum = plot.getPlot().getCurrentSpectrum();
        if ( currentSpectrum == null ) {
            return;
        }

        //  Get copies of it's coordinates and data values.
        double[] xp = (double[])currentSpectrum.getXData().clone();
        double[] yp = (double[])currentSpectrum.getYData().clone();

        //  Generate an AST-friendly form of these.
        double[] xy = new double[2*xp.length];
        for ( int i = 0, j = 0; i < xp.length; i++ ) {
            xy[j++] = xp[i];
            xy[j++] = yp[i];
        }

        //  Transform spectrum coordinates and values into graphics
        //  coordinates.
        double[][] xyt = plot.getPlot().getPlot().transform( xy, false );

        //  Recover the coordinates.
        for ( int i = 0; i < xp.length; i++ ) {
            xp[i] = xyt[0][i];
        }

        //  Evaluate these positions in the InterpolatedCurveFigures
        //  Interpolator (which only works in graphics coordinates).
        Interpolator interp =
            ((InterpolatedCurve2D)(figure.getShape())).getInterpolator();
        double[] yg = interp.evalYDataArray( xp );

        //  Transform spectrum values from graphics coordinates (we
        //  already have the X coordinates from the current spectrum).
        for ( int i = 0, j = 0; i < xp.length; i++ ) {
            xy[j++] = xp[i];
            xy[j++] = yg[i];
        }
        xyt = plot.getPlot().getPlot().transform( xy, true );
        for ( int i = 0; i < xp.length; i++ ) {
            yp[i] = xyt[1][i];
        }

        //  Decide what we're going to display. If replace has been selected,
        //  then we remove the current spectrum, but only if we're generating
        //  an equivalent one (subtracted or divided).
        boolean displayFit = true;
        boolean displaySubtract = ( ! subtractNothing.isSelected() );
        boolean displayDivide = divideSpectrum.isSelected();
        if ( replace && ( displaySubtract || displayDivide ) ) {
            displayFit = false;
        }

        //  Create a spectrum from the interpolation and then display
        //  as a spectrum, unless replacing and generating one of the
        //  corrected forms.
        String name = "Interpolated line: " + (++generateCounter);
        display( name, currentSpectrum, yp, displayFit );

        //  Create and display the subtracted form of the spectrum. This
        //  replaces current spectrum, if we're replacing and subtracting.
        boolean replaceCurrent = ( replace && displaySubtract );
        if ( displaySubtract ) {
            subtractAndDisplay( currentSpectrum, name, yp, replaceCurrent );
        }

        //  Create and display the normalised form of the spectrum. This
        //  replaces current spectrum, if we're replacing and not subtracting.
        if ( displayDivide ) {
            replaceCurrent = ( ( ! replaceCurrent ) && displayDivide );
            divideAndDisplay( currentSpectrum, name, yp, replaceCurrent );
        }
    }

    /**
     * Create and display a new spectrum with given data values.
     */
    protected void display( String name, SpecData spectrum,
                            double[] data, boolean display  )
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
     * Display the current spectrum, minus the generated spectrum in
     * the current plot. The subtracted spectrum is created as a
     * memory spectrum. The default line colour is yellow.
     */
    protected void subtractAndDisplay( SpecData spectrum, String polyName,
                                       double[] genData, boolean replace )
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
            newData = subtractData( data, genData );
        }
        else {
            name = "Diff: (" + polyName + ") - (" + specName + ") ";
            newData = subtractData( genData, data );
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
     * Display the current spectrum divided by the generated spectrum
     * if requested.  The new spectrum is created as a memory spectrum
     * The default line colour is cyan.
     */
    protected void divideAndDisplay( SpecData spectrum, String polyName,
                                     double[] genData, boolean replace )
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
        divideData( data, errors, genData, newData, newErrors );
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
     * Start an interaction so that a curve can be drawn.
     */
    protected void drawCurve()
    {
        DrawActions drawActions = plot.getPlot().getPlot().getDrawActions();
        drawActions.setDrawingMode( DrawActions.CURVE );
        plot.toFront();
    }

    /**
     *  Close the window.
     */
    protected void closeWindowEvent()
    {
        this.dispose();
    }

    /**
     *  Delete all spectra that we have created.
     */
    protected void deleteSpectra()
    {
        localList.deleteAll();
    }

    /**
     * Reset after a generate replace.
     */
    protected void resetReplaceActionEvent()
    {
        //  Remove any spectra.
        deleteSpectra();

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
     * Reset all controls and dispose of all generated spectra.
     */
    protected void resetActionEvent()
    {
        //  Remove any spectra.
        deleteSpectra();
    }

    /**
     * Generate action.
     */
    protected class GenerateAction extends AbstractAction
    {
        public GenerateAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control G" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            generate( false );
        }
    }

    /**
     * Generate and replace action.
     */
    protected class GenerateReplaceAction extends AbstractAction
    {
        public GenerateReplaceAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control T" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            generate( true );
        }
    }

    /**
     * Inner class defining Action for closing window and keeping spectra.
     */
    protected class CloseAction extends AbstractAction
    {
        public CloseAction( String name, Icon icon )
        {
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
        public ResetAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control R" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            resetActionEvent();
        }
    }

    /**
     * Inner class defining the reset of a generate replace action.
     */
    protected class ResetReplaceAction extends AbstractAction
    {
        public ResetReplaceAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control P" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            resetReplaceActionEvent();
        }
    }

    /**
     * Inner class defining action for deleting associated spectra.
     */
    protected class DeleteAction extends AbstractAction
    {
        public DeleteAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control E" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            deleteSpectra();
        }
    }

    /**
     * Inner class defining action for drawing a new curve
     */
    protected class DrawAction extends AbstractAction
    {
        public DrawAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control D" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            drawCurve();
        }
    }
}
