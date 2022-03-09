package uk.ac.starlink.treeview;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.swing.JComponent;
import uk.ac.starlink.datanode.nodes.ComponentMaker;
import uk.ac.starlink.datanode.nodes.IconFactory;
import uk.ac.starlink.datanode.nodes.NodeUtil;
import uk.ac.starlink.datanode.viewers.StyledTextArea;

/**
 * Displays help information.
 * This class subclasses DetailViewer so that it can present the same
 * interface and sit in the detail pane of the Treeview window. 
 * However, it has no associated tree node.
 */
class HelpDetailViewer extends ApplicationDetailViewer {

    public final static String BUILD_PROPS = 
        "uk/ac/starlink/treeview/text/build.properties";

    public HelpDetailViewer() {

        /* Add the "About" information as an overview pane. */
        super( "About" );
        addIcon( IconFactory.getIcon( IconFactory.TREE ) );
        addSpace();
        addTitle( "Starlink Treeview" );
        addSpace();

        /* Get some information from stored properties. */
        InputStream propstrm = getClass().getClassLoader()
                              .getResourceAsStream( BUILD_PROPS );
        if ( propstrm != null ) {
            Properties props = new Properties();
            try {
                props.load( propstrm );
                addSubHead( "Version" );
                addKeyedItem( "Version ID", props.getProperty( "version" ) );
                addKeyedItem( "Build date", props.getProperty( "date" ) );
                addKeyedItem( "Built by", props.getProperty( "built.by" ) );
            }
            catch ( IOException e ) {
                // no action
            }
        }

        addSubHead( "Authors" );
        addText( "Mark Taylor" );

        addSubHead( "Optional components" );
        addKeyedItem( "JNIAST", NodeUtil.hasAST() ? "installed" 
                                                  : "not installed" );
        addKeyedItem( "JNIHDS", NodeUtil.hasHDS() ? "installed" 
                                                  : "not installed" );
        addKeyedItem( "JAI", NodeUtil.hasJAI() ? "installed" 
                                               : "not installed" );
        addKeyedItem( "TAMFITS", NodeUtil.hasTAMFITS() ? "installed"
                                                       : "not installed" );

        addSubHead( "Java" );
        addKeyedItem( "JRE", System.getProperty( "java.vendor" ) + " " 
                           + System.getProperty( "java.version" ) );
        addKeyedItem( "JVM", System.getProperty( "java.vm.vendor" ) + " "
                           + System.getProperty( "java.vm.version" ) );
        addKeyedItem( "Installation", System.getProperty( "java.home" ) );

        addSubHead( "Further information" );
        addKeyedItem( "WWW", "http://www.starlink.ac.uk/treeview/" );
        addKeyedItem( "Email", "m.b.taylor@bristol.ac.uk" );

        /* Add extra help panels. */
        addPane( "Basic use", new HTMLDocComponentMaker( "basic.html" ) );
        addPane( "Buttons", new HTMLDocComponentMaker( "buttons.html" ) );
        addPane( "Node types", new NodeTypePane( "Node types" ) );
        addPane( "Invocation", new HTMLDocComponentMaker( "invocation.html" ) );
        addPane( "Alter-egos", new HTMLDocComponentMaker( "popup.html" ) );

        /* Set the pane which is viewed by default to be the "Basic use" one. */
        setSelectedIndex( 1 );
    }

    private class NodeTypePane implements ComponentMaker {
        private String title;
        private StyledTextArea ta;
        public NodeTypePane( String title ) {
            this.title = title;
        }
        public JComponent getComponent() {
            ta = new StyledTextArea();
            ta.addTitle( "Node types" );
            ta.addText( "The following are the known nodes, represented "
                      + "by the icons shown:" );

            ta.addSeparator();
            addKnownIcon( IconFactory.ARY0, "Scalar item" );
            addKnownIcon( IconFactory.ARY1, "1-dimensional array" );
            addKnownIcon( IconFactory.ARY2, "2-dimensional array" );
            addKnownIcon( IconFactory.ARY3, "N-dimensional array (N>2)" );

            ta.addSeparator();
            addKnownIcon( IconFactory.TABLE, "Table structure" );

            ta.addSeparator();
            addKnownIcon( IconFactory.DIRECTORY, "Directory" );
            addKnownIcon( IconFactory.FILE, "Plain file" );

            ta.addSeparator();
            addKnownIcon( IconFactory.DATA, "Unknown data source" );
            addKnownIcon( IconFactory.COMPRESSED, "Compressed data source" );

            ta.addSeparator();
            addKnownIcon( IconFactory.FITS, "FITS file" );
            addKnownIcon( IconFactory.HDU, "Generic FITS HDU" );

            ta.addSeparator();
            addKnownIcon( IconFactory.WCS, "AST WCS structure" );
            addKnownIcon( IconFactory.FRAME, "AST WCS coordinate Frame" );
            addKnownIcon( IconFactory.SKYFRAME, 
                          "AST WCS Sky coordinate Frame" );
            addKnownIcon( IconFactory.SPECFRAME,
                          "AST WCS Spectral coordinate Frame" );

            ta.addSeparator();
            addKnownIcon( IconFactory.STRUCTURE, "HDS data structure" );
            addKnownIcon( IconFactory.NDF, "NDF data structure" );
            addKnownIcon( IconFactory.HISTORY, "History component" );

            ta.addSeparator();
            addKnownIcon( IconFactory.XML_DOCUMENT,
                          "Well-formed XML document" );
            addKnownIcon( IconFactory.XML_DTD, "XML document type declaration");
            addKnownIcon( IconFactory.XML_ELEMENT, "XML element" );
            addKnownIcon( IconFactory.XML_STRING, "XML text node" );
            addKnownIcon( IconFactory.XML_CDATA, "XML CDATA node" );
            addKnownIcon( IconFactory.XML_COMMENT, "XML comment" );
            addKnownIcon( IconFactory.XML_PI, "XML processing instruction" );

            ta.addSeparator();
            addKnownIcon( IconFactory.HDX_CONTAINER, "HDX container" );
            addKnownIcon( IconFactory.NDX, "NDX structure" );

            ta.addSeparator();
            addKnownIcon( IconFactory.VOTABLE, "VOTable" );
            addKnownIcon( IconFactory.VOCOMPONENT, "Component within VOTable" );

            ta.addSeparator();
            addKnownIcon( IconFactory.ZIPFILE, "Zip/Jar archive" );
            addKnownIcon( IconFactory.ZIPBRANCH, "Zip/Jar archive directory" );

            ta.addSeparator();
            addKnownIcon( IconFactory.TARFILE, "Tar archive" );
            addKnownIcon( IconFactory.TARBRANCH, "Tar archive directory" );

            ta.addSeparator();
            addKnownIcon( IconFactory.MYSPACE, "MySpace container" );

            ta.addSeparator();
            addKnownIcon( IconFactory.ERROR, "Error creating a node" );

            return ta;
        }

        private void addKnownIcon( short iconID, String descrip ) {
            ta.addSpace();
            ta.addIcon( IconFactory.getIcon( iconID ) );
            ta.addSpace();
            ta.addText( descrip );
        }
    }
}
