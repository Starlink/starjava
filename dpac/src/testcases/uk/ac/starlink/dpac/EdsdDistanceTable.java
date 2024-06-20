package uk.ac.starlink.dpac;

import gaia.cu9.tools.parallax.datamodel.StarVariables;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.dpac.math.Edsd;
import uk.ac.starlink.dpac.math.FuncUtils;
import uk.ac.starlink.dpac.math.Function;
import uk.ac.starlink.dpac.math.NumericFunction;
import uk.ac.starlink.table.CalcStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowData;
import uk.ac.starlink.table.StarTable;

public class EdsdDistanceTable
             extends CalcStarTable<EdsdDistanceTable.EdsdResult> {

    private final StarTable base_;
    private final StarReader reader_;
    private final double lkpc_;

    public EdsdDistanceTable( StarTable base, StarReader reader, double lkpc ) {
        super( base, createColumns() );
        base_ = base;
        reader_ = reader;
        lkpc_ = lkpc;
    }

    public EdsdResult createCalculation( RowData rdata )
            throws IOException {
        return createResult( rdata.getRow() );
    }

    public EdsdResult createCalculation( long lrow )
            throws IOException {
        return createResult( base_.getRow( lrow ) );
    }

    public EdsdResult createResult( Object[] baseRow ) {
        StarVariables starvar = reader_.getStarVariables( baseRow );
        Edsd edsd =
            new Edsd( starvar.getVarpi(), starvar.getErrVarpi(), lkpc_ );
        return new EdsdResult( edsd );
    }

    private static Col<EdsdResult,?>[] createColumns() {
        List<EdsdCol> list = new ArrayList<EdsdCol>();
        list.add( new EdsdCol( "best_dist" ) {
            public double getDoubleValue( EdsdResult result ) {
                return result.bestMod_;
            }
        } );
        list.add( new EdsdCol( "dist_lo" ) {
            public double getDoubleValue( EdsdResult result ) {
                return result.p05_;
            }
        } );
        list.add( new EdsdCol( "dist_median" ) {
            public double getDoubleValue( EdsdResult result ) {
                return result.p50_;
            }
        } );
        list.add( new EdsdCol( "dist_hi" ) {
            public double getDoubleValue( EdsdResult result ) {
                return result.p95_;
            }
        } );
        @SuppressWarnings("unchecked")
        Col<EdsdResult,?>[] cols =
            (Col<EdsdResult,?>[]) list.toArray( new Col<?,?>[ 0 ] );
        return cols;
    }

    private static abstract class EdsdCol implements Col<EdsdResult,Double> {
        final ColumnInfo info_;
        EdsdCol( String name ) {
            info_ = new ColumnInfo( name, Double.class, null );
            info_.setUnitString( "pc" );  // not kpc
        }
        public ColumnInfo getInfo() {
            return info_;
        }
        public Double getValue( EdsdResult result ) {
            return Double.valueOf( getDoubleValue( result ) * 1000 );
        }
        abstract double getDoubleValue( EdsdResult result );
    }

    public static class EdsdResult {
        final double bestMod_;
        final double p05_;
        final double p50_;
        final double p95_;
        EdsdResult( Edsd edsd ) {
            bestMod_ = edsd.getBestEstimation();
            NumericFunction ncdf = edsd.calculateCdf( 1e-7 );
            Function scdf = FuncUtils.interpolateQuadratic( ncdf );
            double rmin = 0;
            double rmax = ncdf.getX( ncdf.getCount() - 1 );
            double ytol = 1e-9;
            p05_ = FuncUtils.findValueMonotonic( scdf, rmin, rmax, 0.05, ytol );
            p50_ = FuncUtils.findValueMonotonic( scdf, rmin, rmax, 0.50, ytol );
            p95_ = FuncUtils.findValueMonotonic( scdf, rmin, rmax, 0.95, ytol );
        }
    }
}
