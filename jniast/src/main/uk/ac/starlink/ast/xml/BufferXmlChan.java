package uk.ac.starlink.ast.xml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.ast.XmlChan;

/**
 * XmlChan subclass which stores its lines of data in memory.
 * Any AstObjects which are written into it can be read out of it, FIFO.
 *
 * @author   Mark Taylor (Starlink)
 */
class BufferXmlChan extends XmlChan {

    private List buf = new ArrayList();

    /**
     * Returns a string containing all the XML of objects which have been
     * written down the channel and not yet read out.
     */
    public String getContentString() {
        StringBuffer sbuf = new StringBuffer();
        for ( Iterator it = buf.iterator(); it.hasNext(); ) {
            sbuf.append( (String) it.next() );
        }
        return sbuf.toString();
    }

    /** 
     * Removes any existing XML from the buffer.
     */
    public void clear() {
        buf = new ArrayList();
    }

    protected void sink( String line ) {
        buf.add( line );
    }

    protected String source() {
        return buf.isEmpty() ? null : (String) buf.remove( 0 );
    }
}
