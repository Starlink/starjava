/**
 * Test PlotConfigFrame by creating an instance for interaction.
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;

import uk.ac.starlink.ast.FitsChan;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.ast.gui.PlotConfiguration;
import uk.ac.starlink.ast.gui.PlotConfigurator;
import uk.ac.starlink.ast.gui.PlotController;

import uk.ac.starlink.ast.gui.GraphicsHints;
import uk.ac.starlink.ast.gui.GraphicsHintsControls;

import uk.ac.starlink.ast.gui.GraphicsEdges;
import uk.ac.starlink.ast.gui.GraphicsEdgesControls;

import uk.ac.starlink.ast.gui.ColourStore;
import uk.ac.starlink.ast.gui.ComponentColourControls;


public class TestPlotConfigFrame 
    extends JPanel 
    implements PlotController
{
    /**
     * Create a test instance.
     */
    public static void main( String args[] )
    {
        TestPlotConfigFrame configFrame = new TestPlotConfigFrame();
        JFrame frame = new JFrame();
        frame.getContentPane().add( configFrame );
        frame.setTitle( "AST Plot" );
        frame.setSize( new Dimension( 350, 350 ) );
        frame.setVisible( true );
        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        
        // Exit when window is closed.
        WindowListener closer = new WindowAdapter() {	
                public void windowClosing( WindowEvent e ) {
                    System.exit( 1 );
                }
            };
        frame.addWindowListener( closer );
    }

    //  Create an instance.
    public TestPlotConfigFrame()
    {
        createFrameSet();
        createConfig();
        setVisible( true );
        updatePlot();
    }

    //  Create useful FrameSet, all-sky projection.
    protected FrameSet frameSet = null;
    protected void createFrameSet()
    {
        FitsChan channel = new FitsChan();
        channel.putFits( "NAXIS   =                    2 /", false );
        channel.putFits( "NAXIS1  =                  300 /", false );
        channel.putFits( "NAXIS2  =                  300 /", false );
        channel.putFits( "CTYPE1  = 'GLON-ZEA'           /", false );
        channel.putFits( "CTYPE2  = 'GLAT-ZEA'           /", false );
        channel.putFits( "CRVAL1  =           -149.56866 /", false );
        channel.putFits( "CRVAL2  =           -19.758201 /", false );
        channel.putFits( "CRPIX1  =              150.500 /", false );
        channel.putFits( "CRPIX2  =              150.500 /", false );
        channel.putFits( "CDELT1  =             -1.20000 /", false );
        channel.putFits( "CDELT2  =              1.20000 /", false );
        channel.putFits( "CROTA1  =              0.00000 /", false );
        channel.setCard( 1 );
        frameSet = (FrameSet) channel.read();
        if ( frameSet == null ) {
            System.out.println( "Failed to read FitsChan" );
            System.exit( 1 );
        }
    }


    //  Configuration instances, plus extras
    protected PlotConfigurator plotConfigurator = null;
    protected PlotConfiguration plotConfiguration = new PlotConfiguration();
    protected GraphicsHints graphicsHints = new GraphicsHints();
    protected GraphicsEdges graphicsEdges = new GraphicsEdges();
    protected ColourStore backgroundColour = new ColourStore( "background" );
    
    //  Create the configuration window.
    protected void createConfig()
    {
        //  Add extra models to PlotConfiguration.
        plotConfiguration.add( graphicsHints );
        plotConfiguration.add( graphicsEdges );
        plotConfiguration.add( backgroundColour );

        plotConfigurator = new PlotConfigurator( "Grid Overlay Configuration",
                                                 this, 
                                                 plotConfiguration, "astgui", "test.xml" );

        //  Add extra controls to PlotConfigurator.
        plotConfigurator.addExtraControls
            ( new GraphicsHintsControls( graphicsHints ), true );
        plotConfigurator.addExtraControls
            ( new GraphicsEdgesControls( graphicsEdges ), true );

        ComponentColourControls colourPanel = 
            new ComponentColourControls( this, backgroundColour,
                                         "Plot Background",
                                         "Background", "Colour:" );
        plotConfigurator.addExtraControls( colourPanel, true );

        plotConfigurator.setVisible( true );
    }


    //  Paint the component.
    public void paintComponent( Graphics g )
    {
        super.paintComponent( g );
        Graphics2D g2d = (Graphics2D) g;
        graphicsHints.applyRenderingHints( g2d );
        astPlot.paint( g2d );
    }

    protected Plot astPlot = null;
    public void doPlot()
    {
        double dw = 300.0;
        double dh = 300.0;
        double xo = 0.0;
        double yo = 0.0;

        //  Reserve some of the graphics area for annotations etc.
        double xleft = graphicsEdges.getXLeft() * dw;
        double xright = graphicsEdges.getXRight() * dw;

        double ytop = graphicsEdges.getYTop() * dh;
        double ybottom = graphicsEdges.getYBottom() * dh;
        xo += xleft;
        yo += ytop;
        dw -= xright;
        dh -= ybottom;
        Rectangle graphRect = new Rectangle( (int)xo, (int)yo, (int)dw, 
                                             (int)dh );

        double[] basebox = new double[4];
        basebox[0] = 0.0;
        basebox[1] = 300.0;
        basebox[2] = 300.0;
        basebox[3] = 0.0;

        //  Now create the astPlot.
        astPlot = new Plot( frameSet, graphRect, basebox );

        String options = plotConfiguration.getAst();
        System.out.println( "Options = " + options );
        astPlot.set( options );
        astPlot.grid();
    }

    public void updatePlot()
    {
        //  (re)Create the plot and draw it.
        if ( astPlot != null ) astPlot.clear();
        doPlot();
        repaint();
    }

    public void setPlotColour( Color colour )
    {
        setBackground( colour );
    }

    public Color getPlotColour()
    {
        return getBackground();
    }

    public Frame getPlotCurrentFrame()
    {
        return (Frame) frameSet;
    }
}
