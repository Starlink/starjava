package uk.ac.starlink.ttools.votlint;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides checking of a VOTable element during a SAX parse.
 * There is one ElementHandler for each element encountered by the SAX
 * parser; it is manipulated chiefly by the {@link VotLintContentHandler} which 
 * calls various methods on it in a controlled sequence as its life
 * cycle progresses to handle the current state of the parse.
 * Element-specific subclasses should generally override only the
 * {@link #startElement}, {@link #endElement} and {@link #characters}
 * methods, which are invoked by the ContentHandler's similarly named
 * methods.
 *
 * <p>The checking done by an ElementHandler does not attempt to repeat or
 * replace that done by validating against a DTD,
 * it provides additional semantic checks based on what it knows about
 * VOTables.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class ElementHandler {

    private String name_;
    private VotLintContext context_;
    private Map<String,String> attributes_;
    private Ancestry ancestry_;
    private Map<String,ElementRef> childNames_;
    private ElementRef ref_;

    /**
     * Sets this handler up ready for use.  Must be called before most of
     * the other methods can be used.
     *
     * @param   localName   local name of the element this handler knows about
     * @param   context     lint context
     */
    public void configure( String localName, VotLintContext context ) {
        name_ = localName;
        context_ = context;
        ref_ = new ElementRef( this, context.getLocator() );
    }

    /**
     * Sets the ancestry of this handler.
     *
     * @param  ancestry   family values
     */
    public void setAncestry( Ancestry ancestry ) {
        ancestry_ = ancestry;
    }

    /**
     * Returns an object containing the family relationships of this
     * handler.  This will only return a non-null value while the 
     * element is active (between its startElement and endElement calls).
     * An ancestry object should not be used outside of this context.
     *
     * @return   ancestry
     */
    public Ancestry getAncestry() {
        return ancestry_;
    }

    /**
     * Returns a reference for this element.
     *
     * @return   reference
     */
    public ElementRef getRef() {
        return ref_;
    }

    /**
     * Returns the localName for this element.
     *
     * @return   element name
     */
    public String getName() {
        return name_;
    }

    /**
     * Sets this handler's attributes.
     *
     * @param  atts  name -&gt; value map representing this element's attributes
     */
    public void setAttributes( Map<String,String> atts ) {
        attributes_ = atts;
    }

    /**
     * Returns the value of a named attribute.
     *
     * @param   name  attribute name
     * @return  attribute value
     */
    public String getAttribute( String name ) {
        return attributes_.get( name );
    }

    /**
     * Called after the attributes have been set and checked.
     * The default implementation does nothing.
     */
    public void startElement() {
    }

    /**
     * Called when the element has ended.
     * The default implementation does nothing.
     */
    public void endElement() {
    }

    /**
     * Called when character content is found in the element.
     * The default implementation does nothing.
     */
    public void characters( char[] ch, int start, int length ) {
    }

    /**
     * Called to indicate that a child of this element has a "name" attribute.
     *
     * @param  child   child element
     * @param  name    value of child's name attribute
     */
    public void registerChildName( ElementRef child, String name ) {
        if ( childNames_ == null ) {
            childNames_ = new HashMap<String,ElementRef>();
        }
        if ( childNames_.containsKey( name ) ) {
            ElementRef ref = childNames_.get( name );
            if ( ! name.equals( "QUERY_STATUS" ) ) {  // DAL special case
                warning( new VotLintCode( "DNM" ),
                         "Name '" + name + "' already used in this " + this );
            }
        }
        else {
            childNames_.put( name, child );
        }
    }

    /** 
     * Returns this handler's context.
     *
     * @return   context
     */
    public VotLintContext getContext() {
        return context_;
    }

    /**
     * Writes an info message through the context.
     *
     * @param  code  message identifier
     * @param   msg  message text
     */
    public void info( VotLintCode code, String msg ) {
        getContext().info( code, msg );
    }

    /**
     * Writes a warning message through the context.
     *
     * @param  code  message identifier
     * @param   msg  message text
     */
    public void warning( VotLintCode code, String msg ) {
        getContext().warning( code, msg );
    }

    /**
     * Writes an error message through the context.
     *
     * @param  code  message identifier
     * @param   msg  message text
     */
    public void error( VotLintCode code, String msg ) {
        getContext().error( code, msg );
    }

    public String toString() {
        return name_;
    }
}
