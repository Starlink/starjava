package uk.ac.starlink.treeview;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JPanel;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Plot;

/**
 * Draws an AST Plot which will paint itself to fill the actual size
 * of the component.  The aspect ratio will be retained, and the
 * plot will not shrink in either dimension to below a given threshold
 * (otherwise it gets too small and possibly gives an AstException),
 * but otherwise it will check its component size any time it is
 * drawn and reconfigure itself to fit.
 * 
 * @author   Mark Taylor (Starlink)
 * @see    uk.ac.starlink.ast.Plot
 */
public class ScalingPlot extends JPanel {

    private final FrameSet wcs;
    private final double[] lower;
    private final double[] upper;
    private final double[] bbox;
    private final double gridAspect;
    private final int lgap;
    private final int rgap;
    private final int tgap;
    private final int bgap;
    private Plot plot;
    private Dimension lastSize;

    /**
     * Constructs a new plot window given a FrameSet and the bounds of
     * of the base frame corners.
     *
     * @param   wcs  the WCS FrameSet on which to base the AST Plot
     * @param   lower  a 2-d array giving the x,y coordinates of the bottom 
     *          left hand corner in the base frame of <tt>wcs</tt>
     * @param   upper  a 2-d array giving the x,y coordinates of the top right
     *          hand corner in the base frame of <tt>wcs</tt>
     * @throws  IllegalArgumentException  if <tt>lower</tt> or <tt>upper</tt>
     *          or the base frame of <tt>wcs</tt> do not represent
     *          a 2-d frame
     */
    public ScalingPlot( FrameSet wcs, double[] lower, double[] upper ) {
        if ( lower.length != 2 || upper.length != 2 ||
             wcs.getFrame( FrameSet.AST__BASE ).getNaxes() != 2 ) {
            throw new IllegalArgumentException( "Not a 2d WCS" );
        }
        this.wcs = (FrameSet) wcs.copy();
        this.lower = (double[]) lower.clone();
        this.upper = (double[]) upper.clone();
        bbox = new double[] { lower[ 0 ], lower[ 1 ],
                              upper[ 0 ], upper[ 1 ] };
        gridAspect = ( upper[ 1 ] - lower[ 1 ] )
                   / ( upper[ 0 ] - lower[ 0 ] );
        int[] pixelBounds = getPixelBounds();
        lgap = pixelBounds[ 0 ];
        rgap = pixelBounds[ 1 ];
        tgap = pixelBounds[ 2 ];
        bgap = pixelBounds[ 3 ];
    }

    /**
     * Configures the plot prior to painting it.  This implementation
     * does the following:
     * <pre>
     *     plot.setGrid(true);
     *     plot.clear();
     *     plot.grid();
     * </pre>
     * Subclasses may override this method to change any required
     * plot attributes prior to plotting.
     *
     * @param  plot  the Plot object to configure
     */
    protected void configurePlot( Plot plot ) {
        plot.setGrid( true );
        plot.clear();
        plot.grid();
    }

    /**
     * Returns the minimum gaps in pixels which are left in the window 
     * round the outside of the plot.
     *
     * @return  a 4-element array of pixel gaps in the order left, right,
     *          top, bottom
     */
    protected int[] getPixelBounds() {
        return new int[] {
            70, // lgap
            30, // rgap
            40, // tgap
            60, // bgap
        };
    }

    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        Dimension size = getSize();
        if ( size.width < lgap + rgap + 150 ) {
            size.width = lgap + rgap + 150;
        }
        if ( size.height < tgap + bgap + 150 ) {
            size.height = tgap + bgap + 150;
        }
        if ( ! size.equals( lastSize ) ) {
            lastSize = size;
            makeNewPlot();
        }
        plot.paint( g );
    }

    /**
     * Causes the {@link #configurePlot} method to be called and the 
     * plot repainted (unless it hasn't been painted for the first time yet).
     */
    public void refreshPlot() {
        if ( plot != null ) {
            lastSize = null;
            repaint();
        }
    }

    private void makeNewPlot() {
        Dimension size = getSize();
        Dimension plotWindow = new Dimension( size );
        plotWindow.width -= ( lgap + rgap );
        plotWindow.height -= ( tgap + bgap );
        double windowAspect =
            (double) plotWindow.height / (double) plotWindow.width;
        Dimension plotSize = new Dimension();
        if ( gridAspect < windowAspect ) {
            plotSize.width = plotWindow.width;
            plotSize.height =
                (int) Math.round( plotSize.width * gridAspect );
        }
        else {
            plotSize.height = plotWindow.height;
            plotSize.width =
                (int) Math.round( plotSize.height / gridAspect );
        }
        Rectangle position = new Rectangle( plotSize );
        position.x = lgap +
            ( size.width - ( lgap + plotSize.width + rgap ) ) / 2;
        position.y = tgap +
            ( size.height - ( tgap + plotSize.height + bgap ) ) / 2;
        plot = new Plot( wcs, position, bbox );
        configurePlot( plot );
        setPreferredSize( plotSize );
    }

}
