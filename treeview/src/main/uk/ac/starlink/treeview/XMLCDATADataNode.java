package uk.ac.starlink.treeview;

import javax.swing.Icon;
import javax.swing.JComponent;
import org.jdom.CDATA;
import org.jdom.output.XMLOutputter;

/**
 * A {@link DataNode} representing an XML CDATA section.
 *
 * @author   Mark Taylor
 * @version  $Id$
 */
public class XMLCDATADataNode extends DefaultDataNode {

    private CDATA cdata;
    private Icon icon;
    private JComponent fullView;

    /**
     * Initialises a new XMLCDATADataNode from a CDATA XML node.
     *
     * @param  cdata  the CDATA node
     */
    public XMLCDATADataNode( CDATA cdata ) {
        this.cdata = cdata;
        setLabel( "" );
    }

    public boolean allowsChildren() {
        return false;
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = iconMaker.getIcon( IconFactory.XML_CDATA );
        }
        return icon;
    }

    public String getNodeTLA() {
        return "CDA";
    }

    public String getNodeType() {
        return "XML CDATA section";
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            dv.addSeparator();
            String text = cdata.getText();
            dv.addKeyedItem( "Length", text.length() + " characters" );
            dv.addSubHead( "Content" );
            dv.addText( text );
        }
        return fullView;
    }
}
