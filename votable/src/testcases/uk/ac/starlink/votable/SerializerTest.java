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
import java.net.ConnectException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
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
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.TestCase;

public class SerializerTest extends TestCase {

    StarTable table0;
    VOElementFactory factory = 
        new VOElementFactory( StoragePolicy.PREFER_MEMORY );

    public SerializerTest( String name ) {
        super( name );
        Logger.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
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
        VOElement res = factory.makeVOElement( xsrc );
        TableElement table = (TableElement) res.getChildByName( "TABLE" );
        checkTableValues( table );
    }

    private void checkBadSource( Source xsrc )
            throws TransformerException, IOException {
        try {
            VOElement res = factory.makeVOElement( xsrc );
            TableElement table = (TableElement) res.getChildByName( "TABLE" );
            RowSequence rseq = table.getData().getRowSequence();
            assertTrue( "If the table can be constructed, " +
                        "it should have no rows", ! rseq.next() );
        }
        catch ( FileNotFoundException e ) {
        }
        catch ( SAXException e ) {
        }
    }

    public void testSerializers()
            throws IOException, SAXException, TransformerException {
        try {

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
            writer.close();
            byte[] xmltext = bytestream.toByteArray();
            // new FileOutputStream( "j" ).write( xmltext );

            assertResourceValidDTD( xmltext );
            assertResourceValidXSD( xmltext );

            /* Test all constructors, validating and not.  There are
             * significantly different paths through the code for each one,
             * in particular depending on whether the parse to DOM is
             * done inside or outside the VOTable package. */
            xmltext = ( "<VOTABLE version='1.1'>\n"
                      + new String( xmltext )
                      + "</VOTABLE>" ).getBytes();
            List vodocs = new ArrayList();
            Document docnode1 =
                (Document) new SourceReader()
                          .getDOM( new StreamSource( asStream( xmltext ) ) );
            vodocs.add( factory
                       .makeVOElement( new ByteArrayInputStream( xmltext ), 
                                       null ) );
            vodocs.add( factory.makeVOElement( docnode1, null ) );
            Source xsrc = new DOMSource( docnode1, null );
            vodocs.add( factory.makeVOElement( (Source) xsrc ) );
            vodocs.add( factory.makeVOElement( (DOMSource) xsrc ) );
            DOMSource dsrc0 = factory.
                transformToDOM( new StreamSource( asStream( xmltext ) ),
                                false );
            DOMSource dsrc1 = factory.
                transformToDOM( new StreamSource( asStream( 
                                    prependDeclaration( xmltext ) ) ), 
                                true );
            vodocs.add( factory.makeVOElement( dsrc0 ) );
            vodocs.add( factory.makeVOElement( dsrc1 ) );
            for ( Iterator it = vodocs.iterator(); it.hasNext(); ) {
                exerciseVOTableDocument( (VOElement) it.next() );
            }

            /* Test the streamStarTable method; get each table in turn. */
            boolean strict = true;
            for ( int itable = 0; itable < 5; itable++ ) {
                RowStore rstore = new RowStore();
                InputSource saxsrc = 
                    new InputSource( new ByteArrayInputStream( xmltext ) );
                TableStreamer.streamStarTable( saxsrc, rstore, itable, strict );

                RowSequence rstep = rstore.getRowSequence();
                RowSequence rseq = table0.getRowSequence();
                while ( rseq.next() ) {
                    assertTrue( rstep.next() );
                    assertArrayEquals( rseq.getRow(), rstep.getRow() );
                }
                assertTrue( ! rseq.next() );
                assertTrue( ! rstep.next() );
                rseq.close();
                rstep.close();
                assertTrue( ! rseq.next() );
                assertTrue( ! rstep.next() );
            }

            try {
                TableStreamer.streamStarTable(
                       new InputSource( new ByteArrayInputStream( xmltext ) ),
                       new RowStore(), 5, strict );
                fail();
            }
            catch ( SAXException e ) {
                // ok
            }
            catch ( IOException e ) {
                // ok
            }
        }
        catch ( ConnectException e ) {
            System.err.println( "Couldn't perform test - failed to make " +
                                "connection (probably to remote VOTable DTD)" );
            e.printStackTrace( System.err );
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
        assertEquals( tables.length,
                      res.getElementsByVOTagName( "TABLE" ).getLength() );
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

        assertTrue( ! bStrIn.hasAttribute( "href" ) );
        assertTrue( ! fStrIn.hasAttribute( "href" ) );
        assertTrue( bStrEx.hasAttribute( "href" ) );
        assertTrue( fStrEx.hasAttribute( "href" ) );

        assertEquals( "base64", bStrIn.getAttribute( "encoding" ) );
        assertEquals( "base64", fStrIn.getAttribute( "encoding" ) );
        // assertTrue( "none".equals( bStrEx.getAttribute( "encoding" ) ) ||
        //             null == bStrEx.getAttribute( "encoding" ) );
        // assertTrue( "none".equals( fStrEx.getAttribute( "encoding" ) ) ||
        //             null == fStrEx.getAttribute( "encoding" ) );
    }

    private void checkTableValues( TableElement votab ) throws IOException {
            
        RowSequence rseq = table0.getRowSequence();
        int ncol = table0.getColumnCount();
        assertEquals( ncol, votab.getFields().length );
        RowSequence rstep = votab.getData().getRowSequence();

        Object[] row;
        int irow = 0;
        while ( rstep.next() ) {
            assertTrue( rseq.next() );
            assertArrayEquals( rseq.getRow(), rstep.getRow() );
            irow++;
        }
        assertEquals( table0.getRowCount(), irow );
        assertTrue( ! rseq.next() );
        assertTrue( ! rstep.next() );
        rseq.close();
        rstep.close();
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

    private byte[] prependDeclaration( byte[] votext ) {
        return
            ( "<!DOCTYPE VOTABLE SYSTEM 'http://us-vo.org/xml/VOTable.dtd'>\n" +
              new String( votext ) )
           .getBytes();
    }

    private void assertResourceValidDTD( byte[] xmlbytes ) 
            throws IOException, SAXException {
        String doc =
            "<!DOCTYPE VOTABLE SYSTEM 'http://us-vo.org/xml/VOTable.dtd'>\n" +
            "<VOTABLE version='1.0'>" +
            new String( xmlbytes ) +
            "</VOTABLE>";
        assertValidXML(
            new InputSource( new ByteArrayInputStream( doc.getBytes() ) ) );
    }

    private void assertResourceValidXSD( byte[] xmlbytes ) throws IOException {
        String doc = 
            "<VOTABLE version='1.1' " +
            " xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" +
            " xsi:schemaLocation='http://www.ivoa.net/xml/VOTable/v1.1" +
                                " http://www.ivoa.net/xml/VOTable/v1.1'" +
            " xmlns='http://www.ivoa.net/xml/VOTable/v1.1'>\n" +
            new String( xmlbytes ) +
            "</VOTABLE>";

        try {
            VOTableSchema.getSchema( "1.1" ).newValidator()
                         .validate( new SAXSource(
                                        new InputSource(
                                            new ByteArrayInputStream( 
                                                doc.getBytes() ) ) ) );
        }
        catch ( Exception e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
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

        public RowSequence getRowSequence() {
            return new RowSequence() {
                int irow = -1;
                public boolean next() {
                    return ++irow < rows.size();
                }
                public Object[] getRow() {
                    return (Object[]) rows.get( irow );
                }
                public Object getCell( int icol ) {
                    return getRow()[ icol ];
                }
                public void close() {
                }
            };
        }
    }
}
