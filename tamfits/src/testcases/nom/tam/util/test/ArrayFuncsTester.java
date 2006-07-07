package nom.tam.util.test;

import nom.tam.util.ArrayFuncs;

public class ArrayFuncsTester {
    
    /** Test and demonstrate the ArrayFuncs methods.
      * @param args Unused.
      */
    public static void main(String[] args) {

        int[][][]   test1 = new int[10][9][8];
        boolean[][] test2 = new boolean[4][];
        test2[0] = new boolean[5];
        test2[1] = new boolean[4];
        test2[2] = new boolean[3];
        test2[3] = new boolean[2];

        double[][] test3 = new double[10][20];
        StringBuffer[][] test4 = new StringBuffer[3][2];

        System.out.println("getBaseClass:  test1: Base type of integer array is:"+ ArrayFuncs.getBaseClass(test1));
        System.out.println("getBaseLength: test1: Base length is               : "+ArrayFuncs.getBaseLength(test1));
        System.out.println("arrayDescription of test1: \n"+ArrayFuncs.arrayDescription(test1));
        System.out.println("computeSize of test1 (10*9*8)*4:   "+ArrayFuncs.computeSize(test1));
	System.out.println("\n");
        System.out.println("getBaseClass:  test2: Base type of boolean array is:"+ ArrayFuncs.getBaseClass(test2));
        System.out.println("getBaseLength: test2: "+ArrayFuncs.getBaseLength(test2));
        System.out.println("arrayDescription of  test2: \n"+ArrayFuncs.arrayDescription(test2));
        System.out.println("computeSize of test2 (5+4+3+2)*1:   "+ArrayFuncs.computeSize(test2));
	System.out.println("\n");
        System.out.println("getBaseClass:  test3: Base type of double array is: "+ ArrayFuncs.getBaseClass(test3));
        System.out.println("getBaseLength: test3: "+ArrayFuncs.getBaseLength(test3));
        System.out.println("arrayDescription of test3: \n"+ArrayFuncs.arrayDescription(test3));
        System.out.println("computeSize of test3 (10*20)*8:   "+ArrayFuncs.computeSize(test3));
	System.out.println("\n");
        System.out.println("getBaseClass:  test4: Base type of StringBuffer array is: "+ArrayFuncs.getBaseClass(test4));
        System.out.println("getBaseLength: test4: (should be -1)"+ArrayFuncs.getBaseLength(test4));
        System.out.println("arrayDescription of test4: "+ArrayFuncs.arrayDescription(test4));
        System.out.println("computeSize: test4 (should be 0):   "+ArrayFuncs.computeSize(test4));
	System.out.println("\n");
	System.out.println("\n");


        System.out.println("examinePrimitiveArray: test1");
        ArrayFuncs.examinePrimitiveArray(test1);
        System.out.println("");
        System.out.println("examinePrimitiveArray: test2");
        ArrayFuncs.examinePrimitiveArray(test2);
        System.out.println("");
        System.out.println("    NOTE: this should show that test2 is not a rectangular array");
        System.out.println("");

        System.out.println("Using aggregates:");
        Object[] agg = new Object[4];
        agg[0] = test1;
	agg[1] = test2;
	agg[2] = test3;
        agg[3] = test4;

        System.out.println("getBaseClass: agg: Base class of aggregate is:"+ArrayFuncs.getBaseClass(agg));
        System.out.println("Size of aggregate is (2880+14+1600+0):" + ArrayFuncs.computeSize(agg));
        System.out.println("This ignores the array of StringBuffers");

        ArrayFuncs.testPattern(test1,(byte)0);
        System.out.println("testPattern:");
        for (int i=0; i < test1.length; i += 1) {
            for (int j=0; j <test1[0].length; j += 1) {
                for(int k=0; k<test1[0][0].length; k += 1) {
                    System.out.print(" "+test1[i][j][k]);
                }
                System.out.println("");
             }
             System.out.println(""); // Double space....
        }


        int[][][] test5 = (int[][][]) ArrayFuncs.deepClone(test1);
        System.out.println("deepClone: copied array");
        for (int i=0; i < test5.length; i += 1) {
            for (int j=0; j <test5[0].length; j += 1) {
                for(int k=0; k<test5[0][0].length; k += 1) {
                    System.out.print(" "+test5[i][j][k]);
                }
                System.out.println("");
             }
             System.out.println(""); // Double space....
        }


        test5[2][2][2] = 99;
        System.out.println("Demonstrating that this is a deep clone:"
           +test5[2][2][2]+" "+test1[2][2][2]);



        System.out.println("Flatten an array:");
        int[] test6 = (int[]) ArrayFuncs.flatten(test1);
        System.out.println("    arrayDescription of test6:"+ArrayFuncs.arrayDescription(test6));
        for (int i=0; i<test6.length; i += 1) {
             System.out.print(" "+test6[i]);
             if (i > 0 && i%10 == 0) System.out.println("");
        }
        System.out.println("");

        System.out.println("Curl an array, we'll reformat test1's data");
        int[] newdims = {8,9,10};

        int[][][] test7 = (int[][][]) ArrayFuncs.curl(test6, newdims);
	System.out.println("    arrayDescription of test7:"+ArrayFuncs.arrayDescription(test7));

        for (int i=0; i < test7.length; i += 1) {
            for (int j=0; j <test7[0].length; j += 1) {
                for(int k=0; k<test7[0][0].length; k += 1) {
                    System.out.print(" "+test7[i][j][k]);
                }
                System.out.println("");
             }
             System.out.println(""); // Double space....
        }

        System.out.println("");
        System.out.println("Test array conversions");

        byte[][][] xtest1 = (byte[][][]) ArrayFuncs.convertArray(test1, byte.class);
        System.out.println("  xtest1 is of type:"+ArrayFuncs.arrayDescription(xtest1));
        System.out.println("   test1[3][3][3]="+test1[3][3][3]+"  xtest1="+xtest1[3][3][3]);

        System.out.println("Converting float[700][700] to byte");
        float[][] big=new float[700][700];
	long time = new java.util.Date().getTime();
        byte[][]  img = (byte[][]) ArrayFuncs.convertArray(big, byte.class);
	long delta = new java.util.Date().getTime() - time;
	
        System.out.println("  img="+ArrayFuncs.arrayDescription(img)+" took "+delta+" ms") ;

        System.out.println("End of tests");
    }
}
