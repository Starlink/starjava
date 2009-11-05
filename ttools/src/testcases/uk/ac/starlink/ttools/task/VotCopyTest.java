package uk.ac.starlink.ttools.task;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableBuilder;

public class VotCopyTest extends TableTestCase {

    final Element multiDOM_;
    final StarTable t1_;
    final StarTable t2_;
    final URL multiLoc_ = getClass().getResource( "multi.vot" );

    public VotCopyTest( String name ) throws Exception {
        super( name );
        Logger.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
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
    }

    private URL copyLocation( Map map ) throws Exception {
        MapEnvironment env = new MapEnvironment( map );
        File file = File.createTempFile( "votcopy", ".vot" );
        file.deleteOnExit();
        env.setValue( "out", file.toString() )
           .setValue( "href", "false" );
        new VotCopy().createExecutable( env ).execute();
        return file.toURL();
    }

    private VOElement readDOM( Map map ) throws Exception {
        URL loc = copyLocation( map );


        return new VOElementFactory().makeVOElement( loc );
    }

    public void testDOM() throws Exception {
        Map map = new HashMap();
        map.put( "in", multiLoc_.toString() );

        map.put( "format", "tabledata" );
        VOElement tDom = readDOM( map );
        matchMultiData( tDom );
        matchMultiDOM( checkAndRemoveData( tDom, "TABLEDATA" ) );

        map.put( "format", "binary" );
        VOElement bDom = readDOM( map );
        matchMultiData( bDom );
        matchMultiDOM( checkAndRemoveData( bDom, "BINARY" ) );

        map.put( "format", "fits" );
        VOElement fDom = readDOM( map );
        matchMultiData( fDom );
        matchMultiDOM( checkAndRemoveData( fDom, "FITS" ) );

        map.put( "format", null );
        matchMultiDOM( readDOM( map ) );

        // Skip these for now, since they leave files about the place which
        // are not tidied up.  It's a bit fiddly to tidy them, since we
        // don't rightly know their names.

        // map.put( "format", "binary" );
        // map.put( "href", "true" );
        // VOElement bhDom = readDOM( map );
        // matchMultiData( bhDom );
        // matchMultiDOM( checkAndRemoveData( bhDom, "BINARY" ) );

        // map.put( "format", "fits" );
        // map.put( "href", "true" );
        // VOElement fhDom = readDOM( map );
        // matchMultiData( fhDom );
        // matchMultiDOM( checkAndRemoveData( fhDom, "FITS" ) );
    }

    private void matchMultiDOM( Element el ) {
        assertDOMEquals( multiDOM_, el, "", IGNORE_WHITESPACE );
    }

    private void matchMultiData( VOElement el ) throws Exception {
        NodeList tables =
            ((VOElement) el.getOwnerDocument().getDocumentElement())
           .getElementsByVOTagName( "TABLE" );
        assertEquals( 2, tables.getLength() );
        assertSameData( t1_,
                        new VOStarTable( (TableElement) tables.item( 0 ) ) );
        assertSameData( t2_,
                        new VOStarTable( (TableElement) tables.item( 1 ) ) );
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
