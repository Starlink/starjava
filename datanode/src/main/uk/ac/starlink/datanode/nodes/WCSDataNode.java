package uk.ac.starlink.datanode.nodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.Channel;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.xml.XAstReader;
import uk.ac.starlink.ast.xml.XAstWriter;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.viewers.AstFITSShower;
import uk.ac.starlink.datanode.viewers.AstTextShower;
import uk.ac.starlink.datanode.viewers.TextViewer;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.NDFNdxHandler;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.Ndxs;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.SourceReader;

/**
 * A {@link DataNode} representing the WCS component of 
 * an NDF.
 *
 * @author   Mark Taylor (STARLINK)
 * @version  $Id$
 */
public class WCSDataNode extends DefaultDataNode {

    private FrameSet wcs;
    private String description;

    /**
     * Constructs a WCSDataNode from a FrameSet object.
     */
    public WCSDataNode( FrameSet wcs ) throws NoSuchDataException {
        super( "WCS" );
        this.wcs = wcs;
        description = wcs.getNframe() + " frames;"
                    + " current domain \"" 
                    + wcs.getDomain() + "\"";
        setIconID( IconFactory.WCS );
    }

    /**
     * Constructs a WCSDataNode from an HDS object.
     */
    public WCSDataNode( HDSObject hobj ) throws NoSuchDataException {
        this( getWcsFromHds( hobj ) );
    }

    /**
     * Constructs a WCSDataNode from an HDS path.
     */
    public WCSDataNode( String path ) throws NoSuchDataException {
        this( HDSDataNode.getHDSFromPath( path ) );
    }

    /**
     * Constructs a WCSDataNode from an XML Source.
     */
    public WCSDataNode( Source xsrc ) throws NoSuchDataException {
        this( getWcsFromSource( xsrc ) );
    }

    public boolean allowsChildren() {
        return true;
    }

    /**
     * Gets the children of this node.  This consists of 
     * the frames of which this frameset consists, one per child.
     *
     * @return  an array of children of this node
     */
    public Iterator getChildIterator() {
        List children = new ArrayList();
        int nframe = wcs.getNframe();
        DataNodeFactory childMaker = new DataNodeFactory( getChildMaker() );
        childMaker.setPreferredClass( FrameDataNode.class );
        for ( int i = 1; i <= nframe; i++ ) {
            DataNode dnode;
            try {
                children.add( childMaker.makeChildNode( this, 
                                                        wcs.getFrame( i ) ) );
            }
            catch ( AstException e ) {
                children.add( makeErrorChild( e ) );
            } 
        }
        return children.iterator();
    }

    public String getDescription() {
        return description;
    }

    public String getPathSeparator() {
        return ".";
    }

    public String getNodeTLA() {
        return "WCS";
    }

    public String getNodeType() {
        return "World Coordinate System data";
    }

    public void configureDetail( DetailViewer dv ) {
        int nframe = wcs.getNframe();
        int current = wcs.getCurrent();
        int base = wcs.getBase();
        dv.addKeyedItem( "Number of frames", nframe );
        dv.addKeyedItem( "Base frame", 
                         Integer.toString( base )
                         + " (\"" + wcs.getFrame( base ).getDomain()
                         + "\")" );
        dv.addKeyedItem( "Current frame", 
                         Integer.toString( current )
                         + " (\"" + wcs.getFrame( current ).getDomain()
                         + "\")" );
    
        dv.addSubHead( "Frames" );
        for ( int i = 1; i < nframe + 1; i++ ) {
            Frame frm = wcs.getFrame( i );
            String dom = frm.getDomain();
            dv.addText( "  " + i + ":  " 
                      + dom 
                      + ( ( i == current ) ? "    *" : "" ) );
        }
        dv.addPane( "Text view", new ComponentMaker() {
            public JComponent getComponent() {
                return new AstTextShower( wcs );
            }
        } );
        dv.addPane( "XML view", new ComponentMaker() {
            public JComponent getComponent() throws TransformerException {
                Element el = new XAstWriter().makeElement( wcs );
                return new TextViewer( new DOMSource( el ) );
            }
        } );
        dv.addPane( "FITS view", new ComponentMaker() {
            public JComponent getComponent() {
                return new AstFITSShower( wcs );
            }
        } );
    }

    public FrameSet getWcs() {
        return (FrameSet) wcs.copy();
    }

    private static FrameSet getWcsFromHds( HDSObject hobj ) 
            throws NoSuchDataException {
        if ( ! NodeUtil.hasAST() ) {
            throw new NoSuchDataException( "AST native library not installed" );
        }

        /* See if it looks like a WCS component. */
        HDSObject data;
        try {
            if ( ! hobj.datName().equals( "WCS" ) ) {
                throw new NoSuchDataException( "Not called WCS" );
            }
            if ( ! hobj.datStruc() ) {
                throw new NoSuchDataException( "Not a structure" );
            }
            if ( ! hobj.datThere( "DATA" ) ) {
                throw new NoSuchDataException( "No DATA child" );
            }
            data = hobj.datFind( "DATA" );
            if ( ! data.datType().startsWith( "_CHAR" ) ) {
                throw new NoSuchDataException( "Not character data" );
            }
            if ( data.datShape().length != 1 ) {
                throw new NoSuchDataException( "DATA child not a vector" );
            }
        }
        catch ( HDSException e ) {
            throw new NoSuchDataException( e.getMessage() );
        }

        /* Try to build the WCS component by extracting it from an NDX
         * based on this object's parent.
         * This makes use of the clever stuff that NdxHandler does,
         * for instance looking at AXIS components. */
        try {
            HDSObject hparent = hobj.datParen();
            Ndx ndx = NDFNdxHandler.getInstance()
                     .makeNdx( hparent, null, AccessMode.READ );
            return Ndxs.getAst( ndx );
        }
        catch ( IllegalArgumentException e ) {
            /* May legitimately fail because the parent is not an NDF. */
            // no action.
        }
        catch ( HDSException e ) {
            /* May legitimately fail because hobj has no parent (top level). */
            // no action.
        }

        /* If that didn't work, try reading the object into a Channel
         * directly. */
        Channel chan = new WCSChannel( data );
        FrameSet fs;
        try {
            fs = (FrameSet) chan.read();
        }
        catch ( ClassCastException e ) {
            throw new NoSuchDataException( 
                "Object read from channel is not an AST FrameSet" );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( 
                "Trouble reading from channel: " + e.getMessage() );
        }
        catch ( AstException e ) {
            throw new NoSuchDataException( 
                "Trouble reading from channel: " + e.getMessage() );
        }
        if ( fs == null ) {
            throw new NoSuchDataException( 
                "No object read from AST channel" );
        }
        return fs;
    }

    private static FrameSet getWcsFromSource( Source xsrc ) 
            throws NoSuchDataException {
        try {
            Node node = new SourceReader().getDOM( xsrc );
            if ( node instanceof Element ) {
                Element el = (Element) node;
                if ( el.getTagName().equals( "wcs" ) ) {
                    if ( el.getAttribute( "encoding" ).equals( "AST-XML" ) ) {
                        Element fsel = 
                            DOMUtils.getChildElementByName( el, "FrameSet" );
                        if ( fsel != null ) {
                            return (FrameSet) new XAstReader()
                                             .makeAst( fsel );
                        }
                    }
                }
            }
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
        catch ( TransformerException e ) {
            throw new NoSuchDataException( e );
        }
        throw new NoSuchDataException( "Wrong sort of Source for AST" );
    }
}
