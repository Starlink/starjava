/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     04-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.GeneralPath;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Test routine for fitting a data spectrum using a Voigt function.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class FitVoigt 
    extends JPanel 
{
    /**
     *  Array of X positions used to plot the spectrum.
     */
    protected double[] xPos;
    protected double[] xFit;

    /**
     *  Array of Y positions used to plot the spectrum.
     */
    protected double[] yPos;
    protected double[] yFit;

    /**
     *  Array of weights.
     */
    protected double[] weights;

    /**
     *  Smallest X data value.
     */
    protected double xMin;

    /**
     *  Largest X data value.
     */
    protected double xMax;

    /**
     *  Smallest Y data value.
     */
    protected double yMin;

    /**
     *  Largest Y data value.
     */
    protected double yMax;

    /**
     * Main routine.
     *
     * @param args Command-line arguments, first is name of spectrum.
     */
    public static void main( String args[] ) throws IOException 
    {
        //  Check command-line argument.
        File f = null;
        if ( args.length != 1 ) {
            System.out.println( "Usage: FitVoigt spectrum_file" );
            System.exit( 1 );
        } else {
            //  Check file exists.
            f = new File( args[0] );
            if ( ! f.exists() ) {
                System.out.println( args[0] + " doesn't exist" );
                System.exit( 1 );
            }
        }

        //  Create a frame to hold graphics.
        JFrame frame = new JFrame();

        //  Add a FitPlot object so that it can be called upon to
        //  do the drawing.
        frame.getContentPane().add( new FitVoigt( f ) );

        //  Set the frame size.
        int frameWidth = 400;
        int frameHeight = 200;
        frame.setSize( frameWidth, frameHeight );

        //  Make the frame visible.
        frame.setVisible( true );

        //  Application exits when this window is closed.
        frame.addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent evt ) {
                System.exit( 1 );
            }
        });
    }

    /**
     *  Plot a spectrum.
     *
     *  @param file  Text file with spectrum coordinates as X Y pairs
     *               separated by spaces.
     *
     */
    FitVoigt( File file ) throws IOException {

        //  Get a BufferedReader to read the file line-byte-line.
        //  Avoid StreamTokenizer as this doesn't deal with floating
        //  point very well.
        FileInputStream f = new FileInputStream( file );
        BufferedReader r = new BufferedReader( new InputStreamReader( f ) );

        //  Storage of all values go into two vectors.
        Vector xVec = new Vector();
        Vector yVec = new Vector();

        //  Read file input until end of file occurs.
        String sRaw = null;
        String sClean = null;
        Double x;
        Double y;
        int nlines = 0;
        try {
            while ( ( sRaw = r.readLine() ) != null ) {

                //  Skip comment lines.
                if ( sRaw.charAt(0) != '!' ) {
                    StringTokenizer st = new StringTokenizer( sRaw );
                    int count = st.countTokens();
                    if ( count == 2 ) {
                        x = new Double( st.nextToken() );
                        y = new Double( st.nextToken() );
                        xVec.add( x );
                        yVec.add( y );
                        nlines++;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //  Create memory needed to store these coordinates.
        xPos = new double[nlines];
        yPos = new double[nlines];
        weights = new double[nlines];

        //  Now copy vector into arrays and record the data range.
        xMin = Double.MAX_VALUE;
        xMax = Double.MIN_VALUE;
        yMin = Double.MAX_VALUE;
        yMax = Double.MIN_VALUE;
        for ( int i = 0; i < nlines; i++ ) {
            xPos[i] = ((Double)xVec.get(i)).doubleValue();
            yPos[i] = ((Double)yVec.get(i)).doubleValue();
            if ( yPos[i] < 50.0 ) {
                weights[i] = 0.0;
            } else {
                weights[i] = 1.0;
            }
            xMin = Math.min( xMin, xPos[i] );
            xMax = Math.max( xMax, xPos[i] );
            yMin = Math.min( yMin, yPos[i] );
            yMax = Math.max( yMax, yPos[i] );
        }

        xFit = new double[xPos.length];
        for ( int j = 0; j < xPos.length; j++ ) {
            xFit[j] = xPos[j];
        }

        VoigtFitter fitter = new VoigtFitter( xPos, yPos, weights,
                                              0.5, 4100.0, 4.0, 4.0 );
        double chi = fitter.getChi();
        yFit = fitter.evalYDataArray( xFit );
    }

    /**
     *  Paint method for drawing/redrawing graphics when interface
     *  requires it.
     *
     *  @param g Graphics object to draw.
     *
     */
    public void paintComponent(Graphics g) {
        super.paintComponent( g );
        Graphics2D g2 = (Graphics2D)g;

        //  Scale all graphics to window size. Note Y axes runs
        //  from top-left.
        double xzero = xMin;
        double yzero = yMin;
        double xscale = getWidth() / ( xMax - xMin );
        double yscale = getHeight() / ( yMax - yMin );

        //  Draw a simple polyline connecting all positions.
        GeneralPath path = new GeneralPath();

        //  Data
        double x = ( xPos[0] - xzero ) * xscale;
        double y = ( yPos[0] - yzero ) * yscale;
        y = -y + getHeight();
        path.moveTo( (float) x, (float) y );
        for ( int i = 1; i < xPos.length; i++ ) {
            x = ( xPos[i] - xzero ) * xscale;
            y = ( yPos[i] - yzero ) * yscale;
            y = -y + getHeight();
            path.lineTo( (float) x, (float) y );
        }

        //  Render the path.
        g2.setPaint( Color.blue );
        g2.draw( path );

        //  Fit
        path = new GeneralPath();
        x = ( xFit[0] - xzero ) * xscale;
        y = ( yFit[0] - yzero ) * yscale;
        y = -y + getHeight();
        path.moveTo( (float) x, (float) y );
        for ( int j = 1; j < xPos.length; j++ ) {
            x = ( xFit[j] - xzero ) * xscale;
            y = ( yFit[j] - yzero ) * yscale;
            y = -y + getHeight();
            path.lineTo( (float) x, (float) y );
        }
        
        //  Render the path.
        g2.setPaint( Color.red );
        g2.draw( path );
    }

    /**
     *  Get a rainbow colour.
     *
     *  @param i index of the required colour
     *  @param res number of expected colours in rainbow (i.e. number
     *             you want to choose).
     *
     */
    protected Color getRainbowColour( float i, float res ) 
    {
        float h = i * ( 360.0F / res );
        return Color.getHSBColor( h, 1.0F, 1.0F );
    }
}
