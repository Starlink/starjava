package uk.ac.starlink.treeview;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.swing.JComponent;
import javax.xml.transform.Source;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class XMLElementDataNode extends XMLDataNode {

    private Element el;
    private JComponent fullView;

    public XMLElementDataNode( Element el ) {
        super( el,
               "<" + ( ( el.getLocalName() == null ) ? el.getTagName() 
                                                     : el.getLocalName() ) 
                   + ">",
               "ELE",
               "XML element",
               true,
               IconFactory.XML_ELEMENT,
               elementDescription( el ) );
        this.el = el;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();

            /* Attributes. */
            if ( el.hasAttributes() ) {

                /* Assemble a list sorted by attribute name. */
                NamedNodeMap atts = el.getAttributes();
                int natt = atts.getLength();
                SortedMap amap = new TreeMap();
                for ( int i = 0; i < natt; i++ ) {
                    Attr att = (Attr) atts.item( i );
                    String aname = att.getName();
                    String aval = att.getValue();
                    if ( ! att.getSpecified() ) {
                        aval += " (auto value)";
                    }
                    amap.put( aname, aval );
                }

                /* Output the sorted attributes. */
                dv.addSubHead( "Attributes" );
                for ( Iterator it = amap.entrySet().iterator(); 
                      it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    dv.addKeyedItem( (String) entry.getKey(),
                                     (String) entry.getValue() );
                }
            }

            /* Generic info. */
            addXMLViews( dv, el );
        }
        return fullView;
    }

    /**
     * Provide ad-hoc descriptive text for an element.
     * Just intended to be sortof more informative than not having it.
     */
    private static String elementDescription( Element el ) {
        String desc = "";
        boolean hasAtts = el.hasAttributes();
        int natts = hasAtts ? el.getAttributes().getLength() : 0;
        int nchild = el.getChildNodes().getLength();
        boolean isEmpty = ( nchild - natts ) == 0;

        /* Empty element and one attribute, use that. */
        if ( isEmpty && natts == 1 ) {
            Attr att = (Attr) el.getAttributes().item( 0 );
            desc = att.getName() + "=\"" + att.getValue() + "\"";
        }

        /* Try name/value pairs. */
        else if ( el.hasAttribute( "name" ) && el.hasAttribute( "value" ) ) {
             desc = el.getAttribute( "name" ) + "=\"" 
                  + el.getAttribute( "value" ) + "\"";
        }

        /* Try content, if it's short. */
        else if ( nchild - natts == 1 &&
                  el.getChildNodes().item( 0 ) instanceof Text ) {
            Text tnode = (Text) el.getChildNodes().item( 0 );
            String val = tnode.getData().trim();
            desc = '"' + val.substring( 0, Math.min( 80, val.length() ) ) + '"';
        }

        if ( desc.length() > 42 ) {
            desc = desc.substring( 0, 40 ) + "...";
        }
        return desc;
    }

}
