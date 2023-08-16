package uk.ac.starlink.ttools.example;

import java.awt.Dimension;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.ttools.plot.Picture;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.PlotCaching;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotScene;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ShadeAxis;
import uk.ac.starlink.ttools.plot2.ShadeAxisKit;
import uk.ac.starlink.ttools.plot2.SingleGangerFactory;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.Trimming;
import uk.ac.starlink.ttools.plot2.ZoneContent;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.paper.Compositor;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;
import uk.ac.starlink.ttools.plot2.task.AbstractPlot2Task;
import uk.ac.starlink.ttools.plot2.task.PlotDisplay;

/**
 * Convenience class for generating plots.
 * This allows you to set up the basic parameters of a plot, and use
 * the same object to construct either a JComponent for live display,
 * or a static Icon, or to export the graphics to a file in a graphic
 * file format.
 *
 * <p>Although this class gives quite a lot of configurability, there
 * are some options it does not provide, for instance related to the
 * details of autoranging plot limits based on the data.
 * To take full advantage of these, you can go back to the lower-level
 * API taking the implementations here as a starting point.
 *
 * @author   Mark Taylor
 * @since    27 Jun 2014
 */
public class PlotGenerator<P,A> {

    private final SurfaceFactory<P,A> surfFact_;
    private final ZoneContent<P,A> content_;
    private final Trimming trimming_;
    private final ShadeAxisKit shadeKit_;
    private final PaperTypeSelector ptSel_;
    private final Compositor compositor_;
    private final DataStore dataStore_;
    private final int xpix_;
    private final int ypix_;
    private final Padding padding_;
 
    /**
     * Constructor.
     *
     * @param  surfFact  surface factory
     * @param  content   layer content of plot
     * @param  trimming  specification of additional decorations
     * @param  shadeKit  specifies shader axis, or null if not required
     * @param  ptSel    paper type selector
     * @param  compositor  compositor for pixel composition
     * @param  dataStore   data storage object
     * @param  xpix    initial horizontal size in pixels
     *                 (may get changed by window resizing)
     * @param  ypix    initial vertical size in pixels
     *                 (may get changed by window resizing)
     * @param  padding   requirements for extent of region outside plot
     *                   data box to contain axis labels etc;
     *                   may be null or parts may be blank;
     *                   those requirements not specified will be
     *                   calculated automatically
     */
    public PlotGenerator( SurfaceFactory<P,A> surfFact,
                          ZoneContent<P,A> content, Trimming trimming,
                          ShadeAxisKit shadeKit, PaperTypeSelector ptSel,
                          Compositor compositor, DataStore dataStore,
                          int xpix, int ypix, Padding padding ) {
        surfFact_ = surfFact;
        content_ = content;
        trimming_ = trimming;
        shadeKit_ = shadeKit;
        ptSel_ = ptSel;
        compositor_ = compositor;
        dataStore_ = dataStore;
        xpix_ = xpix;
        ypix_ = ypix;
        padding_ = padding;
    }

    /**
     * Returns a JComponent containing a live plot.
     *
     * @param surfaceAuxRange  determines whether aux ranges are recalculated
     *                         when the surface changes
     * @param  navigator  user gesture navigation controller,
     *                    or null for a non-interactive plot
     * @param  cacheImage  if true, plot image will be cached where applicable,
     *                   if false it will be regenerated from data
     *                   on every repaint
     * @return  plot display component
     */
    public PlotDisplay<P,A> createPlotDisplay( Navigator<A> navigator,
                                               boolean surfaceAuxRange,
                                               boolean cacheImage ) {
        PlotCaching cachePolicy = new PlotCaching();
        cachePolicy.setReuseRanges( ! surfaceAuxRange );
        cachePolicy.setCacheImage( cacheImage );
        cachePolicy.setUsePlans( true );
        PlotScene<P,A> scene =
            new PlotScene<>( surfFact_, content_, trimming_, shadeKit_,
                             ptSel_, compositor_, padding_,
                             cachePolicy );
        PlotDisplay<P,A> display =
            new PlotDisplay<>( scene, navigator, dataStore_ );
        display.setPreferredSize( new Dimension( xpix_, ypix_ ) );
        return display;
    }

    /**
     * Exports a plot to an output stream in a supported graphics format.
     *
     * @param   exporter  defines a graphics output format
     * @param   out   destination stream;
     *                this method buffers it, but doesn't close it
     */
    public void exportPlot( GraphicExporter exporter, OutputStream out )
            throws IOException {
        boolean forceBitmap = false;
        Picture pic = PlotUtil.toPicture( createIcon( forceBitmap ) );
        OutputStream bufOut = new BufferedOutputStream( out );
        exporter.exportGraphic( pic, bufOut );
        bufOut.flush();
    }

    /**
     * Returns a static icon that can be used to paint the plot.
     * The assumption is that the plot will only be painted once;
     * the image is not cached for repeated painting.
     *
     * @param  forceBitmap  true iff the plot layers should be forced to
     *         a pixel map grid rather than (perhaps) being drawn using
     *         vector graphics; usually not necessary
     * @return  icon to paint plot; it may be painted in a headless context
     */
    public Icon createIcon( boolean forceBitmap ) {
        Ganger<P,A> ganger = SingleGangerFactory.createGanger( padding_ );
        return AbstractPlot2Task
              .createPlotIcon( ganger, surfFact_, 1,
                               PlotUtil.singletonArray( content_ ),
                               new Trimming[] { trimming_ },
                               new ShadeAxisKit[] { shadeKit_ },
                               ptSel_, compositor_, dataStore_,
                               xpix_, ypix_, forceBitmap );
    }
}
