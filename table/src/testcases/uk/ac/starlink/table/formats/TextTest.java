package uk.ac.starlink.table.formats;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import junit.framework.TestCase;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.DataSource;

public class TextTest extends TestCase {

    public TextTest( String name ) {
        super( name );
    }

    public void testReader() throws IOException {
        StarTable table = new AsciiTableBuilder()
                         .makeStarTable( new TableDataSource(), true,
                                         StoragePolicy.PREFER_MEMORY );

        assertEquals( 6, table.getColumnCount() );
        assertEquals( 5, table.getRowCount() );
        assertTrue( ((String) table.getParameterByName( "Description" )
                             .getValue()).indexOf( "TEST_TABLE" ) >= 0 );

        for ( int icol = 0; icol < table.getColumnCount(); icol++ ) {
            ColumnInfo cinfo = table.getColumnInfo( icol );
            Class clazz = cinfo.getContentClass();
            String name = cinfo.getName();
            switch ( icol ) {
                case 0:
                    assertEquals( Short.class, clazz );
                    assertEquals( "Short", name );
                    break;
                case 1:
                    assertEquals( Integer.class, clazz );
                    assertEquals( "Integer", name );
                    break;
                case 2:
                    assertEquals( Float.class, clazz );
                    assertEquals( "Float", name );
                    break;
                case 3:
                    assertEquals( Double.class, clazz );
                    assertEquals( "Double", name );
                    break;
                case 4:
                    assertEquals( Boolean.class, clazz );
                    assertEquals( "Boolean", name );
                    assertEquals( -1, cinfo.getElementSize() );
                    break;
                case 5:
                    assertEquals( String.class, clazz );
                    assertEquals( "String", name );
                    assertEquals( 6, cinfo.getElementSize() );
                    break;
                default:
                    fail();
            }
        }
    }

    public void testLengths() {
        AbstractTextTableWriter twriter = new TextTableWriter();
        assertTrue( Double.toString( -Double.MIN_NORMAL ).length()
                  <= twriter.getMaxDataWidth( Double.class ) );
        assertTrue( Float.toString( -Float.MIN_NORMAL ).length()
                  <= twriter.getMaxDataWidth( Float.class ) );
    }

    private static class TableDataSource extends DataSource {
        final byte[] bbuf;
        TableDataSource() {
            String[] tableData = new String[] { 
                "# TEST_TABLE",
                "#",
                "# Short  Integer   Float     Double        Boolean   String",
                "#",
                "      1       12  0.1000     3.14          true      123   ",
                "     -1      -12 -0.1000    -3.14          false     ''    ",
                "  31000    33000  3.1415e00  3.14159265    false     3f1415 ",
                "  31000    33000  3.1415d0   3.14159265    false     3f1415 ",
                "     ''       ''  ''         ''            \"\"      ' '  ",
            };
            StringBuffer sbuf = new StringBuffer();
            for ( int i = 0; i < tableData.length; i++ ) {
                sbuf.append( tableData[ i ] )
                    .append( '\n' );
            }
            bbuf = sbuf.toString().getBytes();
        }
     
        protected InputStream getRawInputStream() {
            return new ByteArrayInputStream( bbuf );
        }
    }
}
