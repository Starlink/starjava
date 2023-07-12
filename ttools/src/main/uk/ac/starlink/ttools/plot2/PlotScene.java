package uk.ac.starlink.ttools.plot2;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.CoordSequence;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.Gang;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.IndicatedRow;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.PlotCaching;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotPlacement;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.SingleGanger;
import uk.ac.starlink.ttools.plot2.ShadeAxis;
import uk.ac.starlink.ttools.plot2.ShadeAxisFactory;
import uk.ac.starlink.ttools.plot2.Slow;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.SubCloud;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.ZoneContent;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.paper.Compositor;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;

/**
 * Contains the state of a plot, which can be painted to a graphics context.
 * The plot's aspect and dimensions can be changed, which means it
 * can form the basis of a 'live' plot.
 *
 * @author   Mark Taylor
 * @since    5 Dec 2019
 */
public class PlotScene<P,A> {

    private final Ganger<P,A> ganger_;
    private final SurfaceFactory<P,A> surfFact_;
    private final int nz_;
    private final PaperTypeSelector ptSel_;
    private final Compositor compositor_;
    private final boolean surfaceAuxRanging_;
    private final boolean cacheImage_;
    private final Zone<P,A>[] zones_;
    private Gang gang_;

    private static final boolean WITH_SCROLL = true;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.task" );

    /**
     * Constructs a PlotScene containing multiple plot surfaces.
     *
     * @param  ganger  defines plot surface grouping
     * @param  surfFact   surface factory
     * @param  nz   number of plot zones in group
     * @param  zoneContents   plot content by zone (nz-element array)
     * @param  profiles   plot surface profiles by zone (nz-element array)
     * @param  aspects    plot surface aspects by zone (nz-element array)
     * @param  shadeFacts   shader axis factories by zone (nz-element array),
     *                      elements may be null if not required
     * @param  shadeFixSpans  fixed shader ranges by zone (nz-element array)
     *                        elements may be null for auto-range or if no
     *                        shade axis
     * @param  ptSel    paper type selector
     * @param  compositor  compositor for pixel composition
     * @param  caching  plot caching policy
     */
    public PlotScene( Ganger<P,A> ganger, SurfaceFactory<P,A> surfFact, int nz,
                      ZoneContent[] zoneContents, P[] profiles, A[] aspects,
                      ShadeAxisFactory[] shadeFacts, Span[] shadeFixSpans,
                      PaperTypeSelector ptSel, Compositor compositor,
                      PlotCaching caching ) {
        ganger_ = ganger;
        surfFact_ = surfFact;
        nz_ = nz;
        @SuppressWarnings("unchecked")
        Zone<P,A>[] zs = (Zone<P,A>[]) new Zone<?,?>[ nz_ ];
        zones_ = zs;
        profiles = ganger.adjustProfiles( profiles.clone() );
        boolean usePlans = caching.getUsePlans();
        A[] okAspects = ganger.adjustAspects( aspects.clone(), -1 );
        for ( int iz = 0; iz < nz_; iz++ ) {
            zones_[ iz ] = new Zone<P,A>( zoneContents[ iz ], profiles[ iz ],
                                          shadeFacts[ iz ], shadeFixSpans[ iz ],
                                          okAspects[ iz ], usePlans );
        }
        ptSel_ = ptSel;
        compositor_ = compositor;
        surfaceAuxRanging_ = ! caching.getReuseRanges();
        cacheImage_ = caching.getCacheImage();
    }

    /**
     * Constructs a PlotScene containing a single plot surface.
     *
     * @param  surfFact   surface factory
     * @param   layers   plot layers to be painted
     * @param   profile  surface profile
     * @param   legend   legend icon if required, or null
     * @param   legPos  legend position if intenal legend is required;
     *                  2-element (x,y) array, each element in range 0-1
     * @param   title   title text, or null
     * @param  aspect    plot surface aspect
     * @param  shadeFact   shader axis factory, or null if not required
     * @param  shadeFixSpan  fixed shader span, or null for auto-range
     * @param  ptSel    paper type selector
     * @param  compositor  compositor for pixel composition
     * @param  padding   user requirements for external space
     * @param  caching  plot caching policy
     */
    public PlotScene( SurfaceFactory<P,A> surfFact, PlotLayer[] layers,
                      P profile, Icon legend, float[] legPos, String title,
                      A aspect, ShadeAxisFactory shadeFact, Span shadeFixSpan,
                      PaperTypeSelector ptSel, Compositor compositor,
                      Padding padding, PlotCaching caching ) {
        this( new SingleGanger<P,A>( padding ), surfFact, 1,
              new ZoneContent[] {
                  new ZoneContent( layers, legend, legPos, title )
              },
              PlotUtil.singletonArray( profile ),
              PlotUtil.singletonArray( aspect ),
              new ShadeAxisFactory[] { shadeFact },
              new Span[] { shadeFixSpan },
              ptSel, compositor, caching );
    }

    /**
     * Returns the Ganger used by this scene.
     *
     * @return  ganger
     */
    public Ganger<P,A> getGanger() {
        return ganger_;
    }

    /**
     * Returns the current plot gang.
     *
     * @return  gang
     */
    public Gang getGang() {
        return gang_;
    }

    /**
     * Returns the current zone content for a given zone.
     *
     * @param   iz  zone index
     * @return  zone content
     */
    public ZoneContent getZoneContent( int iz ) {
        return zones_[ iz ].content_;
    }

    /**
     * Clears the current cached plot image, if any, so that regeneration
     * of the image from the data is forced when the next paint operation
     * is performed; otherwise it may be copied from a cached image.
     */
    public void clearPlot() {
        for ( Zone<P,A> zone : zones_ ) {
            zone.surface_ = null;
            zone.icon_ = null;
        }
    }

    /**
     * Ensures that the plot surfaces are ready to plot this scene.
     * This method is invoked by {@link #paintScene}, but may be
     * invoked directly if only the preparation and not the plotting itself
     * is required.
     *
     * @param  extBounds  external bounds of the plot, including any
     *                    space required for axis labels, legend, padding etc
     * @param  dataStore  data storage object
     */
    @Slow
    public void prepareScene( Rectangle extBounds, DataStore dataStore ) {

        /* Get the data bounds for each plot if we can. 
         * If the surface icon member is non-null, that counts as a flag
         * indicating that plot is up to date; it's the responsibility
         * of the rest of this class to set that member null if the
         * surface may no longer be correct. */
        Rectangle[] dataBoxes = new Rectangle[ nz_ ];
        boolean gotSurfs = true;
        for ( int iz = 0; iz < nz_ && gotSurfs; iz++ ) {
            Surface surf = zones_[ iz ].surface_;
            if ( surf != null ) {
                dataBoxes[ iz ] = surf.getPlotBounds();
            }
            else {
                zones_[ iz ].icon_ = null;
                gotSurfs = false;
            }
        }
        Gang gang = gotSurfs ? ganger_.createGang( dataBoxes ) : null;

        /* Acquire nominal plot bounds that are good enough for working
         * out aux data ranges. */
        Gang approxGang = gang != null ? gang : createGang( extBounds );

        /* (Re)calculate aux ranges if required. */
        long rangeStart = System.currentTimeMillis();
        for ( int iz = 0; iz < nz_; iz++ ) {
            Zone<P,A> zone = zones_[ iz ];
            if ( zone.surface_ == null ) {
                ZoneContent content = zone.content_;
                Surface oldApproxSurf = zone.approxSurf_;
                zone.approxSurf_ =
                    surfFact_.createSurface( approxGang.getZonePlotBounds( iz ),
                                             zone.profile_, zone.aspect_ );
                if ( zone.auxSpans_ == null ||
                     ( surfaceAuxRanging_ &&
                       ! zone.approxSurf_.equals( oldApproxSurf ) ) ) {
                    Object[] plans = zone.plans_ == null
                                   ? null
                                   : zone.plans_.toArray();
                    zone.auxSpans_ =
                        getAuxSpans( content.getLayers(), zone.approxSurf_,
                                     zone.shadeFixSpan_, zone.shadeFact_,
                                     plans, dataStore );
                    Span shadeSpan = zone.auxSpans_.get( AuxScale.COLOR );
                    ShadeAxisFactory shadeFact = zone.shadeFact_;
                    zone.shadeAxis_ = shadeSpan != null && shadeFact != null
                                    ? shadeFact.createShadeAxis( shadeSpan )
                                    : null;
                }
            }
        }
        PlotUtil.logTimeFromStart( logger_, "Range", rangeStart );

        /* If we don't already have fixed data bounds to use for the actual
         * plot, the current state of the zone array now contains enough
         * information to work them out. */
        if ( gang == null ) {
            gang = createGang( extBounds );
        }
        gang_ = gang;

        /* Calculate final surfaces. */
        for ( int iz = 0; iz < nz_; iz++ ) {
            Zone<P,A> zone = zones_[ iz ];
            if ( zone.surface_ == null ) {
                zone.surface_ =
                    surfFact_.createSurface( gang.getZonePlotBounds( iz ),
                                             zone.profile_, zone.aspect_ );
            }
        }
    }

    /**
     * Paints the contents of this plot to a graphics context.
     *
     * @param  g   graphics context
     * @param  extBounds  external bounds of the plot, including any
     *                    space required for axis labels, legend, padding etc
     * @param  dataStore  data storage object
     */
    @Slow
    public void paintScene( Graphics g, Rectangle extBounds,
                            DataStore dataStore ) {
        prepareScene( extBounds, dataStore );

        /* Create plot icons. */
        long planStart = System.currentTimeMillis();
        for ( int iz = 0; iz < nz_; iz++ ) {
            Zone<P,A> zone = zones_[ iz ];
            if ( zone.icon_ == null ) {

                /* Work out plot positioning. */
                ZoneContent content = zone.content_;
                PlotLayer[] layers = content.getLayers();
                Decoration[] decs =
                    PlotPlacement
                   .createPlotDecorations( zone.surface_, WITH_SCROLL,
                                           content.getLegend(),
                                           content.getLegendPosition(),
                                           content.getTitle(),
                                           zone.shadeAxis_ );
                PlotPlacement placer =
                    new PlotPlacement( extBounds, zone.surface_, decs );

                /* Get rendering implementation. */
                LayerOpt[] opts = PaperTypeSelector.getOpts( layers );
                PaperType paperType =
                    ptSel_.getPixelPaperType( opts, compositor_ );

                /* Create the plot icon. */
                zone.icon_ =
                    PlotUtil.createPlotIcon( placer, layers, zone.auxSpans_,
                                             dataStore, paperType,
                                             cacheImage_, zone.plans_ );
            }
        }
        PlotUtil.logTimeFromStart( logger_, "Plan", planStart );

        /* Use a null component for painting the icons.
         * I don't think the component is used anywhere, so this should be OK.
         * It would be possible to require a component as a parameter,
         * but it would confuse the semantics a bit, and in some cases
         * (headless) it's not going to be available, so for now
         * leave it out altogether. */
        Component component = null;

        /* Paint the plot icons to the graphics context. */
        long paintStart = System.currentTimeMillis();
        for ( int iz = 0; iz < nz_; iz++ ) {                     
            Zone<P,A> zone = zones_[ iz ];
            zone.icon_.paintIcon( component, g, extBounds.x, extBounds.y );
            if ( ! cacheImage_ ) {
                zone.icon_ = null;
            }
        }
        PlotUtil.logTimeFromStart( logger_, "Paint", paintStart );
    }

    /**
     * Sets the aspects of the plot zones.
     * Note this method does not test or adjust the supplied aspects for
     * consistency with the ganger.
     *
     * @param   aspects  per-zone array of required aspects
     * @return  true iff the call resulted in a material change of the scene
     *               (requiring a repaint)
     */
    public boolean setAspects( A[] aspects ) {
        A[] oldAspects = aspects.clone();
        Arrays.fill( oldAspects, null );
        boolean changed = false;
        for ( int iz = 0; iz < nz_; iz++ ) {
            Zone<P,A> zone = zones_[ iz ];
            oldAspects[ iz ] = zone.aspect_;
            A aspect = aspects[ iz ];

            /* Note aspects do not sport @Equality, so this isn't a very
             * useful test. */
            if ( aspect != null && ! aspect.equals( zone.aspect_ ) ) {
                zone.aspect_ = aspect;
                zone.surface_ = null;
                zone.icon_ = null;
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Returns the most recently set aspects.
     *
     * @return   per-zone array of current aspects
     */
    public A[] getAspects() {
        A[] aspects = PlotUtil.createAspectArray( surfFact_, nz_ );
        for ( int iz = 0; iz < nz_; iz++ ) {
            aspects[ iz ] = zones_[ iz ].aspect_;
        }
        return aspects;
    }

    /**
     * Returns the current plot surfaces.
     * They will have been generated by this display's SurfaceFactory.
     * Elements may be null if they are not currently up to date
     * (plot is in process of being repainted).
     *
     * @return  per-zone surface array
     */
    public Surface[] getSurfaces() {
        Surface[] surfs = new Surface[ nz_ ];
        for ( int iz = 0; iz < nz_; iz++ ) {
            surfs[ iz ] = zones_[ iz ].surface_;
        }
        return surfs;
    }

    /**
     * Returns the index of the zone in whose data bounds a given point lies.
     *
     * @param   pos   graphics position
     * @return    index of zone containing pos, or -1 if none
     */
    public int getZoneIndex( Point pos ) {
        for ( int iz = 0; iz < nz_; iz++ ) {
            Surface surf = zones_[ iz ].surface_;
            if ( surf != null && surf.getPlotBounds().contains( pos ) ) {
                return iz;
            }
        }
        return -1;
    }

    /**
     * Returns a surface gang based on the current state of this object.
     *
     * @param   extBounds  external bounds of plot
     *                     (includes space for axis labels etc)
     */
    private Gang createGang( Rectangle extBounds ) {
        ZoneContent[] contents = new ZoneContent[ nz_ ];
        A[] aspects = PlotUtil.createAspectArray( surfFact_, nz_ );
        P[] profiles = PlotUtil.createProfileArray( surfFact_, nz_ );
        ShadeAxis[] shadeAxes = new ShadeAxis[ nz_ ];
        for ( int iz = 0; iz < nz_; iz++ ) {
            Zone<P,A> zone = zones_[ iz ];
            contents[ iz ] = zone.content_;
            profiles[ iz ] = zone.profile_;
            aspects[ iz ] = zone.aspect_;
            shadeAxes[ iz ] = zone.shadeAxis_;
        }
        return ganger_.createGang( extBounds, surfFact_, nz_, contents,
                                   profiles, aspects, shadeAxes, WITH_SCROLL );
    }

    /**
     * Assembles and returns a list of row indexes that are plotted close to
     * a given graphics position.
     * May return null, if the thread is interrupted, or possibly under
     * other circumstances.
     *
     * @param  surface   plot surface on which layers are plotted
     * @param  layers   layers plotted
     * @param  point   graphics position to which the selection event refers
     * @param  dataStore  data storage object
     * @return   per-layer array of closest dataset row objects
     */
    @Slow
    public IndicatedRow[] findClosestRows( Surface surface, PlotLayer[] layers,
                                           Point point, DataStore dataStore ) {

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
        int nl = layers.length;
        SubCloud[][] layerClouds = new SubCloud[ nl ][];
        Map<SubCloud,IndicatedRow> cloudMap =
            new LinkedHashMap<SubCloud,IndicatedRow>();
        for ( int il = 0; il < nl; il++ ) {
            SubCloud[] clouds =
                SubCloud
               .createSubClouds( new PlotLayer[] { layers[ il ] }, true );
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
            Supplier<TupleSequence> tupleSupplier =
                () -> dataStore.getTupleSequence( cloud.getDataSpec() );
            cloudMap.put( cloud,
                          PlotUtil.getClosestRow( surface, geom, iPosCoord,
                                                  tupleSupplier,
                                                  dataStore.getTupleRunner(),
                                                  point ) );
        }

        /* Go back to the list of clouds per layer and work out the closest
         * entry for each layer.  At the same time threshold the results,
         * so that ones that are not within a few pixels (NEAR_PIXELS)
         * of the reference point don't count. */
        IndicatedRow[] closestRows = new IndicatedRow[ nl ];
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
            closestRows[ il ] = bestRow;
        }

        /* Return the result, unless we've been interrupted, which would
         * have had the result of terminating the tuple sequences mid-run
         * and hence generating invalid results. */
        return Thread.currentThread().isInterrupted() ? null : closestRows;
    }

    /**
     * Utility method to construct a ganged PlotDisplay, with aspect
     * obtained from a supplied config map.
     * This will perform ranging from data if it is required;
     * in that case, it may take time to execute.
     *
     * @param  ganger  definses plot grouping
     * @param  surfFact   surface factory
     * @param  nz   number of plot zones in group
     * @param  contents   per-zone content information (nz-element array)
     * @param  profiles   per-zone profiles (nz-element array)
     * @param  aspectConfigs   per-zone config map providing entries
     *                         for surf.getAspectKeys (nz-element arrays)
     * @param  shadeFacts   shader axis factorys by zone (nz-element array),
     *                      elements may be null if not required
     * @param  shadeFixSpans   fixed shader ranges by zone (nz-element array)
     *                         elements may be null for auto-range or if no
     *                         shade axis
     * @param  ptSel    paper type selector
     * @param  compositor  compositor for pixel composition
     * @param  dataStore   data storage object
     * @param  caching   plot caching policy
     *                   on every repaint
     * @return   new plot component
     */
    @Slow
    public static <P,A> PlotScene<P,A>
            createGangScene( Ganger<P,A> ganger, SurfaceFactory<P,A> surfFact,
                             int nz, ZoneContent[] contents, P[] profiles,
                             ConfigMap[] aspectConfigs,
                             ShadeAxisFactory[] shadeFacts,
                             Span[] shadeFixSpans,
                             PaperTypeSelector ptSel, Compositor compositor,
                             DataStore dataStore, PlotCaching caching ) {

        /* Determine aspects.  This may or may not require reading the ranges
         * from the data (slow).  */
        A[] aspects = PlotUtil.createAspectArray( surfFact, nz );
        long t0 = System.currentTimeMillis();
        for ( int iz = 0; iz < nz; iz++ ) {
            P profile = profiles[ iz ];
            ConfigMap config = aspectConfigs[ iz ];
            Range[] ranges = surfFact.useRanges( profile, config )
                           ? surfFact.readRanges( profile,
                                                  contents[ iz ].getLayers(),
                                                  dataStore )
                           : null;
            aspects[ iz ] = surfFact.createAspect( profile, config, ranges );
        }
        PlotUtil.logTimeFromStart( logger_, "Range", t0 );
 
        /* Construct and return display. */
        return new PlotScene<P,A>( ganger, surfFact, nz, contents,
                                   profiles, aspects, shadeFacts, shadeFixSpans,
                                   ptSel, compositor, caching );
    }

    /**
     * Gathers requested ranging information from data.
     *
     * @param  layers  plot layers
     * @param  surface  plot surface
     * @param  shadeFixSpan   fixed shade range limits, if any
     * @param  shadeFact  makes shader axis, or null
     * @param  plans   array of calculated plan objects, or null
     * @param  dataStore  data storage object
     * @return   ranging information
     */
    @Slow
    public static Map<AuxScale,Span> getAuxSpans( PlotLayer[] layers,
                                                  Surface surface,
                                                  Span shadeFixSpan,
                                                  ShadeAxisFactory shadeFact,
                                                  Object[] plans,
                                                  DataStore dataStore ) {

        /* Work out what ranges have been requested by plot layers. */
        AuxScale[] scales = AuxScale.getAuxScales( layers );

        /* Add any known fixed range values. */
        Map<AuxScale,Span> auxFixSpans = new HashMap<AuxScale,Span>();
        if ( shadeFixSpan != null ) {
            auxFixSpans.put( AuxScale.COLOR, shadeFixSpan );
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
            AuxScale.getMissingScales( layers, new HashMap<AuxScale,Span>(),
                                       auxFixSpans );

        /* Calculate the ranges from the data. */
        long start = System.currentTimeMillis();
        plans = plans == null ? new Object[ 0 ] : plans;
        Map<AuxScale,Span> auxDataSpans =
            AuxScale.calculateAuxSpans( calcScales, layers, surface, plans,
                                        dataStore );
        PlotUtil.logTimeFromStart( logger_, "AuxRange", start );

        /* Combine all the gathered information to acquire actual
         * data ranges for the plot. */
        return AuxScale.getClippedSpans( scales, auxDataSpans, auxFixSpans,
                                         auxSubranges, auxLogFlags );
    }

    /**
     * Aggregates per-zone information required for plotting.
     * Some of the members are mutable.
     */
    private static class Zone<P,A> {
        final ZoneContent content_;
        final P profile_;
        final ShadeAxisFactory shadeFact_;
        final Span shadeFixSpan_;
        final Set<Object> plans_;
        A aspect_;
        Map<AuxScale,Span> auxSpans_;
        Surface approxSurf_;
        ShadeAxis shadeAxis_;

        /** If non-null, indicates surface is currently up to date. */
        Surface surface_;

        /** If non-null, indicates icon is currently up to date. */
        Icon icon_;

        /**
         * Constructor.
         *
         * @param  content   zone content
         * @param  profile   zone profile
         * @param  shadeFact  shade axis factory, or null
         * @param  shadeFixSpan   fixed range for shader axis, or null
         * @param  initialAspect   aspect for initial display
         * @param  usePlans  if true, store plotting plans for reuse
         */
        Zone( ZoneContent content, P profile, ShadeAxisFactory shadeFact,
              Span shadeFixSpan, A initialAspect, boolean usePlans ) {
            content_ = content;
            profile_ = profile;
            shadeFact_ = shadeFact;
            shadeFixSpan_ = shadeFixSpan;
            aspect_ = initialAspect;
            plans_ = usePlans ? new HashSet<Object>() : null;
        }
    }
}
