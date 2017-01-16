package uk.ac.starlink.ttools.plot2.task;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.Gang;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.IndicatedRow;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotPlacement;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.NavigationListener;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.SingleGanger;
import uk.ac.starlink.ttools.plot2.ShadeAxis;
import uk.ac.starlink.ttools.plot2.ShadeAxisFactory;
import uk.ac.starlink.ttools.plot2.Slow;
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
 * Graphical component which displays a gang of one or more plots.
 * The plots are in general 'live', and may repaint themselves differently
 * over the lifetime of the component according to user navigation actions,
 * window size, and underlying data, depending on configuration.
 *
 * <p>This class can be used as-is, or as a template.
 *
 * @author   Mark Taylor
 * @since    1 Mar 2013
 */
public class PlotDisplay<P,A> extends JComponent {

    private final Ganger<P,A> ganger_;
    private final DataStore dataStore_;
    private final SurfaceFactory<P,A> surfFact_;
    private final int nz_;
    private final PaperTypeSelector ptSel_;
    private final Compositor compositor_;
    private final boolean surfaceAuxRanging_;
    private final boolean cacheImage_;
    private final List<PointSelectionListener> pslList_;
    private final Executor clickExecutor_;
    private final Zone<P,A>[] zones_;
    private Gang gang_;
    private Decoration navDecoration_;

    /**
     * Name of property that changes when plot Aspects are reset.
     * Can be monitored by use of a PropertyChangeListener.
     * The property object type is an array of aspects,
     * that is of this class's parameterised type A[].
     */
    public static final String ASPECTS_PROPERTY = "Plot2Aspects";

    private static final boolean WITH_SCROLL = true;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.task" );

    /**
     * Constructs a PlotDisplay that shows multiple plot surfaces.
     *
     * @param  ganger  defines plot surface grouping
     * @param  surfFact   surface factory
     * @param  nz   number of plot zones in group
     * @param  zoneContents   plot content by zone (nz-element array)
     * @param  profiles   plot surface profiles by zone (nz-element array)
     * @param  aspects    plot surface aspects by zone (nz-element array)
     * @param  shadeFacts   shader axis factories by zone (nz-element array),
     *                      elements may be null if not required
     * @param  shadeFixRanges  fixed shader ranges by zone (nz-element array)
     *                         elements may be null for auto-range or if no
     *                         shade axis
     * @param  navigator  user gesture navigation controller,
     *                    or null for a non-interactive plot
     * @param  ptSel    paper type selector
     * @param  compositor  compositor for pixel composition
     * @param  dataStore   data storage object
     * @param  caching  plot caching policy
     */
    public PlotDisplay( Ganger<P,A> ganger, SurfaceFactory<P,A> surfFact,
                        int nz, ZoneContent[] zoneContents,
                        P[] profiles, A[] aspects,
                        ShadeAxisFactory[] shadeFacts, Range[] shadeFixRanges,
                        final Navigator<A> navigator,
                        PaperTypeSelector ptSel, Compositor compositor,
                        DataStore dataStore, PlotCaching caching ) {
        ganger_ = ganger;
        surfFact_ = surfFact;
        nz_ = nz;
        zones_ = (Zone<P,A>[]) new Zone<?,?>[ nz_ ];
        profiles = ganger.adjustProfiles( profiles.clone() );
        boolean usePlans = caching.getUsePlans();
        A[] okAspects = ganger.adjustAspects( aspects.clone(), -1 );
        for ( int iz = 0; iz < nz_; iz++ ) {
            zones_[ iz ] = new Zone( zoneContents[ iz ], profiles[ iz ],
                                     shadeFacts[ iz ], shadeFixRanges[ iz ],
                                     okAspects[ iz ], usePlans );
        }
        ptSel_ = ptSel;
        compositor_ = compositor;
        dataStore_ = dataStore;
        surfaceAuxRanging_ = ! caching.getReuseRanges();
        cacheImage_ = caching.getCacheImage();
        pslList_ = new ArrayList<PointSelectionListener>();

        /* Add navigation mouse listeners if required. */
        if ( navigator != null ) {
            new NavigationListener<A>() {
                public int getSurfaceIndex( Point pos ) {
                    return gang_.getNavigationZoneIndex( pos );
                }
                public Surface getSurface( int isurf ) {
                    return isurf >= 0 ? zones_[ isurf ].surface_ : null;
                }
                public Navigator<A> getNavigator( int isurf ) {
                    return navigator;
                }
                public Iterable<double[]> createDataPosIterable( Point pos ) {
                    int iz = getZoneIndex( pos );
                    if ( iz >= 0 ) {
                        PlotLayer[] layers = zones_[ iz ].content_.getLayers();
                        return new PointCloud( SubCloud
                                              .createSubClouds( layers, true ) )
                              .createDataPosIterable( dataStore_ );
                    }
                    else {
                        return null;
                    }
                }
                protected void setAspect( int isurf, A aspect ) {
                    A[] aspects = getAspects().clone();
                    aspects[ isurf ] = aspect;
                    setAspects( ganger_.adjustAspects( aspects, isurf ) );
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
                final int iz = getZoneIndex( p );
                if ( iz >= 0 && pslList_.size() > 0 ) {
                    Zone zone = zones_[ iz ];
                    final Surface surface = zone.surface_;
                    final PlotLayer[] layers = zone.content_.getLayers();
                    if ( surface != null && layers.length > 0 &&
                         surface.getPlotBounds().contains( p ) ) {
                        clickExecutor_.execute( new Runnable() {
                            public void run() {
                                final long[] closestRows =
                                    findClosestRows( surface, layers, p );
                                if ( closestRows != null ) {
                                    final PointSelectionEvent evt =
                                        new PointSelectionEvent( PlotDisplay
                                                                .this,
                                                                 p, iz,
                                                                 closestRows );
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
            }
        } );

        /* Executor to handle asynchronous point identification. */
        clickExecutor_ = Executors.newCachedThreadPool( new ThreadFactory() {
            public Thread newThread( Runnable r ) {
                Thread th = new Thread( r, "Point Identifier" );
                th.setDaemon( true );
                return th;
            }
        } );
    }

    /**
     * Constructs a PlotDisplay that shows a single plot surface.
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
     * @param  shadeFixRange  fixed shader range, or null for auto-range
     * @param  navigator  user gesture navigation controller,
     *                    or null for a non-interactive plot
     * @param  ptSel    paper type selector
     * @param  compositor  compositor for pixel composition
     * @param  padding   user requirements for external space
     * @param  dataStore   data storage object
     * @param  caching  plot caching policy
     */
    public PlotDisplay( SurfaceFactory<P,A> surfFact, PlotLayer[] layers,
                        P profile, Icon legend, float[] legPos, String title,
                        A aspect, ShadeAxisFactory shadeFact,
                        Range shadeFixRange, Navigator<A> navigator,
                        PaperTypeSelector ptSel, Compositor compositor,
                        Padding padding, DataStore dataStore,
                        PlotCaching caching ) {
        this( new SingleGanger<P,A>( padding ), surfFact, 1,
              new ZoneContent[] {
                  new ZoneContent( layers, legend, legPos, title )
              },
              PlotUtil.singletonArray( profile ),
              PlotUtil.singletonArray( aspect ),
              new ShadeAxisFactory[] { shadeFact },
              new Range[] { shadeFixRange },
              navigator, ptSel, compositor, dataStore, caching );
    }

    /**
     * Clears the current cached plot image, if any, so that regeneration
     * of the image from the data is forced when the next paint operation
     * is performed; otherwise it may be copied from a cached image.
     * This method is called automatically by <code>invalidate()</code>,
     * but may also be called manually, for instance if the data in the
     * data store may have changed.
     */
    public void clearPlot() {
        for ( Zone zone : zones_ ) {
            zone.surface_ = null;
            zone.icon_ = null;
        }
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
        Rectangle extBox =
            PlotUtil.subtractInsets( new Rectangle( getSize() ), getInsets() );

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
        Gang approxGang = gang != null ? gang : createGang( extBox );

        /* (Re)calculate aux ranges if required. */
        for ( int iz = 0; iz < nz_; iz++ ) {
            Zone<P,A> zone = zones_[ iz ];
            if ( zone.surface_ == null ) {
                ZoneContent content = zone.content_;
                Surface oldApproxSurf = zone.approxSurf_;
                zone.approxSurf_ =
                    surfFact_.createSurface( approxGang.getZonePlotBounds( iz ),
                                             zone.profile_, zone.aspect_ );
                if ( zone.auxRanges_ == null ||
                     ( surfaceAuxRanging_ &&
                       ! zone.approxSurf_.equals( oldApproxSurf ) ) ) {
                    Object[] plans = zone.plans_ == null
                                   ? null
                                   : zone.plans_.toArray();
                    zone.auxRanges_ =
                        getAuxRanges( content.getLayers(), zone.approxSurf_,
                                      zone.shadeFixRange_, zone.shadeFact_,
                                      plans, dataStore_ );
                    Range shadeRange = zone.auxRanges_.get( AuxScale.COLOR );
                    ShadeAxisFactory shadeFact = zone.shadeFact_;
                    zone.shadeAxis_ = shadeRange != null && shadeFact != null
                                    ? shadeFact.createShadeAxis( shadeRange )
                                    : null;
                }
            }
        }

        /* If we don't already have fixed data bounds to use for the actual
         * plot, the current state of the zone array now contains enough
         * information to work them out. */
        if ( gang == null ) {
            gang = createGang( extBox );
        }
        gang_ = gang;

        /* Create plot icons. */
        long cacheStart = System.currentTimeMillis();
        for ( int iz = 0; iz < nz_; iz++ ) {
            Zone<P,A> zone = zones_[ iz ];
            if ( zone.icon_ == null ) {
                ZoneContent content = zone.content_;
                PlotLayer[] layers = content.getLayers();

                /* Work out plot positioning. */
                zone.surface_ =
                    surfFact_.createSurface( gang.getZonePlotBounds( iz ),
                                             zone.profile_, zone.aspect_ );
                Decoration[] decs =
                    PlotPlacement
                   .createPlotDecorations( zone.surface_, content.getLegend(),
                                           content.getLegendPosition(),
                                           content.getTitle(),
                                           zone.shadeAxis_ );
                PlotPlacement placer =
                    new PlotPlacement( extBox, zone.surface_, decs );

                /* Get rendering implementation. */
                LayerOpt[] opts = PaperTypeSelector.getOpts( layers );
                PaperType paperType =
                    ptSel_.getPixelPaperType( opts, compositor_, this );

                /* Create the plot icon. */
                zone.icon_ =
                    PlotUtil.createPlotIcon( placer, layers, zone.auxRanges_,
                                             dataStore_, paperType,
                                             cacheImage_, zone.plans_ );
            }
        }
        PlotUtil.logTime( logger_, "Cache", cacheStart );

        /* Paint the image to this component. */
        long paintStart = System.currentTimeMillis();
        for ( int iz = 0; iz < nz_; iz++ ) {
            Zone zone = zones_[ iz ];
            zone.icon_.paintIcon( this, g, extBox.x, extBox.y );
            if ( ! cacheImage_ ) {
                zone.icon_ = null;
            }
        }
        if ( navDecoration_ != null ) {
            navDecoration_.paintDecoration( g );
        }
        PlotUtil.logTime( logger_, "Paint", paintStart );
    }

    /**
     * Sets the aspects of the plot zones.
     * This triggers a repaint if required.
     * Note this method does not test or adjust the supplied aspects for
     * consistency with the ganger.
     *
     * @param   aspects  per-zone array of required aspects
     */
    public void setAspects( A[] aspects ) {
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
        if ( changed ) {
            repaint();
            firePropertyChange( ASPECTS_PROPERTY, oldAspects, aspects );
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
    private int getZoneIndex( Point pos ) {
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
     * @return   per-layer array of closest dataset row index
     */
    @Slow
    private long[] findClosestRows( Surface surface, PlotLayer[] layers,
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
        return Thread.currentThread().isInterrupted() ? null : closestRows;
    }

    /**
     * Utility method to construct a single-zoned PlotDisplay,
     * with profile, aspect and navigator obtained from a supplied config map.
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
     * @param  padding   user requirements for external space
     * @param  dataStore   data storage object
     * @param  navigable true for an interactive plot
     * @param  caching   plot caching policy
     * @return  new plot component
     */
    @Slow
    public static <P,A> PlotDisplay
            createPlotDisplay( PlotLayer[] layers,
                               SurfaceFactory<P,A> surfFact, ConfigMap config,
                               Icon legend, float[] legPos, String title,
                               ShadeAxisFactory shadeFact,
                               Range shadeFixRange, PaperTypeSelector ptSel,
                               Compositor compositor, Padding padding,
                               DataStore dataStore, boolean navigable,
                               PlotCaching caching ) {

        /* Read profile from config. */
        P profile = surfFact.createProfile( config );

        /* Read ranges from data if necessary. */
        long t0 = System.currentTimeMillis();
        Range[] ranges = surfFact.useRanges( profile, config )
                       ? surfFact.readRanges( profile, layers, dataStore )
                       : null;
        PlotUtil.logTime( logger_, "Range", t0 );

        /* Work out the initial aspect using config. */
        A aspect = surfFact.createAspect( profile, config, ranges );

        /* Construct navigator using config. */
        Navigator<A> navigator = navigable ? surfFact.createNavigator( config )
                                           : null;

        /* Prepare gang configuration; the gang has only a single member. */
        Ganger<P,A> ganger = new SingleGanger<P,A>( padding );
        ZoneContent[] contents = new ZoneContent[] {
            new ZoneContent( layers, legend, legPos, title ),
        };
        A[] aspects = PlotUtil.singletonArray( aspect );
        P[] profiles = PlotUtil.singletonArray( profile );
        ShadeAxisFactory[] shadeFacts = new ShadeAxisFactory[] { shadeFact };
        Range[] shadeFixRanges = new Range[] { shadeFixRange };

        /* Construct and return the component. */
        return new PlotDisplay<P,A>( ganger, surfFact, 1, contents,
                                     profiles, aspects, 
                                     shadeFacts, shadeFixRanges, navigator,
                                     ptSel, compositor, dataStore, caching );
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
     * @param  shadeFixRanges  fixed shader ranges by zone (nz-element array)
     *                         elements may be null for auto-range or if no
     *                         shade axis
     * @param  navigator  user gesture navigation controller,
     *                    or null for a non-interactive plot
     * @param  ptSel    paper type selector
     * @param  compositor  compositor for pixel composition
     * @param  dataStore   data storage object
     * @param  caching   plot caching policy
     *                   on every repaint
     * @return   new plot component
     */
    @Slow
    public static <P,A> PlotDisplay<P,A>
            createGangDisplay( Ganger<P,A> ganger, SurfaceFactory<P,A> surfFact,
                               int nz, ZoneContent[] contents, P[] profiles,
                               ConfigMap[] aspectConfigs,
                               ShadeAxisFactory[] shadeFacts,
                               Range[] shadeFixRanges, Navigator<A> navigator,
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
        PlotUtil.logTime( logger_, "Range", t0 );
 
        /* Construct and return display. */
        return new PlotDisplay<P,A>( ganger, surfFact, nz, contents,
                                     profiles, aspects,
                                     shadeFacts, shadeFixRanges, navigator,
                                     ptSel, compositor, dataStore, caching );
    }

    /**
     * Gathers requested ranging information from data.
     *
     * @param  layers  plot layers
     * @param  surface  plot surface
     * @param  shadeFixRange  fixed shade range limits, if any
     * @param  shadeFact  makes shader axis, or null
     * @param  plans   array of calculated plan objects, or null
     * @param  dataStore  data storage object
     * @return   ranging information
     */
    @Slow
    public static Map<AuxScale,Range> getAuxRanges( PlotLayer[] layers,
                                                    Surface surface,
                                                    Range shadeFixRange,
                                                    ShadeAxisFactory shadeFact,
                                                    Object[] plans,
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
        plans = plans == null ? new Object[ 0 ] : plans;
        Map<AuxScale,Range> auxDataRanges =
            AuxScale.calculateAuxRanges( calcScales, layers, surface, plans,
                                         dataStore );
        PlotUtil.logTime( logger_, "AuxRange", start );

        /* Combine all the gathered information to acquire actual
         * data ranges for the plot. */
        return AuxScale.getClippedRanges( scales, auxDataRanges, auxFixRanges,
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
        final Range shadeFixRange_;
        final Set<Object> plans_;
        A aspect_;
        Map<AuxScale,Range> auxRanges_;
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
         * @param  shadeFixRange  fixed range for shader axis, or null
         * @param  initialAspect   aspect for initial display
         * @param  usePlans  if true, store plotting plans for reuse
         */
        Zone( ZoneContent content, P profile, ShadeAxisFactory shadeFact,
              Range shadeFixRange, A initialAspect, boolean usePlans ) {
            content_ = content;
            profile_ = profile;
            shadeFact_ = shadeFact;
            shadeFixRange_ = shadeFixRange;
            aspect_ = initialAspect;
            plans_ = usePlans ? new HashSet<Object>() : null;
        }
    }
}
