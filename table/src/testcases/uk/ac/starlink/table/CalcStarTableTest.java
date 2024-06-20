package uk.ac.starlink.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CalcStarTableTest extends TableCase {

    public void testValues() throws IOException {
        int npow = 6;
        int nrow = 20;
        StarTable base = createIndexTable( nrow );
        assertTrue( base.isRandom() );
        StarTable tpow = new PowersTable( base, npow );
        assertTrue( tpow.isRandom() );
        checkStarTable( tpow );
        assertEquals( npow, tpow.getColumnCount() );
        assertEquals( nrow, tpow.getRowCount() );
        assertEquals( Double.valueOf( 32 ), tpow.getCell( 2, 5 ) );
    }

    private static class PowerStructure {
        final double[] powers_;
        PowerStructure( int n, double x ) {
            powers_ = new double[ n ];
            powers_[ 0 ] = 1;
            for ( int i = 1; i < n; i++ ) {
                powers_[ i ] = x * powers_[ i - 1 ];
            }
        }
    }

    private static class PowersTable extends CalcStarTable<PowerStructure> {
        private final int nPow_;
        private final StarTable base_;
        PowersTable( StarTable base, int nPow ) { 
            super( base, createPowerColumns( nPow ) );
            base_ = base;
            nPow_ = nPow;
        }
        public PowerStructure createCalculation( long irow )
                throws IOException {
            double x = ((Number) base_.getCell( irow, 0 )).doubleValue();
            return createPowerStructure( x );
        }
        public PowerStructure createCalculation( RowData baseRow )
                throws IOException {
            double x = ((Number) baseRow.getCell( 0 )).doubleValue();
            return createPowerStructure( x );
        }
        private PowerStructure createPowerStructure( double x ) {
            return new PowerStructure( nPow_, x );
        }
        private static Col<PowerStructure,Double>[]
                createPowerColumns( int n ) {
            List<Col<PowerStructure,Double>> list =
                new ArrayList<Col<PowerStructure,Double>>( n );
            for ( int i = 0; i < n; i++ ) {
                final int i0 = i;
                final ColumnInfo info =
                    new ColumnInfo( "p" + i0, Double.class, null );
                list.add( new Col<PowerStructure,Double>() {
                    public ColumnInfo getInfo() {
                        return info;
                    }
                    public Double getValue( PowerStructure ps ) {
                        return ps.powers_[ i0 ];
                    }
                } );
            }
            @SuppressWarnings("unchecked")
            Col<PowerStructure,Double>[] cols =
                (Col<PowerStructure,Double>[]) list.toArray( new Col[ 0 ] );
            return cols;
        }
    }

    private static StarTable createIndexTable( int nrow ) {
        ColumnStarTable t = ColumnStarTable.makeTableWithRows( nrow );
        ValueInfo xInfo = new DefaultValueInfo( "X", Double.class, null );
        t.addColumn( new ColumnData( xInfo ) {
            public Object readValue( long irow ) {
                return Double.valueOf( irow );
            }
        } );
        return t;
    }
}
