package uk.ac.starlink.datanode.nodes;

import java.io.IOException;
import java.net.URL;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrayFactory;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.OrderedNDShape;

public class NDArrayDataNode extends DefaultDataNode {

    private NDArray nda;
    private String name;

    public NDArrayDataNode( NDArray nda ) {
        this.nda = nda;
        URL url = nda.getURL();
        name = ( url == null ) ? "NDArray" : url.getFile();
        setLabel( name );
        setIconID( IconFactory.getArrayIconID( nda.getShape().getNumDims() ) );
        registerDataObject( DataType.ARRAY, new ArrayContainer( nda, null ) );
    }

    public NDArrayDataNode( String loc ) throws NoSuchDataException {
        this( getNda( loc ) );
    }

    public String getDescription() {
        return NDShape.toString( nda.getShape() )
             + "  <" + nda.getType() + ">";
    }

    public String getPathElement() {
        return getLabel();
    }

    public String getPathSeparator() {
        return ".";
    }

    public String getNodeTLA() {
        return "NDA";
    }

    public String getNodeType() {
        return "N-Dimensional Array";
    }

    public void configureDetail( DetailViewer dv ) {
        dv.addKeyedItem( "URL", nda.getURL() );
        OrderedNDShape oshape = nda.getShape();
        int ndim = oshape.getNumDims();
        dv.addSeparator();
        dv.addKeyedItem( "Dimensionality", ndim );
        dv.addKeyedItem( "Origin", NDShape.toString( oshape.getOrigin() ) );
        dv.addKeyedItem( "Dimensions", NDShape.toString( oshape.getDims() ) );
        dv.addKeyedItem( "Pixel bounds", boundsString( oshape ) );
        dv.addKeyedItem( "Ordering", oshape.getOrder() );
        dv.addSeparator();
        dv.addKeyedItem( "Type", nda.getType() );
        Number bh = nda.getBadHandler().getBadValue();
        if ( bh != null ) {
            dv.addKeyedItem( "Bad value", bh );
        }
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
