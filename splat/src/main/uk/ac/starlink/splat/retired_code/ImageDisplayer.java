package star.jspec;
/*
 * Swing.
 */

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;
import star.jspec.imagedata.*;

/* 
 * This application renders an NDF as an image.
 */

public class ImageDisplayer extends JFrame {
    public static void main(String[] args) {
        
        //  Access the NDF.
        NDFJ ndf = new NDFJ();
        ndf.open("frame");
        byte[] stream = ndf.get1DByte( "data", true );
        int[] dims = ndf.getDims();
        
        ImageDisplayer f = new ImageDisplayer();

        //  Create ColorModel.
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        for ( int i = 0; i < 255; i++ ) {
            r[i] = g[i] = b[i] = (byte) i;
        }
        IndexColorModel cm = new IndexColorModel( 8, 256, r, g, b, 0 );

        Image image = f.createImage(new MemoryImageSource(dims[0],
                                                          dims[1], 
                                                          cm,
                                                          stream, 
                                                          0,
                                                          dims[0]));
        ImagePanel imagePanel = new ImagePanel(image);
        
        f.addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent e ) {
                System.exit(0);
            }
        });
        
        f.getContentPane().add( imagePanel, BorderLayout.CENTER );
        f.setSize( new Dimension(500,500) );
        f.setVisible( true );
    }
}

class ImagePanel extends JPanel {
    Image image;

    public ImagePanel(Image image) {
        this.image = image;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g); //paint background

        //Draw image at its natural size.
        g.drawImage( image, 0, 0, this );
    }
}
