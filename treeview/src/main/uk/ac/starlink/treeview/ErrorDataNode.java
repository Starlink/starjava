package uk.ac.starlink.treeview;

import java.io.PrintWriter;
import javax.swing.Icon;
import javax.swing.JComponent;

public class ErrorDataNode extends DefaultDataNode {
    private Throwable thrown;
    private Icon icon;
    private JComponent fullView;

    public ErrorDataNode( Throwable th ) {
        super( "Error" );
        this.thrown = th;
    }

    public String getNodeTLA() {
        return "ERR";
    }

    public String getNodeType() {
        return "Error";
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = IconFactory.getInstance().getIcon( IconFactory.ERROR );
        }
        return icon;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
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
        return fullView;
    }

}
