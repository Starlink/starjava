package uk.ac.starlink.ast.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import uk.ac.starlink.ast.AstObject;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.PcdMap;
import uk.ac.starlink.ast.SkyFrame;
import uk.ac.starlink.ast.XmlChan;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class XAstTest extends TestCase {

    private FrameSet fset;
    private XAstReader xr;
    private XAstWriter xw;

    private String confusingID =
        "<test>test &lt; &quotone&quot <![CDATA[&&<<>>]]>" +
        " test 'two' </test>";

    public XAstTest( String name ) {
        super( name );
    }

    protected void setUp() {
        xr = new XAstReader();
        xw = new XAstWriter();
        Frame frm = new Frame( 2 );
        fset = new FrameSet( frm );
        fset.addFrame( 1, new PcdMap( 1e-5, new double[ 2 ] ), new SkyFrame() );
        frm.setDomain( "INITIAL" );
        fset.setID( confusingID );
    }

    public void testReadWriteElement() throws IOException {
        Element el = xw.makeElement( fset );
        AstObject obj = xr.makeAst( el );
        assertEquals( obj.getID(), confusingID );
        assertTrue( obj.equals( fset ) );
        obj.setID( "something else" );
        assertTrue( ! obj.equals( fset ) );

        Element el2 = xw.makeElement( fset );
        AstObject obj2 = xr.makeAst( el2 );
        assertTrue( obj2.equals( fset ) );
    }

    public void testReadWriteSource() throws IOException {
        Source xsrc = xw.makeSource( fset );
        AstObject obj = xr.makeAst( xsrc );
        assertEquals( obj.getID(), confusingID );
        assertTrue( obj.equals( fset ) );
        obj.setID( "something else" );
        assertTrue( ! obj.equals( fset ) );
    }

    public void testToXmlChan() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XmlChan xc = new XmlChan( null, bos );
        xc.write( fset );
        byte[] buf = bos.toByteArray();
        AstObject obj =
            xr.makeAst( new StreamSource( new ByteArrayInputStream( buf ) ) );
        assertEquals( confusingID, obj.getID() );
        assertTrue( obj.equals( fset ) );
        obj.setID( "something else" );
        assertTrue( ! obj.equals( fset ) );
    }

    public void testFromXmlChan() throws IOException, TransformerException {
        Source xsrc = xw.makeSource( fset );
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SourceReader sr = new SourceReader();
        sr.setIndent( 3 );
        sr.writeSource( xsrc, bos );
        byte[] buf = bos.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream( buf );
        XmlChan xc = new XmlChan( bis, null );
        AstObject obj = xc.read();
        assertEquals( confusingID, obj.getID() );
        assertTrue( obj.equals( fset ) );
        obj.setID( "something else" );
        assertTrue( ! obj.equals( fset ) );
    }

    public void testDefault() throws IOException {
        double discoVal = 23.0;
        Document doc = DOMUtils.newDocument();
        Element pcdEl = doc.createElement( "PcdMap" );
        Element discoEl = doc.createElement( XAstNames.ATTRIBUTE );
        discoEl.setAttribute( XAstNames.NAME, "Disco" );
        discoEl.setAttribute( XAstNames.VALUE, Double.toString( discoVal ) );
        pcdEl.appendChild( discoEl );
        doc.appendChild( pcdEl );

        PcdMap pcd1 = (PcdMap) xr.makeAst( pcdEl );
        assertEquals( pcd1.getDisco(), discoVal );

        discoEl.setAttribute( XAstNames.DEFAULT, "true" );
        PcdMap pcd2 = (PcdMap) xr.makeAst( pcdEl );
        assertTrue( pcd2.getDisco() != discoVal );
    }

}
