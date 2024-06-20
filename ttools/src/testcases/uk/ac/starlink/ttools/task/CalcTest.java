package uk.ac.starlink.ttools.task;

import gnu.jel.CompilationException;
import junit.framework.TestCase;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.QuickTable;

public class CalcTest extends TestCase {

    public CalcTest( String name ) {
        super( name );
    }

    public void testCalc() throws Exception {
        assertEquals( "3", eval( "1+2" ) );
        assertEquals( "53729.0", eval( "isoToMjd(\"2005-12-25T00:00:00\")" ) );

        assertEquals( "One2Three4Five999",
                      eval( "concat(\"One\", "
                                 + "toString((int)2), "
                                 + "\"Three\", "
                                 + "toString(4.0), "
                                 + "\"Five\", "
                                 + "null, "
                                 + "toString(999L))" ) );

        assertEquals( "One2Three4.0Five999",
                      eval( "concat(\"One\", "
                                 + "2, "
                                 + "\"Three\", "
                                 + "4.0, "
                                 + "\"Five\", "
                                 + "null, "
                                 + "999L)" ) );

        assertEquals( "One, 2, Three, 4.0, Five, null, 999",
                      eval( "join(\", \", "
                                + "\"One\", "
                                + "2, "
                                + "\"Three\", "
                                + "4.0, "
                                + "\"Five\", "
                                + "null, "
                                + "999L)" ) );
    }

    public void testWithTable() throws Exception {
        StarTable table = new QuickTable( 4, new ColumnData[] {
            ArrayColumn.makeColumn( "a", new int[] { 1, 2, 3, 4, } ),
        } );
        DefaultValueInfo wibInfo =
            new DefaultValueInfo( "WIBBLENESS", Integer.class, "Who knows?" );
        wibInfo.setUCD( "meta.cryptic;arith.factor" );
        table.setParameter( new DescribedValue( wibInfo,
                                                Integer.valueOf( 23 ) ) );
        assertEquals( "5", eval( "2+3", table ) );
        assertEquals( "46", eval( "param$wibbleness + param$wibbleness",
                                  table ) );
        assertEquals( "46", eval( "ucd$meta_cryptic_arith_factor + "
                                + "ucd$meta_cryptic_arith_factor", table ));
        assertEquals( "46", eval( "ucd$meta_cryptic_ + ucd$meta_cryptic_",
                                  table ) );
        try {
            eval( "7 + ucd$meta_cryptic" );
            fail();
        }
        catch ( TaskException e ) {
            assertTrue( e.getCause() instanceof CompilationException );
        }

        assertEquals( "false", eval( "NULL_param$wibbleness", table ) );
        assertEquals( "false", eval( "NULL_ucd$meta_cryptic_", table ) );
        assertEquals( "false", eval( "NULL_ucd$meta_cryptic_arith_factor",
                                     table ) );
        table.setParameter( new DescribedValue( wibInfo, null ) );
        assertEquals( "true", eval( "NULL_param$wibbleness", table ) );
        assertEquals( "true", eval( "NULL_ucd$meta_cryptic_", table ) );
        assertEquals( "true", eval( "null_ucd$meta_cryptic_arith_factor",
                                    table ) );
    }

    public void testError() throws Exception {
        try {
            eval( "do what?" );
            fail();
        }
        catch ( TaskException e ) {
        }
    }

    private Object eval( String expr ) throws Exception {
        MapEnvironment env = new MapEnvironment()
                      .setValue( "expression", expr );
        new Calc().createExecutable( env ).execute();
        return env.getOutputText().trim();
    }

    private Object eval( String expr, StarTable table ) throws Exception {
        MapEnvironment env = new MapEnvironment()
                            .setValue( "expression", expr );
        if ( table != null ) {
            env.setValue( "table", table );
        }
        new Calc().createExecutable( env ).execute();
        return env.getOutputText().trim();
    }
}
