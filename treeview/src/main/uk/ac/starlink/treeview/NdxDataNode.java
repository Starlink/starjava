package uk.ac.starlink.treeview;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.NDFNdxHandler;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.Ndxs;
import uk.ac.starlink.ndx.NdxIO;
import uk.ac.starlink.ndx.XMLNdxHandler;
import uk.ac.starlink.splat.util.SplatException;

/**
 * An implementation of the <tt>DataNode</tt> interface for representing
 * {@link uk.ac.starlink.ndx.Ndx} objects.
 *
 * @author   Mark Taylor (Starlink)
 */
public class NdxDataNode extends DefaultDataNode {

    private Ndx ndx;
    private String name;
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
    }

    /**
     * Initialises an NdxDataNode from an NDF structure.
     *
     * @param  hobj  an HDSObject, which should reference an NDF structure
     */
    public NdxDataNode( HDSObject hobj ) throws NoSuchDataException {
        try {
            ndx = NDFNdxHandler.getInstance().makeNdx( hobj, AccessMode.READ );
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
     * Initialises an NdxDataNode from a DOM node.  It is passed as 
     * a DOMSource rather than a simple Node so that it can contain the
     * SystemID as well, which may be necessary to resolve relative URL
     * references to data arrays etc.
     *
     * @param  xsrc  a DOMSource containing the XML representation of the Ndx
     */
    public NdxDataNode( DOMSource xsrc ) throws NoSuchDataException {
        try {
            ndx = XMLNdxHandler.getInstance().makeNdx( xsrc, AccessMode.READ );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
        if ( ndx == null ) {
            throw new NoSuchDataException( "Not an NDX" );
        }
        if ( ndx.hasTitle() ) {
            name = ndx.getTitle();
        }
        else {
            name = xsrc.getNode().getNodeName();
        }
        setLabel( name );
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
            children.add( tit );
        }

        DataNode im;
        try {
            im = childMaker.makeDataNode( NDArray.class, ndx.getImage() );
        }
        catch ( NoSuchDataException e ) {
            im = new DefaultDataNode( e );
        }
        im.setLabel( "image" );
        children.add( im );

        if ( ndx.hasVariance() ) {
            DataNode var;
            try {
                var = childMaker
                     .makeDataNode( NDArray.class, ndx.getVariance() );
            }
            catch ( NoSuchDataException e ) {
                var = new DefaultDataNode( e );
            }
            var.setLabel( "variance" );
            children.add( var );
        }

        if ( ndx.hasQuality() ) {
            DataNode qual;
            try {
                qual = childMaker
                      .makeDataNode( NDArray.class, ndx.getQuality() );
            }
            catch ( NoSuchDataException e ) {
                qual = new DefaultDataNode( e );
            }
            qual.setLabel( "quality" );
            children.add( qual );
        }

        int badbits = ndx.getBadBits();
        if ( badbits != 0 ) {
            DataNode bb = 
                new ScalarDataNode( "BadBits", "int", 
                                    "0x" + Integer.toHexString( badbits ) );
            children.add( bb );
        }
    
        if ( ndx.hasWCS() ) {
            DataNode wnode;
            try {
                wnode = childMaker.makeDataNode( ndx.getAst() );
            }
            catch ( NoSuchDataException e ) {
                wnode = new DefaultDataNode( e );
            }
            children.add( wnode );
        }

        if ( ndx.hasEtc() ) {
            DataNode etcNode;
            try {
                etcNode = childMaker.makeDataNode( ndx.getEtc() );
            }
            catch ( NoSuchDataException e ) {
                etcNode = new DefaultDataNode( e );
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

            dv.addPane( "XML representation", new ComponentMaker() {
                public JComponent getComponent() throws TransformerException {
                    return new TextViewer( ndx.toXML( url ) );
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
    public static void addDataViews( DetailViewer dv, Ndx ndx ) 
            throws IOException{
        Requirements req = new Requirements( AccessMode.READ )
                          .setRandom( true );
        final NDArray image =
            NDArrays.toRequiredArray( Ndxs.getMaskedImage( ndx ), req );
    
        final FrameSet ast = Ndxs.getAst( ndx );
        final NDShape shape = image.getShape();
        int ndim = shape.getNumDims();
        List actlist = new ArrayList();
        final String title = ndx.hasTitle() ? ndx.getTitle()
                                            : "Spectrum data";

        if ( ndx.hasWCS() && ndim == 2 ) {
            dv.addPane( "WCS grids", new ComponentMaker() {
                public JComponent getComponent() throws IOException {
                    return new GridPlotter( 200, shape, ast );
                }
            } );
        }

        String ititle = ndx.hasQuality() ? "Masked image data"
                                         : "Image data";
        dv.addPane( ititle, new ComponentMaker() {
            public JComponent getComponent() throws IOException {
                return new ArrayBrowser( image );
            }
        } );

        if ( ndim == 1 ) {
            dv.addPane( "Graph view", new ComponentMaker() {
                public JComponent getComponent()
                        throws IOException, SplatException {
                    return new SpectrumViewer( image, title );
                }
            } );
        }

        if ( ndim == 2 ) {
            dv.addPane( "Image display", new ComponentMaker() {
                public JComponent getComponent() throws IOException {
                    return new ImageViewer( image, ast );
                }
            } );
        }

        if ( ndim > 2 ) {
            dv.addPane( "Slice display", new ComponentMaker2() {
                public JComponent[] getComponents() {
                    return new CubeViewer( image, ast )
                          .getComponents();
                }
            } );
        }
                  
        if ( ndim == 1 ) {
            final ApplicationInvoker displayer = ApplicationInvoker.SPLAT;
            if ( displayer.canDisplayNDX() ) {
                Icon splatic = IconFactory.getInstance()
                                          .getIcon( IconFactory.SPLAT );
                Action splatAct = new AbstractAction( "Splat", splatic ) {
                    public void actionPerformed( ActionEvent evt ) {
                    //  try {
                    //      displayer.displayNDX( ndx );
                            System.out.println( "nope" );
                    //  }
                    //  catch ( ServiceException e ) {
                            beep();  System.out.println( "nope" );
                    //      e.printStackTrace();
                    //  }
                    }
                };
                actlist.add( splatAct );
            }
        }

        if ( ndim == 2 ) {
            final ApplicationInvoker displayer = ApplicationInvoker.SOG;
            if ( displayer.canDisplayNDX() ) {
                Icon sogic = IconFactory.getInstance()
                                        .getIcon( IconFactory.SOG );
                Action sogAct = new AbstractAction( "SoG", sogic ) {
                    public void actionPerformed( ActionEvent evt ) {
                    //  try {
                    //      displayer.displayNDX( ndx );
                            System.out.println( "nope" );
                    //  }
                    //  catch ( ServiceException e ) {
                            beep();
                    //      e.printStackTrace();
                    //  }
                    }
                };
                actlist.add( sogAct );
            }
        }

        dv.addActions( (Action[]) actlist.toArray( new Action[ 0 ] ) );
    }

}
