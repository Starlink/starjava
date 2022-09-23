package uk.ac.starlink.ttools.task;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import uk.ac.starlink.pal.AngleDR;
import uk.ac.starlink.pal.Pal;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RandomStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.util.LogUtils;

public class EllipseMatchTest extends TableTestCase {

    private static final Pal pal_ = new Pal();
    private static final double DEGREE = Math.PI / 180;
    private static final double ARCSEC = DEGREE / 60 / 60;
    private Random random_ = new Random( 230327L );

    public EllipseMatchTest() {
        LogUtils.getLogger( "uk.ac.starlink.ttools.join" )
                .setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.util" )
                .setLevel( Level.WARNING );
    }

    public void testRotation() {
        doTestRotation( false );
        doTestRotation( true );
    }

    private void doTestRotation( boolean useCirc ) {
        // test the sense of the position angle wrangling.
        double unit = ARCSEC;
        double psi = Math.PI * 0.23;
        double[][] m1 = {{1,0,0},{0,1,0},{0,0,1}};
        Ellipse e = new Ellipse( unit * 3, unit * 8, unit * 1, unit * 2, psi );
        EllipseTableSky table =
            new EllipseTableSky( new Ellipse[] { e }, m1, useCirc );
        Object[] row = table.getRow( 0 );
        assertEquals( psi * 180 / Math.PI, ((Number) row[ 4 ]).doubleValue(),
                      1e-3 );
    }

    public void testCartesian() throws Exception {
        doTestCartesian( true );
        doTestCartesian( false );
    }

    private void doTestCartesian( boolean useCirc ) throws Exception {
        int nel = 100;
        double rmin = 0.1;
        double rmax = 1.0;
        double range = rmax * 1000;
        double scale = rmax * 10;
        Ellipse[] ellipses1 = createEllipses( nel, range, rmin, rmax );
        Ellipse[] ellipses2 = scrambleEllipses( ellipses1, rmin, rmax );
        double dlo = rmin / Math.sqrt( 2 ) * 0.99;
        double dhi = rmax / Math.sqrt( 2 ) * 1.01;
        double dmid = ( rmax + rmin ) * 0.5;
        assertEquals( nel,
            selfMatchCount(
            match12( new EllipseTable2d( ellipses1, 0, 0, useCirc ),
                     new EllipseTable2d( ellipses2, 0, 0, useCirc ),
                     scale ) ) );
        assertEquals( nel,
            selfMatchCount(
            match12( new EllipseTable2d( ellipses1, -dlo, -dlo, useCirc ),
                     new EllipseTable2d( ellipses2, +dlo, +dlo, useCirc ),
                     scale ) ) );
        assertEquals( 0,
            selfMatchCount(
            match12( new EllipseTable2d( ellipses1, -dhi, +dhi, useCirc ),
                     new EllipseTable2d( ellipses2, +dhi, -dhi, useCirc ),
                     scale ) ) );
        long nmid =
            selfMatchCount(
            match12( new EllipseTable2d( ellipses1, +dmid, +dmid, useCirc ),
                     new EllipseTable2d( ellipses2, -dmid, -dmid, useCirc ),
                     scale ) );
        assertTrue( nmid > 1 && nmid < nel - 1 );  // not cast iron, but likely
    }

    public void testSky() throws Exception {
        doTestSky( false );
        doTestSky( true );
    }

    private void doTestSky( boolean useCirc ) throws Exception {
        int nel = 100;
        double unit = ARCSEC;
        double rmin = 0.4 * unit;
        double rmax = 1 * unit;
        double range = rmax * 500;
        double scale = rmax * 5;
        Ellipse[] els1 = createEllipses( nel, range, rmin, rmax );
        Ellipse[] els2 = scrambleEllipses( els1, rmin, rmax );

        assertEquals( nel,
                      selfMatchCount(
                      shiftRotMatch( els1, els2, 0, 0, 0, scale, useCirc ) ) );
        checkSkyRotation( els1, els2, rmin, rmax, scale, 0, 1.4, useCirc );
        checkSkyRotation( els1, els2, rmin, rmax, scale, Math.PI * 0.5, 1.4,
                          useCirc );
        checkSkyRotation( els1, els2, rmin, rmax, scale, 2, 2, useCirc );
        checkSkyRotation( els1, els2, rmin, rmax, scale, -1.5, 1.4, useCirc );
    }

    private void checkSkyRotation( Ellipse[] els1, Ellipse[] els2,
                                   double rmin, double rmax, double scale,
                                   double rotPhi, double rotTheta,
                                   boolean useCirc )
            throws Exception {
        int nel = els1.length;
        assertEquals( nel, els2.length );
        double dlo = rmin * 0.99;
        double dhi = rmax * 1.01;
        double dmid = ( rmax + rmin ) * 0.5;
        assertEquals( nel, selfMatchCount(
                           shiftRotMatch( els1, els2, rotPhi, rotTheta,
                                          dlo, scale, useCirc ) ) );
        assertEquals( 0, selfMatchCount(
                         shiftRotMatch( els1, els2, rotPhi, rotTheta,
                                        dhi, scale, useCirc ) ) );
        long nmid = selfMatchCount(
                    shiftRotMatch( els1, els2, rotPhi, rotTheta,
                                   dmid, scale, useCirc ) );
        assertTrue( nmid > 1 && nmid < nel - 1 );
    }

    private Set shiftRotMatch( Ellipse[] ellipses1, Ellipse[] ellipses2,
                               double rotPhi, double rotTheta, double shiftXi,
                               double scale, boolean useCirc )
            throws Exception {
        double[][] rot1 = rotationMatrix( rotPhi, rotTheta - shiftXi );
        double[][] rot2 = rotationMatrix( rotPhi, rotTheta + shiftXi );
        return match12( new EllipseTableSky( ellipses1, rot1, useCirc ),
                        new EllipseTableSky( ellipses2, rot2, useCirc ),
                        scale );
    }

    /**
     * Returns a rotation matrix which will rotate the point (alpha=0, delta=0),
     * or equivalently (x=1,y=0,z=0), by rotTheta radians in a direction given
     * by rotPhi.
     *
     * @param   rotPhi  angle of rotation - 0 is along equator,
     *                  PI/2 is towards pole
     * @param   rotTheta  magnitude of rotation
     */
    private static double[][] rotationMatrix( double rotPhi, double rotTheta ) {
        return pal_.Deuler( "xz", rotPhi, rotTheta, Double.NaN );
    }

    public void testRotateSky() throws Exception {
        doTestRotateSky( false );
        doTestRotateSky( true );
    }

    private void doTestRotateSky( boolean useCirc ) throws Exception {
        int nel = 200;
        double unit = ARCSEC;
        double rmin = 0.1 * unit;
        double rmax = 1.0 * unit;
        double range = rmax * 50;
        double scale = rmax * 1;
        Ellipse[] els1 = createEllipses( nel, range, rmin, rmax );
        Ellipse[] els2 = createEllipses( nel, range, rmin, rmax );
        Set t12 = rotatedSkyMatch( els1, els2, 0, 0, scale, useCirc );
        assertEquals( t12,
                      rotatedSkyMatch( els1, els2, 0, 1, scale, useCirc ) );
        assertEquals( t12,
                      rotatedSkyMatch( els1, els2, 1, 1.5, scale, useCirc ) );
        assertEquals( t12,
                      rotatedSkyMatch( els1, els2, Math.PI*.5, 1.5, scale,
                                       useCirc ) );
        assertEquals( t12,
                      rotatedSkyMatch( els1, els2, 2, -2, scale, useCirc ) );
    }

    private Set rotatedSkyMatch( Ellipse[] ellipses1, Ellipse[] ellipses2,
                                 double rotPhi, double rotTheta, double scale,
                                 boolean useCirc )
            throws Exception {
        double[][] rot = rotationMatrix( rotPhi, rotTheta );
        return match12( new EllipseTableSky( ellipses1, rot, useCirc ),
                        new EllipseTableSky( ellipses2, rot, useCirc ), scale );
    }
    
    private Ellipse[] createEllipses( int count, double range,
                                      double rmin, double rmax ) {
        Ellipse[] ellipses = new Ellipse[ count ];
        for ( int ie = 0; ie < count; ie++ ) {
            double x = ( random_.nextDouble() - 0.5 ) * range;
            double y = ( random_.nextDouble() - 0.5 ) * range;
            ellipses[ ie ] = createRandomEllipse( x, y, rmin, rmax );
        }
        return ellipses;
    }

    private Ellipse[] scrambleEllipses( Ellipse[] inEls,
                                        double rmin, double rmax ) {
        int nel = inEls.length;
        Ellipse[] outEls = new Ellipse[ nel ];
        for ( int ie = 0; ie < nel; ie++ ) {
            Ellipse e = inEls[ ie ];
            double x = e.x_;
            double y = e.y_;
            outEls[ ie ] = createRandomEllipse( x, y, rmin, rmax );
        }
        return outEls;
    }

    private Ellipse createRandomEllipse( double x, double y,
                                         double rmin, double rmax ) {
        double a = rmin + random_.nextDouble() * ( rmax - rmin );
        double b = random_.nextDouble() <= 0.2
                 ? a
                 : rmin + random_.nextDouble() * ( rmax - rmin );
        double psi = ( random_.nextDouble() * Math.PI * 2 );
        return new Ellipse( x, y, a, b, psi );
    }

    private static Set<Link2> match12( EllipseTable t1, EllipseTable t2,
                                       double scale )
            throws Exception {
        MapEnvironment env = new MapEnvironment()
            .setValue( "find", "all" )
            .setValue( "in1", t1 )
            .setValue( "in2", t2 )
            .setValue( "join", "1and2" )
            .setValue( "matcher", t1.getMatcherName() )
            .setValue( "values1", t1.getMatchValues() )
            .setValue( "values2", t2.getMatchValues() )
            .setValue( "params", Double.toString( scale / ARCSEC ) )
            .setValue( "icmd1", "addcol ID1 $0" )
            .setValue( "icmd2", "addcol ID2 $0" )
            .setValue( "progress", "none" );
        new TableMatch2().createExecutable( env ).execute();
        StarTable table = env.getOutputTable( "omode" );
        int idcol1 = getColIndex( table, "ID1" );
        int idcol2 = getColIndex( table, "ID2" );
        Set<Link2> linkSet = new HashSet<Link2>();
        for ( RowSequence rseq = table.getRowSequence(); rseq.next(); ) {
            linkSet
           .add( new Link2( ((Number) rseq.getCell( idcol1 )).intValue(),
                            ((Number) rseq.getCell( idcol2 )).intValue() ) );
        }
        return linkSet;
    }

    private static int selfMatchCount( Collection<Link2> linkSet ) {
        int ns = 0;
        for ( Link2 link : linkSet ) {
            if ( link.isSelfLink() ) {
                ns++;
            }
        }
        return ns;
    }

    private static abstract class EllipseTable extends RandomStarTable {
        final Ellipse[] ellipses_;
        final String matcher_;
        final String[] colNames_;
        EllipseTable( Ellipse[] ellipses, String matcher, String[] colNames ) {
            ellipses_ = ellipses;
            matcher_ = matcher;
            colNames_ = colNames;
        }
        public int getColumnCount() {
            return colNames_.length;
        }
        public ColumnInfo getColumnInfo( int icol ) {
            return new ColumnInfo( colNames_[ icol ], Double.class, null );
        }
        public long getRowCount() {
            return ellipses_.length;
        }
        public Object[] getRow( long irow ) {
            return toRow( ellipses_[ (int) irow ] );
        }
        public Object getCell( long irow, int icol ) {
            return getRow( irow )[ icol ];
        }
        public String getMatchValues() {
            StringBuffer sbuf = new StringBuffer();
            for ( int ic = 0; ic < colNames_.length; ic++ ) {
                if ( ic > 0 ) {
                    sbuf.append( ' ' );
                }
                sbuf.append( colNames_[ ic ] );
            }
            return sbuf.toString();
        }
        public String getMatcherName() {
            return matcher_;
        }
        abstract Object[] toRow( Ellipse ellipse );
    }

    private static class EllipseTable2d extends EllipseTable {
        private final double xoff_;
        private final double yoff_;
        EllipseTable2d( Ellipse[] ellipses, double xoff, double yoff,
                        boolean useCirc ) {
            super( ellipses, useCirc ? "2d_ellipse" : "2d_ellipse-nocirc",
                   new String[] { "X", "Y", "A", "B", "THETA" } );
            xoff_ = xoff;
            yoff_ = yoff;
        }
        Object[] toRow( Ellipse ellipse ) {
            return new Object[] {
                new Double( ellipse.x_ + xoff_ ),
                new Double( ellipse.y_ + yoff_ ),
                new Double( ellipse.a_ ),
                new Double( ellipse.b_ ),
                new Double( ellipse.psi_ )
            };
        }
    }

    private static class EllipseTableSky extends EllipseTable {
        private final double[][] rotation_;
        EllipseTableSky( Ellipse[] ellipses, double[][] rotation,
                         boolean useCirc ) {
            super( ellipses, useCirc ? "skyellipse" : "skyellipse-nocirc",
                   new String[] { "alpha", "delta", "mu", "nu", "zeta" } );
            rotation_ = rotation;
        }
        Object[] toRow( Ellipse ellipse ) {
            double alpha0 = ellipse.x_;
            double delta0 = ellipse.y_;
            double mu = ellipse.a_;
            double nu = ellipse.b_;
            double zeta0 = ellipse.psi_;
            double[] centreRot = rotate( alpha0, delta0 );
            double[] paRot = rotate( alpha0 + mu * Math.sin( zeta0 ),
                                     delta0 + mu * Math.cos( zeta0 ) );
            double alpha1 = centreRot[ 0 ];
            double delta1 = centreRot[ 1 ];
            double zeta1 = posAng( centreRot, paRot );
            return new Object[] {
                new Double( alpha1 / DEGREE ), new Double( delta1 / DEGREE ),
                new Double( mu / ARCSEC ), new Double( nu / ARCSEC ),
                new Double( zeta1 / DEGREE )
            };
        }
        private double[] rotate( double alpha, double delta ) {
            double[] xyz0 = pal_.Dcs2c( new AngleDR( alpha, delta ) );
            double[] xyz1 = pal_.Dmxv( rotation_, xyz0 );
            AngleDR ad1 = pal_.Dcc2s( xyz1 );
            return new double[] { ad1.getAlpha(), ad1.getDelta() };
        }
        private double posAng( double[] ad1, double[] ad2 ) {
            /* From sla_dbear (dbear.f). */
            double alpha1 = ad1[ 0 ];
            double delta1 = ad1[ 1 ];
            double alpha2 = ad2[ 0 ];
            double delta2 = ad2[ 1 ];
            double da = alpha2 - alpha1;
            double y = Math.sin( da ) * Math.cos( delta2 );
            double x = Math.sin( delta2 ) * Math.cos( delta1 )
                     - Math.cos( delta2 ) * Math.sin( delta1 ) * Math.cos( da );
            return x == 0 && y == 0 ? 0
                                    : Math.atan2( y, x );
        } 
    }

    private static class Link2 {
        int ix1_;
        int ix2_;
        Link2( int ix1, int ix2 ) {
            ix1_ = ix1;
            ix2_ = ix2;
        }
        boolean isSelfLink() {
            return ix1_ == ix2_;
        }
        @Override public boolean equals( Object o ) {
            Link2 other = (Link2) o;
            return other.ix1_ == this.ix1_
                && other.ix2_ == this.ix2_;
        }
        @Override public int hashCode() {
            return ( ix1_ << 16 ) | ix2_;
        }
    }

    private static class Ellipse {
        final double x_;
        final double y_;
        final double a_;
        final double b_;
        final double psi_;
        Ellipse( double x, double y, double a, double b, double psi ) {
            x_ = x;
            y_ = y;
            a_ = a;
            b_ = b;
            psi_ = psi;
        }
    }
}
