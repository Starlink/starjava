package diva.graph.schematic;

import diva.gui.AbstractView;
import diva.gui.Document;

import diva.graph.JGraph;
import diva.graph.GraphPane;
import diva.graph.GraphController;
import diva.graph.toolbox.DeletionListener;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.KeyStroke;
import javax.swing.JComponent;

public class GraphView extends AbstractView {
    private JGraph _jgraph = null;
    public GraphView(Document d) {
        super(d);
    }
    public JComponent getComponent() {
        if(_jgraph == null) {
            GraphController controller =
                new SchematicGraphController(getDocument().getApplication());
            _jgraph = new JGraph(new GraphPane(controller,
			    ((GraphDocument)getDocument()).getGraphModel()));
            new GraphDropTarget(_jgraph);

            ActionListener deletionListener = new DeletionListener();
            _jgraph.registerKeyboardAction(deletionListener, "Delete",
                    KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
            _jgraph.setRequestFocusEnabled(true);
        }
        return _jgraph;
    }
    public String getTitle() {
        return "Graph";
    }
    public String getShortTitle() {
        return "Graph";
    }
}
