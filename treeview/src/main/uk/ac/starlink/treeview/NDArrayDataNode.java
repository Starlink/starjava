package uk.ac.starlink.treeview;

import java.io.IOException;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.JComponent;
import uk.ac.starlink.array.AccessMode;
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
            dv.addKeyedItem( "URL", "" + nda.getURL() );
            fullView = dv.getComponent();
            OrderedNDShape oshape = nda.getShape();
            int ndim = oshape.getNumDims();
            dv.addSeparator();
            dv.addKeyedItem( "Dimensionality", "" + ndim );
            dv.addKeyedItem( "Origin",
                             NDShape.toString( oshape.getOrigin() ) );
            dv.addKeyedItem( "Dimensions", 
                             NDShape.toString( oshape.getDims() ) );
            dv.addKeyedItem( "Pixel bounds", boundsString( oshape ) );
            dv.addKeyedItem( "Ordering", oshape.getOrder().toString() );
            dv.addSeparator();
            dv.addKeyedItem( "Type", nda.getType().toString() );
            Number bh = nda.getBadHandler().getBadValue();
            if ( bh != null ) {
                dv.addKeyedItem( "Bad value", bh.toString() );
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
     * @throws  IOException  if something goes wrong in the data access
     */
    public static void addDataViews( DetailViewer dv, NDArray nda, 
                                     final FrameSet wcs ) 
            throws IOException {
        Requirements req = new Requirements( AccessMode.READ )
                          .setRandom( true );
        final NDArray rnda = NDArrays.toRequiredArray( nda, req );
        final NDShape shape = rnda.getShape();
        int ndim = shape.getNumDims();
        if ( wcs != null && ndim == 2 ) {
            dv.addPane( "WCS grids", new ComponentMaker() {
                public JComponent getComponent() throws IOException {
                    return new GridPlotter( 200, shape, wcs );
                }
            } );
        }
        dv.addPane( "Array data", new ComponentMaker() {
            public JComponent getComponent() throws IOException {
                return new ArrayBrowser( rnda );
            }
        } );
        if ( ndim == 1 ) {
            dv.addPane( "Graph view", new ComponentMaker() {
                public JComponent getComponent() 
                        throws IOException, SplatException {
                    return new SpectrumViewer( rnda, "NDArray" );
                }
            } );
        }
        if ( ndim == 2 ) {
            dv.addPane( "Image view", new ComponentMaker() {
                public JComponent getComponent() throws IOException {
                    return new ImageViewer( rnda, wcs );
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

}
