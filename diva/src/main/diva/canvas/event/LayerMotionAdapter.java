/*
 * $Id: LayerMotionAdapter.java,v 1.6 2001/07/22 22:00:35 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.canvas.event;

/** An adapter for layer motion listeners. The methods in this class
 * are empty -- the class is provided to make it easier to
 * produce anonymous LayerMotionListeners.
 *
 * @version	$Revision: 1.6 $
 * @author 	John Reekie
 */
public class LayerMotionAdapter implements LayerMotionListener {

    /** Invoked when the mouse enters a layer or figure.
     */
  public void mouseEntered (LayerEvent e) {}

    /** Invoked when the mouse exits a layer or figure.
     */
  public void mouseExited (LayerEvent e) {}
 
    /** Invoked when the mouse moves while over a layer or figure.
     */
  public void mouseMoved (LayerEvent e) {}
}



