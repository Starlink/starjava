package uk.ac.starlink.datanode.nodes;

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
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.factory.VODataNodeBuilder;
import uk.ac.starlink.datanode.viewers.TextViewer;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.UCD;
import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.SourceReader;

/**
 * Generic node for representing VOTable elements.
 */
public class VOComponentDataNode extends DefaultDataNode {

    private static Set voTagNames = new HashSet( Arrays.asList( new String[] {
        "VOTABLE", "RESOURCE", "DESCRIPTION", "DEFINITIONS", "INFO", 
        "PARAM", "TABLE", "FIELD", "VALUES", "MIN", "MAX", "OPTION", 
        "LINK", "DATA", "TABLEDATA", "TR", "TD", "BINARY", "FITS", 
        "STREAM", "COOSYS", "GROUP", "FIELDref", "PARAMref",
    } ) );

    protected final VOElement vocel;
    protected final String systemId;
    private String name;
    private Object parentObj;
    private TableElement[] tables;
    private StarTable startable;
    private static VOElementFactory vofact = new VOElementFactory();

    public VOComponentDataNode( Source xsrc ) throws NoSuchDataException {
        Node domNode;
        try {
            vocel = vofact.makeVOElement( xsrc );
        }
        catch ( SAXException e ) {
            throw new NoSuchDataException( e );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
        this.systemId = xsrc.getSystemId();

        if ( ! voTagNames.contains( vocel.getTagName() ) ) {
            throw new NoSuchDataException( "Not a known VOTable element" );
        }

        String idval = vocel.getAttribute( "ID" );
        String nameval = vocel.getAttribute( "name" );
        name = vocel.getHandle();

        parentObj = getSource( vocel.getParent() );

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
        List children = new ArrayList();
        for ( Iterator it = getChildElements( vocel ).iterator(); 
              it.hasNext(); ) {
            Source xsrc = getSource( ((VOElement) it.next()) );
            children.add( makeChild( xsrc ) );
        }
        return children.iterator();
    }

    static List getChildElements( VOElement vocel ) {
        List children = new ArrayList( Arrays.asList( vocel.getChildren() ) );
        for ( Iterator it = children.iterator(); it.hasNext(); ) {
            VOElement childEl = (VOElement) it.next();
            String childName = childEl.getTagName();
            if ( childName.equals( "DESCRIPTION" ) ||
                 childName.equals( "INFO" ) ||
                 childName.equals( "PARAM" ) ||
                 childName.equals( "COOSYS" ) ) {
                it.remove();
            }
        }
        return children;
    }

    public void configureDetail( DetailViewer dv ) {
        dv.addKeyedItem( "Element name", vocel.getTagName() );
        addVOComponentViews( dv, vocel );
    }

    VOElement getElement() {
        return vocel;
    }

    public static void addVOComponentViews( DetailViewer dv, 
                                            final VOElement voel ) {

        /* Collect useful children. */
        Map atts = new TreeMap();
        String description = null;
        String systemId = voel.getSystemId();
        List infonames = new ArrayList();
        List infovals = new ArrayList();
        final List params = new ArrayList();
        final List coosyss = new ArrayList();
        int ninfo = 0;

        
        for ( Node child = voel.getFirstChild(); child != null;
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
                    String infohandle = vofact
                                       .makeVOElement( childEl, systemId )
                                       .getHandle();
                    String infovalue = childEl.getAttribute( "value" );
                    infonames.add( infohandle );
                    infovals.add( infovalue );
                    ninfo++;
                }
                else if ( elname.equals( "PARAM" ) ) {
                    params.add( (ParamElement) childEl );
                }
                else if ( elname.equals( "COOSYS" ) ) {
                    coosyss.add( (VOElement) childEl );
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
            String text = DOMUtils.getTextContent( coosys ).trim();
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
                ParamElement param = (ParamElement) it.next();
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
            public JComponent getComponent() {
                return new TextViewer( getSource( voel ) );
            }
        } );
    }

    /**
     * Returns a DOM source associated with a given VO Element.
     *
     * @param  voel  element
     * @return   source
     */
    public static DOMSource getSource( VOElement voel ) {
        return voel == null ? null
                            : new DOMSource( voel, voel.getSystemId() );
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
                ParamElement param = (ParamElement) params.get( i );
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
