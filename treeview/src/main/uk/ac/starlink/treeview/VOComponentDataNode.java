package uk.ac.starlink.treeview;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.JComponent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.UCD;
import uk.ac.starlink.votable.Param;
import uk.ac.starlink.votable.Table;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.SourceReader;

/**
 * Generic node for representing VOTable elements.
 */
public class VOComponentDataNode extends DefaultDataNode
                                 implements TableNodeChooser.Choosable {

    private static Set voTagNames = new HashSet( Arrays.asList( new String[] {
        "VOTABLE", "RESOURCE", "DESCRIPTION", "DEFINITIONS", "INFO", 
        "PARAM", "TABLE", "FIELD", "VALUES", "MIN", "MAX", "OPTION", 
        "LINK", "DATA", "TABLEDATA", "TR", "TD", "BINARY", "FITS", 
        "STREAM", "COOSYS",
    } ) );

    protected final Element vocel;
    protected final String systemId;
    private String name;
    private Object parentObj;
    private NodeList tables;
    private StarTable startable;

    public VOComponentDataNode( Source xsrc ) throws NoSuchDataException {
        Node domNode;
        try {
            domNode = new SourceReader().getDOM( xsrc );
        }
        catch ( TransformerException e ) {
            throw new NoSuchDataException( e );
        }
        if ( domNode instanceof Element ) {
            vocel = (Element) domNode;
        }
        else if ( domNode instanceof Document ) {
            vocel = ((Document) domNode).getDocumentElement();
            if ( vocel.getTagName() != "VOTABLE" ) {
                throw new NoSuchDataException( "Document is not VOTABLE" );
            }
        }
        else {
            throw new NoSuchDataException( "Source is not an element" );
        }
        this.systemId = xsrc.getSystemId();

        if ( ! voTagNames.contains( vocel.getTagName() ) || 
             vocel.getPrefix() != null ) {
            throw new NoSuchDataException( "Not a known VOTable element" );
        }

        String idval = vocel.getAttribute( "ID" );
        String nameval = vocel.getAttribute( "name" );
        if ( nameval.length() > 0 ) {
            name = nameval;
        }
        else if ( idval.length() > 0 ) {
            name = idval;
        }
        else {
            name = vocel.getTagName();
        }

        parentObj = new DOMSource( vocel.getParentNode(), systemId );

        setLabel( name );
        setIconID( IconFactory.VOCOMPONENT );
    }

    protected VOComponentDataNode( Source xsrc, String elname )
            throws NoSuchDataException {
        this( xsrc );
        if ( ! vocel.getTagName().equals( elname ) ) {
            throw new NoSuchDataException( "Not a " + elname + " element" );
        }
    }

    public String getName() {
        return name;
    }

    public String getNodeTLA() {
        return "VOC";
    }

    public String getNodeType() {
        return "VOTable component";
    }

    public String getPathSeparator() {
        return ".";
    }

    public Object getParentObject() {
        return parentObj;
    }

    public String getDescription() {
        return "";
    }

    public DataNodeFactory getChildMaker() {
        DataNodeFactory dfact = super.getChildMaker();
        if ( ! ( dfact.getBuilders().get( 0 ) instanceof VODataNodeBuilder ) ) {
            dfact = new DataNodeFactory( dfact );
            dfact.getBuilders().add( 0, new VODataNodeBuilder() );
        }
        return dfact;
    }

    public boolean allowsChildren() {
        String tagname = vocel.getTagName();
        if ( tagname.equals( "DEFINITIONS" ) ) {
            return false;
        }
        else {
            return true;
        }
    }

    public Iterator getChildIterator() {
        return new Iterator() {
            private Node next = firstUsefulSibling( vocel.getFirstChild() );
            public boolean hasNext() {
                return next != null;
            }
            public Object next() {
                if ( next == null ) {
                    throw new NoSuchElementException();
                }
                Node nd = next;
                next = firstUsefulSibling( nd.getNextSibling() );
                try {
                    Source xsrc = new DOMSource( nd, systemId );
                    DataNode child = makeChild( xsrc );
                    return makeChild( xsrc );
                }
                catch ( Exception e ) {
                    return makeErrorChild( e );
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void configureDetail( DetailViewer dv ) {
        dv.addKeyedItem( "Element name", vocel.getTagName() );
        addVOComponentViews( dv, vocel, systemId );
    }

    private NodeList getTables() {
        if ( tables == null ) {
            tables = vocel.getElementsByTagName( "TABLE" );
        }
        return tables;
    }

    public boolean isStarTable() {
        return getTables().getLength() == 1;
    }

    public StarTable getStarTable() throws IOException {
        if ( ! isStarTable() ) {
            throw new IllegalStateException();
        }
        if ( startable == null ) {
            Element tel = (Element) getTables().item( 0 );
            DOMSource xsrc = new DOMSource( tel, systemId );
            startable = new VOStarTable( new Table( xsrc ) );
        }
        return startable;
    }

    public static void addVOComponentViews( DetailViewer dv, final Element el,
                                            final String systemId ) {

        /* Collect useful children. */
        Map atts = new TreeMap();
        String description = null;
        List infonames = new ArrayList();
        List infovals = new ArrayList();
        final List params = new ArrayList();
        final List coosyss = new ArrayList();
        int ninfo = 0;
        for ( Node child = el.getFirstChild(); child != null;
              child = child.getNextSibling() ) {
            if ( child instanceof Attr ) {
                Attr att = (Attr) child;
                atts.put( att.getName(), att.getValue() );
            }
            else if ( child instanceof Element ) {
                Element childEl = (Element) child;
                String elname = childEl.getTagName();
                DOMSource childSrc = new DOMSource( childEl, systemId );
                if ( elname.equals( "DESCRIPTION" ) ) {
                    description = DOMUtils.getTextContent( childEl ).trim();
                }
                else if ( elname.equals( "INFO" ) ) {
                    String infohandle = 
                        VOElement.makeVOElement( childSrc ).getHandle();
                    String infovalue = childEl.getAttribute( "value" );
                    infonames.add( infohandle );
                    infovals.add( infovalue );
                    ninfo++;
                }
                else if ( elname.equals( "PARAM" ) ) {
                    params.add( new Param( childSrc ) );
                }
                else if ( elname.equals( "COOSYS" ) ) {
                    coosyss.add( VOElement.makeVOElement( childSrc ) );
                }
            }
        }

        /* System ID. */
        if ( systemId != null && systemId.trim().length() > 0 ) {
            dv.addKeyedItem( "System ID", systemId );
        }

        /* Attributes. */
        if ( atts.size() > 0 ) {
            dv.addSubHead( "Attributes" );
            for ( Iterator it = atts.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry att = (Map.Entry) it.next();
                dv.addKeyedItem( (String) att.getKey(),
                                 (String) att.getValue() );
            }
            dv.addSeparator();
        }

        /* Description. */
        if ( description != null && description.length() > 0 ) {
            dv.addSubHead( "Description" );
            dv.addText( description );
            dv.addSeparator();
        }

        /* Coosys. */
        if ( coosyss.size() == 1 ) {
            dv.addSubHead( "Coordinate system" );
            VOElement coosys = (VOElement) coosyss.get( 0 );
            String text = coosys.getTextContent().trim();
            String id = coosys.getID();
            String equinox = coosys.getAttribute( "equinox" );
            String epoch = coosys.getAttribute( "epoch" );
            String system = coosys.getAttribute( "system" );
            if ( text != null ) dv.addText( text );
            if ( id != null ) dv.addKeyedItem( "ID", id );
            if ( equinox != null ) dv.addKeyedItem( "Equinox", equinox );
            if ( epoch != null ) dv.addKeyedItem( "Epoch", epoch );
            if ( system != null ) dv.addKeyedItem( "System", system );
        }

        /* Infos. */
        if ( ninfo > 0 ) {
            dv.addSubHead( "Infos" );
            for ( int i = 0; i < ninfo; i++ ) {
                dv.addKeyedItem( (String) infonames.get( i ),
                                 (String) infovals.get( i ) );
            }
        }

        /* Params. */
        if ( params.size() > 0 ) {
            dv.addSubHead( "Params" );
            for ( Iterator it = params.iterator(); it.hasNext(); ) {
                Param param = (Param) it.next();
                String val = param.getValue();
                String unit = param.getUnit();
                if ( unit != null ) {
                    val += " " + unit;
                }
                dv.addKeyedItem( param.getHandle(), val );
            }
            dv.addPane( "Params", new ComponentMaker() {
                public JComponent getComponent() {
                    return new MetaTable( new ParamMetamapGroup( params ) );
                }
            } );
        }

        /* XML view. */
        dv.addPane( "XML content", new ComponentMaker() {
            public JComponent getComponent() throws TransformerException {
                return new TextViewer( new DOMSource( el, systemId ) );
            }
        } );
    }

    /**
     * Takes a given node and returns it, or the first of its siblings which
     * is of interest to this class.  To count as useful, it must be an
     * Element, and must not have the of one of the generic VOTable 
     * components which are treated internally.
     * These are currently INFO, PARAM, DESCRIPTION and COOSYS.
     */
    private static Node firstUsefulSibling( Node sib ) {
        if ( sib == null ) {
            return null;
        }
        if ( ! ( sib instanceof Element ) ) {
            return firstUsefulSibling( sib.getNextSibling() );
        }
        String elname = ((Element) sib).getTagName();
        if ( elname.equals( "DESCRIPTION" ) ||
             elname.equals( "INFO" ) ||
             elname.equals( "PARAM" ) ||
             elname.equals( "COOSYS" ) ) {
            return firstUsefulSibling( sib.getNextSibling() );
        }
        return sib;
    }
   
    /**
     * Private helper class which builds a MetamapGroup from a list of Params.
     */
    private static class ParamMetamapGroup extends MetamapGroup {

        private static final String NAME_KEY = "Name";
        private static final String ID_KEY = "ID";
        private static final String VALUE_KEY = "Value";
        private static final String UNIT_KEY = "Units";
        private static final String DESCRIPTION_KEY = "Description";
        private static final String UCD_KEY = "UCD";
        private static final String UCD_DESCRIPTION_KEY = "UCD description";
        private static final String DATATYPE_KEY = "Datatype";
        private static final String PRECISION_KEY = "Precision";
        private static final String WIDTH_KEY = "Width";
        private static final String ARRAYSIZE_KEY = "Arraysize";

        private static List keyOrder = Arrays.asList( new String[] {
            NAME_KEY, ID_KEY, VALUE_KEY, UNIT_KEY, DESCRIPTION_KEY,
            UCD_KEY, UCD_DESCRIPTION_KEY,
            DATATYPE_KEY, PRECISION_KEY, WIDTH_KEY, ARRAYSIZE_KEY,
        } );

        public ParamMetamapGroup( List params ) {
            super( params.size() );
            setKeyOrder( keyOrder );
            int np = params.size();
            for ( int i = 0; i < np; i++ ) {
                Param param = (Param) params.get( i );
                addEntry( i, ID_KEY, param.getAttribute( "ID" ) );
                addEntry( i, UNIT_KEY, param.getAttribute( "unit" ) );
                addEntry( i, DATATYPE_KEY, param.getAttribute( "datatype" ) );
                addEntry( i, PRECISION_KEY, param.getAttribute( "precision" ) );
                addEntry( i, WIDTH_KEY, param.getAttribute( "width" ) );
                addEntry( i, NAME_KEY, param.getAttribute( "name" ) );
                addEntry( i, UCD_KEY, param.getAttribute( "ucd" ) );
                addEntry( i, VALUE_KEY, param.getAttribute( "value" ) );
                addEntry( i, ARRAYSIZE_KEY, param.getAttribute( "arraysize" ) );
                addEntry( i, DESCRIPTION_KEY, param.getDescription() );
                if ( hasEntry( i, UCD_KEY ) ) {
                    UCD ucd = UCD.getUCD( (String) getEntry( i, UCD_KEY ) );
                    String desc = ( ucd != null ) ? ucd.getDescription()
                                                  : "<unknown UCD>";
                    addEntry( i, UCD_DESCRIPTION_KEY, desc );
                }
            }
        }
    }
}
