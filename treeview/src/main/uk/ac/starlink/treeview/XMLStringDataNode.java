package uk.ac.starlink.treeview;

import java.io.*;
import javax.swing.Icon;
import javax.swing.JComponent;
import org.jdom.output.XMLOutputter;

/**
 * A {@link DataNode} representing XML character data.
 *
 * @author   Mark Taylor
 * @version  $Id$
 */
public class XMLStringDataNode extends DefaultDataNode {

    private String text;
    private Icon icon;
    private JComponent fullView;

    /**
     * Initialises a new XMLStringDataNode from a String.
     *
     * @param  text  the character data as a string
     */
    public XMLStringDataNode( String text ) {
        this.text = text;
        setLabel( "" );
    }

    public boolean allowsChildren() {
        return false;
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = iconMaker.getIcon( IconFactory.XML_STRING );
        }
        return icon;
    }

    public String getNodeTLA() {
        return "TXT";
    }

    public String getNodeType() {
        return "XML character data";
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            dv.addSeparator();
            dv.addKeyedItem( "Length", text.length() + " characters" );
            dv.addSubHead( "Content" );
            dv.addText( text );
        }
        return fullView;
    }
}
