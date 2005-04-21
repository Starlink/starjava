/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     11-MAR-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import cern.colt.GenericSorting;
import cern.colt.Swapper;
import cern.colt.function.IntComparator;

/**
 * Provides various despoke sorting and searching facilities for SPLAT.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public final class Sort
{
    /**
     *  It's a class of static methods.
     */
    private Sort()
    {
        // Do nothing.
    }

    /**
     * Sort a list of ranges (that is pairs of coordinates) into
     * increasing order and merge any overlapped ranges into single
     * ranges (do this to make sure that ranges define a monotonic set).
     *
     * @param ranges the ranges that require sorting and merging.
     * @return the sorted and merged set of ranges.
     */
    public static double[] sortAndMerge( double[] ranges )
    {
        //  Short number of ranges expected so nothing clever
        //  required, but we do need to retain the lower-upper bounds
        //  pairing for a proper merge when overlapped, cannot just
        //  sort the array and go with that.
        double[] sorted = (double[]) ranges.clone();
        Sort.insertionSort( sorted );

        //  Merge any overlapped ranges into single ranges.
        double[] merged = new double[sorted.length];
        int n = 0;
        merged[0] = sorted[0];
        merged[1] = sorted[1];
        for ( int i = 2; i < merged.length; i+=2 ) {

            if ( merged[n+1] > sorted[i] ) {
                //  Current range end overlaps beginning of next
                //  range, so copy end of next range to be end of this
                //  range.
                merged[n+1] = sorted[i+1];
            } 
            else {

                //  No overlap so just copy range.
                n+=2;
                merged[n] = sorted[i];
                merged[n+1] = sorted[i+1];
            }
        }
        n+=2;

        if ( n != sorted.length ) {

            //  Had some overlaps so trim off unnecessary end.
            double[] trimmed = new double[n];
            System.arraycopy( merged, 0, trimmed, 0, n );
            merged = trimmed;
        }
        return merged;
    }

    /**
     * Return two indices of the values in an array that lie above and
     * below a given value. If the value doesn't lie within the range
     * the two indices are returned as the nearest end point. The
     * array of values must be increasing or decreasing
     * monotonically.
     *
     * @param array the array of values to be searched
     * @param value the value to be located
     */
    public static int[] binarySearch( double[] array, double value )
    {
        int bounds[] = new int[2];
        int low = 0;
        int high = array.length - 1;
        boolean increases = ( array[low] < array[high] );

        // Check off scale.
        if ( ( increases && value < array[low] ) ||
             ( ! increases && value > array[low] ) ) {
            high = low;
        }
        else if ( ( increases && value > array[high] ) ||
                  ( ! increases && value < array[high] ) ) {
            low = high;
        }
        else {
            //  Use a binary search as values should be sorted to increase
            //  in either direction (wavelength, pixel coordinates etc.).
            int mid = 0;
            if ( increases ) {
                while ( low < high - 1 ) {
                    mid = ( low + high ) / 2;
                    if ( value < array[mid] ) {
                        high = mid;
                    }
                    else if ( value > array[mid] ) {
                        low = mid;
                    }
                    else {
                        // Exact match.
                        low = high = mid;
                        break;
                    }
                }
            }
            else {
                while ( low < high - 1 ) {
                    mid = ( low + high ) / 2;
                    if ( value > array[mid] ) {
                        high = mid;
                    }
                    else if ( value < array[mid] ) {
                        low = mid;
                    }
                    else {
                        // Exact match.
                        low = high = mid;
                        break;
                    }
                }
            }
        }
        bounds[0] = low;
        bounds[1] = high;
        return bounds;
    }

    /**
     * Return the index of the value that lies most closely to a given
     * value. The array of values must be increasing or decreasing
     * monotonically.
     *
     * @param array the array of values to be searched
     * @param value the value to be located
     */
    public static int lookup( double[] array, double value )
    {
        int bounds[] = binarySearch( array, value );
        int low = bounds[0];
        int high = bounds[1];

        //  Find which position is nearest in reality.
        int index = 0;
        if ( ( value - array[low] ) < ( array[high] - value ) ) {
            index = low;
        }
        else {
            index = high;
        }
        return index;
    }

    /**
     * Sort a double precision array, using an insertion sort.
     * This sort is very fast for small numbers of values and gets a
     * boost from pre-sorted arrays. Insertion sort is also stable
     * (which can be important for maintaining the relationship to
     * to other data).
     */
    public static void insertionSort( double[] a )
    {
        int i;
        int j;
        double v;

        for ( i = 1; i < a.length; i++ ) {
            v = a[i];
            j = i;
            while ( ( j > 0 ) && ( a[j-1] > v ) ) {
                a[j] = a[j-1];
                j--;
            }
            a[j] = v;
        }
    }

    /**
     * Creates an index that sorts a double precision array. On exit a
     * is sorted. A reordering of associated arrays without the need for
     * additional memory can be performed using the 
     * {@link #applySortIndex} methods.
     */
    public static int[] insertionSort2( double[] a )
    {
        int size = a.length;
        int[] remap = new int[a.length];

        int i;
        int j;
        double v;
        
        for ( i = 1; i < size; i++ ) {
            v = a[i];
            j = i;
            while ( ( j > 0 ) && ( a[j-1] > v ) ) {
                remap[j] = j-1;
                a[j] = a[j-1];
                j--;
            }
            remap[j] = i;
            a[j] = v;
        }

        return remap;
    }
    
    //  XXX re-visit PDA_RINP[x] algorithm sometime to see if we can 
    //  re-order in place.
    public static double[] applySortIndex( double[] a, int[] remap, 
                                           boolean incr )
    {
        int size = a.length;
        double[] newa = new double[size];
        if ( incr ) {
            for ( int j = 0; j < size; j++ ) {
                newa[j] = a[remap[j]];
            }
        }
        else {
            int i = size - 1 ;
            for ( int j = 0; j < size; j++ ) {
                newa[j] = a[remap[i--]];
            }
        }
        return newa;
    }
    

    /**
     * Insertion sort a double precision array into increasing order. Also
     * sorts an associated array doubles. This sort is very fast for
     * small numbers of values and gets a boost from pre-sorted
     * arrays.
     */
    public static void insertionSort2a( double[] a, double[] ia )
    {
        int i;
        int j;
        double v;
        double iv;

        for ( i = 1; i < a.length; i++ ) {
            v = a[i];
            iv = ia[i];
            j = i;
            while ( ( j > 0 ) && ( a[j-1] > v ) ) {
                a[j] = a[j-1];
                ia[j] = ia[j-1];
                j--;
            }
            a[j] = v;
            ia[j] = iv;
        }
    }

    /**
     * Insertion sort a double precision array into increasing order. Also
     * sorts two associated arrays of doubles. This sort is very fast for
     * small numbers of values and gets a boost from pre-sorted
     * arrays.
     */
    public static void insertionSort2a( double[] a, double[] ia, double[] ib )
    {
        int i;
        int j;
        double v;
        double iv;
        double ivv;

        for ( i = 1; i < a.length; i++ ) {
            v = a[i];
            iv = ia[i];
            ivv = ib[i];
            j = i;
            while ( ( j > 0 ) && ( a[j-1] > v ) ) {
                a[j] = a[j-1];
                ia[j] = ia[j-1];
                ib[j] = ib[j-1];
                j--;
            }
            a[j] = v;
            ia[j] = iv;
            ib[j] = ivv;
        }
    }

    /**
     * Insertion sort a double precision array into decreasing order. Also
     * sorts an associated array doubles. This sort is very fast for
     * small numbers of values and gets a boost from pre-sorted
     * arrays.
     */
    public static void insertionSort2d( double[] a, double[] ia )
    {
        int i;
        int j;
        double v;
        double iv;

        for ( i = 1; i < a.length; i++ ) {
            v = a[i];
            iv = ia[i];

            j = i;
            while ( ( j > 0 ) && ( a[j-1] < v ) ) {
                a[j] = a[j-1];
                ia[j] = ia[j-1];
                j--;
            }
            a[j] = v;
            ia[j] = iv;
        }
    }

    /**
     * Sort a double precision array, plus array of associated integers using
     * an insertion sort. This sort is very fast for small numbers of values
     * and gets a boost from pre-sorted arrays.
     */
    public static void insertionSort2( double[] a, int[] ia )
    {
        int i;
        int j;
        double v;
        int iv;

        for ( i = 1; i < a.length; i++ ) {
            v = a[i];
            iv = ia[i];
            j = i;
            while ( ( j > 0 ) && ( a[j-1] > v ) ) {
                a[j] = a[j-1];
                ia[j] = ia[j-1];
                j--;
            }
            a[j] = v;
            ia[j] = iv;
        }
    }

    /**
     * Sort an integer array, plus array of associated integers using
     * an insertion sort. This sort is very fast for small numbers of values
     * and gets a boost from pre-sorted arrays.
     */
    public static void insertionSort2( int[] a, int[] ia )
    {
        int i;
        int j;
        int v;
        int iv;

        for ( i = 1; i < a.length; i++ ) {
            v = a[i];
            iv = ia[i];
            j = i;
            while ( ( j > 0 ) && ( a[j-1] > v ) ) {
                a[j] = a[j-1];
                ia[j] = ia[j-1];
                j--;
            }
            a[j] = v;
            ia[j] = iv;
        }
    }

    /**
     * Sort a double precision array, plus an array of associated integers
     * using a sort suitable for large numbers of values, into ascending
     * order. This is stable and has guaranteed nlogn performance. Based on
     * the {@link cern.colt.GenericSorting} class.
     */
    public static void sort( double a[], int[] ia )
    {
        //  Small numbers might as well get on with it.
        if ( a.length < 10 ) {
            insertionSort2( a, ia );
            return;
        }
        
        final double[] aa = a;
        final int[] iaa = ia;
        Swapper swapper = new Swapper() 
            {
                public void swap( int a, int b )
                {
                    double d = aa[a];
                    aa[a] = aa[b];
                    aa[b] = d;

                    int t = iaa[a];
                    iaa[a] = iaa[b];
                    iaa[b] = t;
                }
            };

        IntComparator comp = new IntComparator() 
            {
                public int compare( int a, int b ) {
                    return aa[a] == aa[b] ? 0 : ( aa[a] < aa[b] ? -1 : 1 );
                }
            };

        GenericSorting.mergeSort( 0, aa.length, comp, swapper );
    }

    /**
     * Sort a double precision array, plus an array of associated doubles
     * using a sort suitable for large numbers of values, into ascending
     * order. This is stable and has guaranteed nlogn performance. Based on
     * the {@link cern.colt.GenericSorting} class.
     */
    public static void sort( double a[], double[] ia )
    {
        //  Small numbers might as well get on with it.
        if ( a.length < 10 ) {
            insertionSort2a( a, ia );
            return;
        }
        
        final double[] aa = a;
        final double[] iaa = ia;
        Swapper swapper = new Swapper() 
            {
                public void swap( int a, int b )
                {
                    double d = aa[a];
                    aa[a] = aa[b];
                    aa[b] = d;
                    
                    d = iaa[a];
                    iaa[a] = iaa[b];
                    iaa[b] = d;
                }
            };

        IntComparator comp = new IntComparator() 
            {
                public int compare( int a, int b ) {
                    return aa[a] == aa[b] ? 0 : ( aa[a] < aa[b] ? -1 : 1 );
                }
            };

        GenericSorting.mergeSort( 0, aa.length, comp, swapper );
    }

    /**
     * Sort a double precision array, plus two arrays of associated doubles
     * using a sort suitable for large numbers of values, into ascending
     * order. This is stable and has guaranteed nlogn performance. Based on
     * the {@link cern.colt.GenericSorting} class.
     */
    public static void sort( double a[], double[] ia, double[] ib )
    {
        //  Small numbers might as well get on with it.
        if ( a.length < 10 ) {
            insertionSort2a( a, ia, ib );
            return;
        }
        
        final double[] aa = a;
        final double[] iaa = ia;
        final double[] ibb = ib;
        Swapper swapper = new Swapper() 
            {
                public void swap( int a, int b )
                {
                    double d = aa[a];
                    aa[a] = aa[b];
                    aa[b] = d;

                    d = iaa[a];
                    iaa[a] = iaa[b];
                    iaa[b] = d;

                    d = ibb[a];
                    ibb[a] = ibb[b];
                    ibb[b] = d;
                }
            };

        IntComparator comp = new IntComparator() 
            {
                public int compare( int a, int b ) {
                    return aa[a] == aa[b] ? 0 : ( aa[a] < aa[b] ? -1 : 1 );
                }
            };

        GenericSorting.mergeSort( 0, aa.length, comp, swapper );
    }
}
