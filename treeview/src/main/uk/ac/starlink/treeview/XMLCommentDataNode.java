package uk.ac.starlink.treeview;

import javax.swing.Icon;
import javax.swing.JComponent;
import org.jdom.Comment;
import org.jdom.output.XMLOutputter;

/**
 * A {@link DataNode} representing an XML Comment.
 *
 * @author   Mark Taylor
 * @version  $Id$
 */
public class XMLCommentDataNode extends DefaultDataNode {

    private Comment comment;
    private Icon icon;
    private JComponent fullView;

    /**
     * Initialises a new XMLCommentDataNode from a Comment object.
     *
     * @param  comm   the XML Comment 
     */
    public XMLCommentDataNode( Comment comm ) {
        this.comment = comm;
        setLabel( "" );
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = iconMaker.getIcon( IconFactory.XML_COMMENT );
        }
        return icon;
    }

    public String getNodeTLA() {
        return "COM";
    }

    public String getNodeType() {
        return "XML comment node";
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            dv.addSeparator();
            String text = comment.getText();
            dv.addKeyedItem( "Length", text.length() + " characters" );
            dv.addSubHead( "Content" );
            dv.addText( "<!--" + text + "-->" );
        }
        return fullView;
    }
}
