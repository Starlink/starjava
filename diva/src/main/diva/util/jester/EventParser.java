/*
 * $Id: EventParser.java,v 1.3 2001/07/22 22:02:08 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util.jester;

import diva.sketch.JSketch;
import diva.util.xml.*;

import java.util.Iterator;
import java.io.FileReader;
import java.io.Reader;
import java.awt.event.*;
import java.awt.Component;
import java.awt.Frame;
import java.io.FileReader;
import java.awt.event.*;
import javax.swing.*;

/**
 * EventParser parses an XML file representing a stream of
 * AWT input events into an array of InputEvent objects.
 *
 * @see EventWriter
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class EventParser implements diva.util.ModelParser {
    /**
     * The public identity of the sketch dtd file.
     */
    public static final String PUBLIC_ID = "-//UC Berkeley//DTD eventStream 1//EN";

    /**
     * The URL where the DTD is stored.
     */
    public static final String DTD_URL = "http://www.gigascale.org/diva/dtd/event.dtd";

    /**
     * The DTD for sketch files.
     */
    public static final String DTD_1 =
    "<!ELEMENT eventStream (key|mouse)*> <!ELEMENT mouse EMPTY> <!ATTLIST mouse id CDATA #REQUIRED when CDATA #REQUIRED modifiers CDATA #REQUIRED x CDATA #REQUIRED y CDATA #REQUIRED clickCount CDATA #REQUIRED popupTrigger CDATA #REQUIRED> <!ELEMENT key EMPTY> <!ATTLIST key id CDATA #REQUIRED when CDATA #REQUIRED modifiers CDATA #REQUIRED keyCode CDATA #REQUIRED keyChar CDATA #REQUIRED>";

    /**
     * 
     */
    public static final String EVENT_STREAM_TAG = "eventStream";
    
    /**
     * 
     */
    public static final String MOUSE_EVENT_TAG = "mouse";

    /**
     * 
     */
    public static final String KEY_EVENT_TAG = "key";

    /**
     * 
     */
    public static final String ID_ATTR_TAG = "id";

    /**
     * 
     */
    public static final String WHEN_ATTR_TAG = "when";

    /**
     * 
     */
    public static final String MODIFIERS_ATTR_TAG = "modifiers";
    
    /**
     * 
     */
    public static final String X_ATTR_TAG = "x";

    /**
     * 
     */
    public static final String Y_ATTR_TAG = "y";

    /**
     * 
     */
    public static final String CLICKCOUNT_ATTR_TAG = "clickCount";

    /**
     * 
     */
    public static final String POPUPTRIGGER_ATTR_TAG = "popupTrigger";

    /**
     * 
     */
    public static final String KEYCODE_ATTR_TAG = "keyCode";

    /**
     * 
     */
    public static final String KEYCHAR_ATTR_TAG = "keyChar";

    /**
     * The source component that is passed to the event
     * constructor.
     */
    private Component _source;
    
    /**
     * Construct a new event parser that parses events from the input
     * string and attributes them to the given source component.
     */
    public EventParser(Component source) {
        _source = source;
    }

    /**
     * 
     */
    public Object parse(Reader in) throws java.lang.Exception  {
        return parseEvents(in);
    }

    /**
     * 
     */
    public InputEvent[] parseEvents(Reader in) throws java.lang.Exception  {
        XmlDocument doc = new XmlDocument();
        doc.setDTDPublicID(PUBLIC_ID);
        doc.setDTD(DTD_1);
        XmlReader reader = new XmlReader();
        reader.parse(doc, in);
        if(reader.getErrorCount() > 0) {
            throw new Exception("errors encountered during parsing");
        }
        XmlElement stream = doc.getRoot();
        if(!stream.getType().equals(EVENT_STREAM_TAG)) {
            throw new Exception("no stream");
        }
        InputEvent[] out = new InputEvent[stream.elementCount()];
        int cnt = 0;
        for(Iterator i = stream.elements(); i.hasNext(); ) {
            XmlElement elt = (XmlElement)i.next();
            out[cnt++] = buildEvent(elt);
        }
        return out;
    }

    /**
     * Given an input event represented by its parsed XML equivalent,
     * reconstruct the original event.
     */
    private InputEvent buildEvent(XmlElement eltXml) throws Exception {
        if(eltXml.getType().equals(MOUSE_EVENT_TAG)) {
            int id = Integer.parseInt(eltXml.getAttribute(ID_ATTR_TAG));
            long when = Long.parseLong(eltXml.getAttribute(WHEN_ATTR_TAG));
            int modifiers = Integer.parseInt(eltXml.getAttribute(MODIFIERS_ATTR_TAG));
            int x = Integer.parseInt(eltXml.getAttribute(X_ATTR_TAG));
            int y = Integer.parseInt(eltXml.getAttribute(Y_ATTR_TAG));
            int clickCount = Integer.parseInt(eltXml.getAttribute(CLICKCOUNT_ATTR_TAG));
            boolean popupTrigger = (Boolean.valueOf(eltXml.getAttribute(POPUPTRIGGER_ATTR_TAG))).booleanValue();
            return new MouseEvent(_source, id, when, modifiers,
                    x, y, clickCount, popupTrigger);
        }
        else if(eltXml.getType().equals(KEY_EVENT_TAG)) {
            int id = Integer.parseInt(eltXml.getAttribute(ID_ATTR_TAG));
            long when = Long.parseLong(eltXml.getAttribute(WHEN_ATTR_TAG));
            int modifiers = Integer.parseInt(eltXml.getAttribute(MODIFIERS_ATTR_TAG));
            int keyCode = Integer.parseInt(eltXml.getAttribute(KEYCODE_ATTR_TAG));
            char keyChar = eltXml.getAttribute(KEYCHAR_ATTR_TAG).charAt(0);
            return new KeyEvent(_source, id, when, modifiers, keyCode, keyChar);
        }
        else {
            throw new Exception("Unexpected element: " + eltXml);
        }
    }

    /**
     * Simple test of this class.
     */
    public static void main (String args[]) throws Exception {
        if(args.length < 1) {
            System.err.println("Usage: java EventParser <outputFile>");
            System.exit(-1);
        }
        JFrame frame = new JFrame();
        JSketch sketch = new JSketch();
        EventPlayer player = new EventPlayer(sketch);
        EventParser demo = new EventParser(sketch);
        frame.getContentPane().add("Center", sketch);
        frame.setSize(600,400);
        frame.setVisible(true);
        try {
            InputEvent[] stream = demo.parseEvents(new FileReader(args[0]));
            player.play(stream);
        }
        catch(Exception e) {
            System.err.println(e);
            System.exit(-1);
        }
    }
}

