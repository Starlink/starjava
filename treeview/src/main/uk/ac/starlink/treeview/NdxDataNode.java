package uk.ac.starlink.treeview;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.xml.rpc.ServiceException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.BufferedDataOutputStream;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.fits.FitsNdxHandler;
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
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.URLUtils;

/**
 * An implementation of the <tt>DataNode</tt> interface for representing
 * {@link uk.ac.starlink.ndx.Ndx} objects.
 *
 * @author   Mark Taylor (Starlink)
 */
public class NdxDataNode extends DefaultDataNode implements Draggable {

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
    
        if ( TreeviewUtil.hasAST() && ndx.hasWCS() ) {
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
                    throws TransformerException, HdxException {
            URI uri = URLUtils.urlToUri( url );
            Source src = HdxFactory.getInstance()
                                   .newHdxContainer( ndx.getHdxFacade() )
                                   .getSource( uri );
                return new TextViewer( src );
            }
        } );

        try {
            addDataViews( dv, ndx );
        }
        catch ( IOException e ) {
            dv.logError( e );
        }
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
    
        final FrameSet ast = TreeviewUtil.hasAST() ? Ndxs.getAst( ndx ) : null;
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
        if ( TreeviewUtil.hasAST() && ndx.hasWCS() &&
             ndim == 2 && endim == 2 ) {
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

        if ( endim == 1 && TreeviewUtil.hasAST() ) {
            dv.addScalingPane( "Graph view", new ComponentMaker() {
                public JComponent getComponent()
                        throws IOException, SplatException {
                    return new GraphViewer( ndx );
                }
            } );
        }

        if ( ( ndim == 2 || endim == 2 ) && TreeviewUtil.hasJAI() ) {
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

        if ( endim > 2 && TreeviewUtil.hasJAI() ) {
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
        if ( endim == 3 && TreeviewUtil.hasJAI() ) {
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
                Icon splatic = IconFactory.getIcon( IconFactory.SPLAT );
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
                Icon sogic = IconFactory.getIcon( IconFactory.SOG );
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

    public static void customiseTransferable( DataNodeTransferable trans,
                                              final Ndx ndx ) {

        /* If this is a persistent NDX, serialise it to XML. */
        if ( ndx.isPersistent() ) {
            DataSource xmlSrc = new DataSource() {
                public InputStream getRawInputStream() throws IOException {
                    try {
                        Source xsrc = ndx.getHdxFacade().getSource( null );
                        return new SourceReader().getXMLStream( xsrc );
                    }
                    catch ( HdxException e ) {
                        throw (IOException) new IOException( e.getMessage() )
                                           .initCause( e );
                    }
                }
                public String getName() {
                    return ndx.hasTitle() ? ndx.getTitle() : "NDX";
                }
                public URL getURL() {
                    return null;
                }
            };
            trans.addDataSource( xmlSrc, "application/xml" );
        }

        /* In any case, we can provide it serialised in its entirety to
         * a FITS file. */
        DataSource fitsSrc = new DataSource() {
            public InputStream getRawInputStream() throws IOException {
                final PipedOutputStream ostrm = new PipedOutputStream();
                InputStream istrm = new PipedInputStream( ostrm );
                new Thread() {
                    public void run() {
                        try {
                            URL dummyUrl = new URL( "file://localhost/dummy" );
                            ArrayDataOutput strm = 
                                new BufferedDataOutputStream( ostrm );
                            FitsNdxHandler.getInstance()
                                          .outputNdx( strm, dummyUrl, ndx );
                            strm.close();
                        }
                        catch ( MalformedURLException e ) {
                            throw new AssertionError( e );
                        }
                        catch ( IOException e ) {
                            // May well catch an exception here if not all
                            // the output is consumed
                        }
                        finally {
                            try {
                                ostrm.close();
                            }
                            catch ( IOException e ) {
                            }
                        }
                    }
                }.start();
                return istrm;
            }
            public String getName() {
                return ndx.hasTitle() ? ndx.getTitle() : "NDX";
            }
            public URL getURL() {
                return null;
            }
        };
        trans.addDataSource( fitsSrc, "application/fits" );
    }

    public void customiseTransferable( DataNodeTransferable trans ) {
        customiseTransferable( trans, ndx );
    }

}
