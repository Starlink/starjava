/*
 * $Id: Protocol.java,v 1.2 2002/02/07 02:58:07 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.pod;

import diva.util.PropertyContainer;


/** A Protocol is a reification of the concept of protocol, which
 * underlies the connection of Generators, Pods, and Views.  Each
 * supported protocol will have a corresponding class, and this class
 * is expected to be implemented as a singleton (ie it must have a
 * private constructor).
 *
 * <p>The protocols implemented by the diva.pod package are defined
 * as inner classes here, so that nice names of the form Protocol.Graph
 * result. However, additional protocols can be created simply
 * by subclassing Protocol.
 *
 * @version $Revision: 1.2 $
 * @author John Reekie
 * @rating Red
 */
public abstract class Protocol {

    // The protocol name
    private String _name; 

    /** Subclasses must have a private constructor that calls
     * this constructor with the protocol name.
     */
    protected Protocol (String name) {
        _name = name;
    }

    /** Get the name of this protocol.
     */
    public String getName () {
        return _name;
    }

    /** Get the single instance of this protocol class.
     */
    public abstract Protocol getInstance ();


    /** The Layout protocol. This protocol represents visual
     * layout information.
     */
    public class Layout extends Protocol {
        
        // The instance
        private Layout _instance = new Layout();

        /** The private constructor
         */
        private Layout () {
            super("layout");
        }

        /** Return the instance
         */
        public Protocol getInstance () {
            return _instance;
        }
    }


    /** The Graph protocol. This protocol represents an
     * annotated graph.
     */
    public class Graph extends Protocol {
        
        // The instance
        private Graph _instance = new Graph();

        /** The private constructor
         */
        private Graph () {
            super("graph");
        }

        /** Return the instance
         */
        public Protocol getInstance () {
            return _instance;
        }
    }
}
