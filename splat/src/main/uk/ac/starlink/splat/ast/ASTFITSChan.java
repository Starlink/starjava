package uk.ac.starlink.splat.ast;

import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.FitsChan;
import uk.ac.starlink.ast.FrameSet;

/**
 *  Class to create and manipulate AST FITS channels. Uses of this are
 *  to read in a series of FITS headers and create an AST frameset
 *  that describes any coordinate systems and the reverse process of
 *  encoding a frameset as a series of FITS header cards.
 *
 *  @author Peter W. Draper
 *  @since 07-SEP-2000
 *  @version $Id$
 *  @see "Starlink User Note 211"
 *  @see  ASTJ
 *
 */
public class ASTFITSChan
{
    //  ============
    //  Constructors
    //  ============

    /**
     *  Default constructor. Creates a channel with null source and
     *  sink and native encoding.
     */
    public ASTFITSChan()
    {
        this( null );
    }

    /**
     *  Default constructor. Creates a channel with null source and
     *  sink and the given encoding.
     *
     *  @param encoding the AST FITS channel encoding.
     */
    public ASTFITSChan( String encoding )
    {
        fitsChan = new FitsChan();
        if ( encoding != null ) {
            fitsChan.setEncoding( encoding );
        }
    }

    //  ===================
    //  Protected variables
    //  ===================

    /**
     *  Reference to the AST FITS channel.
     */
    protected FitsChan fitsChan = null;

    //  ====================
    //  Class public methods, note native methods are all in ASTJ.
    //  ====================

    /**
     * Add a card to the channel.
     *
     *  @param card the FITS card to add.
     */
    public void add( String card )
    {
        try {
            fitsChan.putFits( card, false );
        }
        catch (AstException e) {
            // Usually badly-formatted card, so let someone know.
            System.err.println( e.getMessage() );
        }
    }

    /**
     * Rewind the channel.
     */
    public void rewind()
    {
        fitsChan.clear( "Card" );
    }

    /**
     * Read the channel, creating an AST frameset.
     *
     * @return the AST frameset created from the channel.
     */
    public FrameSet read()
    {
        return (FrameSet) fitsChan.read();
    }

    /**
     * Write a frameset into the channel. Uses the current encoding,
     * if this fails then the routine returns false.
     *
     * @param frameset reference to an AST frameset.
     */
    public boolean write( FrameSet frameset )
    {
        int nwrite = 0;
        try {
            nwrite = fitsChan.write( frameset );
        }
        catch (AstException e) {
            return false;
        }
        return ( nwrite != 0 );
    }

    /**
     * Read a FITS card back from the channel. Returns "" if not
     * found.
     *
     * @param pattern keyword template used to match card.
     * @see "SUN 211"
     */
    public String findCard( String pattern )
    {
        return fitsChan.findFits( pattern, true );
    }

    /**
     * Read the next FITS card back from the channel. Returns ""
     * when last card has been returned.
     */
    public String nextCard()
    {
        return fitsChan.findFits( "%f", true );
    }

    /**
     * Return native AST reference to the channel.
     */
    public FitsChan getFitsChan()
    {
        return fitsChan;
    }
}
