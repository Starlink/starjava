package uk.ac.starlink.treeview;

import org.w3c.dom.Text;

public class XMLTextDataNode extends XMLDataNode {

    private String desc;

    public XMLTextDataNode( Text tnode ) {
        super( tnode,
               "",
               "TXT",
               "XML text node",
               false,
               IconFactory.XML_STRING,
               "" );
        String content = tnode.getData().trim();
        if ( content.length() > 30 ) {
            desc = '"' + content.substring( 0, 30 ) + "...";
        }
        else {
            desc = '"' + content + '"';
        }
    }

    public String getDescription() {
        return desc;
    }

}
