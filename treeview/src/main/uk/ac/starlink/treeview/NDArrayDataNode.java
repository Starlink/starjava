package uk.ac.starlink.treeview;

import java.io.IOException;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.JComponent;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.MouldArrayImpl;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.NDArrayFactory;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.util.SplatException;

public class NDArrayDataNode extends DefaultDataNode {

    private NDArray nda;
    private JComponent fullView;
    private String name;

    public NDArrayDataNode( String loc ) throws NoSuchDataException {
        this( getNda( loc ) );
    }

    public NDArrayDataNode( NDArray nda ) {
        this.nda = nda;
        URL url = nda.getURL();
        name = ( url == null ) ? "NDArray" : url.getFile();
        setLabel( name );
    }

    public String getDescription() {
        return NDShape.toString( nda.getShape() )
             + "  <" + nda.getType() + ">";
    }

    public Icon getIcon() {
        return IconFactory.getInstance()
                          .getArrayIcon( nda.getShape().getNumDims() );
    }

    public String getNodeTLA() {
        return "NDA";
    }

    public String getNodeType() {
        return "N-Dimensional Array";
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            dv.addKeyedItem( "URL", nda.getURL() );
            fullView = dv.getComponent();
            OrderedNDShape oshape = nda.getShape();
            int ndim = oshape.getNumDims();
            dv.addSeparator();
            dv.addKeyedItem( "Dimensionality", ndim );
            dv.addKeyedItem( "Origin",
                             NDShape.toString( oshape.getOrigin() ) );
            dv.addKeyedItem( "Dimensions", 
                             NDShape.toString( oshape.getDims() ) );
            dv.addKeyedItem( "Pixel bounds", boundsString( oshape ) );
            dv.addKeyedItem( "Ordering", oshape.getOrder() );
            dv.addSeparator();
            dv.addKeyedItem( "Type", nda.getType() );
            Number bh = nda.getBadHandler().getBadValue();
            if ( bh != null ) {
                dv.addKeyedItem( "Bad value", bh );
            }

            try {
                addDataViews( dv, nda, null );
            }
            catch ( IOException e ) {
                dv.logError( e );
            }
        }
        return fullView;
    }

    public static String boundsString( NDShape shape ) {
        long[] lbnd = shape.getOrigin();
        long[] ubnd = shape.getUpperBounds();
        int ndim = shape.getNumDims();
        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < ndim; i++ ) {
            if ( i > 0 ) {
                sb.append( ", " );
            }
            sb.append( lbnd[ i ] )
              .append( ':' )
              .append( ubnd[ i ] );
        }
        return sb.toString();
    }

    /**
     * Adds elements to a DetailViewer suitable for a general N-dimensional
     * array.  This method is used by the NDArrayDataNode class to add
     * things like graph view and image view panes, but is provided as
     * a public static method so that other DataNodes which represent
     * N-dimensional arrays can use it too.
     *
     * @param  dv   the DetailViewer into which the new views are to be placed
     * @param  nda  the NDArray which is to be described
     * @param  wcs  WCS FrameSet corresponding to data grid.  May be null
     * @throws  IOException  if something goes wrong in the data access
     */
    public static void addDataViews( DetailViewer dv, NDArray nda, 
                                     final FrameSet wcs ) 
            throws IOException {

        /* Get a random access version of the array since most of the
         * array views will require this. */
        Requirements req = new Requirements( AccessMode.READ )
                          .setRandom( true );
        final NDArray rnda = NDArrays.toRequiredArray( nda, req );
        final NDShape shape = rnda.getShape();
        final int ndim = shape.getNumDims();

        /* Get a version with degenerate dimensions collapsed.  Should really
         * also get a corresponding WCS and use that, but I haven't worked
         * out how to do that properly yet, so the views below either use
         * the original array with its WCS or the effective array with
         * a blank WCS. */
        final NDArray enda = effectiveArray( rnda );
        final int endim = enda.getShape().getNumDims();

        /* Add data views as appropriate. */
        if ( wcs != null && ndim == 2 && endim == 2 ) {
            dv.addPane( "WCS grids", new ComponentMaker() {
                public JComponent getComponent() throws IOException {
                    return new GridPlotter( 200, shape, wcs );
                }
            } );
        }
        dv.addPane( "Pixel values", new ComponentMaker() {
            public JComponent getComponent() throws IOException {
                if ( endim == 2 && ndim != 2 ) {
                    return new ArrayBrowser( enda );
                }
                else {
                    return new ArrayBrowser( rnda );
                }
            }
        } );
        dv.addPane( "Array statistics", new ComponentMaker() {
            public JComponent getComponent() {
                return new StatsViewer( rnda );
            }
        } );
        if ( endim == 1 && Driver.hasAST ) {
            dv.addPane( "Graph view", new ComponentMaker() {
                public JComponent getComponent() 
                        throws IOException, SplatException {
                    return new SpectrumViewer( enda, "NDArray" );
                }
            } );
        }
        if ( endim == 2 ) {
            dv.addPane( "Image view", new ComponentMaker() {
                public JComponent getComponent() throws IOException {
                    if ( endim == 2 && ndim != 2 ) {
                        return new ImageViewer( enda, null );
                    }
                    else {
                        return new ImageViewer( rnda, wcs );
                    }
                }
            } );
        }
        if ( endim > 2 ) {
            dv.addPane( "Slice view", new ComponentMaker2() {
                public JComponent[] getComponents() throws IOException {
                    if ( endim != ndim ) {
                        return new CubeViewer( rnda, null ).getComponents();
                    }
                    else {
                        return new CubeViewer( rnda, wcs ).getComponents();
                    }
                }
            } );
        }
    }

    private static NDArray getNda( String loc ) throws NoSuchDataException {
        NDArray nda;
        try {
            nda = new NDArrayFactory().makeNDArray( loc, AccessMode.READ );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
        if ( nda == null ) {
            throw new NoSuchDataException( "Not a recognised NDArray" );
        }
        return nda;
    }

    /**
     * Returns an NDArray the same as the one input, but with any
     * degenerate dimensions (ones with an extent of unity) removed.
     * This leaves the pixel sequence unchanged.  If nothing needs to
     * be done (no degenerate dimensions) the original array will be
     * returned.
     *
     * @param   nda1   the array to be reshaped (maybe)
     * @return  an array with the same data, but no degenerate dimensions.
     *          May be the same as <tt>nda1</tt>
     */
    static NDArray effectiveArray( NDArray nda1 ) {
        int isig = 0;
        NDShape shape1 = nda1.getShape();
        long[] origin1 = shape1.getOrigin();
        long[] dims1 = shape1.getDims();
        int ndim1 = shape1.getNumDims();
        int ndim2 = 0;
        for ( int i1 = 0; i1 < ndim1; i1++ ) {
            if ( dims1[ i1 ] > 1 ) { 
                ndim2++;
            }
        }
        long[] origin2 = new long[ ndim2 ];
        long[] dims2 = new long[ ndim2 ];
        int i2 = 0;
        for ( int i1 = 0; i1 < ndim1; i1++ ) {
            if ( dims1[ i1 ] > 1 ) {
                origin2[ i2 ] = origin1[ i1 ];
                dims2[ i2 ] = dims1[ i1 ];
                i2++;
            }
        }
        assert i2 == ndim2;

        NDShape shape2 = new NDShape( origin2, dims2 );
        assert shape2.getNumPixels() == shape1.getNumPixels();

        NDArray nda2 = new BridgeNDArray( new MouldArrayImpl( nda1, shape2 ) );
        return nda2;
    }

}
