/*
 * $Id: HMMTrainer.java,v 1.5 2001/08/27 22:21:37 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.*;
import diva.sketch.recognition.*;
import java.io.*;
import java.util.Iterator;
import java.util.HashMap;
//import EDU.ucsd.asr.HMMMixtureOfGaussians;

/**
 * Read a training file and train an HMM for
 * scribble recognitoin.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.5 $
 * @rating Red
 */
public class HMMTrainer {
    public static void main(String[] argv) {
        if(argv.length < 1) {
            System.out.println("Usage: trainingToText file.tc");
            System.exit(-1);
        }
        try {
            SSTrainingParser tp = new SSTrainingParser();
            SSTrainingModel tm = (SSTrainingModel)tp.parse(new FileReader(argv[0]));
            SSTrainingModel tm2 = normalizeModel(tm);
            HashMap vs = velocities(tm2);

            //            HMMMixtureOfGaussians hmm = new HMMMixtureOfGaussians(4,1,100); //FIXME!!!
            
            /*
              TrainingWriter tw = new TrainingWriter();
              FileWriter out = new FileWriter(argv[0]+"2");
              tw.writeModel(tm2, out);
              out.close();
            */
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /** Extract x and y velocities from the positive examples from
     * a training model and put them into a hashmap indexed by the
     * type.
     */
    private static final HashMap velocities(SSTrainingModel tm) {
        HashMap out = new HashMap();
        for(Iterator i = tm.types(); i.hasNext(); ) {
            String type = (String)i.next();
            double[][][] vs = new double[tm.positiveExampleCount(type)][][];
            int k = 0;
            for(Iterator j = tm.positiveExamples(type); j.hasNext(); ) {
                TimedStroke s = (TimedStroke)j.next();
                vs[k++] = velocities(s);
            }
        }
        return out;
    }

    private static final double[][] velocities(TimedStroke s) {
        double[][] out = new double[s.getVertexCount()-1][];
        double prevX = s.getX(0);
        double prevY = s.getY(0);
        long prevT = s.getTimestamp(0);
        for(int i = 1; i < s.getVertexCount(); i++) {
            double curX = s.getX(i);
            double curY = s.getX(i);
            long curT = s.getTimestamp(i);
            out[i-1] = new double[2];
            out[i-1][0] = (curX-prevX)/(curT-prevT);
            out[i-1][1] = (curY-prevY)/(curT-prevT);
            prevX = curX;
            prevY = curY;
            prevT = curT;
        }
        return out;
    }

    /** Normalize the strokes in a training model accordint to time.
     */
    private static final SSTrainingModel normalizeModel(SSTrainingModel tm) {
        SSTrainingModel out = new SSTrainingModel();
        for(Iterator i = tm.types(); i.hasNext(); ) {
            String type = (String)i.next();
            for(Iterator j = tm.positiveExamples(type); j.hasNext(); ) {
                TimedStroke s = (TimedStroke)j.next();
                TimedStroke s2 = NormalizeTimeStrokeFilter.interpolate(s);
                out.addPositiveExample(type, s2);
            }
            for(Iterator j = tm.negativeExamples(type); j.hasNext(); ) {
                TimedStroke s = (TimedStroke)j.next();
                TimedStroke s2 = NormalizeTimeStrokeFilter.interpolate(s);
                out.addNegativeExample(type, s2);
            }
        }
        return out;
    }
}

