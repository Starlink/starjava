// Copyright 1996, Marimba Inc. All Rights Reserved.
// @(#)QuickSort.java, 1.6, 10/10/97

package jsky.util;

/**
 * Quick sort class, highly optimized and very specific.  This one
 * sorts strings.
 *
 * @author  Jonathan Payne
 * @version     1.6, 10/10/97
 */
public final class QuickSort {

    /**
     * This class can't be instanciated.
     */
    QuickSort() {
    }

    /**
     * Sort an array of strings.  Case sensitive.
     */
    public static void sort(String a[], int off, int len) {
        sort0(a, off, off + len - 1, true);
    }

    /**
     * Sort an array of strings.  caseSens determines case sensitivity.
     */
    public static void sort(String a[], int off, int len, boolean caseSens) {
        sort0(a, off, off + len - 1, caseSens);
    }

    static int compareSens(String a, String b, boolean caseSens) {
        if (caseSens) {
            return (a.compareTo(b));
        }
        else {
            return (a.toLowerCase().compareTo(b.toLowerCase()));
        }
    }

    static void sort0(String a[], int lo0, int hi0, boolean caseSens) {
        int lo = lo0;
        int hi = hi0;
        String mid;

        if (hi0 > lo0) {
            mid = a[(lo0 + hi0) / 2];

            while (lo <= hi) {
                while (lo < hi0 && compareSens(a[lo], mid, caseSens) < 0)
                    lo += 1;
                while (hi > lo0 && compareSens(a[hi], mid, caseSens) > 0)
                    hi -= 1;
                if (lo <= hi) {
                    String tmp = a[lo];
                    a[lo] = a[hi];
                    a[hi] = tmp;

                    lo += 1;
                    hi -= 1;
                }
            }
            if (lo0 < hi)
                sort0(a, lo0, hi, caseSens);
            if (lo < hi0)
                sort0(a, lo, hi0, caseSens);
        }
    }

    /**
     * Sort an array of integers.
     */
    public static void sort(int a[], int off, int len) {
        sort0(a, off, off + len - 1);
    }

    static void sort0(int a[], int lo0, int hi0) {
        int lo = lo0;
        int hi = hi0;
        int mid;

        if (hi0 > lo0) {
            mid = a[(lo0 + hi0) / 2];

            while (lo <= hi) {
                while (lo < hi0 && a[lo] < mid)
                    lo += 1;
                while (hi > lo0 && a[hi] > mid)
                    hi -= 1;
                if (lo <= hi) {
                    int tmp = a[lo];
                    a[lo] = a[hi];
                    a[hi] = tmp;

                    lo += 1;
                    hi -= 1;
                }
            }
            if (lo0 < hi)
                sort0(a, lo0, hi);
            if (lo < hi0)
                sort0(a, lo, hi0);
        }
    }

    /**
     * Sort an array of Sortable objects. The rock is passed along
     * to each compare.
     */
    public static void sort(Sortable a[], int off, int len, Object rock) {
        sort0(a, off, off + len - 1, rock);
    }

    static void sort0(Sortable a[], int lo0, int hi0, Object rock) {
        int lo = lo0;
        int hi = hi0;
        Sortable mid;

        if (hi0 > lo0) {
            mid = a[(lo0 + hi0) / 2];

            while (lo <= hi) {
                while (lo < hi0 && a[lo].compareTo(mid, rock) < 0)
                    lo += 1;
                while (hi > lo0 && a[hi].compareTo(mid, rock) > 0)
                    hi -= 1;
                if (lo <= hi) {
                    Sortable tmp = a[lo];
                    a[lo] = a[hi];
                    a[hi] = tmp;

                    lo += 1;
                    hi -= 1;
                }
            }
            if (lo0 < hi)
                sort0(a, lo0, hi, rock);
            if (lo < hi0)
                sort0(a, lo, hi0, rock);
        }
    }
}
