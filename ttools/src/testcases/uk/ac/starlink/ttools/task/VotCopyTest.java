package uk.ac.starlink.ttools.task;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableBuilder;

public class VotCopyTest extends TableTestCase {

    final Element multiDOM_;
    final StarTable t1_;
    final StarTable t2_;
    final StarTable t3_;
    final URL multiLoc_ = getClass().getResource( "multi.vot" );
    final URL fitsLoc_ = getClass().getResource( "fitsin.vot" );
    final URL heteroLoc_ = getClass().getResource( "heteroVOTable.xml" );

    public VotCopyTest( String name ) throws Exception {
        super( name );
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.ttools.copy" )
                .setLevel( Level.SEVERE );
        multiDOM_ = checkAndRemoveData( new VOElementFactory()
                                       .makeVOElement( multiLoc_ ),
                                        "TABLEDATA" );
        VOTableBuilder vbuilder = new VOTableBuilder();
        DataSource dsrc = new URLDataSource( multiLoc_ );
        StoragePolicy policy = StoragePolicy.PREFER_MEMORY;
        dsrc.setPosition( "0" );
        t1_ = vbuilder.makeStarTable( dsrc, true, policy );
        dsrc.setPosition( "1" );
        t2_ = vbuilder.makeStarTable( dsrc, true, policy );
        dsrc.setPosition( "2" );
        t3_ = vbuilder.makeStarTable( dsrc, true, policy );
    }

    private URL copyLocation( Map map ) throws Exception {
        MapEnvironment env = new MapEnvironment( map );
        File file = File.createTempFile( "votcopy", ".vot" );
        file.deleteOnExit();
        env.setValue( "out", file.toString() )
           .setValue( "charset", "utf-8" )
           .setValue( "href", "false" );
        new VotCopy().createExecutable( env ).execute();
        return file.toURI().toURL();
    }

    private VOElement readDOM( Map map ) throws Exception {
        URL loc = copyLocation( map );


        return new VOElementFactory().makeVOElement( loc );
    }

    public void testFits() throws Exception {

        /* Tests a specific bug related to IOUtils.skipBytes and
         * a bug in nom.tam.util.BufferedDataInputStream.
         * Hopefully fixed shortly after this test was introduced. */
        MapEnvironment env = new MapEnvironment();
        File file = File.createTempFile( "votcopy", ".vot" );
        file.deleteOnExit();
        env.setValue( "in", fitsLoc_.toString() );
        env.setValue( "out", file.toString() );
        new VotCopy().createExecutable( env ).execute();
    }

    public void testDOM() throws Exception {
        Map map = new HashMap();
        map.put( "in", multiLoc_.toString() );

        map.put( "format", "tabledata" );
        VOElement tDom = readDOM( map );
        matchMultiData( tDom, false );
        matchMultiDOM( checkAndRemoveData( tDom, "TABLEDATA" ) );

        map.put( "format", "binary" );
        VOElement bDom = readDOM( map );
        matchMultiData( bDom, false );
        matchMultiDOM( checkAndRemoveData( bDom, "BINARY" ) );

        map.put( "format", "fits" );
        VOElement fDom = readDOM( map );
        matchMultiData( fDom, true );
        matchMultiDOM( checkAndRemoveData( fDom, "FITS" ) );

        map.put( "format", null );
        matchMultiDOM( readDOM( map ) );

        // Skip these for now, since they leave files about the place which
        // are not tidied up.  It's a bit fiddly to tidy them, since we
        // don't rightly know their names.

        // map.put( "format", "binary" );
        // map.put( "href", "true" );
        // VOElement bhDom = readDOM( map );
        // matchMultiData( bhDom, false );
        // matchMultiDOM( checkAndRemoveData( bhDom, "BINARY" ) );

        // map.put( "format", "fits" );
        // map.put( "href", "true" );
        // VOElement fhDom = readDOM( map );
        // matchMultiData( fhDom, true );
        // matchMultiDOM( checkAndRemoveData( fhDom, "FITS" ) );
    }

    public void testHetero() throws Exception {
        VOTableBuilder builder = new VOTableBuilder();
        StarTable t0 =
            builder.makeStarTable( new URLDataSource( heteroLoc_ ),
                                   true, StoragePolicy.PREFER_MEMORY );
        for ( DataFormat datfmt :
              new DataFormat[] { DataFormat.TABLEDATA,
                                 DataFormat.BINARY,
                                 DataFormat.BINARY2,
                                 DataFormat.FITS } ) {
            File tmpfile = File.createTempFile( "votcopy", ".vot" );
            tmpfile.deleteOnExit();
            MapEnvironment env = new MapEnvironment();
            env.setValue( "in", heteroLoc_.toString() );
            env.setValue( "out", tmpfile.toString() );
            env.setValue( "format", datfmt );
            env.setValue( "href", Boolean.FALSE );
            if ( DataFormat.BINARY2.equals( datfmt ) ) {
                env.setValue( "version", "1.3" );
            }
            new VotCopy().createExecutable( env ).execute();
            StarTable t1 =
                builder.makeStarTable( new FileDataSource( tmpfile ),
                                       true, StoragePolicy.PREFER_MEMORY );

            // Skip the equality test for FITS.
            // Variable-length array cells are currently padded to
            // their maximum length with zeros, so these cells may not be
            // the same after copying.
            //  It would be possible to change this so that a
            // VariableFitsTableSerializer is used instead of a
            // StandardFitsTableSerializer when writing it
            // (see VOSerializer.makeSerializer method).
            // The FITS serialization is so infrequently used, it hardly
            // seems worth the effort.
            if ( DataFormat.FITS != datfmt ) {
                assertSameData( t0, t1 );
            }
            tmpfile.delete();
        }
    }

    private void matchMultiDOM( Element el ) {
        assertDOMEquals( multiDOM_, el, "", IGNORE_WHITESPACE );
    }

    private void matchMultiData( VOElement el, boolean fudgeUnicode )
            throws Exception {
        NodeList tables =
            ((VOElement) el.getOwnerDocument().getDocumentElement())
           .getElementsByVOTagName( "TABLE" );
        assertEquals( 3, tables.getLength() );
        assertSameData( t1_,
                        new VOStarTable( (TableElement) tables.item( 0 ) ) );
        assertSameData( t2_,
                        new VOStarTable( (TableElement) tables.item( 1 ) ) );
        if ( ! fudgeUnicode ) {
            assertSameData( t3_,
                            new VOStarTable( (TableElement)
                                             tables.item( 2 ) ) );
        }
    }

    private Element checkAndRemoveData( Element el, String formatName ) {
        VOElement top = (VOElement) el.getOwnerDocument().getDocumentElement();
        NodeList datas = top.getElementsByTagName( "DATA" );
        List dataList = new ArrayList();
        for ( int i = 0; i < datas.getLength(); i++ ) {
            dataList.add( datas.item( i ) );
        }
        for ( Iterator it = dataList.iterator(); it.hasNext(); ) {
            Element data = (Element) it.next();
            Element[] children = getChildElements( data );
            assertEquals( 1, children.length );
            assertEquals( formatName, children[ 0 ].getNodeName() );
            data.getParentNode().removeChild( data );
        }
        return el;
    }

    private Element[] getChildElements( Node node ) {
        List eList = new ArrayList();
        for ( Node child = node.getFirstChild(); child != null;
              child = child.getNextSibling() ) {
            if ( child instanceof Element ) {
                eList.add( child );
            }
        }
        return (Element[]) eList.toArray( new Element[ 0 ] );
    }
    
}
