package uk.ac.starlink.treeview;

import java.awt.HeadlessException;
import java.io.File;
import javax.swing.JFrame;
import junit.framework.TestCase;

public class TreeChooserTest extends TestCase {

    public void setUp() {
        String basedir = System.getProperty( "ant.basedir" );
        if ( basedir == null ) {
            basedir = "/mbt/starjava/java";
        }
        System.setProperty( "uk.ac.starlink.treeview.demodir",
                            basedir + File.separator +
                            "etc" + File.separator +
                            "treeview" + File.separator +
                            "demo" );
    }

    public TreeChooserTest( String name ) {
        super( name );
    }

    public void testTreeChooser() throws NoSuchDataException {
        try {
            TreeNodeChooser chooser = new TreeNodeChooser( new DemoDataNode() );
        }
        catch ( HeadlessException e ) {
            System.out.println( "Headless environment - no GUI test" );
        }
    }

    public static void main( String[] args )
            throws NoSuchDataException, InterruptedException {
        new TreeChooserTest( "test" ).setUp();
        DataNode root = args.length == 0 
                      ? new DemoDataNode()
                      : new DataNodeFactory().makeDataNode( null, args[ 0 ] );
        TreeNodeChooser chooser = new TableNodeChooser( root );

        //JFrame frm = new JFrame();
        //frm.getContentPane().add( chooser );
        //frm.pack();
        //frm.setVisible( true );
        //Thread.currentThread().sleep( 160000 );

        System.out.println( chooser
                           .chooseDataNode( null, "Choose", "Chooser test" ) );
        System.exit( 0 );
    }
}
