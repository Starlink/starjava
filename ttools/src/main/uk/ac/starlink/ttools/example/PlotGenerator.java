package uk.ac.starlink.ttools.example;

import java.awt.Dimension;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.ttools.plot.Picture;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ShadeAxis;
import uk.ac.starlink.ttools.plot2.ShadeAxisFactory;
import uk.ac.starlink.ttools.plot2.SingleGanger;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.ZoneContent;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.paper.Compositor;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;
import uk.ac.starlink.ttools.plot2.task.AbstractPlot2Task;
import uk.ac.starlink.ttools.plot2.task.PlotCaching;
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

    private final PlotLayer[] layers_;
    private final SurfaceFactory<P,A> surfFact_;
    private final P profile_;
    private final A aspect_;
    private final Icon legend_;
    private final float[] legPos_;
    private final String title_;
    private final ShadeAxisFactory shadeFact_;
    private final Range shadeFixRange_;
    private final PaperTypeSelector ptSel_;
    private final Compositor compositor_;
    private final DataStore dataStore_;
    private final int xpix_;
    private final int ypix_;
    private final Padding padding_;
 
    /**
     * Constructor.
     *
     * @param  layers   layers constituting plot content
     * @param  surfFact   surface factory
     * @param  profile   surface profile
     * @param  aspect   initial surface aspect (may get changed by zooming etc)
     * @param  legend   legend icon, or null if none required
     * @param  legPos   2-element array giving x,y fractional legend placement
     *                  position within plot (elements in range 0..1),
     *                  or null for external legend
     * @param  title   plot title, or null if not required
     * @param  shadeFact creates shader axis, or null if not required
     * @param  shadeFixRange  fixed shader range,
     *                        or null for auto-range where required
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
    public PlotGenerator( PlotLayer[] layers,
                          SurfaceFactory<P,A> surfFact, P profile, A aspect,
                          Icon legend, float[] legPos, String title,
                          ShadeAxisFactory shadeFact, Range shadeFixRange,
                          PaperTypeSelector ptSel, Compositor compositor,
                          DataStore dataStore, int xpix, int ypix,
                          Padding padding ) {
        layers_ = layers;
        surfFact_ = surfFact;
        profile_ = profile;
        aspect_ = aspect;
        legend_ = legend;
        legPos_ = legPos;
        title_ = title;
        shadeFact_ = shadeFact;
        shadeFixRange_ = shadeFixRange;
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
        PlotDisplay<P,A> display =
            new PlotDisplay( surfFact_, layers_, profile_, legend_, legPos_,
                             title_, aspect_, shadeFact_, shadeFixRange_,
                             navigator, ptSel_, compositor_, padding_,
                             dataStore_, cachePolicy );
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
        return AbstractPlot2Task
              .createPlotIcon( new SingleGanger<P,A>( padding_ ), surfFact_, 1,
                               new ZoneContent[] {
                                   new ZoneContent( layers_, legend_,
                                                    legPos_, title_ )
                               },
                               PlotUtil.singletonArray( profile_ ),
                               PlotUtil.singletonArray( aspect_ ),
                               new ShadeAxisFactory[] { shadeFact_ },
                               new Range[] { shadeFixRange_ },
                               ptSel_, compositor_, dataStore_,
                               xpix_, ypix_, forceBitmap );
    }
}
