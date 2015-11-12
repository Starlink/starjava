package uk.ac.starlink.ttools.plot2.task;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.IndicatedRow;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.NavigationListener;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotPlacement;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ShadeAxis;
import uk.ac.starlink.ttools.plot2.ShadeAxisFactory;
import uk.ac.starlink.ttools.plot2.Slow;
import uk.ac.starlink.ttools.plot2.SubCloud;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.paper.Compositor;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;

/**
 * Graphical component which displays a plot.
 * The plot is in general 'live', and may repaint itself differently
 * over its lifetime according to user navigation actions,
 * window size, and underlying data, depending on how it is configured.
 * 
 * <p>This class can be used as-is, or as a template.
 *
 * @author   Mark Taylor
 * @since    1 Mar 2013
 */
public class PlotDisplay<P,A> extends JComponent {

    private final PlotLayer[] layers_;
    private final DataStore dataStore_;
    private final SurfaceFactory<P,A> surfFact_;
    private final P profile_;
    private final Icon legend_;
    private final float[] legPos_;
    private final String title_;
    private final ShadeAxisFactory shadeFact_;
    private final Range shadeFixRange_;
    private final PaperTypeSelector ptSel_;
    private final boolean surfaceAuxRange_;
    private final Compositor compositor_;
    private final boolean caching_;
    private final List<PointSelectionListener> pslList_;
    private final Executor clickExecutor_;
    private Insets dataInsets_;
    private Decoration navDecoration_;
    private Map<AuxScale,Range> auxRanges_;
    private Surface approxSurf_;
    private Surface surface_;
    private A aspect_;
    private Icon icon_;

    /**
     * Name of property that changes when plot Aspect is reset.
     * Can be monitored by use of a PropertyChangeListener.
     * The property object type is an aspect, that is of this class's
     * parameterised type A.
     */
    public static final String ASPECT_PROPERTY = "Plot2Aspect";

    private static final Level REPORT_LEVEL = Level.INFO;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2" );

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
     * @param  title    plot title, or null
     * @param  shadeFact  makes shader axes, or null if not required
     * @param  shadeFixRange  fixed shader range,
     *                        or null for auto-range where required
     * @param  ptSel    paper type selector
     * @param  compositor  compositor for pixel composition
     * @param  dataStore   data storage object
     * @param surfaceAuxRange  determines whether aux ranges are recalculated
     *                         when the surface changes
     * @param  navigator  user gesture navigation controller,
     *                    or null for a non-interactive plot
     * @param  caching   if true, plot image will be cached where applicable,
     *                   if false it will be regenerated from the data
     *                   on every repaint
     */
    public PlotDisplay( PlotLayer[] layers, SurfaceFactory<P,A> surfFact,
                        P profile, A aspect, Icon legend, float[] legPos,
                        String title, ShadeAxisFactory shadeFact,
                        Range shadeFixRange, PaperTypeSelector ptSel,
                        Compositor compositor, DataStore dataStore,
                        boolean surfaceAuxRange, final Navigator<A> navigator,
                        boolean caching ) {
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
        surfaceAuxRange_ = surfaceAuxRange;
        caching_ = caching;
        pslList_ = new ArrayList<PointSelectionListener>();

        /* Add navigation mouse listeners if required. */
        if ( navigator != null ) {
            new NavigationListener<A>() {
                public Surface getSurface() {
                    return surface_;
                }
                public Navigator<A> getNavigator() {
                    return navigator;
                }
                public Iterable<double[]> createDataPosIterable() {
                    return new PointCloud( SubCloud
                                          .createSubClouds( layers_, true ) )
                          .createDataPosIterable( dataStore_ );
                }
                protected void setAspect( A aspect ) {
                    PlotDisplay.this.setAspect( aspect );
                }
                protected void setDecoration( Decoration dec ) {
                    setNavDecoration( dec );
                }
            }.addListeners( this );
        }

        /* Add mouse listener for clicking to identify a point. */
        addMouseListener( new MouseAdapter() {
            @Override
            public void mouseClicked( MouseEvent evt ) {
                final Point p = evt.getPoint();
                final Surface surface = surface_;
                if ( pslList_.size() > 0 &&
                     PlotUtil.getButtonChangedIndex( evt ) == 1 &&
                     surface.getPlotBounds().contains( p ) ) {
                    clickExecutor_.execute( new Runnable() {
                        public void run() {
                            final PointSelectionEvent evt =
                                createClickEvent( surface, p );
                            if ( evt != null ) {
                                SwingUtilities.invokeLater( new Runnable() {
                                    public void run() {
                                        for ( PointSelectionListener psl :
                                              pslList_ ) {
                                            psl.pointSelected( evt );
                                        }
                                    }
                                } );
                            }
                        }
                    } );
                }
            }
        } );
        clickExecutor_ = Executors.newCachedThreadPool( new ThreadFactory() {
            public Thread newThread( Runnable r ) {
                Thread th = new Thread( r, "Point Identifier" );
                th.setDaemon( true );
                return th;
            }
        } );
    }

    /**
     * Clears the current cached plot image, if any, so that regeneration
     * of the image from the data is forced when the next paint operation
     * is performed; otherwise it may be copied from a cached image.
     * This method is called automatically by <code>invalidate()</code>,
     * but may also be called manually, for instance if the data in the
     * data store may have changed.
     *
     * <p>This method has no effect if caching is not in force.
     */
    public void clearPlot() {
        icon_ = null;
    }

    /**
     * Adds a listener which will be notified when the user clicks on
     * the plot region to select a point.
     *
     * @param  psl  listener to add
     */
    public void addPointSelectionListener( PointSelectionListener psl ) {
        pslList_.add( psl );
    }

    /**
     * Removes a previously added point selection listener.
     *
     * @param  psl  listener to remove
     */
    public void removePointSelectionListener( PointSelectionListener psl ) {
        pslList_.remove( psl );
    }

    @Override
    public void invalidate() {
        clearPlot();
        super.invalidate();
    }

    @Override
    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        Rectangle extBounds =
            PlotUtil.subtractInsets( new Rectangle( getSize() ), getInsets() );

        /* If we already have a cached image, it is for the right plot,
         * just draw that.  If not, generate a new one. */
        Icon icon = icon_;
        if ( icon == null ) {

            /* Get the data bounds if we can. */
            Rectangle dataBounds =
                  dataInsets_ != null
                ? PlotUtil.subtractInsets( extBounds, dataInsets_ )
                : null;

            /* Acquire nominal plot bounds that are good enough for working
             * out aux data ranges. */
            Rectangle approxBounds = dataBounds != null ? dataBounds
                                                        : extBounds;

            /* (Re)calculate aux ranges if required. */
            Surface approxSurf =
                surfFact_.createSurface( approxBounds, profile_, aspect_ );
            final Map<AuxScale,Range> auxRanges;
            if ( auxRanges_ != null &&
                 ( ! surfaceAuxRange_ || approxSurf.equals( approxSurf_ ) ) ) {
                auxRanges = auxRanges_;
            }
            else {
                auxRanges = getAuxRanges( layers_, approxSurf, shadeFixRange_,
                                          shadeFact_, dataStore_ );
            }
            auxRanges_ = auxRanges;
            approxSurf_ = approxSurf;

            /* Get aux axis component if applicable. */
            Range shadeRange = auxRanges.get( AuxScale.COLOR );
            ShadeAxis shadeAxis = shadeRange != null && shadeFact_ != null
                                ? shadeFact_.createShadeAxis( shadeRange )
                                : null;

            /* If we don't already have data bounds to use for the actual
             * plot, we have enough information to work them out now. */
            if ( dataBounds == null ) {
                boolean withScroll = true;
                dataBounds =
                    PlotPlacement
                   .calculateDataBounds( extBounds, surfFact_, profile_,
                                         aspect_, withScroll, legend_,
                                         legPos_, title_, shadeAxis );
            }

            /* Work out plot positioning. */
            surface_ = surfFact_.createSurface( dataBounds, profile_, aspect_ );
            Decoration[] decs =
                PlotPlacement
               .createPlotDecorations( surface_, legend_, legPos_, title_,
                                       shadeAxis );
            PlotPlacement placer =
                new PlotPlacement( extBounds, surface_, decs );

            /* Get rendering implementation. */
            LayerOpt[] opts = PaperTypeSelector.getOpts( layers_ );
            PaperType paperType =
                ptSel_.getPixelPaperType( opts, compositor_, this );

            /* Perform the plot to a possibly cached image. */
            long start = System.currentTimeMillis();
            icon = createIcon( placer, layers_, auxRanges, dataStore_,
                               paperType, true );
            PlotUtil.logTime( logger_, "Cache", start );
        }
        if ( caching_ ) {
            icon_ = icon;
        }

        /* Paint the image to this component. */
        long start = System.currentTimeMillis();
        icon.paintIcon( this, g, extBounds.x, extBounds.y );
        if ( navDecoration_ != null ) {
            navDecoration_.paintDecoration( g );
        }
        PlotUtil.logTime( logger_, "Paint", start );
    }

    /**
     * Sets the surface aspect.  This triggers a repaint if appropriate.
     *
     * @param  aspect  new aspect
     */
    public void setAspect( A aspect ) {
        if ( aspect != null && ! aspect.equals( aspect_ ) ) {
            A oldAspect = aspect_;
            aspect_ = aspect;
            clearPlot();
            repaint();
            firePropertyChange( ASPECT_PROPERTY, oldAspect, aspect );
        }
    }

    /**
     * Sets the navigation decoration.
     *
     * @param  dec  navigation decoration
     */
    private void setNavDecoration( Decoration dec ) {
        if ( ! PlotUtil.equals( navDecoration_, dec ) ) {
            navDecoration_ = dec;
            repaint();
        }
    }

    /**
     * Returns the most recently set aspect.
     *
     * @return  current aspect
     */
    public A getAspect() {
        return aspect_;
    }

    /**
     * Returns the most recently used plot surface.
     * It will have been generated by this display's SurfaceFactory.
     *
     * @return  current plotting surface 
     */
    public Surface getSurface() {
        return surface_;
    }

    /**
     * Sets the geometry of the region between the external bound
     * of this component (excluding component borders) and the data region
     * of the plot.  This insets region is where axis labels, legend,
     * and other plot decorations are drawn.  If null (the default),
     * the extent of the region is worked out automatically and dynamically
     * on the basis of what labels need to be drawn etc.
     * 
     * @param  dataInsets  geometry of the region outside the actual data plot
     */
    public void setDataInsets( Insets dataInsets ) {
        dataInsets_ = dataInsets;
        clearPlot();
    }

    /**
     * Assembles and returns a PointSelectionEvent given a graphics position.
     * May return null, if the thread is interrupted, or possibly under
     * other circumstances.
     *
     * @param  surface  plot surface representing the state of the plot
     * @param  point   graphics position to which the selection event refers
     */
    @Slow
    private PointSelectionEvent createClickEvent( Surface surface,
                                                  Point point ) {

        /* The donkey work here is done by PlotUtil.getClosestRow.
         * However, we need to do some disentangling in order to work out
         * what tuple sequences to send it.  Sending one for each layer is
         * not quite good enough: for one thing some layers may have multiple
         * positions (e.g. pair links), and for another this would do the
         * same work multiple times if the same position sets (point clouds)
         * are represented in multiple layers, which is quite common.
         * So there is not a 1:1 relationship between point clouds and
         * layers. */

        /* Get a list of point clouds for each layer.  Also store the same
         * clouds as keys of a Map.  Since equality is implemented on
         * SubClouds, and plots often have the same data plotted in
         * different layers, this may may well have fewer entries
         * than the number of layers. */
        int nl = layers_.length;
        SubCloud[][] layerClouds = new SubCloud[ nl ][];
        Map<SubCloud,IndicatedRow> cloudMap =
            new LinkedHashMap<SubCloud,IndicatedRow>();
        for ( int il = 0; il < nl; il++ ) {
            SubCloud[] clouds =
                SubCloud
               .createSubClouds( new PlotLayer[] { layers_[ il ] }, true );
            layerClouds[ il ] = clouds;
            for ( SubCloud cloud : clouds ) {
                cloudMap.put( cloud, null );
            }
        }

        /* For each distinct point cloud that we're dealing with,
         * identify the closest plotted data point to the requested
         * reference position. */
        for ( SubCloud cloud : cloudMap.keySet() ) {
            DataGeom geom = cloud.getDataGeom();
            int iPosCoord = cloud.getPosCoordIndex();
            TupleSequence tseq =
                dataStore_.getTupleSequence( cloud.getDataSpec() );
            cloudMap.put( cloud,
                          PlotUtil.getClosestRow( surface, geom, iPosCoord,
                                                  tseq, point ) );
        }

        /* Go back to the list of clouds per layer and work out the closest
         * entry for each layer.  At the same time threshold the results,
         * so that ones that are not within a few pixels (NEAR_PIXELS)
         * of the reference point don't count. */
        long[] closestRows = new long[ nl ];
        for ( int il = 0; il < nl; il++ ) {
            IndicatedRow bestRow = null;
            for ( SubCloud cloud : layerClouds[ il ] ) {
                IndicatedRow row = cloudMap.get( cloud );
                if ( row != null ) {
                    double dist = row.getDistance();
                    if ( dist <= PlotUtil.NEAR_PIXELS &&
                         ( bestRow == null || dist < bestRow.getDistance() ) ) {
                        bestRow = row;
                    }
                }
            }
            closestRows[ il ] = bestRow == null ? -1 : bestRow.getIndex();
        }

        /* Return the result, unless we've been interrupted, which would
         * have had the result of terminating the tuple sequences mid-run
         * and hence generating invalid results. */
        return Thread.currentThread().isInterrupted()
             ? null
             : new PointSelectionEvent( this, point, closestRows );
    }

    /**
     * Creates a new PlotDisplay, interrogating a supplied ConfigMap object.
     * This will perform ranging from data if it is required;
     * in that case, it may take time to execute.
     *
     * @param  layers   layers constituting plot content
     * @param  surfFact   surface factory
     * @param  config   map containing surface profile, initial aspect
     *                  and navigator configuration
     * @param  legend   legend icon, or null if none required
     * @param  legPos   2-element array giving x,y fractional legend placement
     *                  position within plot (elements in range 0..1),
     *                  or null for external legend
     * @param  title    plot title, or null
     * @param  shadeFact  makes shader axis, or null if not required
     * @param  shadeFixRange  fixed shader range,
     *                        or null for auto-range where required
     * @param  ptSel    paper type selector
     * @param  compositor  compositor for pixel composition
     * @param  dataStore   data storage object
     * @param surfaceAuxRange  determines whether aux ranges are recalculated
     *                         when the surface changes
     * @param  navigable true for an interactive plot
     * @param  caching   if true, plot image will be cached where applicable,
     *                   if false it will be regenerated from the data
     *                   on every repaint
     * @return  new plot component
     */
    @Slow
    public static <P,A> PlotDisplay
            createPlotDisplay( PlotLayer[] layers, SurfaceFactory<P,A> surfFact,
                               ConfigMap config, Icon legend, float[] legPos,
                               String title, ShadeAxisFactory shadeFact,
                               Range shadeFixRange, PaperTypeSelector ptSel,
                               Compositor compositor, DataStore dataStore,
                               boolean surfaceAuxRange, boolean navigable,
                               boolean caching ) {
        P profile = surfFact.createProfile( config );

        /* Read ranges from data if necessary. */
        long t0 = System.currentTimeMillis();
        Range[] ranges = surfFact.useRanges( profile, config )
                       ? surfFact.readRanges( profile, layers, dataStore )
                       : null;
        PlotUtil.logTime( logger_, "Range", t0 );

        /* Work out the initial aspect. */
        A aspect = surfFact.createAspect( profile, config, ranges );

        /* Get a navigator. */
        Navigator<A> navigator = navigable ? surfFact.createNavigator( config )
                                           : null;
     
        /* Create and return the component. */
        return new PlotDisplay<P,A>( layers, surfFact, profile, aspect,
                                     legend, legPos, title, shadeFact,
                                     shadeFixRange, ptSel, compositor,
                                     dataStore, surfaceAuxRange, navigator,
                                     caching );
    }

    /**
     * Creates an icon which will paint the content of this plot.
     *
     * @param  placer  plot placement
     * @param  layers   layers constituting plot content
     * @param  auxRanges  requested range information calculated from data
     * @param  dataStore  data storage object
     * @param  paperType  rendering type
     * @param  cached  whether to cache pixels for future use
     */
    @Slow
    public static Icon createIcon( PlotPlacement placer, PlotLayer[] layers,
                                   Map<AuxScale,Range> auxRanges,
                                   DataStore dataStore, PaperType paperType,
                                   boolean cached ) {
        Surface surface = placer.getSurface();
        int nl = layers.length;
        logger_.info( "Layers: " + nl + ", Paper: " + paperType );
        Drawing[] drawings = new Drawing[ nl ];
        Object[] plans = new Object[ nl ];
        long t1 = System.currentTimeMillis();
        for ( int il = 0; il < nl; il++ ) {
            drawings[ il ] = layers[ il ]
                            .createDrawing( surface, auxRanges, paperType );
            plans[ il ] = drawings[ il ].calculatePlan( plans, dataStore );
        }
        PlotUtil.logTime( logger_, "Plans", t1 );
        Icon dataIcon =
            paperType.createDataIcon( surface, drawings, plans, dataStore,
                                      cached );
        if ( logger_.isLoggable( REPORT_LEVEL ) ) {
            for ( int il = 0; il < nl; il++ ) {
                ReportMap report = drawings[ il ].getReport( plans[ il ] );
                if ( report != null ) {
                    String rtxt = report.toString( false );
                    if ( rtxt.length() > 0 ) {
                        String msg = new StringBuffer()
                            .append( "Layer " )
                            .append( il )
                            .append( ": " )
                            .append( rtxt )
                            .toString();
                        logger_.log( REPORT_LEVEL, msg );
                    }
                }
            }
        }
        return placer.createPlotIcon( dataIcon );
    }

    /**
     * Gathers requested ranging information from data.
     *
     * @param  layers  plot layers
     * @param  surface  plot surface
     * @param  shadeFixRange  fixed shade range limits, if any
     * @param  shadeFact  makes shader axis, or null
     * @param  dataStore  data storage object
     */
    @Slow
    public static Map<AuxScale,Range> getAuxRanges( PlotLayer[] layers,
                                                    Surface surface,
                                                    Range shadeFixRange,
                                                    ShadeAxisFactory shadeFact,
                                                    DataStore dataStore ) {

        /* Work out what ranges have been requested by plot layers. */
        AuxScale[] scales = AuxScale.getAuxScales( layers );

        /* Add any known fixed range values. */
        Map<AuxScale,Range> auxFixRanges = new HashMap<AuxScale,Range>();
        if ( shadeFixRange != null ) {
            auxFixRanges.put( AuxScale.COLOR, shadeFixRange );
        }

        /* Prepare list of ranges known to be logarithmic. */
        Map<AuxScale,Boolean> auxLogFlags = new HashMap<AuxScale,Boolean>();
        if ( shadeFact != null ) {
            auxLogFlags.put( AuxScale.COLOR, shadeFact.isLog() );
        }

        /* We will not be using subranges, so prepare an empty map. */
        Map<AuxScale,Subrange> auxSubranges = new HashMap<AuxScale,Subrange>();

        /* Work out what ranges we need to calculate. */
        AuxScale[] calcScales =
            AuxScale.getMissingScales( scales, new HashMap<AuxScale,Range>(),
                                       auxFixRanges );

        /* Calculate the ranges from the data. */
        long start = System.currentTimeMillis();
        Map<AuxScale,Range> auxDataRanges =
            AuxScale.calculateAuxRanges( calcScales, layers,
                                         surface, dataStore );
        PlotUtil.logTime( logger_, "AuxRange", start );

        /* Combine all the gathered information to acquire actual
         * data ranges for the plot. */
        return AuxScale.getClippedRanges( scales, auxDataRanges, auxFixRanges,
                                          auxSubranges, auxLogFlags );
    }
}
