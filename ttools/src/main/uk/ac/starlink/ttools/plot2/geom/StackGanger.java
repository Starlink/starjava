package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.function.Supplier;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.Gang;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.PlotPlacement;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ShadeAxis;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.Surround;
import uk.ac.starlink.ttools.plot2.Trimming;
import uk.ac.starlink.ttools.plot2.ZoneContent;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.IntegerConfigKey;

/**
 * Ganger implementation for a vertically stacked gang of plots,
 * all sharing the same horizontal axis.
 *
 * @author   Mark Taylor
 * @since    25 Jan 2016
 */
public abstract class StackGanger<P,A> implements Ganger<P,A> {

    private final String[] zoneNames_;
    private final int nz_;
    private final boolean isUp_;
    private final Padding padding_;
    private final int zoneGap_;

    /** Config key for vertical gap between zones. */
    public static final ConfigKey<Integer> ZONEGAP_KEY = createZoneGapKey();

    private static final int PAD = PlotPlacement.PAD;

    /**
     * Constructor.
     *
     * @param  zoneNames   one string identifier for each required zone
     * @param  isUp  true if zones are ordered upwards on the graphics plane,
     *               false if they go down
     * @param  padding  defines user preferences, if any, for space
     *                  reserved outside the whole gang
     * @param  zoneGap  vertical gap between zones in gang
     */
    protected StackGanger( String[] zoneNames, boolean isUp, Padding padding,
                           int zoneGap ) {
        zoneNames_ = zoneNames == null || zoneNames.length == 0
                   ? new String[] { "" }
                   : new LinkedHashSet<String>( Arrays.asList( zoneNames ) )
                    .toArray( new String[ 0 ] );
        nz_ = zoneNames_.length;
        isUp_ = isUp;
        padding_ = padding;
        zoneGap_ = zoneGap;
    }

    /**
     * Returns the data limits of the horizontal axis defined by a given aspect.
     *
     * @param  aspect  surface aspect
     * @return   2-element array giving (min,max) values of data coordinates
     *           on the horizontal axis
     */
    public abstract double[] getXLimits( A aspect );

    /**
     * Modifies an aspect object to give it fixed data limits on the
     * horizontal axis.
     *
     * @param  aspect   input surface aspect
     * @param  xmin    required lower limit on horizontal axis
     * @param  xmax    required upper limit on horizontal axis
     * @return   new aspect resembling input aspect but with supplied
     *           horizontal axis limits
     */
    public abstract A fixXLimits( A aspect, double xmin, double xmax );

    public int getZoneCount() {
        return nz_;
    }

    /**
     * Returns a list of identifiers, one for each zone in gangs
     * produced by this ganger.
     *
     * @return  zone names
     */
    public String[] getZoneNames() {
        return zoneNames_;
    }

    public Gang createGang( Rectangle[] zonePlotBounds ) {
        return new StackGang( zonePlotBounds );
    }

    public Gang createGang( Rectangle gangExtBox,
                            SurfaceFactory<P,A> surfFact,
                            ZoneContent<P,A>[] contents,
                            Trimming[] trimmings, ShadeAxis[] shadeAxes,
                            boolean withScroll ) {

        /* Calculate how much space is required for decorations that
         * do not depend (much) on the size of the plot bounds. */
        ZoneContent<P,A> zc0 = contents.length > 0 ? contents[ 0 ] : null;
        Supplier<Captioner> capSupplier =
              zc0 == null
            ? () -> null
            : new Supplier<Captioner>() {
                  private Captioner captioner_;
                  public Captioner get() {
                      if ( captioner_ == null ) {
                          captioner_ = surfFact.createSurface( gangExtBox,
                                                               zc0.getProfile(),
                                                               zc0.getAspect() )
                                               .getCaptioner();
                      }
                      return captioner_;
                  }
              };
        Surround decSurround = Surround.fromInsets( new Insets( 0, 0, 0, 0 ) );
        for ( int iz = 0; iz < nz_; iz++ ) {
            Surround surround =
                PlotPlacement
               .calculateApproxDecorationSurround( gangExtBox, trimmings[ iz ],
                                                   shadeAxes[ iz ],
                                                   capSupplier );
            decSurround = decSurround.union( surround );
        }

        /* Calculate how much space is required for axis labels. */
        Rectangle decBox =
            PlotUtil.subtractInsets( gangExtBox, decSurround.toInsets() );
        Surround axisSurround =
            calculateAxisSurround( decBox, surfFact, contents, withScroll );

        /* Combine the two to work out the area available for the plots
         * themselves, and create a gang based on that. */
        Surround gangSurround = decSurround.union( axisSurround );
        Insets gangInsets =
            Padding.padInsets( padding_, gangSurround.toInsets() );
        return createGang( PlotUtil.subtractInsets( gangExtBox, gangInsets ) );
    }
 
    public Gang createApproxGang( Rectangle extBounds ) {
        int h = extBounds.height / nz_;
        Rectangle[] boxes = new Rectangle[ nz_ ];
        for ( int iz = 0; iz < nz_; iz++ ) {
            boxes[ iz ] =
               new Rectangle( extBounds.x,
                              extBounds.y + h * ( isUp_ ? nz_ - 1 - iz : iz ),
                              extBounds.width,
                              h );
        }
        return new StackGang( boxes );
    }

    public A[] adjustAspects( A[] aspects, int index ) {
        final double[] xlimits;
        if ( index >= 0 ) {
            xlimits = getXLimits( aspects[ index ] );
        }
        else {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for ( A aspect : aspects ) {
                double[] lims = getXLimits( aspect );
                min = Math.min( min, lims[ 0 ] );
                max = Math.max( max, lims[ 1 ] );
            }
            xlimits = min < max ? new double[] { min, max } : null;
        }
        if ( xlimits != null ) {
            A[] newAspects = aspects.clone();
            for ( int iz = 0; iz < aspects.length; iz++ ) {
                newAspects[ iz ] =
                    fixXLimits( aspects[ iz ], xlimits[ 0 ], xlimits[ 1 ] );
            }
            return newAspects;
        }
        else {
            return aspects;
        }
    }

    public P[] adjustProfiles( P[] profiles ) {
        return profiles;
    }

    /**
     * Creates a gang given the bounds of the cells excluding any
     * space for decorations.
     *
     * @param  gangInBox  bounding box for plot internal bounds only
     * @return   new gang
     */
    private StackGang createGang( Rectangle gangInBox ) {
        int zh = ( gangInBox.height + zoneGap_ ) / nz_;
        int cw = gangInBox.width;
        int xoff = gangInBox.x;
        int yoff = gangInBox.y;
        Rectangle[] boxes = new Rectangle[ nz_ ];
        for ( int iz = 0; iz < nz_; iz++ ) {
            int rx = xoff;
            int ry = yoff + ( isUp_ ? nz_ - 1 - iz : iz ) * zh;
            int rw = cw;
            int rh = zh - zoneGap_;
            boxes[ iz ] = new Rectangle( rx, ry, rw, rh );
        }
        return new StackGang( boxes );
    }

    /**
     * Calculates the space surrounding a gang required to accommodate
     * axis labels.
     *
     * @param  gangExtBox  total area available for all zones and associated
     *                     decorations
     * @param  surfFact   surface factory
     * @param  zoneContents  plot content for each zone
     * @param  withScroll  true if the positioning should work well
     *                     even after some user scrolling
     * @return     required surround
     */
    private Surround
            calculateAxisSurround( Rectangle gangExtBox,
                                   SurfaceFactory<P,A> surfFact,
                                   ZoneContent<P,A>[] zoneContents,
                                   boolean withScroll ) {
        Surround surround =
            Surround.fromInsets( new Insets( PAD, PAD, PAD, PAD ) );
        int cw0 = gangExtBox.width;
        int ch0 = gangExtBox.height / nz_;
        Padding zonePadding = null;
        for ( int iz = 0; iz < nz_; iz++ ) {
            ZoneContent<P,A> content = zoneContents[ iz ];
            Rectangle zoneExtBox = new Rectangle( 0, 0, cw0, ch0 );
            Surround zoneSurround =
                PlotPlacement
               .createPlacement( zoneExtBox, zonePadding, surfFact,
                                 content.getProfile(),
                                 content.getAspect(), withScroll,
                                 (Trimming) null, (ShadeAxis) null )
               .getSurface()
               .getSurround( withScroll );
            surround = surround.union( zoneSurround );
        }
        return surround;
    }

    /**
     * Creates a config key suitable for configuring inter-plot gap.
     *
     * @return  new config key
     */
    private static ConfigKey<Integer> createZoneGapKey() {
        ConfigMeta meta = new ConfigMeta( "cellgap", "Cell Gap" );
        meta.setShortDescription( "Vertical gap between plots" );
        meta.setXmlDescription( new String[] {
            "<p>Gives the number of pixels between individual members",
            "in the stack of plots.",
            "</p>",
        } );
        return IntegerConfigKey.createSpinnerKey( meta, 4, 0, 32 );
    }

    /**
     * Gang implementation for vertical plot stacks.
     */
    private static class StackGang implements Gang {

        private final Rectangle[] zoneBoxes_;
 
        /**
         * Constructor.
         *
         * @param  zoneBoxes   per-zone plot bounds array  
         */
        StackGang( Rectangle[] zoneBoxes ) {
            zoneBoxes_ = zoneBoxes;
        }

        public int getZoneCount() {
            return zoneBoxes_.length;
        }

        public Rectangle getZonePlotBounds( int iz ) {
            return new Rectangle( zoneBoxes_[ iz ] );
        }

        /**
         * The returned zone is the one whose Y range the given point
         * falls within.  If none, the closest by Y is used.
         * So if it's off the top/bottom of the plot area,
         * the top/bottom zone is used.
         */
        public int getNavigationZoneIndex( Point pos ) {
            int y = pos.y;
            int minDist = Integer.MAX_VALUE;
            int izClosest = -1;
            for ( int iz = 0; iz < zoneBoxes_.length; iz++ ) {
                Rectangle box = zoneBoxes_[ iz ];
                int ylo = box.y;
                int yhi = box.y + box.height;
                if ( y >= ylo && y < yhi ) {
                    return iz;
                }
                int ydist = Math.min( Math.abs( y - ylo ),
                                      Math.abs( y - yhi ) );
                if ( ydist < minDist ) {
                    minDist = ydist;
                    izClosest = iz;
                }
            }
            return izClosest;
        }
    }
}
