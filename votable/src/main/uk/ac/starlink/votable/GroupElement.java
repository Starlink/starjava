package uk.ac.starlink.votable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Object representing a GROUP element in a VOTable.
 * Methods exist for retrieving the FIELDs and PARAMs associated with 
 * this group, some or all of which may be referenced using 
 * FIELDref/PARAMref elements to reference the originals.
 * If you want the FIELDref/PARAMref children themselves, you can use
 * the generic 
 * {@link VOElement}/{@link org.w3c.dom.Element}/{@link org.w3c.dom.Node}
 * methods.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Sep 2004
 */
public class GroupElement extends VOElement {

    private static final Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.votable" );

    /**
     * Constructs a GroupElement from a DOM element.
     *
     * @param  base  GROUP element
     * @doc    owner document for new element
     */
    GroupElement( Element base, VODocument doc ) {
        super( base, doc, "GROUP" );
    }

    /**
     * Returns an array of the PARAMs associated with this group.
     * The returned list contains all the PARAM children and all
     * the PARAMs referenced by all the PARAMref children.
     * Any PARAMref which doesn't reference an existing PARAM is ignored.
     * The result is in the same order as the children.
     *
     * @return  PARAM elements represented by children of this group
     */
    public ParamElement[] getParams() {
        List paramList = new ArrayList();
        for ( Node ch = getFirstChild(); ch != null; 
              ch = ch.getNextSibling() ) {
            if ( ch instanceof ParamRefElement ) {
                ParamElement pel = ((ParamRefElement) ch).getParam();
                if ( pel == null ) {
                    logger_.warning( "Missing referent for PARAMref" );
                }
                else {
                    paramList.add( pel );
                }
            }
            else if ( ch instanceof ParamElement ) {
                paramList.add( ch );
            }
        }
        return (ParamElement[]) paramList.toArray( new ParamElement[ 0 ] );   
    }

    /**
     * Returns an array of the FIELDs associated with this group.
     * The returned list contains the FIELD elements referenced by the
     * FIELDref children of this element.
     * Any FIELDref which doesn't reference an existing FIELD is ignored.
     * The result is in the same order as the children.
     *
     * @return  FIELD elements represented by children of this group
     */
    public FieldElement[] getFields() {
        List fieldList = new ArrayList();
        for ( Node ch = getFirstChild(); ch != null;
              ch = ch.getNextSibling() ) {
            if ( ch instanceof FieldRefElement ) {
                FieldElement fel = ((FieldRefElement) ch).getField();
                if ( fel == null ) {
                    logger_.warning( "Missing referent for FIELDref" );
                }
                else {
                    fieldList.add( fel );
                }
            }
        }
        return (FieldElement[]) fieldList.toArray( new FieldElement[ 0 ] );
    }

    /**
     * Returns the GROUP children of this element.
     *
     * @return  group elements which are direct children of this
     */
    public GroupElement[] getGroups() {
        VOElement[] voels = getChildrenByName( "GROUP" );
        GroupElement[] gels = new GroupElement[ voels.length ];
        System.arraycopy( voels, 0, gels, 0, voels.length );
        return gels;
    }
}
