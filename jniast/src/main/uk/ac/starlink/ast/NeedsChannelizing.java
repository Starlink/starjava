package uk.ac.starlink.ast;

/**
 * Declares that an <code>AstObject</code> requires object-specific 
 * preparation before writing to and/or after reading from a 
 * <code>Channel</code>.  Any object implementing this interface 
 * will have the <code>channelize</code> method called on it before
 * it is written to a sink by a Channel, and the <code>unChannelize</code>
 * method called on it after it is read from a source by a Channel.  
 * No other <code>uk.ac.starlink.ast</code> methods will be called on it
 * between calls to <code>channelize</code> and <code>unChannelize</code>,
 * so <code>channelize</code> may leave the object in an unusable 
 * state, as long as <code>unChannelize</code> undoes this 
 * situation.
 * 
 * @author    Mark Taylor (Starlink)
 * @version   $Id$
 */
public interface NeedsChannelizing {

    /**
     * Does object-specific preparation for output to a <code>Channel</code>.
     * This method is called by the writing methods of a
     * Channel prior to doing the actual write.
     * <p>
     * The actions of this method should be reversed by the 
     * <code>unChannelize</code> method.  If that method creates a new
     * AST object (i.e. the <code>pointer</code> 
     * field gets set to a new value) then
     * this method should call <code>annul</code> just before returning.
     *
     * @throws   Exception  the implementation may cause any exception
     *                      to be thrown.  This will scupper the write.
     */
    public void channelize() throws Exception;

    /**
     * Reverses the effects of <code>channelize</code>.
     * This method is called by the reading methods of a Channel 
     * after doing the actual read.
     * It may also be called by the writing methods of a 
     * Channel after doing the actual write to undo possible destructive
     * effects of the <code>channelize</code> call 
     * (it is implementation-specific whether this will actually happen).
     *
     * @throws   Exception   the implementation may cause any exception
     *                       to be thrown.  This will scupper the read.
     */
    public void unChannelize() throws Exception;
}
