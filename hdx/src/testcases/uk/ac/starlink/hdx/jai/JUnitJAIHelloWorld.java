package uk.ac.starlink.hdx.jai;

import junit.framework.TestCase;

import javax.swing.JFrame;
import java.awt.Dimension;

public class JUnitJAIHelloWorld extends TestCase
{
    public JUnitJAIHelloWorld( String name )
    {
        super( name );
    }

    public void testJAIHelloWorld()
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
    }
}


