package uk.ac.starlink.treeview;

import org.w3c.dom.Comment;

public class XMLCommentDataNode extends XMLDataNode {

    private String desc;

    public XMLCommentDataNode( Comment cnode ) {
        super( cnode,
               "",
               "COM",
               "XML comment node",
               false,
               IconFactory.XML_COMMENT,
               "" );
        String content = cnode.getData().trim();
        if ( content.length() > 30 ) {
            desc = "<!-- " + content.substring( 0, 30 ) + "...";
        }
        else {
            desc = "<!-- " + content + "-->";
        }
    }

    public String getDescription() {
        return desc;
    }

}
