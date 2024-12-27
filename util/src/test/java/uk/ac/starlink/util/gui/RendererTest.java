package uk.ac.starlink.util.gui;

import javax.swing.JFrame;
import javax.swing.JComboBox;
import javax.swing.JList;
import java.awt.HeadlessException;

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

/**
 * Tests for the FileNameCellRenderer component.
 *
 * @author  Peter W. Draper
 */
public class RendererTest {

    public void testRenderer() 
        throws IOException
    {
        try {
            //  Collect local file names.
            File[] files = new File( ".." ).getCanonicalFile().listFiles();
            Vector v = new Vector();
            for ( int i = 0; i < files.length; i++ ) {
                v.add( files[ i ] );
            }
            
            JFrame frame = new JFrame( "FileNameListCellRenderer Test" );
            frame.getContentPane().setLayout( new BorderLayout() );
            
            JList list = new JList( v );
            FileNameListCellRenderer r = new FileNameListCellRenderer();
            frame.getContentPane().add( list, BorderLayout.NORTH );
            list.setCellRenderer( r );
            
            JComboBox box = new JComboBox( v );
            r = new FileNameListCellRenderer( box );
            frame.getContentPane().add( box, BorderLayout.SOUTH );
            box.setRenderer( r );
            
            frame.pack();
            frame.setVisible( true );
            frame.setSize( new Dimension( 200, frame.getHeight() ) );
            
            try {
                Thread.currentThread().sleep( 5000 );
            }
            catch ( InterruptedException e ) {
                // no action
            }
        }
        catch ( HeadlessException he ) {
            System.out.println( "Headless environment - no GUI test" );
        }
    }
}
