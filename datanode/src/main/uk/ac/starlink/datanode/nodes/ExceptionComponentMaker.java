package uk.ac.starlink.datanode.nodes;

import javax.swing.JComponent;
import uk.ac.starlink.datanode.viewers.TextViewer;

/**
 * ComponentMaker which displays an error when activated.
 */
public class ExceptionComponentMaker implements ComponentMaker {

    private final Throwable error_;

    public ExceptionComponentMaker( Throwable th ) {
        error_ = th;
    }

    public JComponent getComponent() {
        return new TextViewer( error_ );
    }
}
