package uk.ac.starlink.treeview;

import java.io.File;
import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * A DataNode implementation which displays Treeview's known demo data
 * directory.
 *
 * @author   Mark Taylor (Starlink)
 */
public class DemoDataNode extends FileDataNode {

    public static final String DEMO_DIR_PROPERTY = 
        "uk.ac.starlink.treeview.demodir";

    private String name = "Demonstration data";
    private Icon icon;
    private JComponent fullView;

    public DemoDataNode() throws NoSuchDataException {
        super( getDemoDir() );
        setLabel( name );
    }

    public String getName() {
        return name;
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = IconFactory.getInstance().getIcon( IconFactory.DEMO );
        }
        return icon;
    }

    public String getNodeTLA() {
        return "DEM";
    }

    public String getNodeType() {
        return "Demonstration data";
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            dv.addSeparator();
            dv.addPane( "Information", 
                        new HTMLDocComponentMaker( "demo.html" ) );

            /* Questionable use of protected field here to subvert normal
             * behaviour of a DetailViewer; we want the info panel to
             * be displayed immediately since the idea is to make it
             * very clear to users what is going on. */
            dv.tabbed.setSelectedIndex( 1 );
        }
        return fullView;
    }

    private static File getDemoDir() throws NoSuchDataException {
        String demoloc = System.getProperty( DEMO_DIR_PROPERTY );
        if ( demoloc == null || demoloc.trim().length() == 0 ) {
            throw new NoSuchDataException( "No demo data available" );
        }
        File demodir = new File( demoloc );
        if ( ! demodir.canRead() ) {
            throw new NoSuchDataException( "Demo data directory " + demodir + 
                                           " is not readable" );
        }
        return demodir;
    }
}
