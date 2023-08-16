package uk.ac.starlink.ttools.plot2.task;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.CoordSequence;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.IndicatedRow;
import uk.ac.starlink.ttools.plot2.NavigationListener;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.PlotCaching;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotScene;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.ShadeAxisFactory;
import uk.ac.starlink.ttools.plot2.ShadeAxisKit;
import uk.ac.starlink.ttools.plot2.SingleGangerFactory;
import uk.ac.starlink.ttools.plot2.Slow;
import uk.ac.starlink.ttools.plot2.SubCloud;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.Trimming;
import uk.ac.starlink.ttools.plot2.ZoneContent;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.paper.Compositor;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;

/**
 * Graphical component which displays a gang of one or more plots.
 * The plots are in general 'live', and may repaint themselves differently
 * over the lifetime of the component according to user navigation actions,
 * window size, and underlying data, depending on configuration.
 *
 * <p><strong>Note:</strong> The paintComponent method of this class
 * performs time-consuming operations, so it will tie up the Event Dispatch
 * Thread.  It is therefore not suitable as it stands for use as part of
 * a general-purpose GUI application.
 *
 * <p>This class can be used as-is, or as a template.
 *
 * @author   Mark Taylor
 * @since    1 Mar 2013
 */
public class PlotDisplay<P,A> extends JComponent {

    private final PlotScene<P,A> scene_;
    private final DataStore dataStore_;
    private final List<PointSelectionListener> pslList_;
    private final Executor clickExecutor_;
    private Decoration navDecoration_;

    /**
     * Name of property that changes when plot Aspects are reset.
     * Can be monitored by use of a PropertyChangeListener.
     * The property object type is an array of aspects,
     * that is of this class's parameterised type A[].
     */
    public static final String ASPECTS_PROPERTY = "Plot2Aspects";

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.task" );

    /**
     * Constructor.
     *
     * @param  scene   plot scene
     * @param  navigator  user gesture navigation controller,
     *                    or null for a non-interactive plot
     * @param  dataStore  data storage object
     */
    public PlotDisplay( final PlotScene<P,A> scene, Navigator<A> navigator,
                        DataStore dataStore ) {
        scene_ = scene;
        dataStore_ = dataStore;
        pslList_ = new ArrayList<PointSelectionListener>();

        /* Add navigation mouse listeners if required. */
        if ( navigator != null ) {
            new NavigationListener<A>() {
                public int getSurfaceIndex( Point pos ) {
                    return scene.getGang().getNavigationZoneIndex( pos );
                }
                public Surface getSurface( int isurf ) {
                    return isurf >= 0 ? scene.getSurfaces()[ isurf ] : null;
                }
                public Navigator<A> getNavigator( int isurf ) {
                    return navigator;
                }
                public Supplier<CoordSequence>
                        createDataPosSupplier( Point pos ) {
                    int iz = scene.getZoneIndex( pos );
                    if ( iz >= 0 ) {
                        PlotLayer[] layers = scene.getLayers( iz );
                        return new PointCloud( SubCloud
                                              .createSubClouds( layers, true ) )
                              .createDataPosSupplier( dataStore_ );
                    }
                    else {
                        return null;
                    }
                }
                protected void setAspect( int isurf, A aspect ) {
                    A[] oldAspects = scene.getAspects();
                    A[] aspects = oldAspects.clone();
                    aspects[ isurf ] = aspect;
                    if ( scene.setAspects( scene.getGanger()
                                          .adjustAspects( aspects, isurf ) ) ) {
                        repaint();
                        firePropertyChange( ASPECTS_PROPERTY,
                                            oldAspects, aspects );
                    }
                }
                protected void setDecoration( Decoration dec ) {
                    setNavDecoration( dec );
                }
            }.addListeners( this );
        }

        /* Add mouse listener for clicking to identify a point. */
        addMouseListener( new MouseAdapter() {
            @Override
            public void mouseClicked( MouseEvent mEvt ) {
                final Point p = mEvt.getPoint();
                final int iz = scene.getZoneIndex( p );
                if ( iz >= 0 && pslList_.size() > 0 ) {
                    final Surface surface = scene.getSurfaces()[ iz ];
                    final PlotLayer[] layers = scene.getLayers( iz );
                    if ( surface != null && layers.length > 0 &&
                         surface.getPlotBounds().contains( p ) ) {
                        clickExecutor_.execute( () -> {
                            final IndicatedRow[] closestRows =
                                scene.findClosestRows( surface, layers, p,
                                                       dataStore_ );
                            if ( closestRows != null ) {
                                int nc = closestRows.length;
                                long[] lrows = new long[ nc ];
                                for ( int ic = 0; ic < nc; ic++ ) {
                                    lrows[ ic ] = closestRows[ ic ].getIndex();
                                }
                                final PointSelectionEvent pEvt =
                                    new PointSelectionEvent( PlotDisplay.this,
                                                             p, iz, lrows );
                                SwingUtilities.invokeLater( () -> {
                                    for ( PointSelectionListener psl :
                                          pslList_ ) {
                                        psl.pointSelected( pEvt );
                                    }
                                } );
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
     * Returns the PlotScene on which this component is based.
     *
     * @return scene
     */
    public PlotScene<P,A> getScene() {
        return scene_;
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
        scene_.clearPlot();
        super.invalidate();
    }

    @Override
    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        Rectangle extBox =
            PlotUtil.subtractInsets( new Rectangle( getSize() ), getInsets() );
        scene_.paintScene( g, extBox, dataStore_ );
        if ( navDecoration_ != null ) {
            navDecoration_.paintDecoration( g );
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
     * Utility method to construct a single-zoned PlotDisplay,
     * with profile, aspect and navigator obtained from a supplied config map.
     * This will perform ranging from data if it is required;
     * in that case, it may take time to execute.
     *
     * @param  layers   layers constituting plot content
     * @param  surfFact   surface factory
     * @param  config   map containing surface profile, initial aspect
     *                  and navigator configuration
     * @param  trimming  additional decoration specification, or null
     * @param  shadeKit  makes shader axis, or null if not required
     * @param  ptSel    paper type selector
     * @param  compositor  compositor for pixel composition
     * @param  padding   user requirements for external space
     * @param  dataStore   data storage object
     * @param  navigable true for an interactive plot
     * @param  caching   plot caching policy
     * @return  new plot component
     */
    @Slow
    public static <P,A> PlotDisplay<P,A>
            createPlotDisplay( PlotLayer[] layers,
                               SurfaceFactory<P,A> surfFact, ConfigMap config,
                               Trimming trimming, ShadeAxisKit shadeKit,
                               PaperTypeSelector ptSel,
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
        PlotUtil.logTimeFromStart( logger_, "Range", t0 );

        /* Work out the initial aspect using config. */
        A aspect = surfFact.createAspect( profile, config, ranges );

        /* Construct navigator using config. */
        Navigator<A> navigator = navigable ? surfFact.createNavigator( config )
                                           : null;

        /* Prepare gang configuration; the gang has only a single member. */
        Ganger<P,A> ganger = SingleGangerFactory.createGanger( padding );
        ZoneContent<P,A>[] contents =
            PlotUtil
           .singletonArray( new ZoneContent<P,A>( profile, aspect, layers ) );
        Trimming[] trimmings = new Trimming[] { trimming };
        ShadeAxisKit[] shadeKits = new ShadeAxisKit[] { shadeKit };

        /* Construct and return the component. */
        PlotScene<P,A> scene =
            new PlotScene<>( ganger, surfFact, contents, trimmings,
                             shadeKits, ptSel, compositor, caching );
        return new PlotDisplay<>( scene, navigator, dataStore );
    }
}
