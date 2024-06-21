package uk.ac.starlink.datanode.nodes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.datanode.viewers.TextViewer;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.NDFNdxHandler;
import uk.ac.starlink.hds.HDSReference;
import uk.ac.starlink.hdx.HdxException;
import uk.ac.starlink.hdx.HdxFactory;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.Ndxs;
import uk.ac.starlink.ndx.NdxIO;
import uk.ac.starlink.ndx.XMLNdxHandler;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.URLUtils;

/**
 * An implementation of the <code>DataNode</code> interface for representing
 * {@link uk.ac.starlink.ndx.Ndx} objects.
 *
 * @author   Mark Taylor (Starlink)
 */
public class NdxDataNode extends DefaultDataNode {

    private Ndx ndx;
    private String name;
    private String desc;
    private URL url;

    /**
     * Initialises an NdxDataNode from a string giving a URL or filename.
     *
     * @param  loc  the location of a readable Ndx
     */
    public NdxDataNode( String loc ) throws NoSuchDataException {
        try {
            this.url = new URL( new URL( "file:." ), loc );
        }
        catch ( MalformedURLException e ) {
            this.url = null;
        }
        try {
            this.ndx = new NdxIO().makeNdx( loc, AccessMode.READ );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( "Can't make NDX", e );
        }
        catch ( IllegalArgumentException e ) {
            throw new NoSuchDataException( "Can't make NDX", e );
        }
        if ( ndx == null ) {
            throw new NoSuchDataException( "URL " + loc + 
                                           " doesn't look like an NDX" );
        }
        name = loc.replaceFirst( "#.*$", "" );
        name = name.replaceFirst( "^.*/", "" );
        setLabel( name );
        registerDataObject( DataType.NDX, ndx );
    }

    public NdxDataNode( File file ) throws NoSuchDataException {
        this( file.toString() );
    }

    /**
     * Initialises an NdxDataNode from an NDF structure.
     *
     * @param  hobj  an HDSObject, which should reference an NDF structure
     */
    public NdxDataNode( HDSObject hobj ) throws NoSuchDataException {
        try {
            url = new HDSReference( hobj ).getURL();
        }   
        catch ( HDSException e ) {
            url = null;
        }
        try {
            ndx = NDFNdxHandler.getInstance()
                               .makeNdx( hobj, url, AccessMode.READ );
            name = hobj.datName();
        }
        catch ( HDSException e ) {
            throw new NoSuchDataException( e );
        }
        if ( ndx == null ) {
            throw new NoSuchDataException( "Not an NDF structure" );
        }
        setLabel( name );
    }

    /**
     * Initialises an NdxDataNode from an XML source.  It is passed as 
     * a Source rather than a simple Node so that it can contain the
     * SystemID as well, which may be necessary to resolve relative URL
     * references to data arrays etc.
     *
     * @param  xsrc  a Source containing the XML representation of the Ndx
     */
    public NdxDataNode( Source xsrc ) throws NoSuchDataException {
        try {
            ndx = XMLNdxHandler.getInstance().makeNdx( xsrc, AccessMode.READ );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
        catch ( IllegalArgumentException e ) {
            throw new NoSuchDataException( e );
        }
        if ( ndx == null ) {
            throw new NoSuchDataException( "Not an NDX" );
        }
        if ( ndx.hasTitle() ) {
            name = ndx.getTitle();
        }
        else {
            name = "ndx";
        }
        this.url = URLUtils.makeURL( xsrc.getSystemId() );
        setLabel( name );
    }

    /**
     * Constructs a new NdxDataNode from an XML document.
     * For efficiency, this really ought to defer the DOM construction 
     * parse until the contents are actually needed.  However, there
     * probably aren't any large NDX XML documents out there, so it 
     * probably doesn't matter.
     */
    public NdxDataNode( XMLDocument xdoc ) throws NoSuchDataException {
        this( xdoc.constructDOM( false ) );
    }

    public String getDescription() {
        if ( desc == null ) {
            desc = new NDShape( ndx.getImage().getShape() ).toString();
        }
        return desc;
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        List children = new ArrayList();

        if ( ndx.hasTitle() ) {
            DataNode tit;
            tit = new ScalarDataNode( "Title", "string", ndx.getTitle() );
            getChildMaker().configureDataNode( tit, this, null );
            children.add( tit );
        }

        if ( ndx.hasLabel() ) {
            DataNode lab;
            lab = new ScalarDataNode( "Label", "string", ndx.getLabel() );
            getChildMaker().configureDataNode( lab, this, null );
            children.add( lab );
        }

        if ( ndx.hasUnits() ) {
            DataNode uni;
            uni = new ScalarDataNode( "Units", "string", ndx.getUnits() );
            getChildMaker().configureDataNode( uni, this, null );
            children.add( uni );
        }

        DataNode im = makeChild( ndx.getImage() );
        im.setLabel( "image" );
        children.add( im );

        if ( ndx.hasVariance() ) {
            DataNode var = makeChild( ndx.getVariance() );
            var.setLabel( "variance" );
            children.add( var );
        }

        if ( ndx.hasQuality() ) {
            DataNode qual = makeChild( ndx.getQuality() );
            qual.setLabel( "quality" );
            children.add( qual );
        }

        int badbits = ndx.getBadBits();
        if ( badbits != 0 ) {
            DataNode bb = 
                new ScalarDataNode( "BadBits", "int", 
                                    "0x" + Integer.toHexString( badbits ) );
            getChildMaker().configureDataNode( bb, this, null );
            children.add( bb );
        }
    
        if ( NodeUtil.hasAST() && ndx.hasWCS() ) {
            DataNode wnode = makeChild( ndx.getAst() );
            children.add( wnode );
        }

        if ( ndx.hasEtc() ) {
            DataNode etcNode = makeChild( ndx.getEtc() );
            children.add( etcNode );
        }

        return children.iterator();
    }

    public String getName() {
        return name;
    }

    public String getNodeTLA() {
        return "NDX";
    }

    public String getNodeType() {
        return "NDX structure";
    }

    public Icon getIcon() {
        return IconFactory.getIcon( IconFactory.NDX );
    }

    public String getPathElement() {
        return getLabel();
    }

    public String getPathSeparator() {
        return ".";
    }

    public void configureDetail( DetailViewer dv ) {
        if ( ndx.hasTitle() ) {
            dv.addKeyedItem( "Title", ndx.getTitle() );
        }

        dv.addPane( "HDX view", new ComponentMaker() {
            public JComponent getComponent()
                    throws TransformerException, HdxException,
                           MalformedURLException {
                URI uri = URLUtils.urlToUri( url );
                Source src = HdxFactory.getInstance()
                                       .newHdxContainer( ndx.getHdxFacade() )
                                       .getSource( uri );
                return new TextViewer( src );
            }
        } );
    }
}
