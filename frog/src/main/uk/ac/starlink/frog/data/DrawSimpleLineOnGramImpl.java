package uk.ac.starlink.frog.data;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.LutMap;
import uk.ac.starlink.frog.util.FrogException;

/**
 * This class is a simple layer ontop of MEMGramImpl
 *
 * @author Alasdair Allan
 */
public class DrawSimpleLineOnGramImpl extends MEMGramImpl
{
//
//  Implementation of abstract methods.
//

    /**
     * Constructor - just take a symbolic name for the gram, no
     * other significance. 
     *
     * @param name a symbolic name for the gram.
     */
    public DrawSimpleLineOnGramImpl( String name )
    {
        super( name );
        this.shortName = name;
        this.fullName = name;
    }

}
