package uk.ac.starlink.frog.plot;

import diva.canvas.event.LayerEvent;

import java.awt.*;
import java.awt.print.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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


import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.gui.AstDouble;
import uk.ac.starlink.ast.gui.GraphicsEdges;
import uk.ac.starlink.ast.gui.GraphicsHints;
import uk.ac.starlink.ast.gui.PlotConfiguration;
import uk.ac.starlink.ast.gui.PlotController;
import uk.ac.starlink.frog.data.DataLimits;
import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.data.TimeSeriesComp;
import uk.ac.starlink.frog.data.TimeSeriesFactory;
import uk.ac.starlink.frog.iface.DecimalComboBoxEditor;
import uk.ac.starlink.frog.iface.LineRenderer;
import uk.ac.starlink.frog.iface.images.ImageHolder;
import uk.ac.starlink.frog.util.FrogException;
import uk.ac.starlink.frog.util.Utilities;
import uk.ac.starlink.frog.util.FrogDebug;


/**
 * A PlotControl object consists of a Plot inside a scrolled pane and various
 * controls in a panel. The Plot allows you to display many series. Plots
 * wrapped by this object are given a name that is unique within an
 * application (<plotn>) and are assigned an identifying number 
 * (0 upwards). <p>
 *
 * Finally this object can make the DivaPlot produce a postscript and
 * JPEG output copy of itself. <p>
 *
 * See the DivaPlot object for a description of the facilities it provides.
 *
 * @author Peter W. Draper
 * @author Alasdair Allan
 * @version $Id$
 * @see Plot
 */
public class PlotControl extends JPanel
    implements PlotController, MouseMotionTracker,  
               FigureListener, PlotScaledListener, ActionListener 
{
   /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();


    /**
     * DivaPlot object for displaying the series.
     */
    protected DivaPlot plot;

    /**
     * TimeSeriesComp object for retaining all series references.
     */
    protected TimeSeriesComp series = new TimeSeriesComp();

 
    /**
     * Scrolled pane that contains the Plot.
     */
    protected JScrollPane scroller;


    /**
     * Labels for displaying the current coordinates under the cursor.
     */
    protected JLabel xValue = new JLabel( "", JLabel.LEADING );
    protected JLabel yValue = new JLabel( "", JLabel.LEADING );
    protected JLabel xValueLabel = new JLabel( "X coordinate: ", JLabel.RIGHT );
    protected JLabel yValueLabel = new JLabel( "Y value: ", JLabel.RIGHT );

    /**
     * Status Lablel
     */
    protected JLabel statusLabelLine1 = new JLabel( "", JLabel.LEFT );
    protected JLabel statusLabelLine2 = new JLabel( "", JLabel.LEFT );
   
    /**
     * Panel for plot controls.
     */
    protected JPanel controlPanel = new JPanel( new GridLayout( 2, 3) );

    /**
     * Panel for DivaPlot. Use BorderLayout so that we can get a
     * preferred size that fills all the center.
     */
    protected JPanel plotPanel = new JPanel( new BorderLayout() );

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
     * Full data limits control configuration.
     */
    protected DataLimits dataLimits = new DataLimits();

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
     * Create a PlotControl, adding series later.
     */
    public PlotControl()
    {
    
        debugManager.print(
           "            Creating a PlotControl() widget..." );
        try {
            initUI();
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * Plot a list of series referenced in a TimeSeriesComp object.
     *
     * @param series reference to TimeSeriesComp object that is wrapping the
     *      series to display.
     * @exception FrogException thrown if problems reading series.
     */
    public PlotControl( TimeSeriesComp series ) throws FrogException
    {
        debugManager.print( 
           "            Creating PlotControl( TimeSeriesComp )..." );
        this.series = series;
        initUI(); 
        
        // Automatically scale Plot
        dataLimits.setYAutoscaled( true );
        updatePlot();
    }

    /**
     * Plot a series using its filename specification. Standalone
     * constructor.
     *
     * @param file name of file containing series.
     * @exception FrogException thrown if problems opening file.
     */
    public PlotControl( String file ) throws FrogException
    {
        debugManager.print( 
           "            Creating PlotControl( FileName )..." );
        //  Check series exists.
        TimeSeriesFactory factory = TimeSeriesFactory.getReference();
        TimeSeries source = factory.get( file );
        if ( source == null ) {
            System.err.println( "Series '" + file + "' cannot be found" );
        }
        series = new TimeSeriesComp( source );
        initUI(); 
               
        // Automatically scale Plot
        dataLimits.setYAutoscaled( true );
        updatePlot();
    }

    /**
     * Release any locally allocated resources and references.
     *
     * @exception Throwable if finalize fails.
     */
    public void finalize()
        throws Throwable
    {
        super.finalize();
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
     * @exception FrogException Description of the Exception
     */
    protected void initUI() throws FrogException
    {
        debugManager.print( "              Calling initUI()" );
        //  Initialisations.
        setLayout( new BorderLayout() );
        setDoubleBuffered( true );

        //  Generate our name.
        name = "<plot" + plotCounter + ">";
        identifier = plotCounter;
        plotCounter++;

        //  Add the control panel.
        controlPanel.setBorder( BorderFactory.createEtchedBorder() );
        add( controlPanel, BorderLayout.SOUTH );

        //  Add the plot panel. Put in center to fill complete region.
        add( plotPanel, BorderLayout.CENTER );
        
        statusLabelLine1.setText( "                               ");
        controlPanel.add( statusLabelLine1 );
        
        controlPanel.add( xValueLabel  );
        xValue.setText( "                   " );
        xValue.setBorder( BorderFactory.createEtchedBorder() );
        controlPanel.add( xValue  );      
 
        statusLabelLine2.setText( "                               ");
        controlPanel.add( statusLabelLine2 );         
         
        controlPanel.add( yValueLabel  );
        yValue.setText( "                   " );
        yValue.setBorder( BorderFactory.createEtchedBorder() );
        controlPanel.add( yValue  );
      
        //  Create a DivaPlot object so that it can be called upon to
        //  do the drawing.
        plot = new DivaPlot( series );
        plot.addPlotScaledListener( this );

        //  Plot does the drawing of the series, but is contained
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
        //  be done if series changes really, not here).
        xValueLabel.setText( plot.getLabel( 1 ) + ": " );
        yValueLabel.setText( plot.getLabel( 2 ) + ": " );

    }

    /**
     * Return the DivaPlot reference.
     *
     * @return reference to the DivaPlot.
     */
    public DivaPlot getPlot()
    {
        return plot;
    }

    /**
     * Return a DataLimits reference
     *
     * @return reference to the Plot's DataLimits Object
     */
     public DataLimits getDataLimits()
     {
        return dataLimits;
     }    

     
    /**
     * Change text in the 1st line of the PlotControl status label
     *
     * @param string the text
     */
    public void setStatusTextOne( String text )
    {
        statusLabelLine1.setText( text );
    }   
     
    /**
     * Change text in the 2nd line of teh PlotControl status label
     *
     * @param string the text
     */
    public void setStatusTextTwo( String text )
    {
        statusLabelLine2.setText( text );
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
     * Fit series to the displayed width and height at same time.
     */
    public void fitToWidthAndHeight()
    {
        matchViewportSize();
        plot.fitToWidth();
        plot.fitToHeight();
    }

    /**
     * Fit series to the displayed width.
     */
    public void fitToWidth()
    {
        matchViewportSize();
        plot.fitToWidth();
    }

    /**
     * Fit series to the displayed height.
     */
    public void fitToHeight()
    {
        matchViewportSize();
        plot.fitToHeight();
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
        xValue.setText( " " + x );
        yValue.setText( " " + y );
    }

    /**
     * Look for postscript printing services (in the absence of any
     * proper services).
     */
    protected PrintService[] getPostscriptPrintServices()
    {       
        FileOutputStream outstream;
        StreamPrintService psPrinter;
        String psMimeType = "application/postscript";

        StreamPrintServiceFactory[] factories =
            PrinterJob.lookupStreamPrintServices( psMimeType );
        if ( factories.length > 0 ) {
            try {
                outstream = new FileOutputStream( new File( "out.ps" ));
                PrintService[] services = new PrintService[1];
                services[0] =  factories[0].getPrintService( outstream );
                return services;
            }
            catch (FileNotFoundException e) {
                //  No postscript either.
            }
        }
        return new PrintService[0];
    }

    /**
     * Make a printable copy of the Plot content.
     */
    public void print()
    {
        PrinterJob pj = PrinterJob.getPrinterJob();
        PrintService[] services = PrinterJob.lookupPrintServices(); 
        if ( services.length == 0 ) {
            // No print services are available (i.e. no valid local
            // printers), can we still print to a postscript file?
            services = getPostscriptPrintServices();
        }
        if ( services.length > 0 ) {

            //  Create the default PrintRequestAttributeSet if not done
            //  already. If created then first print request will ask
            //  for the page settings to be verified.
            boolean pageVerify = false;
            if ( pageSet == null ) {
                //  Make the default landscape A4.
                pageSet = new HashPrintRequestAttributeSet();
                pageSet.add( OrientationRequested.LANDSCAPE );
                pageSet.add( MediaSizeName.ISO_A4 );
                pageSet.add
                    ( new JobName( Utilities.getTitle("printer job"),null ) );
                pageVerify = true;
            }

            try {
                pj.setPrintService( services[0] );
                pj.setPrintable( plot );
                if ( pageVerify ) {
                    pj.pageDialog( pageSet );
                }
                if ( pj.printDialog( pageSet ) ) {
                    pj.print( pageSet );
                }
            }
            catch ( PrinterException e ) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Add a TimeSeries reference to the list of displayed series.
     *
     * @param spec reference to a series.
     */
    public void addSeries( TimeSeries spec )
    {
        series.add( spec );
        updatePlot();
    }

    /**
     * Remove a series from the plot.
     *
     * @param spec reference to a series.
     */
    public void removeSeries( TimeSeries spec )
    {
        series.remove( spec );
        updatePlot();
    }

    /**
     * Remove a series from the plot.
     *
     * @param index of the series.
     */
    public void removeSeries( int index )
    {
        series.remove( index );
        updatePlot();
    }

    /**
     * Get the number of series currently plotted.
     *
     * @return number of series being displayed.
     */
    public int seriesCount()
    {
        return series.count();
    }

    /**
     * Say if a series is being plotted.
     *
     * @param series the series to check.
     * @return true if the TimeSeries object is being displayed.
     */
    public boolean isDisplayed( TimeSeries s )
    {
        return series.have( s );
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
     * Return reference to TimeSeriesComp.
     *
     * @return the TimeSeriesComp object used to wrapp all series
     *         displayed in the plot.
     */
    public TimeSeriesComp getTimeSeriesComp()
    {
        return series;
    }

    /**
     * Set the TimeSeriesComp component used.
     *
     * @param series new TimeSeriesComp object to use for wrapping any series
     *      displayed in the Plot.
     * @exception FrogException thrown if problems reading series data.
     */
    public void setTimeSeriesComp( TimeSeriesComp series )
        throws FrogException
    {
        this.series = series;
        plot.setTimeSeriesComp( series );
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
     * (i.e. what you can see).
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
     * Get the physical coordinate limits of the complete Plot (i.e. the whole
     * plot, including zoomed regions that you cannot see).
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
     * Return the current series
     *
     * @return the current series.
     */
    public TimeSeries getCurrentSeries()
    {
        int currentIndex = series.getCurrentSeries();
        return series.get( currentIndex );
    }

//
//  Implement the PlotScaledListener interface.
//
    /**
     * Make any adjustments needed to respond to a change in scale by the
     * Plot.
     *
     * @param e object describing event.
     */
    public void plotScaleChanged( PlotScaleChangedEvent e )
    {
        if ( origin != null ) {
            //  Define the limits of the scrollbars to be somewhat
            //  greater the Plot preferred size. This allows sloppy
            //  edges, spiking the BoundedRangeModel...
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
     * Update the plot. Should be called when events that require the Plot to
     * redraw itself occur (i.e. when series are added or removed and when
     * the Plot configuration is changed).
     */
    public void updatePlot()
    {
        try {
            plot.update();
        }
        catch ( FrogException e ) {
            //  Do nothing, probably not fatal.
        }

        // Check if the X or Y data limits are supposed to match the
        // viewable surface or not.
        if ( dataLimits.isXFit() ) {
            fitToWidth();
        }
        if ( dataLimits.isYFit() ) {
            fitToHeight();
        }
    }

    /**
     * Get reference to the JViewport being used to display the series.
     *
     * @return reference to the JViewport.
     */
    public JViewport getViewport()
    {
        return scroller.getViewport();
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
        // do nothing
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
     * Respond to selection of a new series as the current one. This makes
     * the selected series current in the global list.
     *
     * @param e object describing the event.
     */
    public void actionPerformed( ActionEvent e )
    {
         //  Do nothing.
    }

//
// Implement PlotController interface. Note that the updatePlot()
// method is part of this
//
    public void setPlotColour( Color color )
    {
        plot.setBackground( color );
    }
    
    public Color getPlotColour()
    {
        return getBackground();
    }
    
    public Frame getPlotCurrentFrame()
    {
        // Use the FrameSet of the current series.
        return (Frame) plot.getTimeSeriesComp().getAst().getRef();
    }
    
}



