package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Rectangle;
import java.util.Map;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

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
        BinPaper paper = new BinPaper( surface.getPlotBounds() );
        ShapePainter painter =
            create2DPainter( surface, geom, auxRanges, paper.getPaperType() );
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        while( tseq.next() ) {
            painter.paintPoint( tseq, null, paper );
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
     * Accepts glyphs and users their pixel iterators to build a
     * 2-d histogram.
     */
    private static class BinPaper extends GlyphPaper {
        final Rectangle bounds_;
        final Gridder gridder_;
        final int xoff_;
        final int yoff_;
        final int[] counts_;
        long pointCount_;

        /**
         * Constructor.
         *
         * @param  bounds  plot bounds
         */
        BinPaper( Rectangle bounds ) {
            super( bounds );
            bounds_ = new Rectangle( bounds );
            gridder_ = new Gridder( bounds.width, bounds.height );
            xoff_ = bounds_.x;
            yoff_ = bounds_.y;
            counts_ = new int[ gridder_.getLength() ];
        }

        public void glyphPixels( Pixer pixer ) {
            while ( pixer.next() ) {
                int px = pixer.getX();
                int py = pixer.getY();
                assert bounds_.contains( px, py );
                counts_[ gridder_.getIndex( px - xoff_, py - yoff_ ) ]++;
            }
            pointCount_++;
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
