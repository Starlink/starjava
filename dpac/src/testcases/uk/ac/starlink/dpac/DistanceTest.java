package uk.ac.starlink.dpac;

import gaia.cu9.tools.parallax.DistanceEstimator;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.URLDataSource;

public class DistanceTest extends TestCase {

    public static final String CEPTABLE_NAME = "distcep.vot";
    public static final DistanceEstimator L011 = createExpEstimator( 0.11 );
    public static final DistanceEstimator L135 = createExpEstimator( 1.35 );
    private final StarTable cepTable_;

    public DistanceTest() throws IOException {
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        DataSource datsrc =
            new URLDataSource( getClass().getResource( CEPTABLE_NAME ) );
        cepTable_ = new StarTableFactory().makeStarTable( datsrc );
    }

    /**
     * This tests the estimates made by the classes in gaia.cu9.tools.parallax
     * against those for Cepheids published in Tri and Coryn's Paper III
     * (2016ApJ...833..119A).
     */
    public void testCepDistancesCu9() throws IOException {
        long start = System.currentTimeMillis();
        StarTable dc1 = getCu9Distances( L011, "Exp1" );
        StarTable dc2 = getCu9Distances( L135, "Exp2" );
        JoinFixAction fixAct1 =
            JoinFixAction.makeRenameDuplicatesAction( "Exp1" );
        JoinFixAction fixAct2 =
            JoinFixAction.makeRenameDuplicatesAction( "Exp2" );
        StarTable tc =
            new JoinStarTable( new StarTable[] { cepTable_, dc1, dc2 },
                               new JoinFixAction[] { JoinFixAction.NO_ACTION,
                                                     fixAct1, fixAct2 } );

        // The best estimate (mode) values are similar to within a few
        // parts in 1e6, with errors distributed approximately normally.
        checkValues( tc, "diff_rMoExp1", 1.5e-6 );
        checkValues( tc, "diff_rMoExp2", 3.0e-6 );

        // There is a definite discrepancy in the quantile values;
        // in most cases the relative difference is about -0.0004.
        // Enrique Utrilla thinks that the problem is in Tri's code
        // not his; comparisons with Ariadna's python code give
        // smaller discrepancies, without the large -0.0004 signal.
        // I haven't, but perhaps should, included tests against
        // that data here.
        checkValues( tc, "diff_r5Exp1", 0.0005 );
        checkValues( tc, "diff_r95Exp1", 0.0005 );
        checkValues( tc, "diff_r5Exp2", 0.0005 );
        checkValues( tc, "diff_r95Exp2", 0.0005 );

        // this test is about 2 orders of magnitude faster than using CU9
 //     System.out.println( "cu9:\t" + (System.currentTimeMillis() - start) );
 //     new uk.ac.starlink.table.StarTableOutput()
 //        .writeStarTable( tc, "out-cu9.vot", "votable" );
    }

    /**
     * This tests the estimates made by the Edsd class
     * against those for Cepheids published in Tri and Coryn's Paper III
     * (2016ApJ...833..119A).
     */
    public void testCepDistancesEdsd() throws IOException {
        long start = System.currentTimeMillis();
        StarTable dc1 = getEdsdDistances( 0.11, "Exp1" );
        StarTable dc2 = getEdsdDistances( 1.35, "Exp2" );
        JoinFixAction fixAct1 =
            JoinFixAction.makeRenameDuplicatesAction( "Exp1" );
        JoinFixAction fixAct2 =
            JoinFixAction.makeRenameDuplicatesAction( "Exp2" );
        StarTable tc =
            new JoinStarTable( new StarTable[] { cepTable_, dc1, dc2 },
                               new JoinFixAction[] { JoinFixAction.NO_ACTION,
                                                     fixAct1, fixAct2 } );

        // The best estimate (mode) values are similar to within a few
        // parts in 1e6, with errors distributed approximately normally.
        checkValues( tc, "diff_rMoExp1", 1.5e-6 );
        checkValues( tc, "diff_rMoExp2", 3.0e-6 );

        // There is a definite discrepancy in the quantile values;
        // in most cases the relative difference is about -0.0004.
        // Enrique Utrilla thinks that the problem is in Tri's code
        // not his; comparisons with Ariadna's python code give
        // smaller discrepancies, without the large -0.0004 signal.
        // I haven't, but perhaps should, included tests against
        // that data here.
        checkValues( tc, "diff_r5Exp1", 0.0005 );
        checkValues( tc, "diff_r95Exp1", 0.0005 );
        checkValues( tc, "diff_r5Exp2", 0.0005 );
        checkValues( tc, "diff_r95Exp2", 0.0005 );

 //     System.out.println( "edsd:\t" + (System.currentTimeMillis() - start) );
 //     new uk.ac.starlink.table.StarTableOutput()
 //        .writeStarTable( tc, "out-edsd.vot", "votable" );
    }

    private void checkValues( StarTable tc, String cname, double maxval )
            throws IOException {
        int icol = -1;
        for ( int ic = 0; ic < tc.getColumnCount() && icol < 0; ic++ ) {
            if ( cname.equals( tc.getColumnInfo( ic ).getName() ) ) {
                icol = ic;
            }
        }
        for ( RowSequence rseq = tc.getRowSequence(); rseq.next();
              rseq.close() ) {
            double dval = ((Number) rseq.getCell( icol )).doubleValue();
            assertTrue( Math.abs( dval ) < maxval );
        }
    }

    private StarTable getCu9Distances( DistanceEstimator estimator,
                                       String label )
            throws IOException {
        StarTable inTable = cepTable_;
        StarReader rdr = new StarReader( inTable );
        StarTable distTable = new Cu9DistanceTable( inTable, rdr, estimator );
        StarTable inputsTable =
            new JoinStarTable( new StarTable[] { inTable, distTable } );
        inputsTable = Tables.randomTable( inputsTable );
        StarTable cmpTable = new ComparisonTable( inputsTable, label );
        return new JoinStarTable( new StarTable[] { distTable, cmpTable } );
    }

    private StarTable getEdsdDistances( double lkpc, String label )
            throws IOException {
        StarTable inTable = cepTable_;
        StarReader rdr = new StarReader( inTable );
        StarTable distTable = new EdsdDistanceTable( inTable, rdr, lkpc );
        StarTable inputsTable =
            new JoinStarTable( new StarTable[] { inTable, distTable } );
        inputsTable = Tables.randomTable( inputsTable );
        StarTable cmpTable = new ComparisonTable( inputsTable, label );
        return new JoinStarTable( new StarTable[] { distTable, cmpTable } );
    }

    /**
     * @param   lkpc  distance parameter L in kiloparsec
     */
    static DistanceEstimator createExpEstimator( double lkpc ) {
        DistanceEstimator.EstimationType etype =
            DistanceEstimator.EstimationType.EXP_DEC_VOL_DENSITY;
        Map<String,String> params = new LinkedHashMap<String,String>();
        params.put( "L", Double.toString( lkpc ) );
        return new DistanceEstimator( etype, params );
    }

    private static class ComparisonTable extends ColumnStarTable {
        private final StarTable inTable_;
        private final Map<String,Integer> colMap_;
        ComparisonTable( StarTable inTable, String label ) {
            inTable_ = inTable;
            colMap_ = new LinkedHashMap<String,Integer>();
            int ncol = inTable.getColumnCount();
            for ( int i = 0; i < ncol; i++ ) {
                colMap_.put( inTable.getColumnInfo( i ).getName(),
                             Integer.valueOf( i ) );
            }
            addColumn( createDiffColumn( "rMo" + label, "best_dist" ) );
            addColumn( createDiffColumn( "r5" + label, "dist_lo" ) );
            addColumn( createDiffColumn( "r95" + label, "dist_hi" ) );
            addColumn( createDiffColumn( "r50" + label, "dist_median" ) );
        }
        public long getRowCount() {
            return inTable_.getRowCount();
        }
        ColumnData createDiffColumn( String c0, String c1 ) {
            final int icol0 = colMap_.get( c0 ).intValue();
            final int icol1 = colMap_.get( c1 ).intValue();
            ValueInfo info =
                new DefaultValueInfo( "diff_" + c0, Double.class,
                                      "(" + c1 + " - " + c0 + ")/" + c0 );
            return new ColumnData( info ) {
                public Object readValue( long irow ) throws IOException {
                    double d0 = ((Number) inTable_.getCell( irow, icol0 ))
                               .doubleValue();
                    double d1 = ((Number) inTable_.getCell( irow, icol1 ))
                               .doubleValue();
                    return Double.valueOf( ( d1 - d0 ) / d0 );
                }
            };
        }
    }
}
