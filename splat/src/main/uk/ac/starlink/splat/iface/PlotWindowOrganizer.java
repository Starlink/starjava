/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     08-MAY-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.SwingUtilities;

import uk.ac.starlink.splat.plot.PlotControl;

/**
 * Creates an object for organizing the positions of a set of
 * parent frames for a list of PlotControl objects (assumes one
 * each). The main functions offered are the ability to tile and
 * cascade the plots. Most likely to be used with a "Windows" menu.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PlotWindowOrganizer
{
    /**
     *  The global list of spectra and plots.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * Create an instance.
     */
    public PlotWindowOrganizer()
    {
        //  Nothing to do, data is in the global list.
    }

    // Amount of space reserved around the edge of screen.
    private int xReserve = 50;
    private int yReserve = 50;

    // Increment for cascade
    private int xIncrement = 50;
    private int yIncrement = 50;

    /**
     * Return number of windows we're managing at present.
     */
    private int windowCount() 
    {
        return globalList.plotCount();
    }

    /**
     * Arrange all Plot windows in an tiled array. XXX doesn't quite
     * work yet. Not that useful anyway.
     */
    public void tile()
    {
        //  No windows, do nothing.
        int numWin = windowCount(); 
        if ( numWin == 0 ) {
            return;
        }

        //  Keep the reserve region around the edge.
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int xCurrent = xReserve;
        int yCurrent = yReserve;
        int xMax = screenSize.width - xReserve;
        int yMax = screenSize.height - yReserve;

        //  Determine how many windows to fit where. Use an area
        //  algorithm.
        double area = ( xMax - xReserve ) * ( yMax - yReserve );
        double eachArea = area / numWin;
        int xStep = (int) Math.sqrt( eachArea ) * ( screenSize.width /
                                                    screenSize.height );
        int yStep = (int) ( eachArea / (double) xStep );
        int perLine = ( xMax - xReserve ) / xStep;

        int wCount = 0;
        while ( wCount < numWin ) {
            for ( int j = 0; j < perLine && wCount < numWin; j++ ) {
                PlotControl plot = globalList.getPlot( wCount++ );
                Window w = SwingUtilities.getWindowAncestor( plot );
                w.setLocation( xCurrent, yCurrent );
                w.setSize( new Dimension( xStep, yStep ) );
                w.toFront();
                yCurrent += yStep;
            }
            xCurrent += xStep;
            yCurrent = yReserve;
        }
    }

    /**
     * Arrange all Plot windows to be cascaded.
     */
    public void cascade()
    {
        //  No windows, do nothing.
        int numWin = windowCount(); 
        if ( numWin == 0 ) {
            return;
        }
        int xCurrent = xReserve;
        int yCurrent = yReserve;

        for ( int i = 0; i < numWin; i++ ) {
            PlotControl plot = globalList.getPlot( i );
            Window w = SwingUtilities.getWindowAncestor( plot );
            w.setLocation( xCurrent, yCurrent );
            w.toFront();
            xCurrent += xIncrement;
            yCurrent += yIncrement;
        }
    }
}
