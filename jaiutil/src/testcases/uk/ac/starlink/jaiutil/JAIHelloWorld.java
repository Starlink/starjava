package uk.ac.starlink.jaiutil;

import uk.ac.starlink.hdx.*;
import uk.ac.starlink.jaiutil.*;
import uk.ac.starlink.hdx.array.*;
import com.sun.media.jai.codec.ImageCodec;

import javax.swing.*;
import javax.media.jai.*;
import javax.media.jai.widget.ScrollingImagePanel;

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;

public class JAIHelloWorld extends JPanel
{
    private PlanarImage source = null;

    public JAIHelloWorld()
    {
        //java.net.URL url = JAIHelloWorld.class.getResource( "temp.xml" );
        //RenderedImage src = JAI.create( "url", url );
        RenderedImage src = JAI.create( "fileload", "src/testcases/uk/ac/starlink/jaiutil/temp.xml" );

        //  Draw image scaled by 0.5 (for fun).
        Interpolation interp = 
            Interpolation.getInstance( Interpolation.INTERP_NEAREST ); 

        ParameterBlock param = new ParameterBlock();
        param.addSource( src );
        param.add( 0.5F );
        param.add( 0.5F );
        param.add( 0.0F );
        param.add( 0.0F );
        param.add( interp );

        RenderedOp src2 = JAI.create( "scale", param );

        ScrollingImagePanel panel = 
            new ScrollingImagePanel( src2, 100, 100 );

        Font font = new Font( "SansSerif", Font.BOLD, 24 );
        JLabel title = new JLabel( "Hello World" );
        title.setFont( font );
        title.setLocation( 0, 32 );

        setOpaque( true );
        setLayout( new BorderLayout() );

        add( title, BorderLayout.NORTH );
        add( panel, BorderLayout.CENTER );
    }

    // static initializer
    static {
        // add NDArray support
        ImageCodec.registerCodec( new HDXCodec() );
    }

     public static void main( String[] args )
     {
         JFrame frame = new JFrame( "Hello World" );
         frame.getContentPane().add( new JAIHelloWorld() );
         frame.setSize( new Dimension( 200, 200 ) );
         frame.setVisible( true );
         try {
             Thread.currentThread().sleep( 1000 );
         }
         catch ( InterruptedException ex ) {
             // no action
         }
         System.exit( 0 );
     }
}
