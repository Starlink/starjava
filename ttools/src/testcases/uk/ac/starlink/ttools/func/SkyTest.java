package uk.ac.starlink.ttools.func;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.ttools.task.MapEnvironment;
import uk.ac.starlink.ttools.task.TapQuerier;
import uk.ac.starlink.util.IntList;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.votable.VOTableWriter;

public class SkyTest extends TableTestCase {

    /** Table created by running this class's main method. */
    final URL polyLoc_ = getClass().getResource( "polygons.vot" );

    public SkyTest() {
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
    }

    public void testMid() {
        assertEquals( 203.75, Sky.midLon(204.0, 203.5) );
        assertEquals( 0.5, Sky.midLon( 2, 359 ) );
        assertEquals( 0.5, Sky.midLon( 359, 2 ) );
        assertEquals( 23.75, Sky.midLat( 23.5, 24.0 ) );
    }

    public void testSingles() {
        assertTrue( Sky.inSkyPolygon(36,4, 40,7, 36,9, 30,8, 29,2, 39,2) );
        assertFalse( Sky.inSkyPolygon(39,50, 40,7, 36,9, 30,8, 29,2, 39,2) );
    }

    public void testPolygonsTable() throws IOException {

        /* Reads a table set up previously by invoking acquireShapesTable
         * using the main() method of this class, and checks that the
         * results are the same as that.  Note this is not (just) a
         * regression test, since the results in that table were obtained
         * by an independent point-in-polygon implementation. */
        StarTable polyTable = new StarTableFactory()
                             .makeStarTable( polyLoc_.toString(), "votable" );
        RowSequence rseq = polyTable.getRowSequence();
        int ir = 0;
        while ( rseq.next() ) {
            Object[] row = rseq.getRow();
            int hpxLevel = ((Integer) row[ 0 ]).intValue();
            double[] vertices = (double[]) row[ 1 ];
            int[] inPixels = (int[]) row[ 2 ];
            checkPolygon( hpxLevel, vertices, inPixels );
            ir++;
        }
        rseq.close();
        assertTrue( ir > 0 );
    }

    /**
     * This test is redundant, since it duplicates the test done by looking
     * in the VOTable.  It's left here because it's harmless and may be
     * easier to verify visually than the VOTable one.
     */
    public void testPolygons() {
        checkPolygon( 6,
            new double[] {
                9.7,17.6, 354.4,16.8, 4.3,15.5,
                357.4,14.1, 6.4,12.4, 5.2,14.4, 9.4,17.3,
            },
            new int[] {
                19580, 19582, 19663, 19665, 19667, 19668, 19669, 19670,
                19671, 19672, 19673, 19674, 19676, 19677, 19679, 19685,
                19699, 19700, 19701, 19702, 19704, 19705, 19706, 19840,
                19841, 19842, 19843, 19844, 19845, 19846, 19848, 19849,
                19850, 20037,
            } );
        checkPolygon( 6,
            new double[] {
                144.7,78.2, 147.5,87.6, 222.7,76.5, 231.0,76.9, 214.9,88.0,
                344.7,81.3, 10.3,82.2, 39.6,88.7, 80.5,83.1, 92.5,83.8,
                120.7,85.7,
            },
            new int[] {
                3967, 4030, 4031, 4053, 4055, 4061, 4063, 4074, 4075, 4078,
                4079, 4085, 4087, 4090, 4091, 4093, 4094, 4095, 8046, 8132,
                8135, 8141, 8143, 8152, 8154, 8171, 8173, 8174, 8175, 8176,
                8177, 8178, 8179, 8182, 8183, 8184, 8185, 8186, 8187, 8188,
                8189, 8190, 8191, 12093, 12094, 12095, 12136, 12138, 12181,
                12224, 12225, 12226, 12227, 12230, 12233, 12235, 12236, 12238,
                12239, 12260, 12261, 12263, 12269, 12272, 12274, 12280, 12281,
                12282, 12283, 12286, 12287, 16247, 16252, 16253, 16254, 16255,
                16340, 16341, 16342, 16343, 16348, 16349, 16350, 16351, 16372,
                16373, 16375, 16381, 16382, 16383,
            } );
        checkPolygon( 7,
            new double[] {
                109.7,-71.7, 297.7,-74.3, 287.1,-74.5,
            },
            new int[] {
                131072, 131073, 131074, 131075, 131076, 131077, 131080,
                131088, 131089, 147456, 147458, 147464, 147465, 147466,
                147467, 147488, 147489, 147490, 147491, 147496, 147497,
                147498, 147499, 147500, 147502, 147584, 147585, 147586,
                147587, 147588, 147590, 147593, 147595, 147596, 147597,
                147598, 147599, 147620, 147621, 147622, 147623, 147628,
                147629, 147631, 147640, 147642, 147973, 147975, 147984,
                147986, 147992, 147993, 147994, 147995, 148016, 148017,
                148019, 148025, 148027, 148028, 148030, 148116, 148118,
                148124, 148125, 148127, 148149, 148200, 148202, 149571,
                149577, 149606, 163840, 180224, 180225, 180226, 180227,
                180228, 180229, 180230, 180231, 180232, 180233, 180234,
                180235, 180236, 180237, 180238, 180239, 180256, 180257,
                180259, 180260, 180261, 180262, 180263, 180265, 180267,
                180268, 180269, 180270, 180271, 180272, 180274, 180280,
                180282, 180283, 180353, 180356, 180357, 180358, 180359,
                180364, 180365, 180366, 180367, 180368, 180369, 180370,
                180371, 180376, 180377, 180378, 180379, 180380, 180382,
                180388, 180389, 180391, 180397, 180399, 180400, 180401,
                180402, 180403, 180404, 180406, 180407, 180408, 180409,
                180410, 180411, 180412, 180413, 180414, 180415, 180741,
                180752, 180753, 180754, 180755, 180756, 180757, 180758,
                180759, 180760, 180761, 180762, 180763, 180764, 180765,
                180766, 180767, 180784, 180785, 180787, 180788, 180789,
                180790, 180791, 180793, 180795, 180796, 180797, 180798,
                180799, 180800, 180802, 180808, 180810, 180811, 180832,
                180833, 180834, 180835, 180840, 180841, 180842, 180843,
                180844, 180846, 180881, 180884, 180885, 180886, 180887,
                180892, 180893, 180894, 180895, 180917, 180919, 180925,
                180927, 180928, 180929, 180930, 180931, 180932, 180934,
                180935, 180936, 180937, 180938, 180939, 180940, 180941,
                180942, 180943, 180960, 180961, 180962, 180963, 180964,
                180965, 180966, 180967, 180968, 180969, 180970, 180971,
                180972, 180973, 180974, 180976, 180978, 182336, 182337,
                182338,
            } );
    }

    private void checkPolygon( int hpxLevel, double[] vertices,
                               int[] hpxIndices ) {
        int nHpx = 12 << hpxLevel * 2;
        IntList ins = new IntList();
        for ( int ip = 0; ip < nHpx; ip++ ) {
            double lon = Tilings.healpixNestLon( hpxLevel, ip );
            double lat = Tilings.healpixNestLat( hpxLevel, ip );
            if ( Sky.inSkyPolygon( lon, lat, vertices ) ) {
                ins.add( ip );
            }
        }
        assertArrayEquals( hpxIndices, ins.toIntArray() );
    }

    /**
     * Creates a table with three columns:
     *   - vertices of a polygon as a lon,lat array
     *   - a HEALPix level
     *   - the indices of HEALPix pixels at given level whose centers appear
     *        in the polygon
     * This table is created by making TAP/ADQL queries to a geometry-aware
     * TAP service, hence uses a completely independent spherical
     * point-in-polygon implementation than the one being tested by
     * this class.
     */
    private static StarTable acquireShapesTable()
            throws IOException, TaskException {
        TestShape[] shapes = new TestShape[] {
            new TestShape( 6, new double[] {
                9.7,17.6, 354.4,16.8, 4.3,15.5,
                357.4,14.1, 6.4,12.4, 5.2,14.4, 9.4,17.3,
            } ),
            new TestShape( 6, new double[] {
                144.7,78.2, 147.5,87.6, 222.7,76.5, 231.0,76.9, 214.9,88.0,
                344.7,81.3, 10.3,82.2, 39.6,88.7, 80.5,83.1, 92.5,83.8,
                120.7,85.7,
            } ),
            new TestShape( 7, new double[] {
                40,7, 36,9, 30,8, 29,2, 39,2
            } ),
            new TestShape( 7, new double[] {
                109.7,-71.7, 297.7,-74.3, 287.1,-74.5,
            } ),
        };
        ColumnInfo[] infos = new ColumnInfo[] {
            new ColumnInfo( "hpx_level", Integer.class, null ),
            new ColumnInfo( "vertices", double[].class, null ),
            new ColumnInfo( "included_indices", int[].class, null ),
        };
        RowListStarTable table = new RowListStarTable( infos );
        for ( TestShape shape : shapes ) {
            table.addRow( new Object[] {
                new Integer( shape.hpxLevel_ ),
                shape.lonLats_,
                shape.queryIndices(),
            } );
        }
        return table;
    }

    private static class TestShape {
        final int hpxLevel_;
        final double[] lonLats_;
        TestShape( int hpxLevel, double[] lonLats ) {
            hpxLevel_ = hpxLevel;
            lonLats_ = lonLats;
        }
        int[] queryIndices() throws IOException, TaskException {
            String colname = "hpx" + hpxLevel_;
            int nHpx = 12 << 2 * hpxLevel_;
            int[] indices = new int[ nHpx ];
            for ( int i = 0; i < nHpx; i++ ) {
                indices[ i ] = i;
            }
            StringBuffer abuf = new StringBuffer()
                .append( "SELECT " )
                .append( colname )
                .append( "\n" )
                .append( "FROM tap_upload.up1" )
                .append( "\n" )
                .append( "WHERE 1=CONTAINS(POINT(ra, dec)," )
                .append( "\n" )
                .append( "  POLYGON(" );
            for ( int il2 = 0; il2 < lonLats_.length; il2 += 2 ) {
                if ( il2 > 0 ) {
                    abuf.append( "," );
                }
                abuf.append( lonLats_[ il2 + 0 ] )
                    .append( "," )
                    .append( lonLats_[ il2 + 1 ] );
            }
            abuf.append( "))" )
                .append( "\n" )
                .append( "ORDER BY " + colname );
            String adql = abuf.toString();
                
            ColumnStarTable pixTable =
                ColumnStarTable.makeTableWithRows( nHpx );
            pixTable.addColumn( ArrayColumn.makeColumn( colname, indices ) );
            MapEnvironment env = new MapEnvironment()
               .setValue( "tapurl", "http://dc.g-vo.org/tap" )
               .setValue( "sync", Boolean.TRUE )
               .setValue( "nupload", new Integer( 1 ) )
               .setValue( "upload1", pixTable )
               .setValue( "ucmd1",
                          "addcol ra healpixNestLon(" + hpxLevel_ + ","
                                                      + colname + ");"
                        + "addcol dec healpixNestLat(" + hpxLevel_ + ","
                                                       + colname + ")" )
               .setValue( "adql", adql );
            new TapQuerier().createExecutable( env ).execute();
            StarTable outTable = env.getOutputTable( "omode" );
            IntList outList = new IntList();
            RowSequence rseq = outTable.getRowSequence();
            while ( rseq.next() ) {
                outList.add( ((Number) rseq.getCell( 0 )).intValue() );
            }
            rseq.close();
            return outList.toIntArray();
        }
    }

    /**
     * Invoke this main method to create the shapes table against which
     * the polygon tests in this class run.
     */
    public static void main( String[] args ) throws Exception {
        StarTable table = acquireShapesTable();
        new VOTableWriter().writeStarTable( table, System.out );
    }
}
