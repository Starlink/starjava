package uk.ac.starlink.ast;

import java.util.ArrayList;
import java.util.List;

class MemoryXmlChan extends XmlChan {
    List buf = new ArrayList();
    { setXmlIndent( true ); }
    protected void sink( String line ) {
        buf.add( line );
    }
    protected String source() {
        return buf.isEmpty() ? null : (String) buf.remove( 0 );
    }
}
