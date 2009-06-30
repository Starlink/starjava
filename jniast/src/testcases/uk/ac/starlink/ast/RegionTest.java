package uk.ac.starlink.ast;

import uk.ac.starlink.util.TestCase;

public class RegionTest extends TestCase {

    public RegionTest( String name ) {
        super( name );
    }

    public void testBox() {
        Frame frm = new Frame( 2 );
        frm.setIdent( "Region test frame" );
        double[] lbnds = new double[] { 10, 20 };
        double[] ubnds = new double[] { 100, 200 };
        Box box = new Box( frm, 1, lbnds, ubnds, null );
        assertTrue( box.getBounded() );
        double[][] bounds = box.getRegionBounds();
        assertArrayEquals( bounds[ 0 ], lbnds );
        assertArrayEquals( bounds[ 1 ], ubnds );
        assertEquals( frm, box.getRegionFrame() );
        assertTrue( frm != box.getRegionFrame() ); /* It's a deep copy. */
    }

    public void testCircle() {
        Frame frm = new Frame( 2 );
        Circle unit = new Circle( frm, new double[ 2 ], 1., null );
        assertTrue( unit.getBounded() );

        assertBounds( unit, -1, -1, 1, 1 );
        assertSameShape( unit, new Circle( frm, new double[] { 0, 0 } ,
                                                new double[] { 0, 1 }, null ) );

        try {
            new Circle( frm, new double[ 1 ], 1., null );
            fail();
        }
        catch ( IllegalArgumentException e ) {
        }
        try {
            new Circle( frm, 2, new double[ 2 ], new double[ 2 ], null );
            fail();
        }
        catch ( IllegalArgumentException e ) {
        }
    }

    public void testEllipse() {
        Ellipse blob = new Ellipse( new Frame( 2 ), 0, new double[] { 0, 0 },
                                    new double[] { 2, 0 },
                                    new double[] { 0, 1 }, null );
        assertBounds( blob, -2, -1, 2, 1, 1e-3 );
        assertSameShape( blob, new Ellipse( new Frame( 2 ), 1, 
                                            new double[] { 0, 0 },
                                            new double[] { 1, 2 },
                                            new double[] { 0, 0 },
                                            null ) );
    }

    public void testCmpRegion() {
        Box oblong1 = box( 0, 0, 4, 8 );
        Box oblong2 = box( 4, 0, 8, 8 );
        // nope, uncertainties make it OVERLAP_PARTIAL
        // assertEquals( Region.OVERLAP_NONE, oblong1.overlap( oblong2 ) );
        CmpRegion square = new CmpRegion( oblong1, oblong2, CmpRegion.AST__OR );
        assertSameShape( square, box( 0, 0, 8, 8 ) );

        Region s1 = box( 0, 0, 2, 2 );
        Region s2 = box( 1, 1, 3, 3 );
        assertSameShape( new CmpRegion( s1, s2, CmpRegion.AST__AND ),
                         box( 1, 2, 2, 2 ) );
    }

    public void testInterval() {
        Interval pos = new Interval( new Frame( 2 ), 
                                     new double[] { 0, 0 },
                                     new double[] { AstObject.AST__BAD,
                                                    AstObject.AST__BAD },
                                     null );
        Box box = box( -1, -1, 1, 1 );
        Region intersect = new CmpRegion( box, pos, CmpRegion.AST__AND );
        assertBounds( intersect, 0, 0, 1, 1 );

        try {
           new Interval( new Frame( 3 ), new double[ 2 ], new double[ 3 ],
                         null );
           fail();
        }
        catch ( IllegalArgumentException e ) {
        }
    }

    public void testPointList() {
        Frame frm = new Frame( 2 );
        double[][] points = { { 1, 2, 3, 4 }, { 101, 102, 103, 104 } };
        PointList plist = new PointList( frm, 4, points, null );
        assertEquals( 4, plist.getListSize() );
        assertArrayEquals( points[ 0 ], plist.getRegionPoints()[ 0 ] );
        assertArrayEquals( points[ 1 ], plist.getRegionPoints()[ 1 ] );
        assertEquals( 2, plist.getRegionPoints().length );
        assertBounds( plist, 1, 101, 4, 104 );
        plist = new PointList( frm, 4, points, box( .01, .01, .01, .01 ) );

        try {
            new PointList( frm, 4, new double[][] { { 1, 2, 3, 4 },
                                                    { 1, 2, 3 } }, null );
            fail();
        }
        catch ( IllegalArgumentException e ) {
        }
        try {
            new PointList( new Frame( 3 ), 4,
                           new double[][] { { 1, 2, 3, 4 }, { 1, 2, 3, 4 } },
                           null );
            fail();
        }
        catch ( IllegalArgumentException e ) {
        }
        try {
            new PointList( frm, 4, new double[][] { { 1, 2, 3, 4 }, null },
                           null );
            fail();
        }
        catch ( IllegalArgumentException e ) {
        }
    }

    public void testPolygon() {
        assertSameShape( box( 0, 0, 1, 1 ),
                         new Polygon( new Frame( 2 ), 4, 
                                      new double[] { 0, 1, 1, 0 },
                                      new double[] { 0, 0, 1, 1 }, null ) );
    }

    public void testPrism() {
        Prism oblong = new Prism( new Box( new Frame( 1 ), 1,
                                           new double[] { 10 }, 
                                           new double[] { 20 }, null ),
                                  new Interval( new Frame( 1 ),
                                                new double[] { -6 },
                                                new double[] { -3 }, null ) );
        assertBounds( oblong, 10, -6, 20, -3 ); 

        Region hyperSphere = new Circle( new Frame( 4 ),
                                         new double[] { Math.PI, Math.E, 1, 0 },
                                         23., null );
        assertEquals( 6, new Prism( hyperSphere, box( 100, 500, 120, 520 ) )
                        .getNaxes() );
    }

    public void testUnc() {
        Region box = box( 0, 0, 100, 100 );
        assertNull( box.getUnc( false ) );
        assertNotNull( box.getUnc( true ) );

        Region blob = new Box( new Frame( 2 ), 0, new double[ 2 ], 
                               new double[] { 0.1, 0.1 }, null );
        box.setUnc( blob );
        assertNotNull( box.getUnc( false ) );
        assertNotNull( box.getUnc( false ) );
        assertEquals( box.getUnc( false ), box.getUnc( true ) );

        // You might think that the following hold, but they don't -
        // the uncertainty region might be a translated copy of blob.
        // assertEquals( blob, box.getUnc( false ) );
        // assertEquals( blob, box.getUnc( true ) );
    }

    public void testNullRegion() {
        Region unc = box( 0, 0, 1e-6, 1e-6 );
        NullRegion nr = new NullRegion( new Frame( 2 ), unc );
        Box box = box( 1, 2, 3, 4 );
        assertEquals( Region.OVERLAP_NONE, box.overlap( nr ) );
        assertTrue( ! nr.getNegated() );
        nr.negate();
        assertTrue( nr.getNegated() );
        assertEquals( Region.OVERLAP_INSIDE, box.overlap( nr ) );
    }

    public void testMapRegion() {
        Region moved = box( 1, 2, 21, 22 )
                      .mapRegion( new ZoomMap( 2, 2.0 ), new Frame( 2 ) );
        assertBounds( moved, 2, 4, 42, 44 );
    }

    public void testNegate() {
        Region box = box( -50, -23, 51, 51 );
        assertTrue( ! box.getNegated() );
        box.negate();
        assertTrue( box.getNegated() );
        box.negate();
        assertTrue( ! box.getNegated() );
        box.setNegated( true );
        assertTrue( box.getNegated() );
        box.setNegated( false );
        assertTrue( ! box.getNegated() );
    }

    public void testOverlap() {
        Box unit = box( 0, 0, 1, 1 );
        assertEquals( Region.OVERLAP_NONE,
                      unit.overlap( box( 5, 6, 7, 8 ) ) );
        assertEquals( Region.OVERLAP_INSIDE,
                      unit.overlap( box( -1, -1, 2, 2 ) ) );
        assertEquals( Region.OVERLAP_OUTSIDE,
                      unit.overlap( box( .1, .1, .9, .9 ) ) );
        assertEquals( Region.OVERLAP_PARTIAL,
                      unit.overlap( box( .5, .5, 2, 2 ) ) );
        assertEquals( Region.OVERLAP_SAME,
                      unit.overlap( (Region) unit.copy() ) );
        Box unUnit = (Box) unit.copy();
        unUnit.negate();
        assertEquals( Region.OVERLAP_NEGATE,
                      unit.overlap( unUnit ) );
    }

    public void testMask() {
        Mapping map = new UnitMap( 2 );
        int ndim = 2;
        int[] lbnd = new int[] { 10, 20 };
        int[] ubnd = new int[] { 11, 21 };
        Region region = box( 9.5, 19.5, 11.5, 20.5 );

        byte[] inB = new byte[] { 0, 0, 0, 0 };
        assertEquals( 2, region
                        .maskB( null, true, 2, lbnd, ubnd, inB, (byte) -1 ) );
        assertArrayEquals( new byte[] { -1, -1, 0, 0 }, inB );

        short[] inS = new short[] { 5, 5, 5, 5, };
        assertEquals( 2, region
                        .maskS( null, false, 2, lbnd, ubnd, inS, (short) 10 ) );
        assertArrayEquals( new short[] { 5, 5, 10, 10 }, inS );

        int[] inI = new int[] { 0, 0, 0, 0 };
        assertEquals( 2, region.maskI( map, true, 2, lbnd, ubnd, inI, 1 ) );
        assertArrayEquals( new int[] { 1, 1, 0, 0 }, inI );

        try {
            long[] inL = new long[] { 0L, 0L, 0L, 0L };
            assertEquals( 2, region
                            .maskL( map, true, 2, lbnd, ubnd, inL, 1L ) );
            assertArrayEquals( new long[] { 1L, 1L, 0, 0 }, inL );
        }
        catch ( UnsupportedOperationException e ) {
            System.out.println( "Region.maskL: " + e.getMessage() );
        }

        float[] inF = new float[] { 1.f, 2.f, 3.f, 4.f };
        assertEquals( 2, region
                        .maskF( map, true, 2, lbnd, ubnd, inF, Float.NaN ) );
        assertArrayEquals( new float[] { Float.NaN, Float.NaN, 3.f, 4.f },
                           inF );

        double[] inD = new double[] { 99., 99., 99., 99. };
        assertEquals( 1, region.maskD( new ShiftMap( new double[] { 1., 0. } ),
                                       true, 2, lbnd, ubnd, inD, Math.E ) );
        assertArrayEquals( new double[] { 99., Math.E, 99., 99. }, inD );

        try {
            region.maskI( null, true, 3, lbnd, ubnd, new int[ 4 ], -1 );
            fail();
        }
        catch ( IllegalArgumentException e ) {
            // not enough elements in lbnd
        }

        try {
            region.maskD( map, false, 2, lbnd, ubnd, new double[ 3 ], -1. );
            fail();
        }
        catch ( IllegalArgumentException e ) {
            // not enough arguments in in array
        }
    }

    /*
     * Don't run this one - it generates messy output to stdout.
     */
    public void noTestShowMesh() {
        Box box = box( 0, 0, 2, 2 );
        box.showMesh( true, "Mesh" );
    }

    private static Box box( double x1, double y1, double x2, double y2 ) {
        return new Box( new Frame( 2 ), 1, new double[] { x1, y1 },
                        new double[] { x2, y2 }, null );
    }

    private void assertBounds( Region region, double xlo, double ylo,
                                              double xhi, double yhi,
                               double delta ) {
        double[][] bounds = region.getRegionBounds();
        assertArrayEquals( new double[] { xlo, ylo, xhi, yhi },
                           new double[] { bounds[ 0 ][ 0 ],
                                          bounds[ 0 ][ 1 ],
                                          bounds[ 1 ][ 0 ],
                                          bounds[ 1 ][ 1 ] }, delta );
    }

    private void assertBounds( Region region, double xlo, double ylo,
                                              double xhi, double yhi ) {
        assertBounds( region, xlo, ylo, xhi, yhi, 0.0 );
    }

    private void assertSameShape( Region region1, Region region2 ) {
        int ov = region1.overlap( region2 );
        assertEquals( "Regions don't match (" + ov + ")",
                      Region.OVERLAP_SAME, ov );
    }

}
