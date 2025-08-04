package uk.ac.starlink.table.formats;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.util.DataSource;

public class TextTest extends TestCase {

    public TextTest( String name ) {
        super( name );
    }

    public void testReader() throws IOException {
        StarTableFactory tfact = new StarTableFactory();
        RowEvaluator.Decoder<?>[] decoders64 = new RowEvaluator.Decoder<?>[] {
            RowEvaluator.BOOLEAN_DECODER,
            RowEvaluator.LONG_DECODER,
            RowEvaluator.DOUBLE_DECODER,
            RowEvaluator.STRING_DECODER,
        };

        DataSource asciiDatsrc = createExampleDataSource( false );
        exerciseReader( new AsciiTableBuilder(), asciiDatsrc, false );
        exerciseReader( tfact.getTableBuilder( "ascii" ), asciiDatsrc, false );
        exerciseReader( tfact
                       .getTableBuilder( "ascii(notypes=short;int;float)" ),
                        asciiDatsrc, true );
        AsciiTableBuilder asciiHandler = new AsciiTableBuilder();
        asciiHandler.setDecoders( decoders64 );
        exerciseReader( asciiHandler, asciiDatsrc, true );

        DataSource csvDatsrc = createExampleDataSource( true );
        exerciseReader( new CsvTableBuilder(), csvDatsrc, false );
        exerciseReader( tfact.getTableBuilder( "csv" ), csvDatsrc, false );
        exerciseReader( tfact.getTableBuilder( "csv(notypes=short;int;float)" ),
                        csvDatsrc, true );
        CsvTableBuilder csvHandler = new CsvTableBuilder();
        csvHandler.setDecoders( decoders64 );
        exerciseReader( csvHandler, csvDatsrc, true );
    }

    private void exerciseReader( TableBuilder builder, DataSource datsrc,
                                 boolean is64 )
            throws IOException {
        StarTable table =
            builder.makeStarTable( datsrc, true, StoragePolicy.PREFER_MEMORY );
        assertEquals( 7, table.getColumnCount() );
        assertEquals( 5, table.getRowCount() );
        if ( ! ( builder instanceof CsvTableBuilder ) ) {
            assertTrue( ((String) table.getParameterByName( "Description" )
                                 .getValue()).indexOf( "TEST_TABLE" ) >= 0 );
        }

        for ( int icol = 0; icol < table.getColumnCount(); icol++ ) {
            ColumnInfo cinfo = table.getColumnInfo( icol );
            Class clazz = cinfo.getContentClass();
            String name = cinfo.getName();
            switch ( icol ) {
                case 0:
                    assertEquals( is64 ? Long.class : Short.class, clazz );
                    assertEquals( "Short", name );
                    break;
                case 1:
                    assertEquals( is64 ? Long.class : Integer.class, clazz );
                    assertEquals( "Integer", name );
                    break;
                case 2:
                    assertEquals( is64 ? Double.class : Float.class, clazz );
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
                case 6:
                    assertEquals( "blank", name );
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

    private static DataSource createExampleDataSource( boolean isCsv ) {
        List<String> lines = new ArrayList<>();
        if ( !isCsv ) {
            lines.addAll( Arrays.asList(
                "# TEST_TABLE",
                "#"
            ) );
        }
        lines.add(
              isCsv
            ? "Short,Integer,Float,Double,Boolean,String,blank" 
            : "# Short  Integer  Float    Double       Boolean   String blank"
        );
        if ( !isCsv ) {
            lines.add( "#" );
        }
        for ( String dataLine : new String[] {
                "      1    12  0.1000     3.14        true    123    ''",
                "     -1   -12 -0.1000    -3.14        false   ''     ''",
                "  31000 33000  3.1415e00  3.14159265  false   3f1415 ''",
                "  31000 33000  3.1415d0   3.14159265  false   3f1415 ''",
              } ) {
            lines.add( isCsv ? dataLine.trim().replaceAll( " +", "," )
                             : dataLine );
        }
        lines.add( isCsv
            ? ",,,,,,"
            : "     ''       ''  ''         ''            \"\"      ' '  ''  "
        );
        StringBuffer sbuf = new StringBuffer();
        for ( String line : lines ) {
            sbuf.append( line )
                .append( '\n' );
        }
        final byte[] bbuf = sbuf.toString().getBytes();
        return new DataSource() {
            protected InputStream getRawInputStream() {
                return new ByteArrayInputStream( bbuf );
            }
        };
    }
}
