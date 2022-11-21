package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.util.LogUtils;

public class TableGroupTest extends TableTestCase {

    public TableGroupTest() {
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.util" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
    }

    public void testGaussian()
            throws IOException, TaskException {
        RowRunner[] runners = {
            RowRunner.SEQUENTIAL, RowRunner.DEFAULT, RowRunner.PARTEST,
        };
        for ( RowRunner runner : runners ) {
            MapEnvironment env = new MapEnvironment();
            env.setValue( "in", ":loop:1000" );
            env.setValue( "icmd", "addcol ir round(randomGaussian($0))" );
            env.setValue( "keys", "ir" );
            env.setValue( "aggcols", "0;count" );
            env.setValue( "runner", runner );
            checkGaussianCounts( process( new TableGroup(), env ) );
        }
    }

    public void testMessier() throws IOException, TaskException {
        StarTable messier =
            new StarTableFactory()
           .makeStarTable( TableTestCase.class
                          .getResource( "messier.xml" ).toString() );
        MapEnvironment envUniq = new MapEnvironment();
        envUniq.setValue( "in", messier );
        envUniq.setValue( "cmd",
                          String.join( ";",
                              "sort 'con type'",
                              "uniq -count 'con type'",
                              "colmeta -name ngal $1",
                              "keepcols 'con type ngal'",
                              "sort -down 'ngal con type'"
                           ) );
        StarTable tuniq = process( new TablePipe(), envUniq );

        MapEnvironment envFilter = new MapEnvironment();
        envFilter.setValue( "in", messier );
        envFilter.setValue( "cmd",
                            String.join( ";",
                                "keepcols 'con type'",
                                "group con type null@count@ngal",
                                "sort -down 'ngal con type'"
                            ) );
        StarTable tfilter = process( new TablePipe(), envFilter );

        MapEnvironment envCount = new MapEnvironment();
        envCount.setValue( "in", messier );
        envCount.setValue( "keys", "con type" );
        envCount.setValue( "aggcols", "null;count;ngal" );
        envCount.setValue( "sort", Boolean.FALSE );
        envCount.setValue( "ocmd", "sort -down 'ngal con type'" );
        StarTable tcount = process( new TableGroup(), envCount );

        assertSameData( tuniq, tcount );
        assertSameData( tuniq, tfilter );

        MapEnvironment env2 = new MapEnvironment();
        env2.setValue( "in", messier );
        env2.setValue( "icmd",
                       "replacecol Type $0%2==0?NULL:parseInt(\\\"\\\"+Type)" );
        env2.setValue( "keys", "Con" );
        env2.setValue( "aggcols",
                       String.join( " ",
                           "null;count",
                           "type;max",
                           "type;min;minnie_type",
                           "type;mean;mean_type",
                           "type;median;median_type"
                       ) );
        env2.setValue( "ocmd", "sorthead -down 10 count" );
        StarTable t2 = process( new TableGroup(), env2 );
        assertArrayEquals( new String[] { "Con", "count",
                                          "max_Type", "minnie_type",
                                          "mean_type", "median_type", },
                           getColNames( t2 ) );
        Object[] rowSgr = t2.getRow( 0 );
        Object[] rowVir = t2.getRow( 1 );
        Object[] rowCom = t2.getRow( 2 );
        assertArrayEquals( new Object[] { "Sgr", 15, 4, 1, 13./7., 2., },
                           rowSgr );
        assertArrayEquals( new Object[] { "Vir", 11, 6, 5, 29./5., 6., },
                           rowVir );
        assertArrayEquals( new Object[] { "Com", 8, 8, 2, 5., 5., },
                           rowCom );
    }

    public void testArrayStats() throws IOException, TaskException {
        MapEnvironment env0 = new MapEnvironment();
        env0.setValue( "in", ":skysim:1000" );
        env0.setValue( "cmd", "addcol idec (int)abs(dec)" );
        StarTable t0 = process( new TablePipe(), env0 );

        MapEnvironment env1 = new MapEnvironment();
        env1.setValue( "in", t0 );
        env1.setValue( "keys", "idec" );
        env1.setValue( "aggcols", String.join( " ",
                           "gmag;count;count_g",
                           "gmag;ngood;ngood_g",
                           "gmag;count-long;countlong_g",
                           "gmag;ngood-long;ngoodlong_g",
                           "gmag;sum;sum_g",
                           "gmag;mean;mean_g",
                           "gmag;stdev-pop;stdev_g",
                           "gmag;median;median_g",
                           "gmag;max;max_g",
                           "gmag;min;min_g" ) );
        env1.setValue( "ocmd", String.join( ";",
                           "replacecol sum_g (float)sum_g",
                           "replacecol mean_g (float)mean_g",
                           "replacecol stdev_g (float)stdev_g",
                           "replacecol median_g (float)median_g",
                           "replacecol max_g (float)max_g",
                           "replacecol min_g (float)min_g" ) );
        StarTable t1 = process( new TableGroup(), env1 );

        MapEnvironment env2 = new MapEnvironment();
        env2.setValue( "in", t0 );
        env2.setValue( "keys", "idec" );
        env2.setValue( "aggcols", "gmag;array;ga" );
        env2.setValue( "ocmd", String.join( ";",
                          "addcol count_g size(ga)",
                          "addcol ngood_g count(ga)",
                          "addcol countlong_g (long)size(ga)",
                          "addcol ngoodlong_g (long)count(ga)",
                          "addcol sum_g (float)sum(ga)",
                          "addcol mean_g (float)mean(ga)",
                          "addcol stdev_g (float)stdev(ga)",
                          "addcol median_g (float)median(ga)",
                          "addcol max_g (float)maximum(ga)",
                          "addcol min_g (float)minimum(ga)",
                          "delcols ga" ) );
        StarTable t2 = process( new TableGroup(), env2 );

        assertSameData( t1, t2 );
    }

    public void testArray() throws IOException, TaskException {
        StringBuffer abuf = new StringBuffer();
        String[] types = { "byte", "short", "int", "long", "float", "double",
                           "string", "boolean", };
        String[] acs = { "count", "ngood", "array", "array-withblanks" };
        for ( String type : types ) {
            abuf.append( " s_" + type + ";count;count0_" + type )
                .append( " s_" + type + ";ngood;count1_" + type )
                .append( " s_" + type + ";count-long;lcount0_" + type )
                .append( " s_" + type + ";ngood-long;lcount1_" + type )
                .append( " s_" + type + ";array-withblanks;array0_" + type )
                .append( " s_" + type + ";array;array1_" + type );
        }
        String aggcols = abuf.toString().replaceFirst( "^ *", "" );
        MapEnvironment env = new MapEnvironment();
        env.setValue( "in", ":test:9,s" );
        env.setValue( "icmd", "addcol key 0" );
        env.setValue( "keys", "key" );
        env.setValue( "aggcols", aggcols );
        env.setValue( "ocmd", "delcols key" );
        StarTable t = process( new TableGroup(), env );
        Object[] row0 = t.getRow( 0 );
        Map<String,Object> outMap = new LinkedHashMap<>();
        for ( int ic = 0; ic < t.getColumnCount(); ic++ ) {
            outMap.put( t.getColumnInfo( ic ).getName(), row0[ ic ] );
        }
        String string8 = ((String[])outMap.get( "array0_string" ))[ 8 ];
        assertTrue( string8.startsWith( "'" ) &&
                    string8.endsWith( ">" ) );  // is funny
        for ( String type : types ) {
            assertEquals( 9, outMap.get( "count0_" + type ) );
            assertEquals( 8, outMap.get( "count1_" + type ) );
            assertEquals( 9L, outMap.get( "lcount0_" + type ) );
            assertEquals( 8L, outMap.get( "lcount1_" + type ) );
        }
        assertArrayEquals( new byte[] { 0, 0, 2, 3, 4, 5, 6, 7, 8 },
                           outMap.get( "array0_byte" ) );
        assertArrayEquals( new byte[] { 0, 2, 3, 4, 5, 6, 7, 8 },
                           outMap.get( "array1_byte" ) );
        assertArrayEquals( new short[] { 0, 1, 0, 3, 4, 5, 6, 7, 8 },
                           outMap.get( "array0_short" ) );
        assertArrayEquals( new short[] { 0, 1, 3, 4, 5, 6, 7, 8 },
                           outMap.get( "array1_short" ) );
        assertArrayEquals( new int[] { 0, 1, 2, 0, 4, 5, 6, 7, 8 },
                           outMap.get( "array0_int" ) );
        assertArrayEquals( new int[] { 0, 1, 2, 4, 5, 6, 7, 8 },
                           outMap.get( "array1_int" ) );
        assertArrayEquals( new long[] { 0, 1, 2, 3, 0, 5, 6, 7, 8 },
                           outMap.get( "array0_long" ) );
        assertArrayEquals( new long[] { 0, 1, 2, 3, 5, 6, 7, 8 },
                           outMap.get( "array1_long" ) );
        assertArrayEquals( new float[] { 0, 1, 2, 3, 4, Float.NaN, 6, 7, 8 },
                           outMap.get( "array0_float" ) );
        assertArrayEquals( new float[] { 0, 1, 2, 3, 4, 6, 7, 8 },
                           outMap.get( "array1_float" ) );
        assertArrayEquals( new double[] { 0, 1, 2, 3, 4, 5, Double.NaN, 7, 8 },
                           outMap.get( "array0_double" ) );
        assertArrayEquals( new double[] { 0, 1, 2, 3, 4, 5, 7, 8 },
                           outMap.get( "array1_double" ) );
        assertArrayEquals( new String[] { "zero", "one", "two", "three", "four",
                                          "five", "six", null, string8 },
                           outMap.get( "array0_string" ) );
        assertArrayEquals( new String[] { "zero", "one", "two", "three", "four",
                                          "five", "six", string8 },
                           outMap.get( "array1_string" ) );
        assertArrayEquals( new boolean[] { false, true, false, true,
                                           false, true, false, true, false },
                           outMap.get( "array0_boolean" ) );
        assertArrayEquals( new boolean[] { false, true, false, true,
                                           false, true, false, true },
                           outMap.get( "array1_boolean" ) );
    }

    private void checkGaussianCounts( StarTable t ) throws IOException {
        int[] keys = (int[]) unbox( getColData( t, 0 ) );
        int[] counts = (int[]) unbox( getColData( t, 1 ) );
        assertArrayEquals( new int[] { -3, -2, -1, 0, 1, 2, 3 }, keys );
        assertEquals(  61., counts[ 1 ], 10. );
        assertEquals( 241., counts[ 2 ], 10. );
        assertEquals( 383., counts[ 3 ], 10. );
        assertEquals( 241., counts[ 4 ], 10. );
        assertEquals(  61., counts[ 5 ], 10. );
    }

    private StarTable process( SingleMapperTask task, MapEnvironment env )
            throws IOException, TaskException {
        task.createExecutable( env ).execute();
        StarTable result = env.getOutputTable( "omode" );
        if ( result != null ) {
            Tables.checkTable( result );
        }
        return result;
    }
}
