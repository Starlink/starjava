package uk.ac.starlink.ast;

/**
 * Java interface to the AST SkyAxis
 * class.  It stores information associated with a particular axis of
 * an <code>SkyFrame</code>.  It is used internally by AST and has no
 * constructor function.  You should encounter it only within the output
 * of an <code>Channel</code>.
 *
 * @see  <a href='http://star-www.rl.ac.uk/cgi-bin/htxserver/sun211.htx/?xref_SkyAxis'>AST SkyAxis</a>
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class SkyAxis extends Axis {
    /* Dummy constructor. */
    protected SkyAxis() {
    }
}
