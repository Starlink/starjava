/*
 *  Macro definitions of generic functions in NDFJImpl.
 *
 *  Usage:
 *     Include this file in your C source, predefining the macros
 *
 *        JAVATYPE: one of: Double, Float, Int, Short and Byte
 *        JAVASIG: one of "[D", "[F", "[I", "[S", "[B" (signature of
 *                 <JAVATYPE>[])
 *        NDFTYPE:  equivalent one of "_DOUBLE", "_REAL", "_INTEGER", "_WORD"
 *                  and "_BYTE"
 *        CTYPE:    Java equivalent of NDFTYPE in C, i.e.: jdouble, jfloat,
 *                  jint, jshort or jbyte.
 */

 /*
  *  Predefine the following macros to enable token concatenation at
  *  the correct moment
  */

#if !defined(_NDFJIMPLGET_)
#define XGET2(type) Java_uk_ac_starlink_splat_imagedata_NDFJ_nGet2D ## type    /* 2D function name */
#define GET2(type) XGET2(type)

#define XGET1(type) Java_uk_ac_starlink_splat_imagedata_NDFJ_nGet1D ## type    /* 1D function name */
#define GET1(type) XGET1(type)

#define XGETARRAY(type) New ## type ## Array    /* Get Java array function */
#define GETARRAY(type) XGETARRAY(type)

#define XSETARRAY(type) Set ## type ## ArrayRegion /* Set array region function */
#define SETARRAY(type) XSETARRAY(type)

#define XARRAYREF(type) type ## Array        /* a type[] reference */
#define ARRAYREF(type) XARRAYREF(type)

typedef char byte;                    /* used to map byte function name to C */

#define _NDFJIMPLGET_
#endif

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nGet2D<JAVATYPE>
 *
 *  Purpose:
 *     Gets an NDF data component returning a copy as a 2D <JAVATYPE>
 *     array.
 *
 *  Params:
 *     jindf = NDF identifier
 *     jcomp = name of NDF component.
 *     jcomplete = whether to return complete NDF component. Extra
 *                 dimensions are "vectorised" into last.
 *
 *  Returns:
 *     jobjectArray = 2D array of <JAVATYPE> values.
 *
 */

JNIEXPORT jobjectArray JNICALL GET2( JAVATYPE )
    (JNIEnv *env, jclass class, jint jindf, jstring jcomp,
     jboolean jcomplete )
{

   /*  Local variables */
   char comp[DAT__SZNAM+1]; /*  Pointer to name string */
   CTYPE *pntr;             /*  Pointer to data */
   int dims[NDF__MXDIM];    /*  NDF dimensions */
   int el;                  /*  Number of elements in data component */
   int firstDim;            /*  First dimension of NDF */
   int i;                   /*  Loop variable */
   int indf;                /*  NDF identifier */
   int ndim;                /*  Number of dimensions */
   int secondDim;           /*  Second dimension of NDF */
   int status;              /*  NDF library status */
   jsize slen;              /*  String length */
   jclass arrCls;           /*  Reference to class "JAVATYPE[]" */
   jobjectArray result;     /*  JAVATYPE[][] result */
   ARRAYREF(CTYPE) farr;    /*  JAVATYPE[] to store row */
   void *vpntr[1];          /*  Pointer to data */

   /* Start NDF context */
   ndfBegin();

   /*  Default (error) result is NULL */
   result = NULL;

   /*  Import the data component string  */
   slen = (*env)->GetStringUTFLength( env, jcomp );
   slen = MIN( slen, (jsize) DAT__SZNAM );
   (*env)->GetStringUTFRegion( env, jcomp, (jsize) 0, slen, comp );

   /*  Import NDF identifier  */
   indf = (int) jindf;

   /*  Establish local status and stop NDF from issuing errors */
   status = SAI__OK;
   errMark();

   /*  Map the requested NDF data component */
   ndfMap( indf, comp, NDFTYPE, "READ", vpntr, &el, &status );
   pntr = vpntr[0];

   /*  Get the size of the NDF. Note we just want two dimensions. */
   ndfDim( indf, NDF__MXDIM, dims, &ndim, &status );

   /*  Set first dimension */
   if ( status == SAI__OK ) {
      firstDim = dims[0];
      if ( ndim == 1 ) {

         /*  NDF has one dimension */
         secondDim = 1;
      } else {

         /*  Second dimension next significant dimension */
         secondDim = dims[1];

         /*  Fold extra dimensions into this, if requested */
         if ( ndim > 2 && jcomplete == JNI_TRUE ) {
            for ( i = 2; i < ndim; i++ ) secondDim *= dims[i];
         }
      }

      /*  Dimensionality of NDF established. Now create the array of
       *  objects to contain the data (in Java 2D arrays are arrays
       *  of arrays, not a vectorised data segment).
       */
      arrCls = (*env)->FindClass( env, JAVASIG );
      if ( arrCls != NULL ) {
         result = (*env)->NewObjectArray( env, firstDim, arrCls,
                                          NULL );
      }
      if ( result != NULL ) {
         for ( i = 0; i < firstDim; i++, pntr += secondDim ) {

            /*  Now add rows of JAVATYPE[] containing the data */
            farr = (*env)->GETARRAY(JAVATYPE)( env, secondDim );
            if ( farr == NULL ) {
               result = NULL;
               break;
            }

            /*  Finally copy the data into place */
            (*env)->SETARRAY(JAVATYPE)( env, farr, 0, secondDim, pntr );
            (*env)->SetObjectArrayElement( env, result, i, farr );
            (*env)->DeleteLocalRef( env, farr );
         }
      }
   }

   /*  Clear NDF status, if needed and release the error stack */
   /*  XXX how to convert EMS errors into Java exceptions XXX */
   if ( status != SAI__OK ) {
      errFlush( &status );
      errAnnul( &status );
   }
   errRlse();

   /*  Release the mapped NDF component (use begin/end as error
    *  component cannot be unmapped using that name) */
   ndfEnd( &status );

   /*  Return JAVATYPE[][] object array */
   return result;
}

/*
 *  Name:
 *     Java_uk_ac_starlink_splat_imagedata_NDFJ_nGet1D<JAVATYPE>
 *
 *  Purpose:
 *     Gets an NDF data component returning a copy as a 1D <JAVATYPE>
 *     data array.
 *
 *  Params:
 *     jindf = NDF identifier
 *     jcomp = name of NDF component.
 *     jcomplete = whether to return complete NDF component. In this
 *                 case all dimensions are vectorized.
 *
 *  Returns:
 *     <CTYPE>Array = 1D array of <JAVATYPE> values.
 * */
JNIEXPORT ARRAYREF(CTYPE) JNICALL GET1( JAVATYPE )
    (JNIEnv *env, jclass class, jint jindf, jstring jcomp,
     jboolean jcomplete )
{

   /*  Local variables */
   char comp[DAT__SZNAM+1]; /*  Pointer to name string */
   CTYPE *pntr;             /*  Pointer to data */
   int dims[NDF__MXDIM];    /*  NDF dimensions */
   int el;                  /*  Number of elements in data component */
   int dim;                 /*  First dimension of NDF */
   int indf;                /*  NDF identifier */
   int ndim;                /*  Number of dimensions */
   int status;              /*  NDF library status */
   jsize slen;              /*  String length */
   ARRAYREF(CTYPE) farr;    /*  JAVATYPE[] to store row */
   void *vpntr[1];          /*  Pointer to data */

   /* Start NDF context */
   ndfBegin();

   /*  Default (error) result is NULL */
   farr = NULL;

   /*  Import the data component string  */
   slen = (*env)->GetStringUTFLength( env, jcomp );
   slen = MIN( slen, (jsize) DAT__SZNAM );
   (*env)->GetStringUTFRegion( env, jcomp, (jsize) 0, slen, comp );

   /*  Import NDF identifier  */
   indf = (int) jindf;

   /*  Establish local status and stop NDF from issuing errors */
   status = SAI__OK;
   errMark();

   /*  Map the requested NDF data component */
   ndfMap( indf, comp, NDFTYPE, "READ", vpntr, &el, &status );
   pntr = vpntr[0];

   /*  Get the size of the NDF. Note we may want one or all dimensions. */
   ndfDim( indf, NDF__MXDIM, dims, &ndim, &status );

   /*  Set the dimension */
   if ( status == SAI__OK ) {
      dim = dims[0];
      if ( ndim > 1 && jcomplete == JNI_TRUE ) {
         dim = el;
      }

      /*  Dimensionality of NDF established. Now create the array
       *  to contain the data.
       */
      farr = (*env)->GETARRAY(JAVATYPE)( env, (jsize) dim );
      if ( farr != NULL ) {

         /*  Finally copy the data into place */
         (*env)->SETARRAY(JAVATYPE)( env, farr, (jsize) 0, (jsize) dim, pntr );
      }
   }

   /*  Clear NDF status, if needed and release the error stack */
   /*  XXX how to convert EMS errors into Java exceptions XXX */
   if ( status != SAI__OK ) {
      errFlush( &status );
      errAnnul( &status );
   }
   errRlse();

   /*  Release the mapped NDF component (use begin/end as error
    *  component cannot be unmapped using that name) */
   ndfEnd( &status );

   /*  Return JAVATYPE[] array */
   return farr;
}
