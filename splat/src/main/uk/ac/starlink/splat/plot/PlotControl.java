/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 * History:
 *    16-SEP-1999 (Peter W. Draper):
 *       Original version.
 *    06-JUN-2002 (Peter W. Draper):
 *       Renamed Plot class to DivaPlot. Plot is name of class in
 *       JNIAST.
 */
package uk.ac.starlink.splat.plot;

import diva.canvas.event.LayerEvent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Vector;

import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.gui.AstDouble;
import uk.ac.starlink.ast.gui.GraphicsEdges;
import uk.ac.starlink.ast.gui.GraphicsHints;
import uk.ac.starlink.ast.gui.PlotConfiguration;
import uk.ac.starlink.ast.gui.PlotController;
import uk.ac.starlink.splat.data.DataLimits;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataComp;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.DecimalComboBoxEditor;
import uk.ac.starlink.splat.iface.GlobalSpecPlotList;
import uk.ac.starlink.splat.iface.LineRenderer;
import uk.ac.starlink.splat.iface.SimpleDataLimitControls;
import uk.ac.starlink.splat.iface.SpecChangedEvent;
import uk.ac.starlink.splat.iface.SpecListener;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.iface.SpecTransferHandler;

import uk.ac.starlink.diva.FigureListener;
import uk.ac.starlink.diva.FigureChangedEvent;
import uk.ac.starlink.diva.DragRegion;

/**
 * A PlotControl object consists of a Plot inside a scrolled pane and various
 * controls in a panel. The Plot allows you to display many spectra. Plots
 * wrapped by this object are given a name that is unique within an
 * application (<plotn>) and are assigned an identifying number
 * (0 upwards). <p>
 *
 * The controls in the panel allow you to:
 * <ul
 *   <li> apply independent scales in X and Y, thus zooming and
 *   scrolling the DivaPlot,</li>
 *   <li> get a continuous readout of the cursor position,
 *   <li> display a vertical hair,</li>
 *   <li> select the current spectrum and see a list of those displayed,</li>
 *   <li> apply percentile cuts to the data limits.</li>
 * </ul>
 * Methods are provided for scaling about the current centre, zooming in to a
 * given region as well as just setting the X and Y scale factors. <p>
 *
 * You can also scroll to centre on a given X coordinate (or the one shown in
 * the X coordinate readout field) and make the DivaPlot fit itself to
 * the current viewable height and/or width. <p>
 *
 * A not-very exhaustive set of methods for querying the contents of
 * the DivaPlot (e.g. getting access to the plotted spectra) and
 * adding and removing spectra from the DivaPlot are provided. <p>
 *
 * Finally this object can make the DivaPlot produce a postscript and
 * JPEG output copy of itself. <p>
 *
 * See the DivaPlot object for a description of the facilities it provides.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see DivaPlot
 */
public class PlotControl
    extends JPanel
    implements PlotController, MouseMotionTracker, SpecListener,
               FigureListener, PlotScaledListener, ActionListener
{
    /**
     * DivaPlot object for displaying the spectra.
     */
    protected DivaPlot plot;

    /**
     * SpecDataComp object for retaining all spectra references.
     */
    protected SpecDataComp spectra = null;

    /**
     * The global list of spectra and plots.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * Scrolled pane that contains the Plot.
     */
    protected JScrollPane scroller;

    /**
     * Zoom factor controls and labels.
     */
    protected JComboBox xScale = new JComboBox();
    protected JComboBox yScale = new JComboBox();
    protected JLabel xScaleLabel = new JLabel( "X scale: ", JLabel.RIGHT );
    protected JLabel yScaleLabel = new JLabel( "Y scale: ", JLabel.RIGHT );
    protected JButton xIncr = new JButton();
    protected JButton xDecr = new JButton();
    protected JButton yIncr = new JButton();
    protected JButton yDecr = new JButton();
    protected JCheckBox showVHair = new JCheckBox( ":V-hair" );

    /**
     * Labels for displaying the current coordinates under the cursor.
     */
    protected JTextField xValue = new JTextField( "", JLabel.LEADING );
    protected JLabel yValue = new JLabel( "", JLabel.LEADING );
    protected JLabel xValueLabel = new JLabel( "X coordinate: ", JLabel.RIGHT );
    protected JLabel yValueLabel = new JLabel( "Y value: ", JLabel.RIGHT );

    /**
     * Panel for plot controls.
     */
    protected JPanel controlPanel = new JPanel( new GridBagLayout() );

    /**
     * Panel for DivaPlot. Use BorderLayout so that we can get a
     * preferred size that fills all the center.
     */
    protected JPanel plotPanel = new JPanel( new BorderLayout() );

    /**
     * List of spectra displayed and which one is current.
     */
    protected JComboBox nameList = new JComboBox();
    protected JLabel nameLabel = new JLabel( "Displaying: ", JLabel.RIGHT );

    /**
     * The name of this plot (unique among plots).
     */
    protected String name = null;

    /**
     * The identifier of this plot (unique among plots). This is the integer
     * value of plotCounter when the plot is created.
     */
    protected int identifier = -1;

    /**
     * Count of all plots (used to generate name).
     */
    protected static int plotCounter = 0;

    /**
     * Quick selection of data limits.
     */
    protected SimpleDataLimitControls simpleDataLimits = null;

    /**
     * Graphics edges configuration.
     */
    protected GraphicsEdges graphicsEdges = new GraphicsEdges();

    /**
     * Graphics rendering hints configuration.
     */
    protected GraphicsHints graphicsHints = new GraphicsHints();

    /**
     * Page selection for printing.
     */
    protected PrintRequestAttributeSet pageSet = null;

    /**
     * Create a PlotControl, adding spectra later.
     */
    public PlotControl()
    {
        try {
            spectra = new SpecDataComp();
            initUI();
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * Plot a list of spectra referenced in a SpecDataComp object.
     *
     * @param spectra reference to SpecDataComp object that is wrapping the
     *      spectra to display.
     * @exception SplatException thrown if problems reading spectra.
     */
    public PlotControl( SpecDataComp spectra )
        throws SplatException
    {
        this.spectra = spectra;
        initUI();
    }

    /**
     * Plot a spectrum using its filename specification. Standalone
     * constructor.
     *
     * @param file name of file containing spectrum.
     * @exception SplatException thrown if problems opening file.
     */
    public PlotControl( String file )
        throws SplatException
    {
        //  Check spectrum exists.
        SpecDataFactory factory = SpecDataFactory.getInstance();
        SpecData source = factory.get( file );
        if ( source == null ) {
            System.err.println( "Spectrum '" + file + "' cannot be found" );
        }
        spectra = new SpecDataComp( source );
        initUI();
    }

    /**
     * Release any locally allocated resources and references.
     *
     * @exception Throwable if finalize fails.
     */
    public void finalize()
        throws Throwable
    {
        release();
        super.finalize();
    }

    /**
     * Called when this widget is no longer required. Releases any
     * local resources and re-registers from global lists.
     */
    public void release()
    {
        try {
            globalList.removeSpecListener( this );
            nameList.removeActionListener( this );
            plot.getGraphicsPane().removeZoomDraggerListener( this );
            plot.removePlotScaledListener( this );
        }
        catch (Exception e) {
            // Ignored, not essential.
        }

    }

    /**
     * Get the PlotConfiguration being used by the DivaPlot.
     */
    public PlotConfiguration getPlotConfiguration()
    {
        return plot.getPlotConfiguration();
    }

    /**
     * Create the UI controls.
     *
     * @exception SplatException Description of the Exception
     */
    protected void initUI()
        throws SplatException
    {
        //  Initialisations.
        setLayout( new BorderLayout() );
        setDoubleBuffered( true );

        //  Target for SpecData drops.
        setTransferHandler( new SpecTransferHandler() );

        //  Generate our name.
        name = "<plot" + plotCounter + ">";
        identifier = plotCounter;
        plotCounter++;

        //  Add the control panel.
        controlPanel.setBorder( BorderFactory.createEtchedBorder() );
        add( controlPanel, BorderLayout.NORTH );

        //  Add the plot panel. Put in center to fill complete region.
        add( plotPanel, BorderLayout.CENTER );

        // Add the list of spectra that we're displaying.
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets( 0, 3, 3, 0 );
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        controlPanel.add( nameLabel, gbc );

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 1;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        controlPanel.add( nameList, gbc );

        //  The list of names uses a special renderer to also display
        //  the line properties.
        nameList.setRenderer( new LineRenderer( nameList ) );

        //  The nameList uses the SpecDataComp as its model for getting
        //  values.
        nameList.setModel( spectra );

        //  When the current spectrum is modified we pass this on to the
        //  global list.
        nameList.addActionListener( this );

        //  JComboBox sets default size this way!
        nameList.
            setPrototypeDisplayValue( "                                    " );

        //  Add the SimpleDataLimitControls to quickly choose a cut on the Y
        //  range.
        simpleDataLimits = new SimpleDataLimitControls();
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridx = 5;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        controlPanel.add( simpleDataLimits, gbc );

        //  Add the coordinate display labels to the controlPanel.
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0.0;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        controlPanel.add( xValueLabel, gbc );

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        xValue.setText( "                   " );
        xValue.setBorder( BorderFactory.createEtchedBorder() );
        controlPanel.add( xValue, gbc );

        //  Add action to centre on a given X coordinate.
        xValue.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    centreOnXCoordinate();
                }
            } );

        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        controlPanel.add( yValueLabel, gbc );

        gbc.gridx = 5;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        yValue.setText( "                   " );
        yValue.setBorder( BorderFactory.createEtchedBorder() );
        controlPanel.add( yValue, gbc );

        //  Add the toggle for displaying the vertical hair.
        gbc.gridx = 6;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.BOTH;
        controlPanel.add( showVHair, gbc );
        showVHair.setToolTipText( "Toggle display of vertical hair" );

        showVHair.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    toggleVHair();
                }
            } );

        //  Add the zoom factor controls and set their default values.
        addZoomControls();

        //  Create a DivaPlot object so that it can be called upon to
        //  do the drawing.
        plot = new DivaPlot( spectra );
        plot.addPlotScaledListener( this );

        //  Now that this is configured, we can properly configure the
        //  SimpleDataLimits object (needs the DataLimits object from
        //  the DivaPlot).
        simpleDataLimits.setPlot( this );

        //  Plot does the drawing of the spectrum, but is contained
        //  with a JScrollPane, so its size can be greater than the
        //  viewable surface.
        scroller = new JScrollPane( plot );

        //  Make the JScrollPane/Plot fill the area available by
        //  placing it in the center of the BorderLayout
        plotPanel.add( scroller, BorderLayout.CENTER );

        //  Get the Plot to track mouse motion events to update
        //  our X and Y readout widgets.
        plot.trackMouseMotion( this );

        //  Set the description of the coordinates (note this should
        //  be done if spectrum changes really, not here).
        xValueLabel.setText( plot.getLabel( 1 ) + ": " );
        yValueLabel.setText( plot.getLabel( 2 ) + ": " );

        //  Enable/disabled the vertical hair as Plot's status requires.
        showVHair.setSelected( plot.isShowVHair() );

        //  Register ourselves with the global list of plots and
        //  spectra so we can see if any of our displayed spectra are
        //  changed (or removed).
        globalList.addSpecListener( this );

        //  A region dragged out with mouse button 2 should zoom to
        //  that region. Need to listen for events that trigger this.
        //  Also just pressing 2 zooms in by 1 factor and pressing 3
        //  zooms out.
        plot.getGraphicsPane().addZoomDraggerListener( this );
    }

    /**
     * Return the DivaPlot reference.
     *
     * @return reference to the DivaPlot.
     */
    public DivaPlot getPlot()
    {
        // NOTE: This method may be used by TOPCAT classes.

        return plot;
    }

    /**
     * Add controls for zoom.
     */
    protected void addZoomControls()
    {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets( 3, 3, 3, 3 );
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;

        //  Create the plus/minus increment buttons.
        ImageIcon plusImage =
            new ImageIcon( ImageHolder.class.getResource( "plus.gif" ) );
        ImageIcon minusImage =
            new ImageIcon( ImageHolder.class.getResource( "minus.gif" ) );

        xIncr.setIcon( plusImage );
        xIncr.setToolTipText( "Increase X scale by 1 about centre of view " );
        xDecr.setIcon( minusImage );
        xDecr.setToolTipText( "Decrease X scale by 1 about centre of view " );

        yIncr.setIcon( plusImage );
        yIncr.setToolTipText( "Increase Y scale by 1 about centre of view " );
        yDecr.setIcon( minusImage );
        yDecr.setToolTipText( "Decrease Y scale by 1 about centre of view " );

        //  Add this makes the size matching look better.
        xScale.setBorder( BorderFactory.createEtchedBorder() );
        yScale.setBorder( BorderFactory.createEtchedBorder() );

        gbc.gridx = 0;
        gbc.gridy = 2;
        controlPanel.add( xScaleLabel, gbc );
        gbc.gridx = 1;
        gbc.gridy = 2;
        controlPanel.add( xScale, gbc );
        xScale.setToolTipText( "Set the X axis scale factor (any value)" );

        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 2;
        gbc.gridy = 2;
        controlPanel.add( xIncr, gbc );
        gbc.gridx = 3;
        gbc.gridy = 2;
        controlPanel.add( xDecr, gbc );

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridx = 4;
        gbc.gridy = 2;
        controlPanel.add( yScaleLabel, gbc );
        gbc.gridx = 5;
        gbc.gridy = 2;
        controlPanel.add( yScale, gbc );
        xScale.setToolTipText( "Set the Y axis scale factor (any value)" );

        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 6;
        gbc.gridy = 2;
        controlPanel.add( yIncr, gbc );
        gbc.gridx = 7;
        gbc.gridy = 2;
        controlPanel.add( yDecr, gbc );

        //  Add the preset scale factors.
        float incr = 0.5F;
        for ( int i = 2; i < 41; i++ ) {
            Float value = new Float( incr * (float) i );
            xScale.addItem( value );
            yScale.addItem( value );
        }

        //  The input to these fields should only be decimals and can
        //  be editted.
        DecimalComboBoxEditor xEditor =
            new DecimalComboBoxEditor( new DecimalFormat() );
        DecimalComboBoxEditor yEditor =
            new DecimalComboBoxEditor( new DecimalFormat() );
        xScale.setEditor( xEditor );
        yScale.setEditor( yEditor );
        xScale.setEditable( true );
        yScale.setEditable( true );

        //  Setup increment and decrement actions.
        xIncr.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    zoomAboutTheCentre( 1, 0 );
                }
            } );
        xDecr.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    zoomAboutTheCentre( -1, 0 );
                }
            } );
        yIncr.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    zoomAboutTheCentre( 0, 1 );
                }
            } );
        yDecr.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    zoomAboutTheCentre( 0, -1 );
                }
            } );

        //  The initial scale is 1.0 (do now to avoid pre-emptive
        //  trigger).
        resetScales();

        //  When an item is selected or value entered update the zoom.
        xScale.addItemListener(
            new ItemListener()
            {
                public void itemStateChanged( ItemEvent e )
                {
                    if ( e.getStateChange() == ItemEvent.SELECTED ) {
                        maybeScaleAboutCentre();
                    }
                }
            } );
        yScale.addItemListener(
            new ItemListener()
            {
                public void itemStateChanged( ItemEvent e )
                {
                    if ( e.getStateChange() == ItemEvent.SELECTED ) {
                        maybeScaleAboutCentre();
                    }
                }
            } );
    }

    /**
     * Set the display scale factor to that shown.
     */
    public void setScale()
    {
        plot.setScale( getXScale(), getYScale() );
    }

    /**
     * Set the display scale factor to that shown, keeping the current centre
     * of field, unless a centre operation is already pending.
     */
    public void maybeScaleAboutCentre()
    {
        if ( origin == null ) {
            double[] centre = getCentre();
            zoomAbout( 0, 0, centre[0], centre[1] );
        }
        setScale();
    }

    /**
     *  Record the current centre of the region. This must be applied after
     *  the scaling the plot is complete (doesn't usually work otherwise as
     *  scrollbars movement is bounded).
     */
    protected void recordOrigin( double xc, double yc )
    {
        try {
            double[] gCoords = new double[2];
            gCoords[0] = xc;
            gCoords[1] = yc;
            double[][] tmp = plot.transform( gCoords, true );
            origin = new double[2];
            origin[0] = tmp[0][0];
            origin[1] = tmp[1][0];
        }
        catch (Exception e) {
            //  Can fail if plot isn't realized yet. Not fatal.
        }
    }

    /**
     * Zoom plot about the current center by the given increments in the X and
     * Y scale factors.
     *
     * @param xIncrement value to add to current X scale factor.
     * @param yIncrement value to add to current Y scale factor.
     */
    public void zoomAboutTheCentre( int xIncrement, int yIncrement )
    {
        double[] centre = getCentre();
        zoomAbout( xIncrement, yIncrement, centre[0], centre[1] );
    }

    /**
     * Centre on the value shown in the xValue field, if possible.
     */
    public void centreOnXCoordinate()
    {
        double[] physical = new double[2];
        physical[0] = plot.unFormat( 1, xValue.getText() );
        physical[1] = plot.unFormat( 2, yValue.getText() );
        double[][] graphics = plot.transform( physical, false );
        if ( AstDouble.isBad( graphics[0][0] ) ||
            AstDouble.isBad( graphics[1][0] ) ) {
            return;
        }
        double[] currentGraphics = getCentre();
        graphics[1][0] = currentGraphics[1];
        zoomAbout( 0, 0, graphics[0][0], graphics[1][0] );
        try {
            plot.update();
        }
        catch ( Exception e ) {
            //  Do nothing.
        }
        if ( ! AstDouble.isBad( graphics[0][0] ) ) {
            plot.setVHairPosition( (float) graphics[0][0] );
        }
    }

    /**
     * Centre on a given coordinate. The coordinate is a formatted value.
     *
     * @param coord coordinate to centre on.
     */
    public void centreOnXCoordinate( String coord )
    {
        xValue.setText( coord );
        centreOnXCoordinate();
    }

    /**
     * Set the display scale factors. Also scales the plot.
     *
     * @param xs X scale factor.
     * @param ys Y scale factor.
     */
    public void setScale( float xs, float ys )
    {
        xScale.setSelectedItem( new Float( xs ) );
        yScale.setSelectedItem( new Float( ys ) );
    }

    /**
     * Scale the scale factors.
     *
     * @param xs value to scale the X scale factor by.
     * @param ys value to scale the Y scale factor by.
     */
    public void scaleScale( float xs, float ys )
    {
        setScale( xs * getXScale(), ys * getYScale() );
    }

    /**
     * Get the current X scale factor.
     *
     * @return the current X scale factor.
     */
    public float getXScale()
    {
        float xs = 1.0F;
        Object xObj = xScale.getSelectedItem();
        if ( xObj instanceof Float ) {
            xs = ( (Float) xObj ).floatValue();
        }
        else {
            //  Not a Float so get string and convert.
            xs = ( new Float( xObj.toString() ) ).floatValue();
        }
        return xs;
    }

    /**
     * Set the X scale factor.
     *
     * @param xs the new X scale factor.
     */
    public void setXScale( float xs )
    {
        xScale.setSelectedItem( new Float( xs ) );
    }

    /**
     * Get the current Y scale factor.
     *
     * @return the current Y scale factor.
     */
    public float getYScale()
    {
        float ys = 1.0F;
        Object yObj = yScale.getSelectedItem();
        if ( yObj instanceof Float ) {
            ys = ( (Float) yObj ).floatValue();
        }
        else {
            //  Not a Float so get string and convert.
            ys = ( new Float( yObj.toString() ) ).floatValue();
        }
        return ys;
    }

    /**
     * Set the Y scale factor.
     *
     * @param ys the new Y scale factor.
     */
    public void setYScale( float ys )
    {
        yScale.setSelectedItem( new Float( ys ) );
    }

    /**
     * Reset the apparent scales of the Plot. This means that whatever the
     * current size of the Plot its scale becomes 1x1.
     */
    public void resetScales()
    {
        xScale.setSelectedIndex( 0 );
        yScale.setSelectedIndex( 0 );
    }

    /**
     * Make sure that the plot is the same size as the viewport. If not done
     * before sizing requests the plot will just refit itself.
     */
    protected void matchViewportSize()
    {
        Dimension size = getViewport().getExtentSize();
        plot.setSize( size );
    }

    /**
     * Fit spectrum to the displayed width and height at same time.
     */
    public void fitToWidthAndHeight()
    {
        matchViewportSize();
        plot.fitToWidth();
        plot.fitToHeight();
        xScale.setSelectedIndex( 0 );
        yScale.setSelectedIndex( 0 );
        double[] centre = getCentre();
        recordOrigin( centre[0], centre[1] );
        setScale();
    }

    /**
     * Fit spectrum to the displayed width.
     */
    public void fitToWidth()
    {
        matchViewportSize();
        plot.fitToWidth();
        xScale.setSelectedIndex( 0 );
        double[] centre = getCentre();
        recordOrigin( centre[0], centre[1] );
        setScale();
    }

    /**
     * Fit spectrum to the displayed height.
     */
    public void fitToHeight()
    {
        matchViewportSize();
        plot.fitToHeight();
        yScale.setSelectedIndex( 0 );
        double[] centre = getCentre();
        recordOrigin( centre[0], centre[1] );
        setScale();
    }

    /**
     * Toggle the display of the plot vertical hair.
     */
    protected void toggleVHair()
    {
        plot.setShowVHair( showVHair.isSelected() );
    }

    /**
     * Update the displayed coordinates (implementation from
     * MouseMotionTracker).
     *
     * @param x the X coordinate value to show.
     * @param y the Y coordinate value to show.
     */
    public void updateCoords( String x, String y )
    {
        xValue.setText( x );
        yValue.setText( y );
    }

    /**
     * Look for postscript printing services.
     */
    protected PrintService[] getPostscriptPrintServices( String fileName )
        throws SplatException
    {
        FileOutputStream outstream;
        StreamPrintService psPrinter;
        String psMimeType = "application/postscript";

        StreamPrintServiceFactory[] factories =
            PrinterJob.lookupStreamPrintServices( psMimeType );
        if ( factories.length > 0 ) {
            try {
                outstream = new FileOutputStream( new File( fileName ) );
                PrintService[] services = new PrintService[1];
                services[0] =  factories[0].getPrintService( outstream );
                return services;
            }
            catch ( FileNotFoundException e ) {
                throw new SplatException( e );
            }
        }
        return new PrintService[0];
    }

    /**
     * Print to a postscript file.
     */
    public void printPostscript( String fileName )
        throws SplatException
    {
        PrintService[] services = getPostscriptPrintServices( fileName );
        if ( services.length != 0 ) {
            try {
                PrinterJob pj = PrinterJob.getPrinterJob();
                pj.setPrintService( services[0] );
                pj.setPrintable( plot );
                makePageSet();
                if ( pj.printDialog( pageSet ) ) {
                    pj.print( pageSet );
                    ((StreamPrintService)services[0]).dispose();
                    ((StreamPrintService)services[0]).getOutputStream().close();
                }
            }
            catch ( PrinterException e ) {
                throw new SplatException( e );
            }
            catch ( IOException e ) {
                throw new SplatException( e );
            }
        }
        else {
            // Report there are no printers available.
            throw new SplatException( "Sorry no printers are available" );
        }
    }

    /**
     * Make a printable copy of the Plot content.
     */
    public void print()
        throws SplatException
    {
        PrintService[] services = PrinterJob.lookupPrintServices();
        if ( services.length == 0 ) {

            // No actual print services are available (i.e. no valid local
            // printers), then fall back to the postscript option.
            printPostscript( "out.ps" );
            return;
        }
        try {
            PrinterJob pj = PrinterJob.getPrinterJob();
            pj.setPrintService( services[0] );
            pj.setPrintable( plot );
            makePageSet();
            if ( pj.printDialog( pageSet ) ) {
                pj.print( pageSet );
            }
        }
        catch ( PrinterException e ) {
            throw new SplatException( e );
        }
    }

    /**
     * Create the default PageSet for printing. This is A4.
     */
    private void makePageSet()
    {
        if ( pageSet == null ) {
            pageSet = new HashPrintRequestAttributeSet();
            pageSet.add( OrientationRequested.LANDSCAPE );
            pageSet.add( MediaSizeName.ISO_A4 );
            pageSet.add
                ( new JobName( Utilities.getTitle( "printer job" ), null ) );
        }
    }

    /**
     * Add a SpecData reference to the list of displayed spectra.
     *
     * @param spec reference to a spectrum.
     */
    public void addSpectrum( SpecData spec )
        throws SplatException
    {
        spectra.add( spec );
        try {
            updateThePlot( null );
        }
        catch (SplatException e) {
            spectra.remove( spec );
            throw e;
        }
    }

    /**
     * Remove a spectrum from the plot.
     *
     * @param spec reference to a spectrum.
     */
    public void removeSpectrum( SpecData spec )
    {
        spectra.remove( spec );
        try {
            updateThePlot( null );
        }
        catch (SplatException e) {
            // Do nothing, should be none-fatal.
        }
    }

    /**
     * Remove a spectrum from the plot.
     *
     * @param index of the spectrum.
     */
    public void removeSpectrum( int index )
    {
        spectra.remove( index );
        try {
            updateThePlot( null );
        }
        catch (SplatException e) {
            // Do nothing, should be none-fatal.
        }
    }

    /**
     * Get the number of spectrum currently plotted.
     *
     * @return number of spectra being displayed.
     */
    public int specCount()
    {
        return spectra.count();
    }

    /**
     * Say if a spectrum is being plotted.
     *
     * @param spec the spectrum to check.
     * @return true if the SpecData object is being displayed.
     */
    public boolean isDisplayed( SpecData spec )
    {
        return spectra.have( spec );
    }

    /**
     * Return the integer identifier of this plot (unique among plots).
     *
     * @return integer identifier.
     */
    public int getIdentifier()
    {
        return identifier;
    }

    /**
     * Return the name of this plot (unique among plots).
     *
     * @return name of the plot.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Override toString to return the name of this plot (unique among plots).
     *
     * @return name of this plot.
     */
    public String toString()
    {
        return name;
    }

    /**
     * Return reference to SpecDataComp.
     *
     * @return the SpecDataComp object used to wrapp all spectra
     *         displayed in the plot.
     */
    public SpecDataComp getSpecDataComp()
    {
        return spectra;
    }

    /**
     * Set the SpecDataComp component used.
     *
     * @param spectra new SpecDataComp object to use for wrapping any spectra
     *      displayed in the Plot.
     * @exception SplatException thrown if problems reading spectral data.
     */
    public void setSpecDataComp( SpecDataComp spectra )
        throws SplatException
    {
        // NOTE: This method may be used by TOPCAT classes.
        // At time of writing, TOPCAT calls this method, followed by
        // updateThePlot(null), to change the data conten of the plot.

        this.spectra = spectra;
        plot.setSpecDataComp( spectra );

        // The list of spectra is used as the model for the drop-down list.
        nameList.setModel( spectra );
    }

    /**
     * Increment scales in both dimensions about a centre.
     *
     * @param xIncrement increment for X scale factor.
     * @param yIncrement increment for Y scale factor.
     * @param x X coordinate to zoom about.
     * @param y Y coordinate to zoom about.
     */
    public void zoomAbout( int xIncrement, int yIncrement, double x,
                           double y )
    {
        recordOrigin( x, y );

        //  Scale the plot by the increment.
        float xs = Math.max( getXScale() + xIncrement, 1.0F );
        float ys = Math.max( getYScale() + yIncrement, 1.0F );
        setScale( xs, ys );
    }

    /**
     * Padding kept around any zoomed region.
     */
    private final static double ZOOMPADDING = 1.10;

    /**
     * Zoom/scroll the plot to display a given rectangular region. <p>
     *
     * If the region has zero width or height then the zoom is merely
     * incremented by 1 in both dimensions. Also if the scale factors are
     * increased by more than a factor of 20, then they are clipped to this
     * value (the most likely action here is a user accidently returning a
     * very small region).
     *
     * @param region the region to zoom into.
     */
    public void zoomToRectangularRegion( Rectangle2D region )
    {
        if ( region != null &&
            region.getWidth() != 0.0 && region.getHeight() != 0.0 ) {

            //  Scale the region to allow some "slack" around it. When
            //  zooming this looks much better (and is less confusing).
            double centreX = region.getX() + region.getWidth() / 2.0;
            double centreY = region.getY() + region.getHeight() / 2.0;
            double scaledWidth = region.getWidth() * ZOOMPADDING;
            double scaledHeight = region.getHeight() * ZOOMPADDING;

            recordOrigin( centreX, centreY );

            //  Get the viewport size and scale region.
            Dimension viewSize = getViewport().getExtentSize();
            float xs = (float) ( (double) viewSize.width / scaledWidth );
            float ys = (float) ( (double) viewSize.height / scaledHeight );
            if ( xs > 20.0F ) {
                xs = 20.0F;
            }
            if ( ys > 20.0F ) {
                ys = 20.0F;
            }
            scaleScale( xs, ys );
        }
        else if ( region != null ) {
            zoomAbout( 1, 1, region.getX(), region.getY() );
        }
    }

    /**
     * The scroll position that should be established after a change in plot
     * scale.
     */
    private double[] origin = null;

    /**
     * Get the coordinate range displayed in the current view.
     *
     * @return array of two doubles, the lower and upper limits of the Plot in
     *      X coordinates.
     */
    public double[] getViewRange()
    {
        double[] pRange = getViewCoordinates();
        double[] xRange = new double[2];
        xRange[0] = pRange[0];
        xRange[1] = pRange[2];
        return xRange;
    }

    /**
     * Get the physical coordinates limits of the current view of the Plot
     * (i.e.<!-- --> what you can see).
     *
     * @return array of four doubles, the lower X, lower Y, upper X and upper
     *      Y coordinates.
     */
    public double[] getViewCoordinates()
    {
        //  Viewport extent in component coordinates.
        Rectangle view = getViewport().getViewRect();
        double[] gRange = new double[4];
        gRange[0] = view.getX();
        gRange[1] = view.getY();
        gRange[2] = view.getX() + view.getWidth();
        gRange[3] = view.getY() + view.getHeight();

        //  Transform these into physical coordinates.
        double[][] tmp = plot.transform( gRange, true );
        gRange[0] = tmp[0][0];
        gRange[1] = tmp[1][0];
        gRange[2] = tmp[0][1];
        gRange[3] = tmp[1][1];
        return gRange;
    }

    /**
     * Get the physical coordinate limits of the complete Plot
     * (i.e.&nbsp;the whole plot, including zoomed regions that you
     * cannot see).
     *
     * @return array of four doubles, the lower X, lower Y, upper X and upper
     *      Y coordinates.
     */
    public double[] getDisplayCoordinates()
    {
        //  Ask plot for graphics limits it is using.
        float[] gLimits = plot.getGraphicsLimits();

        //  Transform these into physical coordinates.
        double[] gRange = new double[4];
        gRange[0] = gLimits[0];
        gRange[1] = gLimits[1];
        gRange[2] = gLimits[2];
        gRange[3] = gLimits[3];
        double[][] tmp = plot.transform( gRange, true );
        gRange[0] = tmp[0][0];
        gRange[1] = tmp[1][0];
        gRange[2] = tmp[0][1];
        gRange[3] = tmp[1][1];
        return gRange;
    }

    /**
     * Update the plot. Should be called when events that require the Plot to
     * redraw itself occur (i.e. when spectra are added or removed and when
     * the Plot configuration is changed).
     * <p>
     * If referenceSpec is set then any DataLimit values held by the Plot will
     * be transformed from the coordinates of the referenceSpec to the
     * coordinate of the current spectrum (this resets the limits to match a
     * change in reference spectrum, that could also be a change in coordinate
     * system).
     */
    public void updateThePlot( SpecData referenceSpec )
        throws SplatException
    {
        // NOTE: This method may be used by TOPCAT classes.
        // At time of writing, TOPCAT calls this method, following
        // setSpecDataComp, to change the data in the plot window.
        if ( referenceSpec != null ) {
            try {
                DataLimits dataLimits = plot.getDataLimits();
                double[] range = new double[4];
                range[0] = dataLimits.getXLower();
                range[1] = dataLimits.getXUpper();
                range[2] = dataLimits.getYLower();
                range[3] = dataLimits.getYUpper();
                range = spectra.transformRange(getCurrentSpectrum(),
                                               referenceSpec,
                                               range,
                                               spectra.isDataUnitsMatching());
                dataLimits.setXLower( range[0] );
                dataLimits.setXUpper( range[1] );
                dataLimits.setYLower( range[2] );
                dataLimits.setYUpper( range[3] );

                //  If a reference spectrum has been given we need to refit to
                //  the displayed size (the limits that apply to the graphics
                //  coordinate have changed). Note we do not change the scale.
                plot.staticUpdate();
            }
            catch (SplatException e) {
                //  Get normal redraw.
                referenceSpec = null;
            }
        }

        if ( referenceSpec == null ) {
            //  Get a normal redraw. Note plot.update may throw a
            //  SplatException.
            plot.update();
        }

        // Check if the X or Y data limits are supposed to match the
        // viewable surface or not.
        if ( plot.getDataLimits().isXFit() ) {
            fitToWidth();
        }
        if ( plot.getDataLimits().isYFit() ) {
            fitToHeight();
        }
    }

    /**
     * Return the current spectrum (top of the combobox of names). This method
     * allows toolboxes that can only work on a single spectrum to make a
     * choice from all those displayed.
     *
     * @return the current spectrum.
     */
    public SpecData getCurrentSpectrum()
    {
        return (SpecData) spectra.getCurrentSpectrum();
    }

    /**
     * Set whether the percentile cuts also have a fit to Y scale
     * automatically applied.
     */
    public void setAutoFitPercentiles( boolean autofit )
    {
        simpleDataLimits.setAutoFit( autofit );
    }

//
//  Implement the PlotScaledListener interface.
//
    /**
     * Make any adjustments needed to respond to a change in scale by the
     * Plot.
     */
    public void plotScaleChanged( PlotScaleChangedEvent e )
    {
        if ( origin != null ) {

            //  Define the limits of the scrollbars to be somewhat greater the
            //  Plot preferred size. This allows sloppy edges, spiking the
            //  BoundedRangeModel...
            Dimension plotSize = plot.getPreferredSize();
            scroller.getHorizontalScrollBar().setMaximum( plotSize.width * 2 );
            scroller.getHorizontalScrollBar().setMinimum( -plotSize.width );
            scroller.getVerticalScrollBar().setMaximum( plotSize.height * 2 );
            scroller.getVerticalScrollBar().setMinimum( -plotSize.height );

            double[][] gCoords = plot.transform( origin, false );
            setCentre( gCoords[0][0], gCoords[1][0] );
            origin = null;
        }
    }

    /**
     * Set the viewport to show a given position as the centre.
     *
     * @param x the X coordinate of new centre (view coordinates).
     * @param y the Y coordinate of new centre (view coordinates).
     */
    public void setCentre( double x, double y )
    {
        Dimension viewsize = getViewport().getExtentSize();
        int left = (int) ( x - viewsize.getWidth() / 2.0 );
        int right = (int) ( y - viewsize.getHeight() / 2.0 );
        getViewport().setViewPosition( new Point( left, right ) );
    }

    /**
     * Get the centre of the current view.
     *
     * @return array of two doubles, the X and Y coordinates of the centre of
     *      the current view (view coordinates).
     */
    public double[] getCentre()
    {
        Rectangle view = getViewport().getViewRect();
        double[] centre = new double[2];
        centre[0] = view.getX() + view.getWidth() / 2.0;
        centre[1] = view.getY() + view.getHeight() / 2.0;
        return centre;
    }

    /**
     * Get reference to the JViewport being used to display the spectra.
     *
     * @return reference to the JViewport.
     */
    public JViewport getViewport()
    {
        return scroller.getViewport();
    }

//
//  Implement the SpecListener interface.
//
    /**
     * A new spectrum is added. Do nothing, until it is added to this plot.
     *
     * @param e object describing the event.
     */
    public void spectrumAdded( SpecChangedEvent e )
    {
        // Do nothing.
    }

    /**
     * React to a spectrum being removed, if one of ours.
     *
     * @param e object describing the event.
     */
    public void spectrumRemoved( SpecChangedEvent e )
    {
        int globalIndex = e.getIndex();
        SpecData spectrum = globalList.getSpectrum( globalIndex );
        int localIndex = spectra.indexOf( spectrum );
        if ( localIndex > -1 ) {
            removeSpectrum( localIndex );
        }
    }

    /**
     * React to a spectrum property change, if one of ours.
     *
     * @param e object describing the event.
     */
    public void spectrumChanged( SpecChangedEvent e )
    {
        int globalIndex = e.getIndex();
        SpecData spectrum = globalList.getSpectrum( globalIndex );
        int localIndex = spectra.indexOf( spectrum );
        if ( localIndex > -1 ) {
            try {
                updateThePlot( null );
            }
            catch (SplatException ignored) {
                // Do nothing, should be none-fatal.
            }
        }
    }

    /**
     * React to a change in the global current spectrum. Do nothing.
     *
     * @param e object describing the event.
     */
    public void spectrumCurrent( SpecChangedEvent e )
    {
        // Do nothing.
    }

//
//  Implement the FigureListener interface.
//
    /**
     * Sent when the zoom figure is created. Do nothing.
     *
     * @param e object describing the event.
     */
    public void figureCreated( FigureChangedEvent e )
    {
        //  Do nothing.
    }

    /**
     * Sent when a zoom interaction is complete. Events by mouse button 3 are
     * assumed to mean unzoom.
     *
     * @param e object describing the event.
     */
    public void figureRemoved( FigureChangedEvent e )
    {
        Rectangle2D region = (Rectangle2D)
            ( (DragRegion) e.getSource() ).getFinalShape();
        LayerEvent le = e.getLayerEvent();
        if ( le.getModifiers() == LayerEvent.BUTTON3_MASK ) {
            zoomAbout( -1, -1, region.getX(), region.getY() );
        }
        else {
            zoomToRectangularRegion( region );
        }
    }

    /**
     * Send when the zoom figure is changed. Do nothing.
     *
     * @param e object describing the event.
     */
    public void figureChanged( FigureChangedEvent e )
    {
        //  Do nothing.
    }

//
// Implement ActionListener interface.
//
    /**
     * Respond to selection of a new spectrum as the current one. This makes
     * the selected spectrum current in the global list and forces the Plot
     * to undergo a re-draw.
     *
     * @param e object describing the event.
     */
    public void actionPerformed( ActionEvent e )
    {
        try {
            globalList.setCurrentSpectrum
                ( globalList.getSpectrumIndex( getCurrentSpectrum() ) );
            // Could change underlying coordinates/labelling etc.
            updateThePlot( spectra.getLastCurrentSpectrum() );
        }
        catch ( Exception ex ) {
            // Do nothing
        }
    }

//
// Implement PlotController interface.
//
    public void updatePlot()
    {
        try {
            updateThePlot( null );
        }
        catch (SplatException e) {
            e.printStackTrace();
        }
    }

    public void setPlotColour( Color color )
    {
        plot.setBackground( color );
    }

    public Color getPlotColour()
    {
        return plot.getBackground();
    }

    public Frame getPlotCurrentFrame()
    {
        // Return current Frame of the graphics FrameSet.
        return (Frame) plot.getSpecDataComp().getAst().getRef();
    }

}
