package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Rectangle;
import javax.swing.Icon;
import java.util.Map;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;

/**
 * Partial Outliner implementation which calculates its bin plan
 * simply by using the <code>create2DPainter</code> method.
 * If no clever shortcut presents itself, this is a reasonable way to do it.
 *
 * @author   Mark Taylor
 * @since    26 Feb 2013
 */
public abstract class PixOutliner implements Outliner {

    public Object calculateBinPlan( Surface surface, DataGeom geom,
                                    Map<AuxScale,Range> auxRanges,
                                    DataStore dataStore, DataSpec dataSpec,
                                    Object[] knownPlans ) {

        /* If one of the presented plans fits the bill, return it. */
        for ( int ip = 0; ip < knownPlans.length; ip++ ) {
            if ( knownPlans[ ip ] instanceof PixBinPlan ) {
                PixBinPlan plan = (PixBinPlan) knownPlans[ ip ];
                if ( plan.matches( geom, dataSpec, surface, this ) ) {
                    return plan;
                }
            }
        }

        /* Otherwise set up a limited PaperType implementation that takes
         * glyphs and turns them into a bit map, and plot the glyphs on it. */
        BinPaperType ptype = new BinPaperType();
        ShapePainter painter =
            create2DPainter( surface, geom, auxRanges, ptype );
        BinPaper paper = new BinPaper( ptype, surface.getPlotBounds() );
        Color color = Color.BLACK;
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        while( tseq.next() ) {
            painter.paintPoint( tseq, color, paper );
        }

        /* Extract the result as a bin plan. */
        return new PixBinPlan( paper.counts_, paper.pointCount_,
                               geom, dataSpec, surface, this );
    }

    public int[] getBinCounts( Object binPlan ) {
        return ((PixBinPlan) binPlan).counts_;
    }

    public long getPointCount( Object binPlan ) {
        return ((PixBinPlan) binPlan).pointCount_;
    }

    /**
     * Partial PaperType implementation that accepts glyphs and uses their
     * pixel iterators to build a 2-d histogram.  It doesn't do all the
     * other things that PaperTypes do, like create an icon or support
     * decals.  That's OK, we're not going to ask it to do those things.
     */
    private static class BinPaperType implements PaperType2D {

        public void placeGlyph( Paper paper, double dx, double dy, Glyph glyph,
                                Color color ) {
            int gx = PlotUtil.ifloor( dx );
            int gy = PlotUtil.ifloor( dy );

            /* Acquire requisite information from paper object. */
            BinPaper binPaper = (BinPaper) paper;
            Rectangle bounds = binPaper.bounds_;
            Gridder gridder = binPaper.gridder_;
            int[] counts = binPaper.counts_;
            int xoff = bounds.x;
            int yoff = bounds.y;

            /* Get the presented glyph's pixels clipped to bounds. */
            Rectangle cbox = new Rectangle( bounds );
            cbox.translate( -gx, -gy );
            Pixer pixer = glyph.createPixer( cbox );

            /* Increment bins per pixel as appropriate. */
            if ( pixer != null ) {
                while ( pixer.next() ) {
                    int px = gx + pixer.getX();
                    int py = gy + pixer.getY();
                    assert bounds.contains( px, py );
                    int ix = px - xoff;
                    int iy = py - yoff;
                    counts[ gridder.getIndex( ix, iy ) ]++;
                }
            }
            binPaper.pointCount_++;
        }

        public boolean isBitmap() {
            return true;
        }

        public Icon createDataIcon( Surface surface, Drawing[] drawings,
                                    Object[] plans, DataStore dataStore,
                                    boolean requireCached ) {
            throw new UnsupportedOperationException();
        }

        public void placeDecal( Paper paper, Decal decal ) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Paper implementation for use with BinPaperType.
     */
    private static class BinPaper implements Paper {
        final PaperType paperType_;
        final Rectangle bounds_;
        final Gridder gridder_;
        final Binner binner_;
        final int[] counts_;
        long pointCount_;
 
        /**
         * Constructor.
         *
         * @param  paperType  paper type
         * @param  bounds  bitmap bounds
         */
        BinPaper( BinPaperType paperType, Rectangle bounds ) {
            paperType_ = paperType;
            bounds_ = new Rectangle( bounds );
            gridder_ = new Gridder( bounds.width, bounds.height );
            binner_ = new Binner( gridder_.getLength() );
            counts_ = new int[ gridder_.getLength() ];
        }

        public PaperType getPaperType() {
            return paperType_;
        }
    }

    /**
     * BinPlan implementation for use with this class.
     */
    private static class PixBinPlan {
        final int[] counts_;
        final long pointCount_;
        final DataGeom geom_;
        final DataSpec dataSpec_;
        final Surface surface_;
        final PixOutliner outliner_;

        /**
         * Constructor.
         *
         * @param  counts  bin values
         * @param  pointCount  number of data positions contributing data
         * @param  geom  coordinate geometry
         * @param  dataSpec   data spec
         * @param  surface  plot surface
         * @param  outliner  marker shape
         */
        PixBinPlan( int[] counts, long pointCount, DataGeom geom,
                    DataSpec dataSpec, Surface surface, PixOutliner outliner ) {
            counts_ = counts;
            pointCount_ = pointCount;
            geom_ = geom;
            dataSpec_ = dataSpec;
            surface_ = surface;
            outliner_ = outliner;
        }

        /**
         * Indicates whether a set of constraints would produce a BinPlan
         * whose payload (counts array) is the same as this one.
         *
         * @param  geom  coordinate geometry
         * @param  dataSpec   data spec
         * @param  surface  plot surface
         * @param  outliner  marker shape
         * @return  true if this plan already has the answer
         */
        boolean matches( DataGeom geom, DataSpec dataSpec, Surface surface,
                         PixOutliner outliner ) {
            return geom_.equals( geom )
                && dataSpec_.equals( dataSpec )
                && surface_.equals( surface )
                && outliner_.equals( outliner );
        }
    }
}
