package uk.ac.starlink.votable.dom;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
//DOM3 import org.w3c.dom.TypeInfo;

public class DelegatingAttr extends DelegatingNode implements Attr {

    private final Attr base_;
    private final DelegatingDocument doc_;
    private Boolean isId_;

    /**
     * Constructor.
     *
     * @param   base  base node
     * @param   doc   owner document
     */
    protected DelegatingAttr( Attr base, DelegatingDocument doc ) {
        super( base, doc );
        base_ = base;
        doc_ = doc;
    }

    /**
     * Constructs a DelegatingAttr with information about whether it is
     * an ID-type attribute or not.  This information is only used at
     * DOM Level 3 (J2SE1.5), when it is used as the result of the 
     * <code>isId()</code> method.
     *
     * @param   base  base node
     * @param   doc   owner document
     * @param   isId  whether it will be an ID type attribute
     */
    protected DelegatingAttr( Attr base, DelegatingDocument doc,
                              boolean isId ) {
        this( base, doc );
        isId_ = Boolean.valueOf( isId );
    }

    //
    // Level 2 implementation.
    //

    public String getName() {
        return base_.getName();
    }

    static Attr getBaseAttr( Attr attr, Document doc ) {
        return (Attr) DelegatingNode.getBaseNode( attr, doc );
    }

    public boolean getSpecified() {
        return base_.getSpecified();
    }

    public String getValue() {
        return base_.getValue();
    }

    public void setValue( String value ) {
        base_.setValue( value );
    }

    public Element getOwnerElement() {
        return (Element) doc_.getDelegator( base_.getOwnerElement() );
    }

//DOM3     //
//DOM3     // Level 3 implementation.
//DOM3     //
//DOM3 
//DOM3     public TypeInfo getSchemaTypeInfo() {
//DOM3         return base_.getSchemaTypeInfo();
//DOM3     }
//DOM3 
//DOM3     public boolean isId() {
//DOM3         return isId == null ? base_.isId()
//DOM3                             : isId_.booleanValue();
//DOM3     }
}
