/*
 * $Id: TrainingToMatlab.java,v 1.3 2001/08/27 22:21:38 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.*;
import diva.sketch.recognition.*;
import java.io.*;
import java.util.Iterator;

/**
 * A utility class for converting training files to
 * text files that can be read into Matlab matrices
 * directly.  Given a file "foo.tc" create "foo.t1",
 * "foo.t2", etc. where ti is a type.  Each file will
 * contain:
 *   N x1 y1 t1 ...\n
 *   ...
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class TrainingToMatlab {
    public static void main(String[] argv) {
        if(argv.length < 1) {
            System.out.println("Usage: trainingToText file.tc");
            System.exit(-1);
        }
        try {
            SSTrainingParser p = new SSTrainingParser();
            SSTrainingModel tm = (SSTrainingModel)p.parse(new FileReader(argv[0]));
            for(Iterator i = tm.types(); i.hasNext(); ) {
                String type = (String)i.next();
                PrintWriter out = new PrintWriter(new FileWriter(argv[0] + "." + type));
                for(Iterator j = tm.positiveExamples(type); j.hasNext(); ) {
                    TimedStroke s = (TimedStroke)j.next();
                    out.print("" + s.getVertexCount());
                    for(int k = 0; k < s.getVertexCount(); k++) {
                        out.print(" " + s.getX(k) + " " + s.getY(k) + " " + s.getTimestamp(k));
                    }
                    out.print("\n");
                }
                out.close();
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

