package uk.ac.starlink.votable.dom;

import org.w3c.dom.ProcessingInstruction;

public class DelegatingProcessingInstruction extends DelegatingNode
                                             implements ProcessingInstruction {

    private final ProcessingInstruction base_;

    protected DelegatingProcessingInstruction( ProcessingInstruction base,
                                               DelegatingDocument doc ) {
        super( base, doc );
        base_ = base;
    }

    public String getData() {
        return base_.getData();
    }

    public String getTarget() {
        return base_.getTarget();
    }

    public void setData( String data ) {
        base_.setData( data );
    }
}
