/*
 * $Id: BasicStrokeBuilder.java,v 1.3 2001/07/22 22:01:51 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.rcl;

import java.util.List;
import java.util.Map;
import diva.sketch.recognition.BasicStrokeRecognizer;
import java.io.FileReader;
import diva.util.xml.*;

/**
 * Build a BasicStrokeRecognizer using the trainingFile
 * parameter.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class BasicStrokeBuilder extends AbstractXmlBuilder {
    /** The parameter for the training file that will be
     * used to train the recognizer.
     */
    public static final String TRAINING_PARAM = "trainingFile";

    /**
     * Build a BasicStrokeRecognizer using the trainingFile
     * parameter.
     */
    public Object build(XmlElement elt, String type) 
            throws Exception {
        String trainingFile = (String)elt.getAttribute(TRAINING_PARAM);
        return new BasicStrokeRecognizer(new FileReader(trainingFile));
    }
}

