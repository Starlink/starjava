package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Rectangle;
import java.util.Map;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.util.SplitCollector;

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
                                    Map<AuxScale,Span> auxRanges,
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
        BinPaper paper =
            dataStore.getTupleRunner()
           .collectPool( new BinCollector( surface, geom, dataSpec, auxRanges ),
                         () -> dataStore.getTupleSequence( dataSpec ) );

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
     * Collector implementation for accumulating to BinPaper.
     */
    private class BinCollector
            implements SplitCollector<TupleSequence,BinPaper> {

        private final Surface surface_;
        private final DataGeom geom_;
        private final DataSpec dataSpec_;
        private final Map<AuxScale,Span> auxRanges_;

        /**
         * Constructor.
         *
         * @param  surface  plot surface
         * @param  geom   data geom
         * @param  dataSpec  data specification
         * @param  auxRanges   range bounds
         */
        BinCollector( Surface surface, DataGeom geom, DataSpec dataSpec,
                      Map<AuxScale,Span> auxRanges ) {
            surface_ = surface;
            geom_ = geom;
            dataSpec_ = dataSpec;
            auxRanges_ = auxRanges;
        }

        public BinPaper createAccumulator() {
            return new BinPaper( surface_.getPlotBounds() );
        }

        public void accumulate( TupleSequence tseq, BinPaper paper ) {
            GlyphPaper.GlyphPaperType ptype = paper.getPaperType();
            ShapePainter painter =
                  surface_ instanceof CubeSurface
                ? create3DPainter( (CubeSurface) surface_, geom_, dataSpec_,
                                   auxRanges_, ptype )
                : create2DPainter( surface_, geom_, dataSpec_,
                                   auxRanges_, ptype );
            while ( tseq.next() ) {
                painter.paintPoint( tseq, null, paper );
            }
        }

        public BinPaper combine( BinPaper paper1, BinPaper paper2 ) {
            paper1.add( paper2 );
            return paper1;
        }
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

        /**
         * Merges the contents of another BinPaper instance into this one.
         * The effect is as if all the glyphs that have been drawn on
         * the other one have been drawn to this one too.
         *
         * @param  other  other paper instance
         */
        void add( BinPaper other ) {
            pointCount_ += other.pointCount_;
            int n = Math.min( counts_.length, other.counts_.length );
            int[] otherCounts = other.counts_;
            for ( int i = 0; i < n; i++ ) {
                counts_[ i ] += otherCounts[ i ];
            }
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
            return PlotUtil.equals( geom_, geom )
                && dataSpec_.equals( dataSpec )
                && surface_.equals( surface )
                && outliner_.equals( outliner );
        }
    }
}
