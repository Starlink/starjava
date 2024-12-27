package uk.ac.starlink.util;

import java.util.Iterator;
import java.io.StringReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.Test;
import org.w3c.dom.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests NodeDescendants.
 *
 * @author   Norman Gray (Starlink)
 */
public class NodeDescendantsTest {

    private SourceReader srcrdr;

    public NodeDescendantsTest() {
        srcrdr = new SourceReader();
        srcrdr.setIncludeDeclaration(false);
    }

    public void setUp() throws TransformerException {
    }

    @Test
    public void testTraversal()
            throws Exception {
        
        String inputXML =
                "<top><n1/><n2><n3>text4</n3><n5><n6/></n5><n7/><n8><n9>text10</n9></n8></n2><n11/></top>";
        Element input = StringToDom(inputXML);
        final String[] nodename = {
            "top", "n1", "n2", "n3", "#text", "n5", "n6", "n7",
            "n8", "n9", "#text", "n11", 
        };
        int i = 0;
        for (Iterator ni = new NodeDescendants(input).iterator();
             ni.hasNext(); i++) {
            assertTrue(i < nodename.length);
            assertEquals(nodename[i], ((Node)ni.next()).getNodeName());
        }
        i = 0;
        for (Iterator ni = new NodeDescendants
                     (input, NodeDescendants.SHOW_ELEMENT).iterator();
             ni.hasNext(); i++) {
            while (nodename[i].equals("#text")) // skipping these
                i++;
            assertTrue(i < nodename.length);
            assertEquals(nodename[i], ((Node)ni.next()).getNodeName());
        }
        java.util.List l = new java.util.ArrayList();
        for (Iterator ni = new NodeDescendants
                     (input, NodeDescendants.SHOW_TEXT).iterator();
             ni.hasNext();
             )
        {
            l.add(((Node)ni.next()).getNodeValue());
        }
        assertEquals(2, l.size());
        assertEquals("text4",  (String)l.get(0));
        assertEquals("text10", (String)l.get(1));

        // Test visitNode
        class MatchEm implements NodeDescendants.Visitor {
            int num = 0;
            public Object visitNode(Node n) {
                assertEquals(nodename[num], n.getNodeName());
                num++;
                return null;
            }
        }

        MatchEm m = new MatchEm();
        Object r = new NodeDescendants(input).visitTree(m);
        assertNull(r);
        assertEquals(nodename.length, m.num);
        r = new NodeDescendants(input).visitTree
                (new NodeDescendants.Visitor() {
                        public Object visitNode(Node n) {
                            if (n.getNodeName().equals("n3"))
                                return n;
                            else
                                return null;
                        }
                    });
        assertNotNull(r);
        assertTrue(r instanceof Node);
        assertEquals("n3", ((Node)r).getNodeName());

        // Test degenerate cases:
        // Node with no children...
        i = 0;
        input = StringToDom("<lonely/>");
        for (Iterator ni = new NodeDescendants(input).iterator();
             ni.hasNext(); ) {
            assertNotNull(ni.next());
            i++;
        }
        assertEquals(1, i);
        
        i = 0;
        // A NodeDescendants created with a null argument should 
        // have no children... 
        for (Iterator ni = new NodeDescendants(null).iterator();
             ni.hasNext(); ) {
            fail("null NodeDescendants has no children");
        }
    }

    private Element StringToDom(String s) 
            throws TransformerException {
        return srcrdr.getElement(new StreamSource(new StringReader(s)));
    }
}
