package uk.ac.starlink.treeview;

import java.io.IOException;

public interface Draggable {
    void customiseTransferable( DataNodeTransferable trans ) throws IOException;
}

