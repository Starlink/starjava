package uk.ac.starlink.treeview;

import java.util.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.ast.*;
import uk.ac.starlink.ast.xml.XAstReader;
import uk.ac.starlink.ast.xml.XAstWriter;
import uk.ac.starlink.hds.*;
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

    private Icon icon;
    private JComponent fullView;
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
    }

    /**
     * Constructs a WCSDataNode from a Hierarchical.
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
    public DataNode[] getChildren() {
        int nframe = wcs.getNframe();
        DataNode[] dchil = null;
        if ( dchil == null ) {
            dchil = new DataNode[ 0 ];
        }
        DataNode[] children = new DataNode[ dchil.length + nframe ];
        System.arraycopy( dchil, 0, children, 0, dchil.length );
        for ( int i = 1; i <= nframe; i++ ) {
            DataNode child;
            try {
                child = new FrameDataNode( wcs.getFrame( i ) );
            }
            catch ( NoSuchDataException e ) {
                child = new DefaultDataNode( "frame " + i + ": " 
                                           + e.getMessage() );
            }
            catch ( AstException e ) {
                child = new DefaultDataNode( "frame " + i + ": " 
                                           + e.getMessage() );
            } 
            children[ i - 1 + dchil.length ] = child;
        }
        return children;
    }

    public String getDescription() {
        return description;
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = IconFactory.getInstance().getIcon( IconFactory.WCS );
        }
        return icon;
    }

    public String getNodeTLA() {
        return "WCS";
    }

    public String getNodeType() {
        return "World Coordinate System data";
    }

    public boolean hasFullView() {
        return true;
    }
    public JComponent getFullView() {
        if ( fullView == null ) {
            int nframe = wcs.getNframe();
            int current = wcs.getCurrent();
            int base = wcs.getBase();
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            dv.addSeparator();
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
                uk.ac.starlink.ast.Frame frm = wcs.getFrame( i );
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
                    Element el = new XAstWriter().makeElement( wcs, null );
                    return new TextViewer( new DOMSource( el ) );
                }
            } );
            dv.addPane( "FITS view", new ComponentMaker() {
                public JComponent getComponent() {
                    return new AstFITSShower( wcs );
                }
            } );
        }
        return fullView;
    }

    public FrameSet getWcs() {
        return wcs;
    }

    private static FrameSet getWcsFromHds( HDSObject hobj ) 
            throws NoSuchDataException {
        if ( ! Driver.hasAST ) {
            throw new NoSuchDataException( "AST native library not installed" );
        }
        try {
            if ( hobj.datStruc() && hobj.datShape().length == 0 ) {
                HDSObject data = hobj.datFind( "DATA" );
                if ( data.datType().startsWith( "_CHAR" ) &&
                    data.datShape().length == 1 ) {
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
                else {
                    throw new NoSuchDataException( 
                        "HDSObject is not a 1-D _CHAR array" ); 
                }
            }
            else {
                throw new NoSuchDataException(
                        "HDSObject has no DATA component" );
            }
        }
        catch ( HDSException e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
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
                                             .makeAst( fsel, null );
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
