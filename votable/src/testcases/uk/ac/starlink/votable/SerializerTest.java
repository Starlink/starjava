package uk.ac.starlink.votable;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.PrimitiveArrayColumn;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.TestCase;

public class SerializerTest extends TestCase {

    public SerializerTest( String name ) {
        super( name );
    }

    public void testSerializers() throws IOException, SAXException {
        URL votloc = getClass().getResource( "docexample.xml" );

        int nrow = 23;
        int[] cd1 = new int[ nrow ];
        float[] cd2 = new float[ nrow ];
        String[] cd3 = new String[ nrow ];
        for ( int i = 0; i < nrow; i++ ) {
            cd1[ i ] = i;
            cd2[ i ] = (float) i;
            cd3[ i ] = "row " + i;
        }
        ColumnInfo ci1 = new ColumnInfo( "ints", Integer.class, null );
        ColumnInfo ci2 = new ColumnInfo( "floats", Float.class, null );
        ColumnInfo ci3 = new ColumnInfo( "strings", String.class, null );
        ColumnStarTable table0 = ColumnStarTable.makeTableWithRows( nrow );
        table0.addColumn( ArrayColumn.makeColumn( ci1, cd1 ) );
        table0.addColumn( ArrayColumn.makeColumn( ci2, cd2 ) );
        table0.addColumn( ArrayColumn.makeColumn( ci3, cd3 ) );
        int ncol = table0.getColumnCount();

        VOSerializer tSer = VOSerializer
                           .makeSerializer( DataFormat.TABLEDATA, table0 );
        VOSerializer bSer = VOSerializer
                           .makeSerializer( DataFormat.BINARY, table0 );
        VOSerializer fSer = VOSerializer
                           .makeSerializer( DataFormat.FITS, table0 );

        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
        BufferedWriter writer = 
            new BufferedWriter( new OutputStreamWriter( bytestream ) );

        writer.write( "<!DOCTYPE VOTABLE SYSTEM " +
                      "'http://us-vo.org/xml/VOTable.dtd'>" );
        writer.newLine();
        writer.write( "<VOTABLE version='1.0'>" );
        writer.newLine();
        writer.write( "<RESOURCE>" );
        writer.newLine();

        writeTableInline( tSer, writer );
        writeTableInline( bSer, writer );
        writeTableInline( fSer, writer );
        writeTableHref( bSer, writer, File.createTempFile( "stest", ".bin" ) );
        writeTableHref( fSer, writer, File.createTempFile( "stest", ".fits" ) );

        /* Can't write TABLEDATA as a stream. */
        try {
            tSer.writeHrefDataElement( writer, "/dev/null", null );
            fail();
        }
        catch ( UnsupportedOperationException e ) {
        }

        writer.write( "</RESOURCE>" );
        writer.newLine();
        writer.write( "</VOTABLE>" );
        writer.close();
        byte[] xmltext = bytestream.toByteArray();
        // new FileOutputStream( "j" ).write( xmltext );

        assertValidXML( new ByteArrayInputStream( xmltext ) );

        VOTable vodoc =
            new VOTable( new ByteArrayInputStream( xmltext ), true );

        VOElement res = vodoc.getChildByName( "RESOURCE" );
        VOElement[] tables = res.getChildren();
        assertEquals( tables.length, res.getChildrenByName( "TABLE" ).length );
        assertEquals( 5, tables.length );

        for ( int itab = 0; itab < tables.length; itab++ ) {
            Table votab = (Table) tables[ itab ];
            RowSequence rseq = table0.getRowSequence();
            assertEquals( ncol, votab.getColumnCount() );
            for ( int irow = 0; votab.hasNextRow(); irow++ ) {
                rseq.next();
                assertArrayEquals( rseq.getRow(), votab.nextRow() );
            }
            assertTrue( ! rseq.hasNext() );
            assertTrue( ! votab.hasNextRow() );
        }

        Table tTabIn = (Table) tables[ 0 ];
        Table bTabIn = (Table) tables[ 1 ];
        Table fTabIn = (Table) tables[ 2 ];
        Table bTabEx = (Table) tables[ 3 ];
        Table fTabEx = (Table) tables[ 4 ];

        VOElement bStrIn = bTabIn.getChildByName( "DATA" )
                                 .getChildByName( "BINARY" )
                                 .getChildByName( "STREAM" );
        VOElement fStrIn = fTabIn.getChildByName( "DATA" )
                                 .getChildByName( "FITS" )
                                 .getChildByName( "STREAM" );
        VOElement bStrEx = bTabEx.getChildByName( "DATA" )
                                 .getChildByName( "BINARY" )
                                 .getChildByName( "STREAM" );
        VOElement fStrEx = fTabEx.getChildByName( "DATA" )
                                 .getChildByName( "FITS" )
                                 .getChildByName( "STREAM" );

        assertNull( bStrIn.getAttribute( "href" ) );
        assertNull( fStrIn.getAttribute( "href" ) );
        assertNotNull( bStrEx.getAttribute( "href" ) );
        assertNotNull( fStrEx.getAttribute( "href" ) );

        assertEquals( "base64", bStrIn.getAttribute( "encoding" ) );
        assertEquals( "base64", fStrIn.getAttribute( "encoding" ) );
        assertTrue( "none".equals( bStrEx.getAttribute( "encoding" ) ) ||
                    null == bStrEx.getAttribute( "encoding" ) );
        assertTrue( "none".equals( fStrEx.getAttribute( "encoding" ) ) ||
                    null == fStrEx.getAttribute( "encoding" ) );
    }

    private void writeTableInline( VOSerializer ser, BufferedWriter writer ) 
            throws IOException {
        writer.write( "<TABLE>" );
        writer.newLine();
        ser.writeFields( writer );
        ser.writeInlineDataElement( writer );
        writer.write( "</TABLE>" );
        writer.newLine();
    }

    private void writeTableHref( VOSerializer ser, BufferedWriter writer,
                                 File file ) throws IOException {
        writer.write( "<TABLE>" );
        writer.newLine();
        ser.writeFields( writer );
        DataOutputStream out =
            new DataOutputStream(
                new BufferedOutputStream(
                    new FileOutputStream( file ) ) );
        ser.writeHrefDataElement( writer, file.toURI().toString(), out );
        out.close();
        writer.write( "</TABLE>" );
        file.deleteOnExit();
    }
}
