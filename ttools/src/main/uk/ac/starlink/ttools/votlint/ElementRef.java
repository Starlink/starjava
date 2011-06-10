package uk.ac.starlink.ttools.votlint;

import org.xml.sax.Locator;

/**
 * Provides a description of an element.  This can be used at any time,
 * unlike an {@link ElementHandler}, which is mostly unusable when it's
 * not on the processing stack (i.e. after its SAX endElement has been
 * processed).
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class ElementRef {

    private final String name_;
    private final int line_;
    private final int col_;
    private final ElementHandler handler_;
    private final String rep_;

    /**
     * Constructor.  This is called by ElementHandler itself - in general
     * if you want an ElementRef you should use 
     * {@link ElementHandler#getRef} rather than constructing a new one.
     *
     * @param  handler  handler 
     * @param  locator   locator describing the current parse position
     */
    public ElementRef( ElementHandler handler, Locator locator ) {
        handler_ = handler;
        name_ = handler.getName();
        line_ = locator == null ? -1 : locator.getLineNumber();
        col_ = locator == null ? -1 : locator.getColumnNumber();
        StringBuffer refbuf = new StringBuffer( handler.toString() );
        if ( line_ > 0 ) {
            refbuf.append( " (l." )
                  .append( line_ );
            if ( col_ > 0 ) {
                refbuf.append( ", c." )
                      .append( col_ );
            }
            refbuf.append( ")" );
        }
        rep_ = refbuf.toString();
    }

    /**
     * Returns the element name.
     *
     * @return   name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns the element handler to which this ref refers.
     * Note however that many of its methods may be useless if it's
     * not in scope.  In general this is only useful for recovering
     * subclass-specific information stashed in the handler.
     *
     * @return   handler
     */
    public ElementHandler getHandler() {
        return handler_;
    }

    public boolean equals( Object o ) {
        if ( o instanceof ElementRef ) {
            ElementRef other = (ElementRef) o;
            return other.line_ == this.line_
                && other.col_ == this.col_
                && other.name_.equals( this.name_ );
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int code = 23;
        code = ( 97 * code ) + name_.hashCode();
        code = ( 97 * code ) + line_;
        code = ( 97 * code ) + col_;
        return code;
    }

    public String toString() {
        return rep_;
    }
}
