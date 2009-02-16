/*
*+
*  Name:
*     IntraMap.c

*  Purpose:
*     JNI implementations of native methods of IntraMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor

*  History:
*     28-SEP-2001 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_IntraMap.h"


/* Typedefs. */
typedef struct {
   jobject trans;
   jmethodID tranpID;
} IntraInfo;
   

/* Static functions. */
static void getIntraInfo( JNIEnv *env, AstIntraMap *map,
                          jobject *transp, jmethodID *tranpIDp );
static void setIntraInfo( JNIEnv *env, AstIntraMap *map, jobject trans );
static void clearIntraInfo( JNIEnv *env, AstIntraMap *map );
static void tranWrap( AstMapping *map, int npoint, int ncoord_in, 
                      const double *ptr_in[], int forward, int ncoord_out, 
                      double *ptr_out[] );


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_IntraMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject trans,        /* Transformer object */
   jint nin,             /* Number of input coordinates */
   jint nout             /* Number of output coordinates */
) {
   AstPointer pointer;
   jclass tclass;
   jint hash;
   jstring jAuthor;
   jstring jCname;
   jstring jContact;
   jstring jPurpose;
   jboolean canmap = 0;
   jmethodID canTransformCoordsMethodID;
   jmethodID hasForwardMethodID;
   jmethodID hasInverseMethodID;
   jmethodID simpFIMethodID;
   jmethodID simpIFMethodID;
   jmethodID getAuthorMethodID;
   jmethodID getContactMethodID;
   jmethodID getPurposeMethodID;
   const char *author;
   const char *cname;
   const char *contact;
   const char *purpose;
   char *namebuf;
   int flags;

   if ( jniastCheckNotNull( env, trans ) ) {

      /* Get method IDs for the transformer. */
      ( tclass = (*env)->GetObjectClass( env, trans ) ) &&
      ( canTransformCoordsMethodID = 
           (*env)->GetMethodID( env, tclass, 
                                "canTransformCoords", "(II)Z" ) ) &&
      ( hasForwardMethodID = 
           (*env)->GetMethodID( env, tclass, "hasForward", "()Z" ) ) &&
      ( hasInverseMethodID = 
           (*env)->GetMethodID( env, tclass, "hasInverse", "()Z" ) ) &&
      ( simpFIMethodID = 
           (*env)->GetMethodID( env, tclass, "simpFI", "()Z" ) ) &&
      ( simpIFMethodID =
           (*env)->GetMethodID( env, tclass, "simpIF", "()Z" ) ) &&
      ( getPurposeMethodID =
           (*env)->GetMethodID( env, tclass, "getPurpose", 
                                "()Ljava/lang/String;" ) ) &&
      ( getAuthorMethodID =
           (*env)->GetMethodID( env, tclass, "getAuthor",
                                "()Ljava/lang/String;" ) ) &&
      ( getContactMethodID = 
           (*env)->GetMethodID( env, tclass, "getContact",
                                "()Ljava/lang/String;" ) ) &&
      1;
        
      /* See if the Transformer object is happy to do this mapping. */
      if ( ! (*env)->ExceptionCheck( env ) ) {
         canmap = (*env)->CallBooleanMethod( env, trans, 
                                             canTransformCoordsMethodID,
                                             nin, nout );
      }
      if ( canmap == JNI_FALSE && ! (*env)->ExceptionCheck( env ) ) {
         jniastThrowIllegalArgumentException( env, "Transformer will not map "
                                              " %d -> %d coordinates",
                                              nin, nout );
      }
      else if ( ! (*env)->ExceptionCheck( env ) ) {

         /* Construct a unique name to register this function. */
         jCname = (*env)->CallObjectMethod( env, tclass,
                                            ClassGetNameMethodID ); 
         if ( ! (*env)->ExceptionCheck( env ) ) {
            hash = (*env)->CallIntMethod( env, trans, ObjectHashCodeMethodID );
         }
         cname = jniastGetUTF( env, jCname );
         namebuf = jniastMalloc( env, strlen( cname ) + 20 );
         strcpy( namebuf, cname );

         /* I don't like the cast in this line, however without it 
          * gcc -Wformat complains that a jint is a 'long unsigned int arg'
          * - surely it can't be??? */
         sprintf( namebuf + strlen( cname ), "#%x", (signed int) hash );
         jniastReleaseUTF( env, jCname, cname );
         
         /* Construct the flag integer. */
         flags = 0;
         if ( ! (*env)->ExceptionCheck( env ) && 
              (*env)->CallBooleanMethod( env, trans, hasForwardMethodID ) 
              == JNI_FALSE ) {
            flags = flags | AST__NOFWD;
         }
         if ( ! (*env)->ExceptionCheck( env ) &&
             (*env)->CallBooleanMethod( env, trans, hasInverseMethodID )
              == JNI_FALSE ) {
            flags = flags | AST__NOINV;
         }
         if ( ! (*env)->ExceptionCheck( env ) && 
              (*env)->CallBooleanMethod( env, trans, simpFIMethodID ) 
              == JNI_TRUE ) {
            flags = flags | AST__SIMPFI;
         }
         if ( ! (*env)->ExceptionCheck( env ) &&
              (*env)->CallBooleanMethod( env, trans, simpIFMethodID )
              == JNI_TRUE ) {
            flags = flags | AST__SIMPIF;
         }
         
         /* Get the associated strings (purpose, author, contact). */
         purpose = "";
         contact = "";
         author = "";
         jPurpose = NULL;
         jContact = NULL;
         jAuthor = NULL;
         if ( ! (*env)->ExceptionCheck( env ) &&
              ( jPurpose = (*env)->CallObjectMethod( 
                                   env, trans, getPurposeMethodID ) ) ) {
            purpose = jniastGetUTF( env, jPurpose );
         }
         if ( ! (*env)->ExceptionCheck( env ) &&
              ( jContact = (*env)->CallObjectMethod( 
                                   env, trans, getContactMethodID ) ) ) {
            contact = jniastGetUTF( env, jContact );
         }
         if ( ! (*env)->ExceptionCheck( env ) &&
              ( jAuthor = (*env)->CallObjectMethod( 
                                  env, trans, getAuthorMethodID ) ) ) {
            author = jniastGetUTF( env, jAuthor );
         }

         /* Register the transformation function, and construct an AST
          * IntraMap object based on it. */
         ASTCALL(
            astIntraReg( namebuf, (int) nin, (int) nout, tranWrap, flags,
                         purpose, author, contact );
            pointer.IntraMap = astIntraMap( namebuf, nin, nout, "" );
         )

         /* Release temporary resources. */
         if ( ! (*env)->ExceptionCheck( env ) ) {
            free( namebuf );
            if ( jPurpose ) { 
               jniastReleaseUTF( env, jPurpose, purpose );
            }
            if ( jContact ) {
               jniastReleaseUTF( env, jContact, contact );
            }
            if ( jAuthor ) {
               jniastReleaseUTF( env, jAuthor, author );
            }
         }

         /* Store a reference to the Transformer in the IntraMap. */
         setIntraInfo( env, pointer.IntraMap, trans );

         /* Set the pointer field of the java object to reference the 
          * AST IntraMap. */
         jniastInitObject( env, this, pointer );
      }
   }
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_IntraMap_destroy(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );

   clearIntraInfo( env, pointer.IntraMap );
}


static void tranWrap( AstMapping *map, int npoint, int ncoord_in,
                      const double **ptr_in, int forward, int ncoord_out,
                      double **ptr_out ) {
/*
*+
*  Name:
*     tranWrap

*  Purpose:
*     Invoke the IntraMap's tranP method on behalf of AST.

*  Description:
*     This function is called under the control of AST when an 
*     IntraMap's mapping is used to transform points.  It invokes the
*     java object's tranP method and returns the result got from it.

*  Arguments:
*     map
*        Pointer to the Mapping to be applied.
*        Must be locked on entry; will be locked on exit.
*     npoint
*        The number of points to be transformed.
*     ncoord_in
*        The number of coordinates being supplied for each input point
*        (i.e. the number of dimensions of the space in which the
*        input points reside).
*     ptr_in
*        An array of pointers to double, with "ncoord_in"
*        elements. Element "ptr_in[coord]" should point at the first
*        element of an array of double (with "npoint" elements) which
*        contain the values of coordinate number "coord" for each
*        input (untransformed) point. The value of coordinate number
*        "coord" for input point number "point" is therefore given by
*        "ptr_in[coord][point]" (assuming both indices are
*        zero-based).
*     forward
*        A non-zero value indicates that the Mapping's forward
*        coordinate transformation is to be applied, while a zero
*        value indicates that the inverse transformation should be
*        used.
*     ncoord_out
*        The number of coordinates being generated by the Mapping for
*        each output point (i.e. the number of dimensions of the space
*        in which the output points reside). This need not be the same
*        as "ncoord_in".
*     ptr_out
*        An array of pointers to double, with "ncoord_out"
*        elements. Element "ptr_out[coord]" should point at the first
*        element of an array of double (with "npoint" elements) into
*        which the values of coordinate number "coord" for each output
*        (transformed) point will be written.  The value of coordinate
*        number "coord" for output point number "point" will therefore
*        be found in "ptr_out[coord][point]".
*-
*/
   JNIEnv *env;
   jobject trans;
   jobject jIn;
   jobject jOut;
   jdoubleArray jArr;
   jmethodID tranpID;
   int i;

   /* Retrieve the auxiliary information about this IntraMap. */
   env = jniastGetEnv();
   getIntraInfo( env, (AstIntraMap *) map, &trans, &tranpID );

   /* Prepare arguments for calling the java IntraMap object's tranP method. */
   if ( ! (*env)->ExceptionCheck( env ) ) {
      jIn = (*env)->NewObjectArray( env, ncoord_in, DoubleArrayClass, NULL );
   }
   if ( ! (*env)->ExceptionCheck( env ) ) {
      for ( i = 0; i < ncoord_in; i++ ) {
         if ( ( jArr = (*env)->NewDoubleArray( env, npoint ) ) ) {
            (*env)->SetDoubleArrayRegion( env, jArr, 0, npoint, 
                                          (jdouble *) ptr_in[ i ] );
            (*env)->SetObjectArrayElement( env, jIn, i, jArr );
         }
         else {
            break;
         }
      }
   }
   
   /* Call the java method. */
   if ( ! (*env)->ExceptionCheck( env ) ) {
      jOut = (*env)->CallObjectMethod( env, trans, tranpID, 
                                       (jint) npoint, (jint) ncoord_in, jIn, 
                                       ( forward ? JNI_TRUE : JNI_FALSE ), 
                                       (jint) ncoord_out );
   }

   /* Construct a C array from the result of the java method. */
   if ( jniastCheckArrayLength( env, jOut, ncoord_out ) ) {
      for ( i = 0; i < ncoord_out; i++ ) {
         jArr = (*env)->GetObjectArrayElement( env, jOut, i );
         if ( jniastCheckArrayLength( env, jArr, npoint ) ) {
            (*env)->GetDoubleArrayRegion( env, jArr, 0, npoint, 
                                          (jdouble *) ptr_out[ i ] );
         }
         else {
            break;
         }
      }
   }

   /* Set status if an exception is pending. */
   if ( (*env)->ExceptionCheck( env ) ) {
      astSetStatus( AST__ITFER );
   }
}


static void setIntraInfo( JNIEnv *env, AstIntraMap *map, jobject trans ) {
/*
*+
*  Name:
*     setIntraInfo

*  Purpose:
*     Store auxiliary information in an IntraMap.

*  Description:
*     This function stores a pointer to additional information about an
*     AST IntraMap in its IntraFlag attribute.  It makes global 
*     references where necessary to prevent unwanted garbage collection.
*     The stored information may be got at a later date by calling
*     getIntraInfo.  
*
*     For every call to this function a corresponding call to 
*     clearIntraInfo should be made at some point to ensure that 
*     resources are reclaimed.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI environment.
*     map = AstIntraMap *
*        The AST IntraMap object in which to store the information.
*        Must be locked on entry; will be locked on exit.
*     trans = jobject
*        The IntraMap's Transformer object.
*-
*/
   IntraInfo *info;
   jclass tclass;

   if ( ! (*env)->ExceptionCheck( env ) ) {
      tclass = (*env)->GetObjectClass( env, trans );
      info = jniastMalloc( env, sizeof( IntraInfo ) );
      info->trans = (*env)->NewGlobalRef( env, trans );
      info->tranpID = (*env)->GetMethodID( env, tclass, "tranP", 
                                           "(II[[DZI)[[D" );
      ASTCALL(
         astSet( map, "IntraFlag = %p", info );
      )
   }
}


static void getIntraInfo( JNIEnv *env, AstIntraMap *map, jobject *transp,
                          jmethodID *tranpIDp ) {
/*
*+
*  Name:
*     getIntraInfo

*  Purpose:
*     Retrieve auxiliary information about an IntraMap.

*  Description:
*     This function reads auxiliary information from the IntraFlag 
*     attribute of an AST IntraMap - it must previously have been
*     stored there using setInfo.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI environment.
*     map = AstIntraMap *
*        The AST IntraMap object to retrieve information from.
*        Must be locked on entry; will be locked on exit.
*     transp = jobject *
*        The address of the variable to receive the Transformer 
*        associated with the IntraMap.
*     tranpIDp = jmethodID *
*        The address of the variable to receive the method ID for the
*        Transformer's tranP method.
*-
*/
   const char *intraflag;
   IntraInfo *info;
   int nconv;

   /* Read a pointer to the IntraInfo structure from the IntraFlag value. */
   intraflag = astGetC( map, "IntraFlag" );
   if ( ! (*env)->ExceptionCheck( env ) ) {
      nconv = sscanf( intraflag, "%p", &info );
      if ( nconv != 1 ) {
         jniastThrowError( env, "Error reading IntraFlag: %s", intraflag );
      }

      /* Set the return variables from the structure. */
      *transp = info->trans;
      *tranpIDp = info->tranpID;
   }
}


static void clearIntraInfo( JNIEnv *env, AstIntraMap *map ) {
/*
*+
*  Name:
*     clearIntraInfo

*  Purpose:
*     Reclaim resources allocated by setIntraInfo.

*  Description:
*     This function should be called after the Transformer object 
*     which was stored in the IntraFlag attribute of an AST IntraMap
*     will no longer be needed.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI environment.
*     map = AstIntraMap *
*        The AST IntraMap object whose resources are to be reclaimed.
*-
*/
   IntraInfo *info;
   const char *ipc;
   THASTCALL( jniastList( 1, map ),
      ipc = astGetC( map, "IntraFlag" );
   )
   if ( ! (*env)->ExceptionCheck( env ) && 
        sscanf( ipc, "%p", &info ) == 1 ) {
      (*env)->DeleteGlobalRef( env, info->trans );
      free( info );
   }
}

/* $Id$ */
