package uk.ac.starlink.treeview;

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import uk.ac.starlink.hdx.array.NDArray;
import uk.ac.starlink.hdx.array.NDArrayFactory;
import uk.ac.starlink.hdx.array.NDShape;
import uk.ac.starlink.hdx.array.OrderedNDShape;
import uk.ac.starlink.hdx.array.Requirements;
import uk.ac.starlink.splat.util.SplatException;

public class NDArrayDataNode extends DefaultDataNode {

    private NDArray nda;
    private Cartesian shape;
    private Cartesian origin;
    private JComponent fullView;
    private String name;
    private String desc;

    public NDArrayDataNode( String loc ) throws NoSuchDataException {
        this( getNda( loc ) );

        OrderedNDShape oshape = nda.getShape();
        shape = new Cartesian( oshape.getDims() );
        origin = new Cartesian( oshape.getOrigin() );
        desc = shape.shapeDescriptionWithOrigin( origin )
             + "  <" + nda.getType() + ">";
    }

    public NDArrayDataNode( NDArray nda ) {
        this.nda = nda;
        OrderedNDShape oshape = nda.getShape();
        shape = new Cartesian( oshape.getDims() );
        origin = new Cartesian( oshape.getOrigin() );
        desc = shape.shapeDescriptionWithOrigin( origin )
             + "  <" + nda.getType() + ">";
        URL url = nda.getURL();
        name = ( url == null ) ? "NDArray" : url.getFile();
        setLabel( name );
    }

    public String getDescription() {
        return desc;
    }

    public Icon getIcon() {
        return IconFactory.getInstance().getArrayIcon( shape.getNdim() );
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
            dv.addKeyedItem( "Pixel bounds",
                             shape.shapeDescriptionWithOrigin( origin ) );
            dv.addKeyedItem( "Ordering", oshape.getOrder().toString() );
            dv.addSeparator();
            dv.addKeyedItem( "Type", nda.getType().toString() );
            Number bh = nda.getBadHandler().getBadValue();
            if ( bh != null ) {
                dv.addKeyedItem( "Bad value", bh.toString() );
            }

            // Action act1 = new AbstractAction( "Do stuff" ) {
            //     public void actionPerformed( ActionEvent evt ) {
            //         System.out.println( "oof" );
            //     }
            // };
            // Action act2 = new AbstractAction( "More stuff" ) {
            //     public void actionPerformed( ActionEvent evt ) {
            //         System.out.println( "foo" );
            //     }
            // };
            // dv.addActions( new Action[] { act1, act2 } );

            dv.addPane( "Array data", new ComponentMaker() {
                public JComponent getComponent() throws IOException {
                    return new ArrayBrowser( getRandomNDA() );
                }
            } );
            if ( ndim == 1 ) {
                dv.addPane( "Graph view", new ComponentMaker() {
                    public JComponent getComponent() throws IOException, 
                                                            SplatException {
                        return new SpectrumViewer( getNDA(), "NDArray" );
                    }
                } );
            }
            if ( ndim == 2 ) {
                dv.addPane( "Image view", new ComponentMaker() {
                    public JComponent getComponent() throws IOException {
                        return new ImageViewer( getRandomNDA(), null );
                    }
                } );
            }
        }
        return fullView;
    }

    private static NDArray getNda( String loc ) throws NoSuchDataException {
        URL url;
        try {
            url = NDArrayFactory.makeURL( loc );
        }
        catch ( MalformedURLException e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
        NDArray nda;
        try {
            nda = NDArrayFactory.makeReadableNDArray( url );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
        return nda;
    }

    private NDArray getNDA() throws IOException {
        return NDArrayFactory.makeReadableNDArray( nda.getURL() );
    }

    private NDArray getRandomNDA() throws IOException {
        NDArray newnda = NDArrayFactory.makeReadableNDArray( nda.getURL() );
        Requirements req = new Requirements( Requirements.Mode.READ ) 
                          .setRandom( true );
        return NDArrayFactory.toRequiredNDArray( newnda, req );
    }
}
