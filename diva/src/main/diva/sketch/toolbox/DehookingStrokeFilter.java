/*
 * $Id: DehookingStrokeFilter.java,v 1.7 2002/07/19 21:59:47 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.TimedStroke;
import diva.sketch.features.PathLengthFE;
import diva.sketch.features.FEUtilities;
import java.awt.geom.Line2D;

/**
 * Eliminate the serifs at the ends of a stroke.  (W.W.Loy and
 * I.D.Landau, "An On-Line Procedure for Recognition of Handprinted
 * Alphanumeric Characters", IEEE transactions on PAMI, Vol. PAMI-4,
 * No.4, July 1982)
 * 
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.7 $
 * @rating Red
 */
public class DehookingStrokeFilter extends StrokeFilter {
    public static final double DEFAULT_ANGLE_THRESHOLD = 45.0;//degrees
    
    private double _angleThresh;

    /**
     * Create a dehooking stroke filter with the default parameters.
     */
    public DehookingStrokeFilter(){
        _angleThresh = DEFAULT_ANGLE_THRESHOLD;
        
    }

    
    public TimedStroke apply(TimedStroke s) {
        return dehook(s);
    }

    public static TimedStroke dehook2(TimedStroke s){
        int num = s.getVertexCount();
        double fwSegLen[] = new double[num];
        double bwSegLen[] = new double[num];
        fwSegLen[0]=0;
        for(int i=1; i<num; i++){
            double segLen = FEUtilities.distance(s.getX(i-1),s.getY(i-1),s.getX(i),s.getY(i));
            fwSegLen[i]=fwSegLen[i-1]+segLen;
        }
        double totalMag = fwSegLen[num-1];
        for(int i=0; i<num; i++){
            bwSegLen[i]=totalMag-fwSegLen[num-1-i];
        }

        double secLen = 0.2*totalMag;
        int f1=0;//mark the point that just past 2/10 of the total path length
        for(int i=0; i<num; i++){
            if(fwSegLen[i]>secLen){
                f1=i;
                break;
            }
        }
        //System.out.println("f1="+f1);
        double angles_f[] = new double[f1];
        for(int i=1; i<f1+1; i++){
            int b_index=i-1;
            int f_index=i+1;
            double val =FEUtilities.dotProduct(s.getX(f_index),s.getY(f_index),s.getX(i),s.getY(i),s.getX(b_index),s.getY(b_index),s.getX(i),s.getY(i));
            if(Math.abs(-1-val)<0.0001){
                val=-1;
            }
            angles_f[i-1] = Math.toDegrees(Math.acos(val));
            //System.out.println("["+s.getX(f_index)+", "+s.getY(f_index)+"]["+s.getX(i)+", "+s.getY(i)+"]["+s.getX(b_index)+", "+s.getY(b_index)+"] ==> ["+val+", "+angles_f[i-1]+"]"+Math.round(val)+", "+Math.ceil(val));
        }

        int b1=0;
        for(int i=0; i<num; i++){
            if(bwSegLen[i]>secLen){
                b1=i;
                break;
            }
        }
        double angles_b[] = new double[b1];
        for(int i=1; i<b1+1; i++){
            int j=num-1-i;
            int b_index=j-1;
            int f_index=j+1;
            double val =FEUtilities.dotProduct(s.getX(f_index),s.getY(f_index),s.getX(j),s.getY(j),s.getX(b_index),s.getY(b_index),s.getX(j),s.getY(j));
            angles_b[i-1] = Math.toDegrees(Math.acos(val));
        }
        
        //find hook from front
        int cur_f = 0; //index to points
        for(int i=0; i<angles_f.length-1; i++){
            //System.out.println(i);
            if(angles_f[i]<100){
                //System.out.println("F1");                
                cur_f=i+1;
            }
            else{
                //System.out.println("F2");                
                double diff = Math.abs(angles_f[i+1]-angles_f[i]);
                //System.out.println("diff="+diff);
                if(diff>20){
                    //System.out.println("F3");                    
                    cur_f=i+1;
                }
                else {
                    //System.out.println("F4");                    
                    if(i+1-cur_f<3){
                        //System.out.println("F5");                        
                        continue;
                    }
                    else{
                        //System.out.println("F6");                        
                        break;
                    }
                }
            }
        }


        //find hook from back
        int cur_b = 0; //index to points
        for(int i=0; i<angles_b.length-1; i++){
            if(angles_b[i]<100){
                cur_b=i+1;
            }
            else{
                double diff = Math.abs(angles_b[i+1]-angles_b[i]);
                if(diff>20){
                    cur_b=i+1;
                }
                else {
                    if(i+1-cur_b<3){
                        continue;
                    }
                    else{
                        break;
                    }
                }
            }
        }

        cur_b=num-cur_b-1;
        System.out.println("start = "+cur_f+", end = "+cur_b);
        TimedStroke result = new TimedStroke();
        for(int i=cur_f; i<=cur_b; i++){
            result.addVertex((float)s.getX(i),(float)s.getY(i),s.getTimestamp(i));
        }
        return result;
    }

    /**
     * At the beginning and the end of the given stroke, calculate the
     * general direction vectors by taking the 2/10 of the total
     * length from each end.  Then start from 1/10 of the total length
     * going toward the end of the stroke, compute the angle formed by
     * the local vector and the general vector.  If the angle value
     * exceeds the DEFAULT_ANGLE_THRESH, eliminate the remaining points to get
     * rid of the hook.
     */
    public static TimedStroke dehook(TimedStroke s) {
        return dehook(s, DEFAULT_ANGLE_THRESHOLD);
    }

    public static TimedStroke dehook(TimedStroke s, double angleThresh){
        int num = s.getVertexCount();
        double fwSegLen[] = new double[num];
        double bwSegLen[] = new double[num];
        fwSegLen[0]=0;
        for(int i=1; i<num; i++){
            double segLen = FEUtilities.distance(s.getX(i-1),s.getY(i-1),s.getX(i),s.getY(i));
            fwSegLen[i]=fwSegLen[i-1]+segLen;
        }
        double totalMag = fwSegLen[num-1];
        for(int i=0; i<num; i++){
            bwSegLen[i]=totalMag-fwSegLen[num-1-i];
        }
        double secLen = 0.1*totalMag;
        double secLen2 = 0.2*totalMag;
        //dehook the beginning
        int m1 = 0;//mark the index when we've reached 1/10 of the path length
        int m2 = 0;//mark the index when we've reached 2/10 of the path length
        for(int i=1; i<num; i++){
            if(fwSegLen[i]>secLen){
                m1=i;
                break;
            }
        }
        for(int i=m1; i<num; i++){
            if(fwSegLen[i]>secLen2){
                m2=i;
                break;
            }
        }
        //        System.out.println("["+m1+", "+m2+"]");
        double genVec[] = new double[2];
        genVec[0]=s.getX(m2)-s.getX(0);
        genVec[1]=s.getY(m2)-s.getY(0);
        double genVecMag = Math.sqrt(Math.pow(genVec[0],2)+Math.pow(genVec[1],2));
        double vec[] = new double[2];
        double vecMag=0;
        int startI = 0;
        for(int i=m1-1; i>=0; i--){
            vec[0]=s.getX(i+1)-s.getX(i);
            vec[1]=s.getY(i+1)-s.getY(i);
            vecMag=Math.sqrt(Math.pow(vec[0],2)+Math.pow(vec[1],2));
            double dotval = (genVec[0]*vec[0]+genVec[1]*vec[1])/(genVecMag*vecMag);
            double angle = Math.toDegrees(Math.acos(dotval));
            //            System.out.println("i="+i+", dotval="+dotval+", angle="+angle);
            if(angle>DEFAULT_ANGLE_THRESHOLD){
                //                System.out.println("found");
                startI=i+1;
                break;
            }
        }
        //        System.out.println("startI="+startI);
        //dehook the end
        m1=1;//marks the index when we reach 1/10 of the path length from the end
        m2=1;//marks the index when we reach 2/10 of the path length from the end        
        for(int i=0; i<num; i++){
            if(bwSegLen[i]>secLen){
                m1=i;
                break;
            }
        }
        for(int i=m1; i<num; i++){
            if(bwSegLen[i]>secLen2){
                m2=i;
                break;
            }
        }
        m1=num-m1-1;
        m2=num-m2-1;
        //        System.out.println("***["+m1+", "+m2+"]");
        genVec[0]=s.getX(m2)-s.getX(num-1);
        genVec[1]=s.getY(m2)-s.getY(num-1);//general direction vector
        genVecMag=Math.sqrt(Math.pow(genVec[0],2)+Math.pow(genVec[1],2));
        int endI=num-1;
        for(int i=m1; i<num-1; i++){
            vec[0]=s.getX(i)-s.getX(i+1);
            vec[1]=s.getY(i)-s.getY(i+1);
            vecMag=Math.sqrt(Math.pow(vec[0],2)+Math.pow(vec[1],2));
            double dotval = (genVec[0]*vec[0]+genVec[1]*vec[1])/(genVecMag*vecMag);
            double angle = Math.toDegrees(Math.acos(dotval));
            if(angle>DEFAULT_ANGLE_THRESHOLD){
                endI=i;
                break;
            }
        }
        //        System.out.println("["+startI+", "+endI+"]");
        TimedStroke result = new TimedStroke();
        for(int i=startI; i<=endI; i++){
            result.addVertex((float)s.getX(i),(float)s.getY(i),s.getTimestamp(i));
        }
        return result;
    }
    
    /**
     * Set the threshold angle value for testing for a hook.
     */
    public void setHookAngleThresh(double val){
        _angleThresh = val;
    }

    /**
     * Return the threshold angle value for a hook.
     */
    public double getHookAngleThresh(){
        return _angleThresh;
    }
}
