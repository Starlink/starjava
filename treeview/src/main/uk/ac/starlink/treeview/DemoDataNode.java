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

            /* Display the info panel not the overview immediately to make it
             * very clear to users what is going on. */
            dv.setSelectedIndex( 1 );
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
