package uk.ac.starlink.votable;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.PrimitiveArrayColumn;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.TestCase;

public class SerializerTest extends TestCase {

    StarTable table0;

    public SerializerTest( String name ) {
        super( name );
    }

    public void setUp() {
        int nrow = 23;
        int[] cd1 = new int[ nrow ];
        float[] cd2 = new float[ nrow ];
        String[] cd3 = new String[ nrow ];
        for ( int i = 0; i < nrow; i++ ) {
            int j = i + 1;
            cd1[ i ] = j;
            cd2[ i ] = (float) j;
            cd3[ i ] = "row " + j;
        }
        ColumnInfo ci1 = new ColumnInfo( "ints", Integer.class, null );
        ColumnInfo ci2 = new ColumnInfo( "floats", Float.class, null );
        ColumnInfo ci3 = new ColumnInfo( "strings", String.class, null );
        ColumnStarTable t0 = ColumnStarTable.makeTableWithRows( nrow );
        t0.addColumn( ArrayColumn.makeColumn( ci1, cd1 ) );
        t0.addColumn( ArrayColumn.makeColumn( ci2, cd2 ) );
        t0.addColumn( ArrayColumn.makeColumn( ci3, cd3 ) );
        table0 = t0;
    }
        
    public void testURLs() 
            throws IOException, TransformerException, SAXException {
        exerciseStreamSerializer( VOSerializer
                                 .makeSerializer( DataFormat.BINARY, table0 ) );
        exerciseStreamSerializer( VOSerializer
                                 .makeSerializer( DataFormat.FITS, table0 ) );
    }

    private void exerciseStreamSerializer( VOSerializer ser ) 
            throws IOException, TransformerException, SAXException {

        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
        BufferedWriter writer = 
            new BufferedWriter( new OutputStreamWriter( bytestream ) );

        File tmpDir = new File( System.getProperty( "java.io.tmpdir" ) );
        File tmpFile = File.createTempFile( "stest", ".dat", tmpDir );

        DataOutputStream out = 
            new DataOutputStream( new FileOutputStream( tmpFile ) );

        /* Write an XML document with out-of-line streamed tabular data,
         * using a relative href, so it will only work if the system ID
         * is set right. */
        writer.write( "<RESOURCE><TABLE>" );
        ser.writeFields( writer );
        ser.writeHrefDataElement( writer, tmpFile.getName(), out );
        out.close();
        writer.write( "</TABLE></RESOURCE>" );
        tmpFile.deleteOnExit();
        writer.close();
        byte[] xmltext = bytestream.toByteArray();

        String systemId = new File( tmpDir, "unfile.xml" ).toURI().toString();
        checkOKSource( 
            new StreamSource( new ByteArrayInputStream( xmltext ), systemId ) );
        checkBadSource(
            new StreamSource( new ByteArrayInputStream( xmltext ) ) );
        Document doc = (Document) new SourceReader()
           .getDOM( new StreamSource( new ByteArrayInputStream( xmltext ) ) );
        checkOKSource( new DOMSource( doc, systemId ) );
        checkBadSource( new DOMSource( doc, null ) );
    }

    private void checkOKSource( Source xsrc ) throws SAXException, IOException {
        VOElement res = VOElementFactory.makeVOElement( xsrc );
        TableElement table = (TableElement) res.getChildByName( "TABLE" );
        checkTableValues( table );
    }

    private void checkBadSource( Source xsrc )
            throws TransformerException, IOException {
        try {
            VOElement res = VOElementFactory.makeVOElement( xsrc );
            TableElement table = (TableElement) res.getChildByName( "TABLE" );
            RowStepper rstep = table.getData().getRowStepper();
            assertNull( "If the table can be constructed, " +
                        "it should have no rows", rstep.nextRow() );
        }
        catch ( FileNotFoundException e ) {
        }
        catch ( SAXException e ) {
        }
    }

    public void testSerializers()
            throws IOException, SAXException, TransformerException {

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
        File tmpDir = new File( System.getProperty( "java.io.tmpdir" ) );
        File bTempFile = File.createTempFile( "stest", ".bin", tmpDir );
        File fTempFile = File.createTempFile( "stest", ".fits", tmpDir );
        writeTableHref( bSer, writer, bTempFile );
        writeTableHref( fSer, writer, fTempFile );

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

        /* Test all constructors, validating and not.  There are significantly
         * different paths through the code for each one, in particular 
         * depending on whether the parse to DOM is done inside or outside
         * the VOTable package. */
        List vodocs = new ArrayList();
        Document docnode1 = (Document) new SourceReader()
                           .getDOM( new StreamSource( asStream( xmltext ) ) );
        vodocs.add( VOElementFactory
                   .makeVOElement( new ByteArrayInputStream( xmltext ), 
                                   null ) );
        vodocs.add( VOElementFactory.makeVOElement( docnode1, null ) );
        Source xsrc = new DOMSource( docnode1, null );
        vodocs.add( VOElementFactory.makeVOElement( (Source) xsrc ) );
        vodocs.add( VOElementFactory.makeVOElement( (DOMSource) xsrc ) );
        DOMSource dsrc0 = VOElementFactory.
            transformToDOM( new StreamSource( asStream( xmltext ) ), false );
        DOMSource dsrc1 = VOElementFactory.
            transformToDOM( new StreamSource( asStream( xmltext ) ), true );
        vodocs.add( VOElementFactory.makeVOElement( dsrc0 ) );
        vodocs.add( VOElementFactory.makeVOElement( dsrc1 ) );
        for ( Iterator it = vodocs.iterator(); it.hasNext(); ) {
            exerciseVOTableDocument( (VOElement) it.next() );
        }

        /* Test the streamStarTable method; get each table in turn. */
        for ( int itable = 0; itable < 5; itable++ ) {
            RowStore rstore = new RowStore();
            InputSource saxsrc = 
                new InputSource( new ByteArrayInputStream( xmltext ) );
            TableStreamer.streamStarTable( saxsrc, rstore, itable );

            RowStepper rstep = rstore.getRowStepper();
            RowSequence rseq = table0.getRowSequence();
            while ( rseq.hasNext() ) {
                rseq.next();
                assertArrayEquals( rseq.getRow(), rstep.nextRow() );
            }
            assertTrue( ! rseq.hasNext() );
            assertNull( rstep.nextRow() );
        }

        try {
            TableStreamer.streamStarTable(
                       new InputSource( new ByteArrayInputStream( xmltext ) ),
                       new RowStore(), 5 );
            fail();
        }
        catch ( SAXException e ) {
            // ok
        }

    }

    private static InputStream asStream( byte[] text ) {
        return new ByteArrayInputStream( text );
    }

    private void exerciseVOTableDocument( VOElement vodoc ) throws IOException {
        int ncol = table0.getColumnCount();

        VOElement res = vodoc.getChildByName( "RESOURCE" );
        VOElement[] tables = res.getChildren();
        assertArrayEquals( tables, res.getChildrenByName( "TABLE" ) );
        assertArrayEquals( tables, res.getDescendantsByName( "TABLE" ) );
        assertEquals( 5, tables.length );

        for ( int itab = 0; itab < tables.length; itab++ ) {
            checkTableValues( (TableElement) tables[ itab ] );
        }

        TableElement tTabIn = (TableElement) tables[ 0 ];
        TableElement bTabIn = (TableElement) tables[ 1 ];
        TableElement fTabIn = (TableElement) tables[ 2 ];
        TableElement bTabEx = (TableElement) tables[ 3 ];
        TableElement fTabEx = (TableElement) tables[ 4 ];

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

    private void checkTableValues( TableElement votab ) throws IOException {
            
        RowSequence rseq = table0.getRowSequence();
        int ncol = table0.getColumnCount();
        assertEquals( ncol, votab.getColumnCount() );
        RowStepper rstep = votab.getData().getRowStepper();

        Object[] row;
        int irow = 0;
        for ( Object[] rowCells; ( rowCells = rstep.nextRow() ) != null; ) {
            rseq.next();
            assertArrayEquals( rseq.getRow(), rowCells );
            irow++;
        }
        assertEquals( table0.getRowCount(), irow );
        assertTrue( ! rseq.hasNext() );
        assertNull( rstep.nextRow() );
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
        ser.writeHrefDataElement( writer, file.toString(), out );
        out.close();
        writer.write( "</TABLE>" );
        file.deleteOnExit();
    }

    private static class RowStore implements TableSink {
        List rows = new ArrayList();
        Class[] classes;
        int ncol;
        int nrow;
        
        public void acceptMetadata( StarTable meta ) {
            ncol = meta.getColumnCount();
            classes = new Class[ ncol ];
            for ( int icol = 0; icol < ncol; icol++ ) {
                classes[ icol ] = meta.getColumnInfo( icol ).getContentClass();
            }
            nrow = (int) meta.getRowCount();
        }

        public void acceptRow( Object[] row ) {
            assertEquals( ncol, row.length );
            for ( int icol = 0; icol < ncol; icol++ ) {
                if ( row[ icol ] != null ) {
                    assertEquals( classes[ icol ], row[ icol ].getClass() );
                }
            }
            rows.add( row );
        }

        public void endRows() {
            if ( nrow >= 0 ) {
               assertEquals( nrow, rows.size() );
            }
        }

        public RowStepper getRowStepper() {
            return new RowStepper() {
                int irow = 0;
                public Object[] nextRow() {
                    return irow < rows.size() ? (Object[]) rows.get( irow++ )
                                              : null;
                }
            };
        }
    }
}
