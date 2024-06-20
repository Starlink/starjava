package uk.ac.starlink.dpac;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import gaia.cu9.tools.parallax.DistanceEstimator;
import gaia.cu9.tools.parallax.datamodel.DistanceEstimation;
import gaia.cu9.tools.parallax.datamodel.StarVariables;
import uk.ac.starlink.table.CalcStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowData;
import uk.ac.starlink.table.StarTable;

public class Cu9DistanceTable extends CalcStarTable<DistanceEstimation> {

    private final StarTable base_;
    private final StarReader reader_;
    private final DistanceEstimator estimator_;

    public Cu9DistanceTable( StarTable base, StarReader reader,
                             final DistanceEstimator estimator ) {
        super( base, createColumns() );
        base_ = base;
        reader_ = reader;
        estimator_ = estimator;
    }

    public DistanceEstimation createCalculation( RowData rdata )
            throws IOException {
        return createEstimation( rdata.getRow() );
    }

    public DistanceEstimation createCalculation( long lrow )
            throws IOException {
        return createEstimation( base_.getRow( lrow ) );
    }

    private DistanceEstimation createEstimation( Object[] baseRow ) {
        return estimator_.estimate( reader_.getStarVariables( baseRow ) );
    }

    private static Col<DistanceEstimation,?>[] createColumns() {
        List<EstimateCol> list = new ArrayList<EstimateCol>();
        list.add( new EstimateCol( "best_dist" ) {
            public double getDoubleValue( DistanceEstimation est ) {
                return est.getBestDistance();
            }
        } );
        list.add( new EstimateCol( "best_mod" ) {
            public double getDoubleValue( DistanceEstimation est ) {
                return est.getBestModulus();
            }
        } );
        list.add( new EstimateCol( "dist_lo" ) {
            public double getDoubleValue( DistanceEstimation est ) {
                return est.getDistanceInterval()[ 0 ];
            }
        } );
        list.add( new EstimateCol( "dist_hi" ) {
            public double getDoubleValue( DistanceEstimation est ) {
                return est.getDistanceInterval()[ 1 ];
            }
        } );
        list.add( new EstimateCol( "mod_lo" ) {
            public double getDoubleValue( DistanceEstimation est ) {
                return est.getModulusInterval()[ 0 ];
            }
        } );
        list.add( new EstimateCol( "mod_hi" ) {
            public double getDoubleValue( DistanceEstimation est ) {
                return est.getModulusInterval()[ 1 ];
            }
        } );
        list.add( new EstimateCol( "dist_median" ) {
            public double getDoubleValue( DistanceEstimation est ) {
                return Double.NaN;
            }
        } );
        @SuppressWarnings("unchecked")
        Col<DistanceEstimation,?>[] cols =
            (Col<DistanceEstimation,?>[]) list.toArray( new Col<?,?>[ 0 ] );
        return cols;
    }

    private static abstract class EstimateCol
                                  implements Col<DistanceEstimation,Double> {
        final ColumnInfo info_;
        EstimateCol( String name ) {
            info_ = new ColumnInfo( name, Double.class, null );
            info_.setUnitString( "pc" );  // not kpc
        }
        public ColumnInfo getInfo() {
            return info_;
        }
        public Double getValue( DistanceEstimation est ) {
            return Double.valueOf( getDoubleValue( est ) * 1000 );
        }
        abstract double getDoubleValue( DistanceEstimation est );
    }
}
