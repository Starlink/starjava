package uk.ac.starlink.datanode.nodes;

import java.io.PrintWriter;
import javax.swing.JComponent;
import uk.ac.starlink.datanode.viewers.TextViewer;

public class ErrorDataNode extends DefaultDataNode {
    private Throwable thrown;

    public ErrorDataNode( Throwable th ) {
        super( th.getClass().getName() );
        this.thrown = th;
        setIconID( IconFactory.ERROR );
    }

    public String getNodeTLA() {
        return "ERR";
    }

    public String getNodeType() {
        return "Error";
    }

    public String getDescription() {
        return "(" + thrown.getMessage() + ")";
    }

    public void configureDetail( DetailViewer dv ) {
        dv.addKeyedItem( "Throwable class", thrown.getClass().getName() );
        dv.addKeyedItem( "Message", thrown.getMessage() );
        dv.addPane( "Stack trace", new ComponentMaker() {
            public JComponent getComponent() {
                TextViewer tv = new TextViewer();
                PrintWriter pw = new PrintWriter( tv.getAppender() );
                thrown.printStackTrace( pw );
                return tv;
            }
        } );
    }

}
