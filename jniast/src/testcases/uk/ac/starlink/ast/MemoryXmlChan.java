package uk.ac.starlink.ast;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.transform.stream.StreamSource;

class MemoryXmlChan extends XmlChan {
    List buf = new ArrayList();
    protected void sink( String line ) {
        buf.add( line );
    }
    protected String source() {
        return buf.isEmpty() ? null : (String) buf.remove( 0 );
    }
    public StreamSource getSource() {
        StringBuffer sbuf = new StringBuffer();
        for ( Iterator it = buf.iterator(); it.hasNext(); ) {
            sbuf.append( (String) it.next() );
        }
        return new StreamSource( new StringReader( sbuf.toString() ) );
    }
}
