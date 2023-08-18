package uk.ac.starlink.ttools.plot2;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
import uk.ac.starlink.ttools.plot2.SingleGangerFactory;
import uk.ac.starlink.ttools.plot2.ShadeAxis;
import uk.ac.starlink.ttools.plot2.ShadeAxisFactory;
import uk.ac.starlink.ttools.plot2.ShadeAxisKit;
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
import uk.ac.starlink.util.Bi;
import uk.ac.starlink.util.Util;

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
    private final Trimming globalTrimming_;
    private final ShadeAxisKit globalShadeKit_;
    private final Set<Object> plans_;
    private Span globalShadeSpan_;
    private ShadeAxis globalShadeAxis_;
    private Gang gang_;

    private static final boolean WITH_SCROLL = true;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.task" );

    /**
     * Constructs a PlotScene containing multiple plot surfaces.
     *
     * <p>The zoneContents array must have a number of entries that
     * matches the zone count of the ganger.
     * The trimmings and shadeKits are supplied as arrays, and in each case
     * may be either a 1- or nzone-element array depending on the Ganger's
     * {@link Ganger#isTrimmingGlobal}/{@link Ganger#isShadingGlobal} flags.
     *
     * @param  ganger  defines plot surface grouping
     * @param  surfFact   surface factory
     * @param  zoneContents   plot content with initial aspect by zone
     *                        (nz-element array)
     * @param  trimmings   plot decoration specification by zone
     *                     (nz- or 1-element array, elements may be null)
     * @param  shadeKits   shader axis kits by zone
     *                     (nz- or 1-element array, elements may be null)
     * @param  ptSel    paper type selector
     * @param  compositor  compositor for pixel composition
     * @param  caching  plot caching policy
     */
    public PlotScene( Ganger<P,A> ganger, SurfaceFactory<P,A> surfFact,
                      ZoneContent<P,A>[] zoneContents, Trimming[] trimmings,
                      ShadeAxisKit[] shadeKits, PaperTypeSelector ptSel,
                      Compositor compositor, PlotCaching caching ) {
        ganger_ = ganger;
        surfFact_ = surfFact;
        nz_ = ganger.getZoneCount();
        boolean isTrimGlobal = ganger.isTrimmingGlobal();
        boolean isShadeGlobal = ganger.isShadingGlobal();
        if ( zoneContents.length != nz_ ) {
            throw new IllegalArgumentException( "zone count mismatch" );
        }
        if ( trimmings.length != ( isTrimGlobal ? 1 : nz_ ) ) {
            throw new IllegalArgumentException( "trimmings count mismatch" );
        }
        if ( shadeKits.length != ( isShadeGlobal ? 1 : nz_ ) ) {
            throw new IllegalArgumentException( "shadings count mismatch" );
        }
        globalTrimming_ = isTrimGlobal ? trimmings[ 0 ] : null;
        globalShadeKit_ = isShadeGlobal ? shadeKits[ 0 ] : null;
        @SuppressWarnings("unchecked")
        Zone<P,A>[] zs = (Zone<P,A>[]) new Zone<?,?>[ nz_ ];
        zones_ = zs;
        P[] initialProfiles = PlotUtil.createProfileArray( surfFact, nz_ );
        A[] initialAspects = PlotUtil.createAspectArray( surfFact, nz_ );
        for ( int iz = 0; iz < nz_; iz++ ) {
            ZoneContent<P,A> content = zoneContents[ iz ];
            initialProfiles[ iz ] = content.getProfile();
            initialAspects[ iz ] = content.getAspect();
        }
        P[] okProfiles = ganger.adjustProfiles( initialProfiles );
        A[] okAspects = ganger.adjustAspects( initialAspects, -1 );
        for ( int iz = 0; iz < nz_; iz++ ) {
            zones_[ iz ] =
                new Zone<P,A>( zoneContents[ iz ].getLayers(), okProfiles[ iz ],
                               isTrimGlobal ? null : trimmings[ iz ],
                               isShadeGlobal ? null : shadeKits[ iz ],
                               okAspects[ iz ] );
        }
        ptSel_ = ptSel;
        compositor_ = compositor;
        surfaceAuxRanging_ = ! caching.getReuseRanges();
        cacheImage_ = caching.getCacheImage();
        plans_ = caching.getUsePlans() ? new HashSet<Object>() : null;
    }

    /**
     * Constructs a PlotScene containing a single plot surface.
     *
     * @param  surfFact   surface factory
     * @param  content  plot content with initial aspect
     * @param  trimming    specification of additional decoration
     * @param  shadeKit   shader axis specifier, or null if not required
     * @param  ptSel    paper type selector
     * @param  compositor  compositor for pixel composition
     * @param  padding   user requirements for external space
     * @param  caching  plot caching policy
     */
    public PlotScene( SurfaceFactory<P,A> surfFact, ZoneContent<P,A> content,
                      Trimming trimming, ShadeAxisKit shadeKit,
                      PaperTypeSelector ptSel, Compositor compositor,
                      Padding padding, PlotCaching caching ) {
        this( SingleGangerFactory.createGanger( padding ), surfFact,
              PlotUtil.singletonArray( content ),
              new Trimming[] { trimming }, new ShadeAxisKit[] { shadeKit },
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
     * Returns the plot layers contained in a given zone.
     *
     * @param   iz  zone index
     * @return  plot layers
     */
    public PlotLayer[] getLayers( int iz ) {
        return zones_[ iz ].layers_;
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
        boolean surfChanged = false;
        Object[] plans = plans_ == null ? null : plans_.toArray();
        for ( int iz = 0; iz < nz_; iz++ ) {
            Zone<P,A> zone = zones_[ iz ];
            if ( zone.surface_ == null ) {
                Surface oldApproxSurf = zone.approxSurf_;
                zone.approxSurf_ =
                    surfFact_.createSurface( approxGang.getZonePlotBounds( iz ),
                                             zone.profile_, zone.aspect_ );
                boolean zoneSurfChanged =
                    ! zone.approxSurf_.equals( oldApproxSurf );
                surfChanged = surfChanged || zoneSurfChanged;
                if ( zone.auxSpans_ == null ||
                     ( surfaceAuxRanging_ && zoneSurfChanged ) ) {

                    /* Calculate non-shading spans, which is always per-zone. */
                    zone.auxSpans_ =
                        calculateNonShadeSpans( zone.layers_, zone.approxSurf_,
                                                plans, dataStore );

                    /* If shading span is per-zone, calculate it now. */
                    ShadeAxisKit shadeKit = zone.shadeKit_;
                    if ( shadeKit != null ) {
                        ShadeAxisFactory shadeFact = shadeKit.getAxisFactory();
                        List<Bi<Surface,PlotLayer>> surfLayers =
                            AuxScale.pairSurfaceLayers( zone.approxSurf_,
                                                        zone.layers_ );
                        Span shadeSpan =
                            calculateShadeSpan( surfLayers, shadeKit,
                                                plans, dataStore );
                        if ( shadeSpan != null ) {
                            zone.auxSpans_.put( AuxScale.COLOR, shadeSpan );
                        }
                        zone.shadeAxis_ = shadeSpan != null && shadeFact != null
                                        ? shadeFact.createShadeAxis( shadeSpan )
                                        : null;
                    }
                }
            }
        }

        /* If shading span is global, calculate and record it here. */
        if ( globalShadeKit_ != null ) {
            if ( globalShadeSpan_ == null ||
                 ( surfaceAuxRanging_ && surfChanged ) ) {
                List<Bi<Surface,PlotLayer>> surfLayers =
                    Arrays.stream( zones_ )
                   .flatMap( z -> AuxScale
                                 .pairSurfaceLayers( z.approxSurf_, z.layers_ )
                                 .stream() )
                   .collect( Collectors.toList() );
                globalShadeSpan_ =
                    calculateShadeSpan( surfLayers, globalShadeKit_, plans,
                                        dataStore );
                if ( globalShadeSpan_ != null ) {
                    for ( Zone<P,A> zone : zones_ ) {
                        zone.auxSpans_.put( AuxScale.COLOR, globalShadeSpan_ );
                    }
                }
                ShadeAxisFactory shadeFact = globalShadeKit_.getAxisFactory();
                globalShadeAxis_ = globalShadeSpan_ != null && shadeFact != null
                                 ? shadeFact.createShadeAxis( globalShadeSpan_ )
                                 : null;
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
                PlotLayer[] layers = zone.layers_;
                PlotFrame frame =
                    PlotFrame.createPlotFrame( zone.surface_, WITH_SCROLL );
                Decoration[] decs =
                    PlotPlacement.createPlotDecorations( frame, zone.trimming_,
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
                                             cacheImage_, plans_ );
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

        /* Paint global decorations if applicable. */
        if ( globalTrimming_ != null || globalShadeAxis_ != null ) {
            Surface[] surfs = Arrays.stream( zones_ )
                             .map( z -> z.surface_ )
                             .toArray( n -> new Surface[ n ] );
            PlotFrame extFrame =
                PlotFrame.createPlotFrame( surfs, WITH_SCROLL, extBounds );
            Decoration[] decs =
                PlotPlacement.createPlotDecorations( extFrame, globalTrimming_,
                                                     globalShadeAxis_ );
            for ( Decoration dec : decs ) {
                dec.paintDecoration( g );
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
        ZoneContent<P,A>[] contents =
            Arrays.stream( zones_ )
           .map( z -> new ZoneContent<P,A>( z.profile_, z.aspect_, z.layers_ ) )
           .toArray( n -> PlotUtil.createZoneContentArray( surfFact_, n ) );
        Trimming[] trimmings = ganger_.isTrimmingGlobal()
                             ? new Trimming[] { globalTrimming_ }
                             : Arrays.stream( zones_ )
                                     .map( z -> z.trimming_ )
                                     .toArray( n -> new Trimming[ n ] );
        ShadeAxis[] shadeAxes = ganger_.isShadingGlobal()
                              ? new ShadeAxis[] { globalShadeAxis_ }
                              : Arrays.stream( zones_ )
                                      .map( z -> z.shadeAxis_ )
                                      .toArray( n -> new ShadeAxis[ n ] );
        return ganger_.createGang( extBounds, surfFact_, contents,
                                   trimmings, shadeAxes, WITH_SCROLL );
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
     * <p>The layerArrays, profiles and aspectConfigs arrays must have the
     * same length as the ganger zone count.
     * The trimmings and shadeKits are supplied as arrays, and in each case
     * may be either a 1- or nzone-element array depending on the Ganger's
     * {@link Ganger#isTrimmingGlobal}/{@link Ganger#isShadingGlobal} flags.
     *
     * @param  ganger  defines plot grouping
     * @param  surfFact   surface factory
     * @param  layerArrays   per-zone layer arrays (nz-element array)
     * @param  profiles   per-zone profiles (nz-element array)
     * @param  aspectConfigs   per-zone config map providing entries
     *                         for surf.getAspectKeys (nz-element arrays)
     * @param  trimmings   plot decoration specification by zone
     *                     (nz- or 1-element array,elements may be null)
     * @param  shadeKits   shader axis specifiers by zone
     *                     (nz- or 1-element array, elements may be null)
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
                             PlotLayer[][] layerArrays, P[] profiles,
                             ConfigMap[] aspectConfigs, Trimming[] trimmings,
                             ShadeAxisKit[] shadeKits,
                             PaperTypeSelector ptSel, Compositor compositor,
                             DataStore dataStore, PlotCaching caching ) {

        /* Determine aspects.  This may or may not require reading the ranges
         * from the data (slow).  */
        int nz = ganger.getZoneCount();
        long t0 = System.currentTimeMillis();
        ZoneContent<P,A>[] contents =
            PlotUtil.createZoneContentArray( surfFact, nz );
        for ( int iz = 0; iz < nz; iz++ ) {
            PlotLayer[] layers = layerArrays[ iz ];
            P profile = profiles[ iz ];
            ConfigMap config = aspectConfigs[ iz ];
            Range[] ranges = surfFact.useRanges( profile, config )
                           ? surfFact.readRanges( profile, layers, dataStore )
                           : null;
            A aspect = surfFact.createAspect( profile, config, ranges );
            contents[ iz ] = new ZoneContent<P,A>( profile, aspect, layers );
        }
        PlotUtil.logTimeFromStart( logger_, "Range", t0 );
 
        /* Construct and return display. */
        return new PlotScene<P,A>( ganger, surfFact, contents, trimmings,
                                   shadeKits, ptSel, compositor, caching );
    }

    /**
     * Gathers requested ranging information from data,
     * excluding the AuxScale.COLOR item (the aux shading axis).
     * The result is a map with an entry for every AuxScale required
     * by any of the submitted layers, apart from AuxScale.COLOR,
     * which must be calculated separately.
     *
     * @param  layers  plot layers
     * @param  surface  surface on which layers will be plotted
     * @param  plans   array of calculated plan objects, or null
     * @param  dataStore  data storage object
     * @return   ranging information
     * @see    #calculateShadeSpan
     * @see    AuxScale#COLOR
     */
    @Slow
    public static Map<AuxScale,Span>
            calculateNonShadeSpans( PlotLayer[] layers, Surface surface,
                                    Object[] plans, DataStore dataStore ) {

        /* Work out what ranges have been requested by plot layers,
         * but remove AuxScale.COLOR, which is treated specially. */
        AuxScale[] scales = Arrays.stream( AuxScale.getAuxScales( layers ) )
                           .filter( s -> ! Util.equals( s, AuxScale.COLOR ) )
                           .toArray( n -> new AuxScale[ n ] );

        /* Calculate the ranges from the data. */
        plans = plans == null ? new Object[ 0 ] : plans;
        long start = System.currentTimeMillis();
        List<Bi<Surface,PlotLayer>> surfLayers =
            AuxScale.pairSurfaceLayers( surface, layers );
        return AuxScale.calculateAuxSpans( scales, surfLayers, plans,
                                           dataStore );
    }

    /**
     * Gathers ranging information for the aux shading axis from data.
     * The result is a Span relating to the AuxScale.COLOR scale.
     *
     * @param   surfLayers  list of paired (surface,layer) items corresponding
     *                      to the plot that will be performed
     * @param  shadeKit   specifies shader axis, or null
     * @param  plans   array of calculated plan objects, or null
     * @param  dataStore  data storage object
     * @return   AuxScale.COLOR ranging information,
     *           or null if none is required by the surfLayers
     * @see    AuxScale#COLOR
     */
    @Slow
    public static Span
            calculateShadeSpan( List<Bi<Surface,PlotLayer>> surfLayers,
                                ShadeAxisKit shadeKit,
                                Object[] plans, DataStore dataStore ) {

        /* Find out if we need to calculate the AuxScale.COLOR span. */
        PlotLayer[] layers = surfLayers.stream()
                            .map( Bi::getItem2 )
                            .toArray( n -> new PlotLayer[ n ] );
        Map<AuxScale,Span> dataSpans = Collections.emptyMap();
        Map<AuxScale,Span> auxFixSpans = new HashMap<AuxScale,Span>();
        Span shadeFixSpan = shadeKit == null ? null : shadeKit.getFixSpan();
        if ( shadeFixSpan != null ) {
            auxFixSpans.put( AuxScale.COLOR, shadeFixSpan );
        }
        boolean requireColor =
            AuxScale.getMissingScales( layers, dataSpans, auxFixSpans )
           .contains( AuxScale.COLOR );

        /* Do the calculation if required. */
        if ( requireColor ) {
            long start = System.currentTimeMillis();
            plans = plans == null ? new Object[ 0 ] : plans;
            Span shadeDataSpan =
                AuxScale.calculateAuxSpans( new AuxScale[] { AuxScale.COLOR },
                                            surfLayers, plans, dataStore )
               .get( AuxScale.COLOR );
            ShadeAxisFactory shadeFact = shadeKit == null
                                       ? null
                                       : shadeKit.getAxisFactory();
            Subrange shadeSubrange = shadeKit == null
                                   ? null
                                   : shadeKit.getSubrange();
            boolean shadeLog = shadeFact != null && shadeFact.isLog();
            PlotUtil.logTimeFromStart( logger_, "AuxRange.COLOR", start );
            return AuxScale.clipSpan( shadeDataSpan, shadeFixSpan,
                                      shadeSubrange, shadeLog );
        }
        else {
            return shadeFixSpan;
        }
    }

    /**
     * Aggregates per-zone information required for plotting.
     * Some of the members are mutable.
     */
    private static class Zone<P,A> {
        final PlotLayer[] layers_;
        final P profile_;
        final Trimming trimming_;
        final ShadeAxisKit shadeKit_;
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
         * @param  layers   plot layers
         * @param  profile   zone profile
         * @param  trimming   specification for decorations
         * @param  shadeKit  shade axis specifier, or null
         * @param  initialAspect   aspect for initial display
         */
        Zone( PlotLayer[] layers, P profile, Trimming trimming,
              ShadeAxisKit shadeKit, A initialAspect ) {
            layers_ = layers;
            profile_ = profile;
            trimming_ = trimming;
            shadeKit_ = shadeKit;
            aspect_ = initialAspect;
        }
    }
}
