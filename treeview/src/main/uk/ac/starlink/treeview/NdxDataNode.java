package uk.ac.starlink.treeview;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.xml.rpc.ServiceException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.ast.FrameSet;
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
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.util.URLUtils;

/**
 * An implementation of the <tt>DataNode</tt> interface for representing
 * {@link uk.ac.starlink.ndx.Ndx} objects.
 *
 * @author   Mark Taylor (Starlink)
 */
public class NdxDataNode extends DefaultDataNode {

    private Ndx ndx;
    private String name;
    private String desc;
    private Icon icon;
    private JComponent fullView;
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
    }

    public NdxDataNode( File file ) throws NoSuchDataException {
        this( file.toString() );
        setPath( file.getAbsolutePath() );
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


    public String getDescription() {
        if ( desc == null ) {
            desc = new NDShape( ndx.getImage().getShape() ).toString();
        }
        return desc;
    }

    public boolean allowsChildren() {
        return true;
    }

    protected DataNode[] getChildren() {
        DataNodeFactory childMaker = getChildMaker();
        List children = new ArrayList();

        if ( ndx.hasTitle() ) {
            DataNode tit;
            tit = new ScalarDataNode( "Title", "string", ndx.getTitle() );
            tit.setCreator( new CreationState( this ) );
            children.add( tit );
        }

        DataNode im;
        try {
            im = childMaker.makeDataNode( this, ndx.getImage() );
        }
        catch ( NoSuchDataException e ) {
            im = childMaker.makeErrorDataNode( this, e );
        }
        im.setLabel( "image" );
        children.add( im );

        if ( ndx.hasVariance() ) {
            DataNode var;
            try {
                var = childMaker.makeDataNode( this, ndx.getVariance() );
            }
            catch ( NoSuchDataException e ) {
                var = childMaker.makeErrorDataNode( this, e );
            }
            var.setLabel( "variance" );
            children.add( var );
        }

        if ( ndx.hasQuality() ) {
            DataNode qual;
            try {
                qual = childMaker.makeDataNode( this, ndx.getQuality() );
            }
            catch ( NoSuchDataException e ) {
                qual = childMaker.makeErrorDataNode( this, e );
            }
            qual.setLabel( "quality" );
            children.add( qual );
        }

        int badbits = ndx.getBadBits();
        if ( badbits != 0 ) {
            DataNode bb = 
                new ScalarDataNode( "BadBits", "int", 
                                    "0x" + Integer.toHexString( badbits ) );
            bb.setCreator( new CreationState( this ) );
            children.add( bb );
        }
    
        if ( Driver.hasAST && ndx.hasWCS() ) {
            DataNode wnode;
            try {
                wnode = childMaker.makeDataNode( this, ndx.getAst() );
            }
            catch ( NoSuchDataException e ) {
                wnode = childMaker.makeErrorDataNode( this, e );
            }
            children.add( wnode );
        }

        if ( ndx.hasEtc() ) {
            DataNode etcNode;
            try {
                etcNode = childMaker.makeDataNode( this, ndx.getEtc() );
            }
            catch ( NoSuchDataException e ) {
                etcNode = childMaker.makeErrorDataNode( this, e );
            }
            children.add( etcNode );
        }

        return (DataNode[]) children.toArray( new DataNode[ 0 ] );
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
        if ( icon == null ) {
            icon = IconFactory.getInstance().getIcon( IconFactory.NDX );
        }
        return icon;
    }

    public String getPathElement() {
        return getLabel();
    }

    public String getPathSeparator() {
        return ".";
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            dv.addSeparator();
            if ( ndx.hasTitle() ) {
                dv.addKeyedItem( "Title", ndx.getTitle() );
            }

            dv.addPane( "HDX representation", new ComponentMaker() {
                public JComponent getComponent()
                        throws TransformerException, HdxException {
                    URI uri = URLUtils.urlToUri( url );
                    Source src = HdxFactory
                                .getInstance()
                                .newHdxContainer( ndx.getHdxFacade() )
                                .getSource( null );
                    return new TextViewer( ndx.getHdxFacade()
                                              .getSource( uri ) );
                }
            } );

            try {
                addDataViews( dv, ndx );
            }
            catch ( IOException e ) {
                dv.logError( e );
            }
        }
        return fullView;
    }

    /**
     * Adds visual elements to a DetailViewer suitable for a general
     * NDX-type structure. This method is used by the NdxDataNode class
     * to add things like an image view pane, but is provided as a
     * public static method so that other DataNodes which represent
     * NDX-type data can use it too.
     *
     * @param   dv   the DetailViewer into which the new visual elements are
     *               to be placed
     * @param   ndx  the NDX which is to be described
     * @throws  IOException  if anything goes wrong with the data access
     */
    public static void addDataViews( DetailViewer dv, final Ndx ndx ) 
            throws IOException{
        Requirements req = new Requirements( AccessMode.READ )
                          .setRandom( true );
        final NDArray image =
            NDArrays.toRequiredArray( Ndxs.getMaskedImage( ndx ), req );
    
        final FrameSet ast = Driver.hasAST ? Ndxs.getAst( ndx ) : null;
        final NDShape shape = image.getShape();
        final int ndim = shape.getNumDims();

        /* Get a version with degenerate dimensions collapsed.  Should really
         * also get a corresponding WCS and use that, but I haven't worked
         * out how to do that properly yet, so the views below either use
         * the original array with its WCS or the effective array with
         * a blank WCS. */
        final NDArray eimage;
        final int endim;
        if ( shape.getNumPixels() > 1 ) {
            eimage = NDArrayDataNode.effectiveArray( image );
            endim = eimage.getShape().getNumDims();
        }
        else {
            eimage = image;
            endim = ndim;
        }

        /* Add data views as appropriate. */
        if ( Driver.hasAST && ndx.hasWCS() && ndim == 2 && endim == 2 ) {
            dv.addScalingPane( "WCS grids", new ComponentMaker() {
                public JComponent getComponent() throws IOException {
                    return new GridPlotter( shape, ast );
                }
            } );
        }

        dv.addScalingPane( "Pixel values", new ComponentMaker() {
            public JComponent getComponent() throws IOException {
                if ( endim == 2 && ndim != 2 ) {
                    return new ArrayBrowser( eimage );
                }
                else {
                    return new ArrayBrowser( image );
                }
            }
        } );

        dv.addPane( "Statistics", new ComponentMaker() {
            public JComponent getComponent() {
                return new StatsViewer( image );
            }
        } );

        if ( endim == 1 && Driver.hasAST ) {
            dv.addScalingPane( "Graph view", new ComponentMaker() {
                public JComponent getComponent()
                        throws IOException, SplatException {
                    return new GraphViewer( ndx );
                }
            } );
        }

        if ( ( ndim == 2 || endim == 2 ) && Driver.hasJAI ) {
            dv.addPane( "Image display", new ComponentMaker() {
                public JComponent getComponent() throws IOException {
                    if ( endim == 2 && ndim != 2 ) {
                        return new ImageViewer( eimage, null );
                    }
                    else {
                        return new ImageViewer( image, ast );
                    }
                }
            } );
        }

        if ( endim > 2 && Driver.hasJAI ) {
            dv.addPane( "Slices", new ComponentMaker() {
                public JComponent getComponent() {
                    if ( endim != ndim ) {
                        return new SliceViewer( image, null );
                    }
                    else {
                        return new SliceViewer( image, ast );
                    }
                }
            } );
        }
        if ( endim == 3 && Driver.hasJAI ) {
            dv.addPane( "Collapsed", new ComponentMaker() {
                public JComponent getComponent() throws IOException {
                    if ( endim != ndim ) {
                        return new CollapseViewer( eimage, null );
                    }
                    else {
                        return new CollapseViewer( image, ast );
                    }
                }
            } );
        }
                  
        /* Add actions as appropriate. */
        List actlist = new ArrayList();
        if ( ndim == 1 && endim == 1 ) {
            final ApplicationInvoker displayer = ApplicationInvoker.SPLAT;
            if ( displayer.canDisplayNDX() ) {
                Icon splatic = IconFactory.getInstance()
                                          .getIcon( IconFactory.SPLAT );
                Action splatAct = new AbstractAction( "Splat", splatic ) {
                    public void actionPerformed( ActionEvent evt ) {
                        try {
                            displayer.displayNDX( ndx );
                        }
                        catch ( ServiceException e ) {
                            beep();
                            e.printStackTrace();
                        }
                    }
                };
                actlist.add( splatAct );
            }
        }

        if ( ndim == 2 && endim == 2 ) {
            final ApplicationInvoker displayer = ApplicationInvoker.SOG;
            if ( displayer.canDisplayNDX() ) {
                Icon sogic = IconFactory.getInstance()
                                        .getIcon( IconFactory.SOG );
                Action sogAct = new AbstractAction( "SoG", sogic ) {
                    public void actionPerformed( ActionEvent evt ) {
                        try {
                            displayer.displayNDX( ndx );
                        }
                        catch ( ServiceException e ) {
                            beep();
                            e.printStackTrace();
                        }
                    }
                };
                actlist.add( sogAct );
            }
        }
        dv.addActions( (Action[]) actlist.toArray( new Action[ 0 ] ) );
    }

}
