/* jsubpar.c - a replacement for the SUBPAR library of the Starlink Software
 *            Environment (ADAM). This library is implemented to use the Java
 *            jpcs parameter system and thus permits existing Starlink ADAM 
 *            tasks written in Fortran or C to be run as 'native' methods of
 *            Java classes interfacing nicely with other, pure-Java methods.
 *
 * Authors:
 *    AJC: Alan Chipperfield (Starlink, RAL)
 *
 * History:
 *    1-JUN-2002 (AJC):
 *       Original Version.
*/

#include <jni.h>
#include <stdio.h>
#include <sae_par.h>
#include <f77.h>
#include <par_par.h>
#include <par_err.h>
#include "subpar.h"
#include "subpar_err.h"
#ifdef DEBUG
 #define DEB(a) printf(a);
 #define DEB1(a,b) printf(a,b);
 #define DEB2(a,b,c) printf(a,b,c);
#else
 #define DEB(a) ;
 #define DEB1(a,b) ;
 #define DEB2(a,b,c) ;
#endif

#ifdef DUMMY
 #define DEBDUM(a) printf(a);
#else
 #define DEBDUM(a) ;
#endif

/* Variables initialised by subpar_activ
*/
JNIEnv *parenv;
jclass ParameterList;
int plistSize;
jclass Msg;
jobject plist;
jobject msg;

/* Commonly required classes initialised to 0 by subpar_activ and subsequently
*  set as required by calling subpar1_getClass.
*/
jclass Parameter;
jclass ParameterNullException;
jclass ParameterAbortException;

/* Commonly required methods - zeroised in subpar_activ
*/ 
jmethodID get;          /* Set in subpar1_getParameter */
jmethodID out;          /* Set in subpar_write */


/* Check if there was a Java exception thrown.
*  If there was return a suitable status value
*  Arguments:
*  id = int *
*     Id of the relevant Parameter. If id == 0, there is no relevant parameter.
*  status
*     Global status value may be returned as:
*        PAR__NULL
*        PAR__ABORT
*        SUBPAR__ERROR
*/
void subpar1_checkexc( int *id, int *status ) {
int istat;
jthrowable exc;
char *name;
jmethodID getMessage;
jclass cls;
jstring jmess;
const char *ctmpstr;
char *cmess;

   istat = SAI__OK;
   
   exc = (*parenv)->ExceptionOccurred( parenv );
   if ( exc != 0 ) {
/* There was an exception */   
DEB("1checkexc: Exception detected\n");

/* Clear it and check for Null or Abort */
DEB("1checkexc: Clear it and check for Null and Abort\n");
#ifdef DEBUG
      (*parenv)->ExceptionDescribe(parenv);
#endif
      (*parenv)->ExceptionClear(parenv);

/* If the exception is associated with a parameter (*id != 0), check for
 * NullParameterException and AbortParameterException
 */
      if ( id ) {      
         name = subpar1_getkey( *id, status );
         if ( *status == SAI__OK ) {
            emsSetc( "NAME", name );
         }
         subpar1_getClass( "ParameterNullException", &ParameterNullException,
           status );
         if ( 
          (*parenv)->IsInstanceOf( parenv, (jobject)exc, ParameterNullException 
            ) ) {
DEB("1checkexc: is Null\n");
            *status = PAR__NULL;
            emsRep( "SUP1_CHECKEXC_1", "Null value (!) for parameter ^NAME",
              status );
         } else {
            subpar1_getClass(
             "ParameterAbortException", &ParameterAbortException, status );
            if (
              (*parenv)->IsInstanceOf( parenv, (jobject)exc,
               ParameterAbortException ) ) {
DEB("1checkexc: is abort\n");
               *status = PAR__ABORT;
               emsRep( "SUP1_CHECKEXC_2",
                "Abort value (!!) for parameter ^NAME", status );
            }
         }
      }

         if ( *status == SAI__OK ) {
DEB("1checkexc: is other error\n");
            *status = SUBPAR__ERROR;
            cls = (*parenv)->GetObjectClass(parenv, exc );
            if( cls != 0 ) {
            getMessage = (*parenv)->GetMethodID(
               parenv, cls, "getMessage", "()Ljava/lang/String;");
            if( getMessage !=0 ) {
DEB("1checkexc: Getting the exception message\n" );
            jmess = (*parenv)->CallObjectMethod(parenv, exc, getMessage );

/* Convert the returned Java String to UTF and then to Fortran */
DEB("1checkexc: Converting to C string\n" );
            if( ( ctmpstr = (*parenv)->GetStringUTFChars(parenv, jmess, 0) )
                != 0 ) {
               cmess = malloc( strlen(ctmpstr) + 1 );
               if( cmess ) {
                  strcpy( cmess, ctmpstr );
                  emsRep( "SUP1_CHECKEXC_3a", cmess, status );
                  free( cmess ); 
               }
      
/* Free the UTF string */    
DEB("1checkexc: Releasing UTF string\n" );
               (*parenv)->ReleaseStringUTFChars(parenv, jmess, ctmpstr);
            }

            if ( id ) {
               emsRep( "SUP1_CHECKEXC_3", "Error obtaining parameter ^NAME",
                   status );
            }
            
            } else {
DEB("Can't get getMessage() method of exception\n");
            }
            
            } else {
DEB("Can't get class of Exception\n");
            }
         }
      
   }
DEB1("1checkexc: Returns status %d\n",*status) 
}

void subpar1_getClass( char *name, jclass *class, int *status ) {
/* Check if the jclass variable is already set. If not, set it to the named
*  class
*/

   jclass cls;
   char namebuff[50];
DEB1("getClass: Ensure class %s is available\n",name );
   if ( *class == 0 ) {
DEB1("getClass: Get class %s\n",name );
      strcpy( namebuff, "uk/ac/starlink/jpcs/" );
      strcat( namebuff, name );
      cls = (*parenv)->FindClass(parenv, namebuff );
      if ( cls != 0) {
         *class = cls; 

      } else {
         subpar1_checkexc( 0, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsSetc( "NAME", name );
         emsRep( "SUBPAR1_GETCLASS",
          "SUBPAR1_GETCLASS: Can't find class ^NAME", status );
      }
   }
}

jobject subpar1_getObjectClass( jobject obj, int *status ) {
/* Return the class of the given object. Exit without action if status is set
*  and set status if the class cannot be found.
*/
   jclass cls;
   
   if( *status != SAI__OK ) return (jclass)0;
   
DEB("1getobjectclass: Get parameter class\n");
   cls = (*parenv)->GetObjectClass( parenv, obj );
   if( cls == 0 ) {
      cls = (jclass)0;
      subpar1_checkexc( 0, status );
      if( *status == SAI__OK ) *status = SUBPAR__ERROR;
      emsRep( "SUBPAR_GETOBJECTCLASS",
       "SUBPAR_GETOBJECTCLASS: Can't find Object class.",
       status );
   }
   
   return cls;
   
}

jobject subpar1_getParameter( int id, int *status ) {
/* Return the Parameter object with the given id
*/
   int istat;
   jobject param;
   
/*   subpar1_getClass( "Parameter", &Parameter, status );
*/
   if ( *status == SAI__OK ) {
      if ( get == 0 ) {
         get = (*parenv)->GetMethodID(parenv, ParameterList,
          "get", "(I)Ljava/lang/Object;");
         if (get == 0) {
            subpar1_checkexc( 0, status );
            if( *status == SAI__OK ) *status = SUBPAR__ERROR;
            emsRep( "SUBPAR1_GETPARAMETER1",
             "SUBPAR1_GETPARAMETER: Can't find ParameterList.get()", status );
         } 
      }
   }
   
   if ( *status == SAI__OK ) {
DEB1("getParameter: Getting param ID %d\n", (jint)id);
      param = (*parenv)->CallObjectMethod(parenv, plist, get, (jint)id );    
      if (param != 0) {
DEB1("getParameter: Got param ID %d\n", (jint)id);
         return param;

      } else {
         subpar1_checkexc( 0, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsSeti( "ID", id );
         emsRep( "SUBPAR1_GETPARAMETER1",
          "SUBPAR1_GETPARAMETER: Can't get parameter id ^ID", status );
      }
   }
}

jmethodID subpar1_getParameterIsMethod( char *name, int *status ) {

   jmethodID meth;
   
   subpar1_getClass( "Parameter", &Parameter, status );

DEB1("getParameterIsMethod: get %s method\n",name);
   meth = (*parenv)->GetMethodID(parenv, Parameter, name, "()Z");
   if ( meth != 0 ) {
      return meth;
            
   } else {
      subpar1_checkexc( 0, status );
      if( *status == SAI__OK ) *status = SUBPAR__ERROR;
      emsSetc( "NAME", name );
      emsRep( "SUBPAR1_GETPARAMETERISMETHOD1",
       "SUBPAR1_GETPARAMETERISMEYHOD: Can't find Parameter.^NAME()", status );
      return 0;
   }
}

char *subpar1_getkey( int id, int *status ) {
/* Return the keyword of the Parameter identified by id in the current 
 * ParameterList.
 */
   jclass cls;
   jmethodID getKeyword;
   jobject param;
   jobject jstr;
   const char *ctmpstr; 
   char *cstr=0;   
   
DEB("1getkey: Entered\n" );
        
/* Get the specified  parameter */
   param = subpar1_getParameter( id, status );
   
DEB1("1getKey: getParameter returns status %d\n",*status);
   if ( *status == SAI__OK ) {   

/* Get the class of this parameter */
DEB("1getKey: Get parameter class\n");
      cls = subpar1_getObjectClass( param, status );

      if( *status == SAI__OK ) {
DEB("1getkey: Getting getKeyword method\n" );
         getKeyword = (*parenv)->GetMethodID(parenv, cls, "getKeyword",
          "()Ljava/lang/String;");
         if (getKeyword != 0) {
DEB("1getkey: Getting param keyword\n" );
            jstr = (*parenv)->CallObjectMethod(parenv, param, getKeyword );

/* Convert the returned Java String to UTF and then to Fortran */
DEB("1getkey: Converting to C string\n" );
            if( 
            ( ctmpstr = (*parenv)->GetStringUTFChars(parenv, jstr, 0) ) == 0 ) {
               *status = SUBPAR__ERROR;
            } else {
               cstr = malloc( strlen(ctmpstr) + 1 );
               if( cstr ) {
                  strcpy( cstr, ctmpstr );
               } else {
                  *status = SUBPAR__ERROR;
               }
      
/* Free the UTF string */    
DEB("1getkey: Releasing UTF string\n" );
               (*parenv)->ReleaseStringUTFChars(parenv, jstr, ctmpstr);
            }
         
         } else {
            subpar1_checkexc( 0, status );
            if( *status == SAI__OK ) *status = SUBPAR__ERROR;
            emsRep( "SUBPAR1_GETKEY1",
             "SUBPAR1_GETKEY: Can't find parameter getKeyword() method.",
             status );
         }
      }      
   }
      
   return cstr;
}

int subpar1_arrsize( int ndims, int dims[] ) {
/* Return the size of the ndims dimensional array whose dimensions are in dims[]
*/
int i;
int size = 0;

   if( ndims > 0 ) {
      size = dims[0];
      if( ndims > 1 ) {
         for (i=1;i<ndims;i++) {
            size = size * dims[i];
         }
      }
   }
   
   return (jsize)size;
}

jobjectArray subpar1_creStringArray( int size, char *arr, int elsize,
                                     int *status ) {
/* Returns a Java int array of size size, containing the elements of
 * the C array arr. elsize is the size of each element of the array.
 * No action occurrs if *status is not SAI__OK on entry.
 * *status will be returned as SUBPAR__ERR if the process fails
 */
jclass String;
jobjectArray jarr = 0;
jstring jstr;
jobject jel;
int i;

   if( *status != SAI__OK ) return 0;

/* Create the Java array */
   String = (*parenv)->FindClass( parenv, "java/lang/String" );
   if ( String ) {      

      jarr = (*parenv)->NewObjectArray( parenv, size, String , 
       (jobject)((*parenv)->NewStringUTF(parenv, " ")) );
      if( jarr ) {
         for ( i=0; i<size; i++ ) {
DEB1("creStringArray: Make Java String: %s\n",arr);
DEB1("creStringArray: size: %d\n",strlen(arr));
            jstr = (*parenv)->NewStringUTF(parenv, arr);
            arr += elsize;
            if( jstr ) {
DEB("creStringArray: Set object array element\n");
               (*parenv)->SetObjectArrayElement( parenv, jarr, (jsize)i, jstr );
               
            } else {
DEB("creStringArray: Failed to create String\n");
               subpar1_checkexc( 0, status );
               if( *status == SAI__OK ) *status = SUBPAR__ERROR;
               emsRep( "SUP1_CRESTRINGARRAY_1",
                 "subpar1_creStringArray: Failed to create String", *status );
               i = size;
            }
         }

         subpar1_checkexc( 0, status );

      } else {
DEB("creStringArray: Failed to create array\n");
         subpar1_checkexc( 0, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP1_CRESTRINARRAY_2",
           "subpar1_creStringArray: Failed create array", *status );
      }
   
   } else {
      subpar1_checkexc( 0, status );
      if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP1_CRESTRINGARRAY_3",
           "subpar1_creStringArray: Failed to get String class", *status );
   }
   
   
   return jarr;
}

jintArray subpar1_creDoubleArray( int size, double arr[], int *status ) {
/* Returns a Java double array of size size, containing the elements of
 * the C array arr.
 * No action occurrs if *status is not SAI__OK on entry.
 * *status will be returned as SUBPAR__ERR if the process fails
 */
jdoubleArray jarr;
jdouble *tbody;
jdouble *body;
double *tarr;
int i;

   if( *status != SAI__OK ) return 0;

/* Create the Java array */
   jarr = (*parenv)->NewDoubleArray( parenv, size );
   if( jarr ) {

/* Copy the C array into the Java Array */
      body = (*parenv)->GetDoubleArrayElements( parenv, jarr, 0 );
      if( body ) {
DEB("creDoubleArray: got Java array elements\n");
         tarr = arr;
         tbody = body;
         for ( i=0; i<size; i++ ) {
DEB1("creDoubleArray: copy %d\n",*tarr);
            *tbody++ = *tarr++;
         }
 
         (*parenv)->ReleaseDoubleArrayElements( parenv, jarr, body, 0 );
         subpar1_checkexc( 0, status );
         
      } else {
DEB("creDoubleArray: get Java array elements failed\n");
         subpar1_checkexc( 0, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP1_CREDOUBLEARRAY_1",
              "subpar1_creDoubleArray: Failed to get array elements", *status );
      }

   } else {
DEB("creIntArray: Failed to create array\n");
      subpar1_checkexc( 0, status );
      if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP1_CREDOUBLEARRAY_2",
           "subpar1_creDoubleArray: Failed create array", *status );
   }
   
   return jarr;
}

jintArray subpar1_creIntArray( int size, int arr[], int *status ) {
/* Returns a Java int array of size size, containing the elements of
 * the C array arr.
 * No action occurrs if *status is not SAI__OK on entry.
 * *status will be returned as SUBPAR__ERR if the process fails
 */
jintArray jarr;
jint *tbody;
jint *body;
int *tarr;
int i;

   if( *status != SAI__OK ) return 0;

/* Create the Java array */
   jarr = (*parenv)->NewIntArray( parenv, size );
   if( jarr ) {

/* Copy the C array into the Java Array */
      body = (*parenv)->GetIntArrayElements( parenv, jarr, 0 );
      if( body ) {
DEB("creIntArray: got Java array elements\n");
         tarr = arr;
         tbody = body;
         for ( i=0; i<size; i++ ) {
DEB1("creIntArray: copy %d\n",*tarr);
            *tbody++ = *tarr++;
         }
 
         (*parenv)->ReleaseIntArrayElements( parenv, jarr, body, 0 );
         subpar1_checkexc( 0, status );
         
      } else {
DEB("creIntArray: get Java array elements failed\n");
         subpar1_checkexc( 0, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP1_CREINTARRAY_1",
              "subpar1_creIntArray: Failed to get array elements", *status );
      }

   } else {
DEB("creIntArray: Failed to create array\n");
      subpar1_checkexc( 0, status );
      if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP1_CREINTARRAY_2",
           "subpar1_creIntArray: Failed create array", *status );
   }
   
   return jarr;
}

jbooleanArray subpar1_creBooleanArray(
     int size, LOGICAL_ARRAY(arr), int *status ) {
/* Returns a Java int array of size size, containing the elements of
 * the C array arr.
 * No action occurrs if *status is not SAI__OK on entry.
 * *status will be returned as SUBPAR__ERR if the process fails
 */
jbooleanArray jarr;
jboolean *tbody;
jboolean *body;
F77_LOGICAL_TYPE *tarr;
int i;

GENPTR_LOGICAL(arr)

   if( *status != SAI__OK ) return 0;

/* Create the Java array */
   jarr = (*parenv)->NewBooleanArray( parenv, size );
   if( jarr ) {

/* Copy the C array into the Java Array */
      body = (*parenv)->GetBooleanArrayElements( parenv, jarr, 0 );
      if( body ) {
DEB("creBooleanArray: got Java array elements\n");
         tarr = arr;
         tbody = body;
         for ( i=0; i<size; i++ ) {
DEB1("creBooleanArray: copy %d\n",*tarr);
            *tbody++ = F77_ISTRUE(*tarr++)?JNI_TRUE:JNI_FALSE;
         }
 
         (*parenv)->ReleaseBooleanArrayElements( parenv, jarr, body, 0 );
         subpar1_checkexc( 0, status );
         
      } else {
DEB("creBooleanArray: get Java array elements failed\n");
         subpar1_checkexc( 0, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP1_CREBOOLEANARRAY_1",
              "subpar1_creBooleanArray: Failed to get array elements", *status );
      }

   } else {
DEB("creBooleanArray: Failed to create array\n");
      subpar1_checkexc( 0, status );
      if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP1_CREINTARRAY_2",
           "subpar1_creBooleanArray: Failed create array", *status );
   }
   
   return jarr;
}

jintArray subpar1_creFloatArray( int size, float arr[], int *status ) {
/* Returns a Java float array of size size, containing the elements of
 * the C array arr.
 * No action occurrs if *status is not SAI__OK on entry.
 * *status will be returned as SUBPAR__ERR if the process fails
 */
jfloatArray jarr;
jfloat *tbody;
jfloat *body;
float *tarr;
int i;

   if( *status != SAI__OK ) return 0;

/* Create the Java array */
   jarr = (*parenv)->NewFloatArray( parenv, size );
   if( jarr ) {

/* Copy the C array into the Java Array */
      body = (*parenv)->GetFloatArrayElements( parenv, jarr, 0 );
      if( body ) {
DEB("creFloatArray: got Java array elements\n");
         tarr = arr;
         tbody = body;
         for ( i=0; i<size; i++ ) {
DEB1("creFloatArray: copy %d\n",*tarr);
            *tbody++ = *tarr++;
         }
 
         (*parenv)->ReleaseFloatArrayElements( parenv, jarr, body, 0 );
         subpar1_checkexc( 0, status );
         
      } else {
DEB("creFloatArray: get Java array elements failed\n");
         subpar1_checkexc( 0, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP1_CREFLOATARRAY_1",
              "subpar1_creFloatArray: Failed to get array elements", *status );
      }

   } else {
DEB("creFloatArray: Failed to create array\n");
      subpar1_checkexc( 0, status );
      if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP1_CREFLOATARRAY_2",
           "subpar1_creFloatArray: Failed create array", *status );
   }
   
   return jarr;
}

jobject subpar1_creArrPValC( int ndims, int dims[], char arr[], int elsize,
                             int *status ) {
/* Construct and return an ArrayParameterValue object from the given arguments 
 * ndims - the number of dimensions
 * dims  - an array of ndims dimensions
 * arr   - an array of int
 */
   jsize size;
   jobjectArray jarr;
   jintArray jdims;
   jmethodID constr;
   jclass ArrayPVal;
   jobject apval = 0;
   
/* First create the Java data array */
DEB("creArrPValC: find array size\n");
   size = subpar1_arrsize( ndims, dims );
DEB("creArrPValC: create String array\n");
   jarr = subpar1_creStringArray( size, arr, elsize, status );
   if( *status == SAI__OK ) {

/* The Java data array is created */
DEB("creArrPValC: Java array created\n");

/* Now create the Java dimesions array */
      jdims = subpar1_creIntArray( ndims, dims, status );

      if( *status == SAI__OK ) {
/* Now create the ArrayParameterValue */
         ArrayPVal = (*parenv)->FindClass( parenv,
                  "uk/ac/starlink/jpcs/ArrayParameterValue" );
         if( ArrayPVal ) {
DEB("creArrPValC: got ArrayParameterValue class\n");
            constr =
              (*parenv)->GetMethodID( parenv, ArrayPVal, "<init>",
              "([Ljava/lang/String;I[I)V" );                 
            if( constr ) {
DEB("creArrPValC: got ArrayParameterValue constructor\n");
               apval =
                 (*parenv)->NewObject( parenv, ArrayPVal, constr, jarr,
                 (jint)ndims, jdims );
               if( apval == 0 ) {
DEB("creArrPValC: failed to construct ArrayParameterValue\n");
                  subpar1_checkexc( 0, status );
                  if( *status == SAI__OK ) *status = SUBPAR__ERROR;
                  emsRep( "SUP1_CREARRPValC_1",
                    "subpar1_creArrPValC: Failed to construct "
                    "ArrayParameterValue", *status );
               }

            } else {
               subpar1_checkexc( 0, status );
               if( *status == SAI__OK ) *status = SUBPAR__ERROR;
               emsRep( "SUP1_CREARRPVALC_2",
                "subpar1_creArrPValC: Failed to get ArrayParameterValue "
                "constructor", *status );
            }

         } else {
            subpar1_checkexc( 0, status );
            if( *status == SAI__OK ) *status = SUBPAR__ERROR;
            emsRep( "SUP1_CREARRPVALC_3",
              "subpar1_creArrPValC: Failed to get ArrayParameterValue class",
              *status );
         }
      }
   }
DEB1("creArrPValC: return status %d\n",*status);
   return apval;
}

jobject subpar1_creArrPValD( int ndims, int dims[], double arr[], int *status ) {
/* Construct and return an ArrayParameterValue object from the given arguments 
 * ndims - the number of dimensions
 * dims  - an array of ndims dimensions
 * arr   - an array of int
 */
   jdoubleArray jarr;
   jintArray jdims;
   jmethodID constr;
   jclass ArrayPVal;
   jobject apval = 0;
   
/* First create the Java data array */
   jsize size = subpar1_arrsize( ndims, dims );
   jarr = subpar1_creDoubleArray( size, arr, status );
   if( *status == SAI__OK ) {

/* The Java data array is created
DEB("creArrPValD: Java array created\n");

/* Now create the Java dimesions array */
      jdims = subpar1_creIntArray( ndims, dims, status );

      if( *status == SAI__OK ) {
/* Now create the ArrayParameterValue */
         ArrayPVal = (*parenv)->FindClass( parenv,
                  "uk/ac/starlink/jpcs/ArrayParameterValue" );
         if( ArrayPVal ) {
DEB("creArrPValD: got ArrayParameterValue class\n");
            constr =
              (*parenv)->GetMethodID( parenv, ArrayPVal, "<init>", "([DI[I)V" );                 
            if( constr ) {
DEB("creArrPValD: got ArrayParameterValue constructor\n");
               apval =
                 (*parenv)->NewObject( parenv, ArrayPVal, constr, jarr,
                 (jint)ndims, jdims );
               if( apval == 0 ) {
DEB("creArrPValD: failed to construct ArrayParameterValue\n");
                  subpar1_checkexc( 0, status );
                  if( *status == SAI__OK ) *status = SUBPAR__ERROR;
                  emsRep( "SUP1_CREARRPVALD_1",
                    "subpar1_creArrPValD: Failed to construct "
                    "ArrayParameterValue", *status );
               }

            } else {
               subpar1_checkexc( 0, status );
               if( *status == SAI__OK ) *status = SUBPAR__ERROR;
               emsRep( "SUP1_CREARRPVALD_2",
                "subpar1_creArrPValD: Failed to get ArrayParameterValue "
                "constructor", *status );
            }

         } else {
            subpar1_checkexc( 0, status );
            if( *status == SAI__OK ) *status = SUBPAR__ERROR;
            emsRep( "SUP1_CREARRPVALS_3",
              "subpar1_creArrPValD: Failed to get ArrayParameterValue class",
              *status );
         }
      }
   }
DEB1("creArrPValD: return status %d\n",*status);
   return apval;
}

jobject subpar1_creArrPValI( int ndims, int dims[], int arr[], int *status ) {
/* Construct and return an ArrayParameterValue object from the given arguments 
 * ndims - the number of dimensions
 * dims  - an array of ndims dimensions
 * arr   - an array of int
 */
   jintArray jarr;
   jintArray jdims;
   jmethodID constr;
   jclass ArrayPVal;
   jobject apval = 0;
   
/* First create the Java data array */
   jsize size = subpar1_arrsize( ndims, dims );
   jarr = subpar1_creIntArray( size, arr, status );
   if( *status == SAI__OK ) {

/* The Java data array is created */
DEB("creArrPValI: Java array created\n");

/* Now create the Java dimesions array */
      jdims = subpar1_creIntArray( ndims, dims, status );

      if( *status == SAI__OK ) {
/* Now create the ArrayParameterValue */
         ArrayPVal = (*parenv)->FindClass( parenv,
                  "uk/ac/starlink/jpcs/ArrayParameterValue" );
         if( ArrayPVal ) {
DEB("creArrPValI: got ArrayParameterValue class\n");
            constr =
              (*parenv)->GetMethodID( parenv, ArrayPVal, "<init>", "([II[I)V" );                 
            if( constr ) {
DEB("creArrPValI: got ArrayParameterValue constructor\n");
               apval =
                 (*parenv)->NewObject( parenv, ArrayPVal, constr, jarr,
                 (jint)ndims, jdims );
               if( apval == 0 ) {
DEB("creArrPValI: failed to construct ArrayParameterValue\n");
                  subpar1_checkexc( 0, status );
                  if( *status == SAI__OK ) *status = SUBPAR__ERROR;
                  emsRep( "SUP1_CREARRPVALI_1",
                    "subpar1_creArrPValI: Failed to construct "
                    "ArrayParameterValue", *status );
               }

            } else {
               subpar1_checkexc( 0, status );
               if( *status == SAI__OK ) *status = SUBPAR__ERROR;
               emsRep( "SUP1_CREARRPVALI_2",
                "subpar1_creArrPValI: Failed to get ArrayParameterValue "
                "constructor", *status );
            }

         } else {
            subpar1_checkexc( 0, status );
            if( *status == SAI__OK ) *status = SUBPAR__ERROR;
            emsRep( "SUP1_CREARRPVALI_3",
              "subpar1_creArrPValI: Failed to get ArrayParameterValue class",
              *status );
         }
      }
   }
DEB1("creArrPValI: return status %d\n",*status);
   return apval;
}

jobject subpar1_creArrPValB(
          int ndims, int dims[], LOGICAL_ARRAY(arr), int *status ) {
/* Construct and return an ArrayParameterValue object from the given arguments 
 * ndims - the number of dimensions
 * dims  - an array of ndims dimensions
 * arr   - an array of int
 */
   jbooleanArray jarr;
   jintArray jdims;
   jmethodID constr;
   jclass ArrayPVal;
   jobject apval = 0;
   
/* First create the Java data array */
   jsize size = subpar1_arrsize( ndims, dims );
   jarr = subpar1_creBooleanArray( size, arr, status );
   if( *status == SAI__OK ) {

/* The Java data array is created */
DEB("creArrPValB: Java array created\n");

/* Now create the Java dimesions array */
      jdims = subpar1_creIntArray( ndims, dims, status );

      if( *status == SAI__OK ) {
/* Now create the ArrayParameterValue */
         ArrayPVal = (*parenv)->FindClass( parenv,
                  "uk/ac/starlink/jpcs/ArrayParameterValue" );
         if( ArrayPVal ) {
DEB("creArrPValB: got ArrayParameterValue class\n");
            constr =
              (*parenv)->GetMethodID( parenv, ArrayPVal, "<init>", "([ZI[I)V" );                 
            if( constr ) {
DEB("creArrPValB: got ArrayParameterValue constructor\n");
               apval =
                 (*parenv)->NewObject( parenv, ArrayPVal, constr, jarr,
                 (jint)ndims, jdims );
               if( apval == 0 ) {
DEB("creArrPValB: failed to construct ArrayParameterValue\n");
                  subpar1_checkexc( 0, status );
                  if( *status == SAI__OK ) *status = SUBPAR__ERROR;
                  emsRep( "SUP1_CREARRPVALB_1",
                    "subpar1_creArrPValB: Failed to construct "
                    "ArrayParameterValue", *status );
               }

            } else {
               subpar1_checkexc( 0, status );
               if( *status == SAI__OK ) *status = SUBPAR__ERROR;
               emsRep( "SUP1_CREARRPVALB_2",
                "subpar1_creArrPValB: Failed to get ArrayParameterValue "
                "constructor", *status );
            }

         } else {
            subpar1_checkexc( 0, status );
            if( *status == SAI__OK ) *status = SUBPAR__ERROR;
            emsRep( "SUP1_CREARRPVALB_3",
              "subpar1_creArrPValB: Failed to get ArrayParameterValue class",
              *status );
         }
      }
   }
DEB1("creArrPValB: return status %d\n",*status);
   return apval;
}

jobject subpar1_creArrPValF( int ndims, int dims[], float arr[], int *status ) {
/* Construct and return an ArrayParameterValue object from the given arguments 
 * ndims - the number of dimensions
 * dims  - an array of ndims dimensions
 * arr   - an array of int
 */
   jfloatArray jarr;
   jintArray jdims;
   jmethodID constr;
   jclass ArrayPVal;
   jobject apval = 0;
   
/* First create the Java data array */
   jsize size = subpar1_arrsize( ndims, dims );
   jarr = subpar1_creFloatArray( size, arr, status );
   if( *status == SAI__OK ) {

/* The Java data array is created
DEB("creArrPValF: Java array created\n");

/* Now create the Java dimesions array */
      jdims = subpar1_creIntArray( ndims, dims, status );

      if( *status == SAI__OK ) {
/* Now create the ArrayParameterValue */
         ArrayPVal = (*parenv)->FindClass( parenv,
                  "uk/ac/starlink/jpcs/ArrayParameterValue" );
         if( ArrayPVal ) {
DEB("creArrPValF: got ArrayParameterValue class\n");
            constr =
              (*parenv)->GetMethodID( parenv, ArrayPVal, "<init>", "([FI[I)V" );                 
            if( constr ) {
DEB("creArrPValF: got ArrayParameterValue constructor\n");
               apval =
                 (*parenv)->NewObject( parenv, ArrayPVal, constr, jarr,
                 (jint)ndims, jdims );
               if( apval == 0 ) {
DEB("creArrPValF: failed to construct ArrayParameterValue\n");
                  subpar1_checkexc( 0, status );
                  if( *status == SAI__OK ) *status = SUBPAR__ERROR;
                  emsRep( "SUP1_CREARRPVALF_1",
                    "subpar1_creArrPValF: Failed to construct "
                    "ArrayParameterValue", *status );
               }

            } else {
               subpar1_checkexc( 0, status );
               if( *status == SAI__OK ) *status = SUBPAR__ERROR;
               emsRep( "SUP1_CREARRPVALF_2",
                "subpar1_creArrPValF: Failed to get ArrayParameterValue "
                "constructor", *status );
            }

         } else {
            subpar1_checkexc( 0, status );
            if( *status == SAI__OK ) *status = SUBPAR__ERROR;
            emsRep( "SUP1_CREARRPVALF_3",
              "subpar1_creArrPValF: Failed to get ArrayParameterValue class",
              *status );
         }
      }
   }
DEB1("creArrPValF: return status %d\n",*status);
   return apval;
}


void subpar_activ( JNIEnv *env, jobject obj, jobject parList, jobject tmsg,
                   int *status ) {
/* Set up the JNI environment for SUBPAR and initialise the ERR system
*/
/*   jclass cmsg;*/

/* Local Variables:*/
   jmethodID size;
   
   ParameterList=0;
   Msg=0;
   Parameter=0;
   ParameterNullException=0;
   ParameterAbortException=0;
   get=0;
   out=0;

   parenv = env;
   plist = parList;
   msg = tmsg;

/* Start the error system */
   errStart();
       
/* Set up some well-used classes. */
   subpar1_getClass( "ParameterList", &ParameterList, status );
DEB1("activ: getClass ParameterList - status %d\n",*status );
   subpar1_getClass( "Msg", &Msg, status );
DEB1("activ: getClass Msg - status %d\n",*status );

/* Set the ParameterList size */
   size = (*parenv)->GetMethodID(parenv, ParameterList, "size", "()I");
DEB("activ: getMethidID for ArrayList size() returned.\n");
   if (size == 0) {
      subpar1_checkexc( 0, status );
      if( *status == SAI__OK ) *status = SUBPAR__ERROR;
      emsRep( "SUBPAR1_ACTIV1",
       "SUBPAR1_ACTIV: Can't find ParameterList.size() method.",
       status );

   } else {
      plistSize = (*parenv)->CallIntMethod( parenv, plist, size );    
/* Check for Exception */
      if( plistSize == 0 ) {
         subpar1_checkexc( 0, status );
         if ( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUBPAR1_ACTIV2",
          "SUBPAR1_ACTIV: Can't find ParameterList size method.", status );
      }
   }
}

void subpar_deact( int *status ) { 
   int istat;
   int level;
   
/* Flush any pending error messages and Deactivate the parameter system
*/
   if ( *status != SAI__OK ) {
      emsFacer( "STAT", *status );
      emsRep( "SUP_DEACT_1", "Application exit status ^STAT", status );
   } 
   errStop( status );

/* Return to level 1 so subpar_activ will start again
*/
   istat = SAI__OK;
   emsTune( "MSGDEF", 1, &istat );
   emsRlse( );
   emsLevel( &level );
DEB1( "deact: Final level is %d\n", level );  

}

F77_SUBROUTINE(subpar_findpar)(CHARACTER(name), INTEGER(id), INTEGER(status)
   TRAIL(name) ) {
   GENPTR_CHARACTER(name)
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(status)
   char *cnam;
   jstring jstr;
   int namecode;

   jmethodID findID;
   jthrowable exc;

   if ( *status != SAI__OK ) return;
      
   cnam = cnfCreim( name, name_length );
   if ( cnam ) {
DEB1("findpar: %s\n", cnam);
      findID = (*parenv)->GetMethodID(parenv, ParameterList,
        "findID", "(Ljava/lang/String;)I");

/* Check for exception */
      subpar1_checkexc( 0, status );
      if ( *status != SAI__OK ) {
         emsSetc( "NAME", cnam );
         emsRep( "SUP_FINDPAR1",
          "SUBPAR_FINDPAR: Can't find ParameterList.findID()\n", status );

      } else {
DEB("findpar: Converting name to jstr\n");
         jstr = (*parenv)->NewStringUTF(parenv, cnam);
         if (jstr == 0) {
            *status = SUBPAR__ERROR;
            emsRep( "SUP_FINDPAR2",
              "SUBPAR_FINDPAR: Out of memory\n", status );
         }

DEB("Invoke the ParameterList.findID() method\n");
         namecode = (int)(*parenv)->CallIntMethod(parenv, plist, findID, jstr);    

/* Check for exception */
         subpar1_checkexc( NULL, status );
         if( *status == SAI__OK ) {
DEB2("findpar: Param %s is ID %d\n",cnam,namecode);
            *id = namecode;
         }
      }

      cnfFree( cnam );
   }

}

/* The subpar_get0x routines get a scalar value for the identified Parameter.
*  The value obtained will be of the type specified by x (c,d,i,l,r) to which
*  the Parameter value must be convertible.
*  Arguments:
*     id (Given)     the Parameter id
*     x  (Returned)  the Parameter value
*     status (Given and Returned)  the global status
*/
F77_SUBROUTINE(subpar_get0c)( INTEGER(id), CHARACTER(str), INTEGER(status)
 TRAIL(str) ) {
   GENPTR_INTEGER(id)
   GENPTR_CHARACTER(str)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID getString;
   jobject param;
   jobject jstr;
   const char *cstr; 

   if ( *status != SAI__OK ) return;
        
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );

/* Get the class of this parameter */
DEB("get0c: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );

   if( *status == SAI__OK ) {

/* See if this parameter class has an appropriate method */
DEB("get0c: Getting param as String\n");
      getString = (*parenv)->GetMethodID(parenv, cls, "getString",
        "()Ljava/lang/String;");
      if (getString != 0) {
DEB("get0c: Getting param value\n" );
         jstr = (*parenv)->CallObjectMethod(parenv, param, getString );
   
/* Check for Exception */
         subpar1_checkexc( id, status );
DEB1("get0c: status is %d\n", *status);

         if ( *status == SAI__OK ) {
/* Convert the returned Java String to UTF and then to Fortran */
DEB("get0c: Converting to C string\n" );
            cstr = (*parenv)->GetStringUTFChars(parenv, jstr, 0);
DEB1("  %s\n",cstr);
            cnfExprt( cstr, str, str_length );
   
/* Free the UTF string */    
DEB("get0c: Releasing UTF string\n" );
            (*parenv)->ReleaseStringUTFChars(parenv, jstr, cstr);
         }
      } else {
         subpar1_checkexc( 0, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUBPAR_GET0C1",
          "SUBPAR_GET0C: Can't find parameter getString() method.",
          status );
      }

   }
}


F77_SUBROUTINE(subpar_get0d)( INTEGER(id), DOUBLE(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_DOUBLE(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID getDouble;
   jobject param;

   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );

/* Get the class of this parameter */
DEB("get0d: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );

   if( *status == SAI__OK ) {
/* See if this parameter class has an appropriate method */
DEB1("get0d: get getDouble method\n", *id);
      getDouble = (*parenv)->GetMethodID(parenv, cls, "getDouble", "()D");
      if (getDouble != 0) {
DEB1("get0d: Getting param ID %d as a double\n", *id);
         *x = (double)(*parenv)->CallDoubleMethod(parenv, param, getDouble );    
DEB1("get0d: Param is: %f\n", *x);
   
/* Check for Exception */
         subpar1_checkexc( id, status );

      } else {
         subpar1_checkexc( 0, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUBPAR_GET0D2",
          "SUBPAR_GET0D: Can't find parameter getDouble() method.",
          status );
      }
   }
}

F77_SUBROUTINE(subpar_get0i)( INTEGER(id), INTEGER(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID getInt;
   jobject param;
   
   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );

/* Get the class of this parameter */
DEB("get0i: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );

   if( *status == SAI__OK ) {       
/* See if this parameter class has an appropriate method */
DEB1("get0i: get getInt method\n", *id);
      getInt = (*parenv)->GetMethodID(parenv, cls, "getInt", "()I");

      if (getInt != 0) {
DEB1("get0i: Getting param ID %d as an int\n", *id);
         *x = (int)(*parenv)->CallIntMethod(parenv, param, getInt );    
DEB1("get0i: Param is: %d\n", *x);
   
/* Check for Exception */
         subpar1_checkexc( id, status );

      } else {
         subpar1_checkexc( 0, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUBPAR_GET0I1",
          "SUBPAR_GET0I: Can't find parameter getInt() method.",
          status );
      }
   }
}

F77_SUBROUTINE(subpar_get0l)( INTEGER(id), LOGICAL(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_LOGICAL(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID getBoolean;
   jobject param;

   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );

/* Get the class of this parameter */
DEB("get0i: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );
   
   if( *status == SAI__OK ) {       
/* See if this parameter class has an appropriate method */
      getBoolean = (*parenv)->GetMethodID(parenv, cls, "getBoolean", "()Z");

      if (getBoolean != 0) {
DEB("get0l: Invoke getBoolean\n");
         *x = (*parenv)->CallBooleanMethod(parenv, param, getBoolean );
   
/* Check for Exception */
         subpar1_checkexc( id, status );

      } else {
         subpar1_checkexc( 0, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUBPAR_GET0L1",
          "SUBPAR_GET0L: Can't find parameter getBoolean() method.",
          status );
      }   
   }
}

F77_SUBROUTINE(subpar_get0r)( INTEGER(id), REAL(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_REAL(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID getFloat;
   jobject param;

   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );

/* Get the class of this parameter */
DEB("get0: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );
   if( *status == SAI__OK ) {       
   
/* See if this parameter class has an appropriate method */
DEB1("get0r: get getFloat method\n", *id);
      getFloat = (*parenv)->GetMethodID(parenv, cls, "getFloat", "()F");

      if (getFloat != 0) {
DEB1("get0r: Getting param ID %d as a float\n", *id);
         *x = (float)(*parenv)->CallFloatMethod(parenv, param, getFloat );    
DEB1("get0r: Param is: %f\n", *x);
   
/* Check for Exception */
         subpar1_checkexc( id, status );

      } else {
         subpar1_checkexc( 0, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUBPAR_GET0R1",
          "SUBPAR_GET0R: Can't find parameter getFloat() method.",
          status );
      }
   }
}

F77_SUBROUTINE(subpar_getname)( INTEGER(id), CHARACTER(str), INTEGER(status)
 TRAIL(str) ) {
   GENPTR_INTEGER(id)
   GENPTR_CHARACTER(str)
   GENPTR_INTEGER(status)

   if ( *status != SAI__OK ) return;

DEB("getname: entered\n");
   F77_CALL(subpar_get0c)( INTEGER_ARG(id), CHARACTER_ARG(str),
     INTEGER_ARG(status) TRAIL_ARG(str) );
DEB("getname: exit\n");
   
}

F77_SUBROUTINE(subpar_getkey)( INTEGER(id), CHARACTER(str), INTEGER(status)
 TRAIL(str) ) {
   GENPTR_INTEGER(id)
   GENPTR_CHARACTER(str)
   GENPTR_INTEGER(status)
   
   char *cstr;
   
   if ( *status != SAI__OK ) return;
DEB("getkey: Status OK\n" );

DEB1("getkey: Getting keyword for parameter %d\n",*id);
   cstr = subpar1_getkey( *id, status );
DEB1("getkey: Got %s\n",cstr);
   if( *status == SAI__OK ) {
      cnfExprt( cstr, str, str_length );
   }
   
   if( cstr != 0 ) {
      free( cstr );
   } 
} 

F77_SUBROUTINE(subpar_getloc)( INTEGER(id), LOGICAL(valid), CHARACTER(str),
 INTEGER(status) TRAIL(str) ) {
/* This routine has no meaning in the new scheme of things. However it is called
 * by NDF_ASSOC to determine whether NDF should store the locators for the 
 * object. To ensure the the locators are not created, we tell NDF that they are
 * already stored.
 *
 * It is also used by MSG1_GREF to obtain the associated name associated with
 * the parameter
*/
   GENPTR_INTEGER(id)
   GENPTR_LOGICAL(valid)
   GENPTR_CHARACTER(str)
   GENPTR_INTEGER(status)

   if ( *status != SAI__OK ) return;

   *valid = F77_TRUE;
}

F77_SUBROUTINE(subpar_state)( INTEGER(id), INTEGER(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID getState;
   jobject param;
   int state;

   if ( *status != SAI__OK ) return;
   
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );    

   if ( *status == SAI__OK ) {
/* Get the class of this parameter */
DEB("get0: Get parameter class\n");
      cls = (*parenv)->GetObjectClass( parenv, param );
      if( cls != 0 ) {
         getState = (*parenv)->GetMethodID(parenv, cls, "getState", "()I");
         if (getState != 0) {
            *x = (*parenv)->CallIntMethod(parenv, param, getState );
            subpar1_checkexc( id, status );

         } else {
         subpar1_checkexc( 0, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
            emsRep( "SUBPAR_STATE",
             "SUBPAR_STATE: Can't find parameter.getState() method.",
             status );
         }

      } else {
         subpar1_checkexc( 0, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUBPAR_STATE",
          "SUBPAR_STATE: Can't find parameter class.",
          status );
      }
   }
}

F77_SUBROUTINE(subpar_partype)( INTEGER(id), INTEGER(x), INTEGER(status) ) {
/* This is called by MSG1_GREF_ADAM to determine if the object has a name
*  stored - for Java version the object is the name so we return a number
*  >20 to indicate that.
*/
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(x)
   GENPTR_INTEGER(status)
DEB1("partype: entry status %d\n",*status);
   if ( *status != SAI__OK ) return;

   *x = 21;    
}

F77_SUBROUTINE(subpar_fetchc)( INTEGER(id), CHARACTER(str), INTEGER(status)
 TRAIL(str) ) {
/* This is the same as subpar_get0c in the Java version
*/
DEB1("fetchc: entry status %d\n",*status);
   if ( *status != SAI__OK ) return;
   
   F77_CALL(subpar_get0c)( INTEGER_ARG(id), CHARACTER_ARG(str),
     INTEGER_ARG(status) TRAIL_ARG(str) );
DEB1("fetchc: exit string - %s\n",*str);
   
}

F77_SUBROUTINE(subpar_write)(CHARACTER(mess), INTEGER(status) TRAIL(mess) ) {
/* This is the routine through which all output from ADAM programs should be
*  routed (the ultimate destination of all MSG and ERR messages.
*  The Java version writes the message to a Java Msg object, created for this
*  invocation of this application.
*  If the System property MsgBuffer was "true" when the Msg object was created,
*  messages will be buffered until the Msg is flushed; otherwise they will be
*  output immediately.
*/
   GENPTR_CHARACTER(mess)
   GENPTR_INTEGER(status)
   char *cmess;

   jstring jstr;
   
   cmess = cnfCreim( mess, mess_length );
DEB1( "write: %s\n", cmess );

/* Get the out method */
   if( out == 0 ) {
DEB("write: Get the out method\n");
      out = (*parenv)->GetMethodID(parenv, Msg, "out", "(Ljava/lang/String;)V");
      if (out == 0) {
         *status = SUBPAR__ERROR;
         fprintf(stderr, "Can't find Msg.out()\n");
      }
   }
   
   if ( *status == SAI__OK ) {
/* method out found */

/* Convert msg to a Java String */ 
      jstr = (*parenv)->NewStringUTF(parenv, cmess);
      if (jstr == 0) {
         fprintf(stderr, "Out of memory\n");
         *status = SUBPAR__ERROR;
      } else {
/* Output the message */ 
DEB("write: Call the out method\n");
         (*parenv)->CallVoidMethod(parenv, msg, out, jstr );
      }
   }
       
DEB("write: Free cmess\n");
   cnfFree( cmess );
}

F77_SUBROUTINE(subpar_wrerr)(CHARACTER(mess), INTEGER(status) TRAIL(mess) ) {
   GENPTR_CHARACTER(mess)
   GENPTR_INTEGER(status)
   
   F77_CALL(subpar_write)(CHARACTER_ARG(mess), INTEGER_ARG(status)
     TRAIL_ARG(mess) );
   
}

F77_SUBROUTINE(subpar_wrmsg)(CHARACTER(mess), INTEGER(status) TRAIL(mess) ) {
   GENPTR_CHARACTER(mess)
   GENPTR_INTEGER(status)
   
   F77_CALL(subpar_write)(CHARACTER_ARG(mess), INTEGER_ARG(status)
     TRAIL_ARG(mess) );
   
}

F77_SUBROUTINE(subpar_sync)( INTEGER(status) ) {
   GENPTR_INTEGER(status)
/* a dummy routine for use with Starlink on Java */   
   
}

F77_SUBROUTINE(subpar_cancl)( INTEGER(id), INTEGER(status) ) {
/* Cancel the identified Parameter in the current ParameterList
*/
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(status)

   int istat = SAI__OK;
   
   jclass cls;
   jmethodID cancel;
   jobject param;
   
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, &istat );    
        
   if( istat == SAI__OK ) {

/* Get the class of this parameter */
DEB("cancl: Get parameter class\n");
      cls = (*parenv)->GetObjectClass( parenv, param );

      if( cls != 0 ) {
         cancel = (*parenv)->GetMethodID(parenv, cls, "cancel", "()V");
         if (cancel != 0) {
            (*parenv)->CallVoidMethod(parenv, param, cancel );
            subpar1_checkexc( id, &istat );   

         } else {
            subpar1_checkexc( 0, status );
            if( *status == SAI__OK ) *status = SUBPAR__ERROR;
            emsRep( "SUBPAR_CANCL2",
              "SUBPAR_CANCL: Can't find parameter cancel() method.",
              status );
         }

      } else {
         subpar1_checkexc( 0, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUBPAR_CANCL2",
           "SUBPAR_CANCL: Can't find parameter class.",
           status );
      }
   }

   if ( ( *status == SAI__OK ) && ( istat != SAI__OK ) ) {
      *status = istat;
   }
}


/* The subpar_get1x routines get the identified Parameter's value as a 1-d
*  array of the indicated type (c,d,i,l,r). A scalar value will be returned as
*  a 1-element array.
*  Arguments:
*  id     (Given)  the Parameter id.
*  maxval (Given)  the maximum number of elements permitted.
*  x      (Returned) the aray
*  actval (Returned) the actual number of elements of x
*  status (Given and Returned) the global status
*/
F77_SUBROUTINE(subpar_get1c)( INTEGER(id), INTEGER(maxval),
   CHARACTER_ARRAY(x), INTEGER(actval), INTEGER(status) TRAIL(x) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(maxval)
   GENPTR_INTEGER(actval)
   GENPTR_INTEGER(status)
   GENPTR_CHARACTER_ARRAY(x)
   
   jobject param;
   jclass cls;
   jmethodID getArrayObject;
   jobjectArray jarr;
   jobject jstr;
   jsize arrlen;
   
   const char *cstr;
   char *carr[132];
   char *name;
   int i;
   
   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
       

/* Get the class of this parameter */
DEB("get1c: Get parameter class\n");
    cls = subpar1_getObjectClass( param, status );

    if ( *status == SAI__OK ) {
/* get getArrayObject method */
DEB1("get1c: get getArrayObject method\n", *id);
      getArrayObject = (*parenv)->GetMethodID(parenv, cls,
       "getStringArray", "()[Ljava/lang/String;");

      if (getArrayObject != 0) {
DEB1("get1c: Getting param ID %d as a String[]\n", *id);
         jarr = (*parenv)->CallObjectMethod( parenv, param, getArrayObject );    

         if ( jarr != 0 ) {
            *actval = (int)(*parenv)->GetArrayLength( parenv, jarr );
DEB1("get1c: Size is %d\n",*actval);

            if ( *actval > *maxval ) {
               name = subpar1_getkey( *id, status ); 
               *status = SUBPAR__ARRDIM;                     
               emsSetc( "NAME", name );
               free( name );
               emsSeti( "MAXVAL", *maxval );
               emsRep( "SUP_GET1C_1",
                "SUBPAR: No more than ^MAXVAL elements are allowed "
                "for parameter ^NAME", status );
     
            } else {
DEB("get1c: convert array elements to C");
               for ( i=0; i<*actval; i++ ) {
                  jstr =
                   (*parenv)->GetObjectArrayElement( parenv, jarr, (jsize)i );
DEB("get1c: Converting to C string\n" );
                  if( jstr != 0 ) {
                     cstr = (*parenv)->GetStringUTFChars(parenv, jstr, 0);
                     if( cstr != 0 ) {
DEB1("  %s\n",cstr);
                        carr[i] = (char *)malloc( strlen(cstr) + 1 );
                        strcpy( carr[i], cstr );   
/* Free the UTF string */    
DEB("get1c: Releasing UTF string\n" );
                        (*parenv)->ReleaseStringUTFChars(parenv, jstr, cstr);
                     } else {
                        subpar1_checkexc( id, status );
                        if( *status == SAI__OK ) *status = SUBPAR__ERROR;
                        emsRep( "SUP_GET1C_2",
                                "SUBPAR_GET1C: Out of memory", status );
                     }
                  } else {
                     subpar1_checkexc( id, status );
                     if( *status == SAI__OK ) *status = SUBPAR__ERROR;
                     emsRep( "SUP_GET1C_3",
                             "SUBPAR_GET1C: Can't get array elements", status );
                  }

               }
               
               if( *status == SAI__OK ) {

                  F77_EXPORT_CHARACTER_ARRAY_P( carr, x, x_length, *actval );
             
/* free the C strings */
DEB("get1c: free the C strings\n");
                  for ( i=0; i<*actval; i++ ) {
                     free( carr[i] );
                  }
               }
            }
         }

/* Check for Exception (could be null or abort or other ) */
         subpar1_checkexc( id, status );

      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_GET1C_4",
          "SUBPAR1_GET1C: Can't find parameter getStringArray() method.",
          status );
      }
   }   
}

F77_SUBROUTINE(subpar_get1d)( INTEGER(id), INTEGER(maxval),
   DOUBLE_ARRAY(x), INTEGER(actval), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(maxval)
   GENPTR_INTEGER(actval)
   GENPTR_INTEGER(status)
   GENPTR_DOUBLE_ARRAY(x)
   
   jobject param;
   jclass cls;
   jmethodID getArrayObject;
   jdoubleArray jarr;
   jsize arrlen;
   jdouble *carr;
   
   char *name;
   int i;
   
   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );

/* Get the class of this parameter */
DEB("get1d: Get parameter class\n");
    cls = subpar1_getObjectClass( param, status );

    if( *status == SAI__OK ) {       
/* get getArrayObject method */
DEB1("get1d: get getArrayObject method\n", *id);
      getArrayObject = (*parenv)->GetMethodID(parenv, cls,
       "getDoubleArray", "()[D");

      if (getArrayObject != 0) {
DEB1("get1d: Getting param ID %d as a double[]\n", *id);
         jarr = (*parenv)->CallObjectMethod( parenv, param, getArrayObject );    

         if ( jarr != 0 ) {
            *actval = (int)(*parenv)->GetArrayLength( parenv, jarr );
DEB1("Size is %d\n",*actval);

            if ( *actval > *maxval ) {
               name = subpar1_getkey( *id, status ); 
               *status = SUBPAR__ARRDIM;                     
               emsSetc( "NAME", name );
               free( name );
               emsSeti( "MAXVAL", *maxval );
               emsRep( "SUP_GET1D_1",
                "SUBPAR: No more than ^MAXVAL elements are allowed "
                    "for parameter ^NAME", status );
     
            } else {     
DEB("get1d: Get pointer to elements\n");
               carr = (*parenv)->GetDoubleArrayElements( parenv, jarr, NULL );
DEB1("get1d: carr[0] is %f\n",carr[0]);
/*         F77_EXPORT_DOUBLE_ARRAY( x, carr, *actval );*/

               for( i=0; i<*actval; i++ ) {
                  x[i] = carr[i];
               }
             
            }

         }

/* Check for null or abort status */
         subpar1_checkexc( id, status );   

      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_GET1D_2",
          "SUBPAR1_GET1D: Can't find parameter getDoubleArray() method.",
          status );
      }
   }
}

F77_SUBROUTINE(subpar_get1i)( INTEGER(id), INTEGER(maxval),
   INTEGER_ARRAY(x), INTEGER(actval), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(maxval)
   GENPTR_INTEGER(actval)
   GENPTR_INTEGER(status)
   GENPTR_INTEGER_ARRAY(x)
   
   jobject param;
   jclass cls;
   jmethodID getArrayObject;
   jintArray jarr;
   jsize arrlen;
   jint *carr;
   
   char *name;
   int i;
   
   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );

/* Get the class of this parameter */
DEB("get1i: Get parameter class\n");
    cls = subpar1_getObjectClass( param, status );

    if( *status == SAI__OK ) {       
/* get getArrayObject method */
DEB1("get1i: get getArrayObject method\n", *id);
      getArrayObject = (*parenv)->GetMethodID(parenv, cls,
       "getIntArray", "()[I");

      if (getArrayObject != 0) {
DEB1("get1i: Getting param ID %d as an int[]\n", *id);
         jarr = (*parenv)->CallObjectMethod( parenv, param, getArrayObject );    

         if ( jarr != 0 ) {
            *actval = (int)(*parenv)->GetArrayLength( parenv, jarr );
DEB1("get1i: Size is %d\n",*actval);

            if ( *actval > *maxval ) {
               name = subpar1_getkey( *id, status ); 
               *status = SUBPAR__ARRDIM;                     
               emsSetc( "NAME", name );
               free( name );
               emsSeti( "MAXVAL", *maxval );
               emsRep( "SUP_GET1I_1",
                 "SUBPAR: No more than ^MAXVAL elements are allowed "
                 "for parameter ^NAME", status );
     
            } else {     
DEB("get1i: Get pointer to elements\n");
            carr = (*parenv)->GetIntArrayElements( parenv, jarr, NULL );
DEB1("get1i: carr[0] is %f\n",carr[0]);
/*         F77_EXPORT_DOUBLE_ARRAY( x, carr, *actval );*/

               for( i=0; i<*actval; i++ ) {
                  x[i] = carr[i];
               }
             
            }

         }

/* Check for null or abort status */
         subpar1_checkexc( id, status );
    

      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_GET1I_2",
          "SUBPAR1_GET1I: Can't find parameter getIntArray() method.",
          status );
      }   
   }
}
   


F77_SUBROUTINE(subpar_get1l)( INTEGER(id), INTEGER(maxval),
   LOGICAL_ARRAY(x), INTEGER(actval), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(maxval)
   GENPTR_INTEGER(actval)
   GENPTR_INTEGER(status)
   GENPTR_LOGICAL_ARRAY(x)
   
   jobject param;
   jclass cls;
   jmethodID getArrayObject;
   jbooleanArray jarr;
   jsize arrlen;
   jboolean *carr;
   
   char *name;
   int i;
   
   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );

/* Get the class of this parameter */
DEB("get1l: Get parameter class\n");
    cls = subpar1_getObjectClass( param, status );

    if( *status == SAI__OK ) {       
/* get getArrayObject method */
DEB1("get1l: get getArrayObject method\n", *id);
      getArrayObject = (*parenv)->GetMethodID(parenv, cls,
       "getBooleanArray", "()[Z");

      if (getArrayObject != 0) {
DEB1("get1l: Getting param ID %d as a boolean[]\n", *id);
         jarr = (*parenv)->CallObjectMethod( parenv, param, getArrayObject );    

         if ( jarr != 0 ) {
            *actval = (int)(*parenv)->GetArrayLength( parenv, jarr );
DEB1("get1l: Size is %d\n",*actval);

            if ( *actval > *maxval ) {
               name = subpar1_getkey( *id, status ); 
               *status = SUBPAR__ARRDIM;                     
               emsSetc( "NAME", name );
               free( name );
               emsSeti( "MAXVAL", *maxval );
               emsRep( "SUP_GET1L_1",
                "SUBPAR: No more than ^MAXVAL elements are allowed "
                 "for parameter ^NAME", status );
     
            } else {     
DEB("get1l: Get pointer to elements\n");
               carr = (*parenv)->GetBooleanArrayElements( parenv, jarr, NULL );
DEB1("get1l: carr[0] is %d\n",carr[0]);
DEB1("get1l: carr[1] is %d\n",carr[1]);
               for ( i=0; i<*actval; i++ ) {
                  if ( carr[i] == JNI_TRUE ) {
                     x[i] = F77_TRUE;
                  } else {
                     x[i] = F77_FALSE;
                  }
               }
            }
         }

/* Check for null or abort status */
         subpar1_checkexc( id, status );
    

      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_GET1L_2",
          "SUBPAR1_GET1L: Can't find parameter getBooleanArray() method.",
          status );
      }   
   }
   
}

F77_SUBROUTINE(subpar_get1r)( INTEGER(id), INTEGER(maxval),
   REAL_ARRAY(x), INTEGER(actval), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(maxval)
   GENPTR_INTEGER(actval)
   GENPTR_INTEGER(status)
   GENPTR_REAL_ARRAY(x)
   
   jobject param;
   jclass cls;
   jmethodID getArrayObject;
   jfloatArray jarr;
   jsize arrlen;
   jfloat *carr;
   
   char *name;
   int i;
   
   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );

/* Get the class of this parameter */
DEB("get1r: Get parameter class\n");
    cls = subpar1_getObjectClass( param, status );

    if( *status == SAI__OK ) {       
/* get getArrayObject method */
DEB1("get1r: get getArrayObject method\n", *id);
      getArrayObject = (*parenv)->GetMethodID(parenv, cls,
       "getFloatArray", "()[F");

      if (getArrayObject != 0) {
DEB1("get1r: Getting param ID %d as a float[]\n", *id);
         jarr = (*parenv)->CallObjectMethod( parenv, param, getArrayObject );    

         if ( jarr != 0 ) {
            *actval = (int)(*parenv)->GetArrayLength( parenv, jarr );
DEB1("Size is %d\n",*actval);
            if ( *actval > *maxval ) {
               name = subpar1_getkey( *id, status ); 
               *status = SUBPAR__ARRDIM;                     
               emsSetc( "NAME", name );
               free( name );
               emsSeti( "MAXVAL", *maxval );
               emsRep( "SUP_GET1R_1",
                 "SUBPAR: No more than ^MAXVAL elements are allowed "
                 "for parameter ^NAME", status );
     
            } else {     
DEB("get1r: Get pointer to elements\n");
               carr = (*parenv)->GetFloatArrayElements( parenv, jarr, NULL );
DEB1("get1r: carr[0] is %f\n",carr[0]);
/*         F77_EXPORT_DOUBLE_ARRAY( x, carr, *actval );*/

               for( i=0; i<*actval; i++ ) {
                  x[i] = carr[i];
               }
            }
         }

/* Check for null or abort status */
         subpar1_checkexc( id, status );
    

      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_GET1R_2",
          "SUBPAR1_GET1C: Can't find parameter getFloatArray() method.",
          status );
      }   
   }
}

F77_SUBROUTINE(subpar_getnc)( int status )
{DEBDUM("Called dummy getnc\n");}
F77_SUBROUTINE(subpar_getnd)( int status )
{DEBDUM("Called dummy getnl\n");}
F77_SUBROUTINE(subpar_getni)( int status )
{DEBDUM("Called dummy getnl\n");}
F77_SUBROUTINE(subpar_getnl)( int status )
{DEBDUM("Called dummy getnl\n");}
F77_SUBROUTINE(subpar_getnr)( int status )
{DEBDUM("Called dummy getnl\n");}

F77_SUBROUTINE(subpar_getvc)( INTEGER(id), INTEGER(maxval),
   CHARACTER_ARRAY(x), INTEGER(actval), INTEGER(status) TRAIL(x) ) {
   
   F77_CALL(subpar_get1c)( INTEGER_ARG(id), INTEGER_ARG(maxval),
     CHARACTER_ARRAY_ARG(x), INTEGER_ARG(actval), INTEGER_ARG(status)
     TRAIL_ARG(x) );
}
   
F77_SUBROUTINE(subpar_getvd)( INTEGER(id), INTEGER(maxval),
   DOUBLE_ARRAY(x), INTEGER(actval), INTEGER(status) ) {
   
   F77_CALL(subpar_get1d)( INTEGER_ARG(id), INTEGER_ARG(maxval),
     DOUBLE_ARRAY_ARG(x), INTEGER_ARG(actval), INTEGER_ARG(status) );
}
F77_SUBROUTINE(subpar_getvi)( INTEGER(id), INTEGER(maxval),
   INTEGER_ARRAY(x), INTEGER(actval), INTEGER(status) ) {
   
   F77_CALL(subpar_get1i)( INTEGER_ARG(id), INTEGER_ARG(maxval),
     INTEGER_ARRAY_ARG(x), INTEGER_ARG(actval), INTEGER_ARG(status) );
}
F77_SUBROUTINE(subpar_getvl)( INTEGER(id), INTEGER(maxval),
   LOGICAL_ARRAY(x), INTEGER(actval), INTEGER(status) ) {
   
   F77_CALL(subpar_get1l)( INTEGER_ARG(id), INTEGER_ARG(maxval),
     LOGICAL_ARRAY_ARG(x), INTEGER_ARG(actval), INTEGER_ARG(status) );
}
F77_SUBROUTINE(subpar_getvr)( INTEGER(id), INTEGER(maxval),
   REAL_ARRAY(x), INTEGER(actval), INTEGER(status) ) {
   
   F77_CALL(subpar_get1r)( INTEGER_ARG(id), INTEGER_ARG(maxval),
     REAL_ARRAY_ARG(x), INTEGER_ARG(actval), INTEGER_ARG(status) );
}

/* The subpar_def0x routines set a dynamic default of the type indicated by
*  x (c,d,i,l,r) for the identified Parameter. The given value must be valid for
*  the type of the Parameter.
*  Arguments:
*     id  (Given)  the Parameter id
*     x   (Given)  the dynamic default value
*     status (Given and Returned) the global status
*/
F77_SUBROUTINE(subpar_def0c)( INTEGER(id), CHARACTER(x), INTEGER(status) 
  TRAIL(x) ) {
   GENPTR_INTEGER(id)
   GENPTR_CHARACTER(x)
   GENPTR_INTEGER(status)

   jmethodID setDynamic;
   jobject param;
   jclass cls;
   char *cstring;
   jobject string;

   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
       
/* Get parameter class */
DEB("def0c: Get parameter class\n");
    cls = subpar1_getObjectClass( param, status );

    if( *status == SAI__OK ) {       
DEB1("def0c: get setDynamic method\n", *id);
      setDynamic =
       (*parenv)->GetMethodID(parenv, cls,
        "setDynamic", "(Ljava/lang/Object;)V");
DEB("def0c: returned from GetMethodID\n");

      if ( setDynamic != 0) {
/* Create a Java String */
DEB1("def0c: string length is: %d\n", x_length);
         cstring = cnfCreim( x, x_length ); 
DEB1("def0c: Default is: %s\n", cstring);
         string = (*parenv)->NewStringUTF(parenv, cstring);
         cnfFree( cstring );   

DEB1("def0c: Setting param ID %d dynamic default as a String\n", *id);
         (*parenv)->CallVoidMethod(parenv, param, setDynamic, string );    
   
/* Check for Exception */
         subpar1_checkexc( id, status );
DEB1("def0c: Final STATUS is: %d\n", *status);

      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_DEF0C_2",
          "SUBPAR1_DEF0C: Can't find parameter setDynamic() method.",
          status );
      }   

   }
}

F77_SUBROUTINE(subpar_def0d)( INTEGER(id), DOUBLE(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_DOUBLE(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID setDynamic;
   jobject param;

   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
       
/* Get parameter class */
DEB("def0d: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );

   if( *status == SAI__OK ) {       
DEB1("def0d: get setDynamic method\n", *id);
      setDynamic = (*parenv)->GetMethodID(parenv, cls, "setDynamic", "(D)V");
      if ( setDynamic != 0) {

DEB1("def0d: Setting param ID %d dynamic default as a double\n", *id);
         (*parenv)->CallVoidMethod(parenv, param, setDynamic, *x );    
DEB1("def0d: Default is: %f\n", *x);
   
/* Check for Exception */
         subpar1_checkexc( id, status );

      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_DEF0D_2",
          "SUBPAR1_DEF0D: Can't find parameter setDynamic() method.",
          status );
      }
   }
}

F77_SUBROUTINE(subpar_def0i)( INTEGER(id), INTEGER(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID setDynamic;
   jobject param;

   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
       
/* Get parameter class */
DEB("def0i: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );

   if( *status == SAI__OK ) {       
DEB1("def0i: get setDynamic method\n", *id);
      setDynamic = (*parenv)->GetMethodID(parenv, cls, "setDynamic", "(I)V");

      if ( setDynamic != 0) {
DEB1("def0i: Setting param ID %d dynamic default as an int\n", *id);
         (*parenv)->CallVoidMethod(parenv, param, setDynamic, *x );    
DEB1("def0i: Default is: %d\n", *x);
   
/* Check for Exception */
         subpar1_checkexc( id, status );


      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_DEF0I_2",
          "SUBPAR1_DEF0I: Can't find parameter setDynamic() method.",
          status );
      }
   }
}

F77_SUBROUTINE(subpar_def0l)( INTEGER(id), LOGICAL(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_LOGICAL(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID setDynamic;
   jobject param;
   jboolean v;

   if ( *status != SAI__OK ) return;
   
/* Convert the value to JNI boolean */
   v = (jboolean)F77_ISTRUE( *x ); 
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
       
/* Get parameter class */
DEB("def0l: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );

   if( *status == SAI__OK ) {       
DEB1("def0l: get setDynamic method\n", *id);
      setDynamic = (*parenv)->GetMethodID(parenv, cls, "setDynamic", "(Z)V");
      if ( setDynamic != 0) {

DEB1("def0l: Setting param ID %d dynamic default as a boolean\n", *id);
         (*parenv)->CallVoidMethod(parenv, param, setDynamic, v );    
DEB1("def0l: Default is: %d\n", v);
   
/* Check for Exception */
         subpar1_checkexc( id, status );

      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_DEF0L_2",
          "SUBPAR1_DEF0L: Can't find parameter setDynamic() method.",
          status );
      }
   }
}

F77_SUBROUTINE(subpar_def0r)( INTEGER(id), REAL(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_REAL(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID setDynamic;
   jobject param;

   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
       
/* Get parameter class */
DEB("def0r: Get parameter class\n");
    cls = subpar1_getObjectClass( param, status );

    if( *status == SAI__OK ) {       
DEB1("def0r: get setDynamic method\n", *id);
      setDynamic = (*parenv)->GetMethodID(parenv, cls, "setDynamic", "(F)V");
      if ( setDynamic != 0) {

DEB1("def0r: Setting param ID %d dynamic default as a double\n", *id);
      (*parenv)->CallVoidMethod(parenv, param, setDynamic, *x );    
DEB1("def0r: Default is: %f\n", *x);
   
/* Check for Exceptio */
      subpar1_checkexc( id, status );

      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_DEF0R_2",
          "SUBPAR1_DEF0R: Can't find parameter setDynamic() method.",
          status );
      }
   }
}


F77_SUBROUTINE(subpar_def1c)( INTEGER(id), INTEGER(nvals), CHARACTER_ARRAY(x),
                              INTEGER(status) TRAIL(x) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(nvals)
   GENPTR_CHARACTER_ARRAY(x)
   GENPTR_INTEGER(status)
}

F77_SUBROUTINE(subpar_def1d)( INTEGER(id), INTEGER(nvals), DOUBLE_ARRAY(x),
                              INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(nvals)
   GENPTR_DOUBLE_ARRAY(x)
   GENPTR_INTEGER(status)
}

F77_SUBROUTINE(subpar_def1i)( INTEGER(id), INTEGER(nvals), INTEGER_ARRAY(x),
                              INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(nvals)
   GENPTR_INTEGER_ARRAY(x)
   GENPTR_INTEGER(status)
}

F77_SUBROUTINE(subpar_def1r)( INTEGER(id), INTEGER(nvals), REAL_ARRAY(x),
                              INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(nvals)
   GENPTR_REAL_ARRAY(x)
   GENPTR_INTEGER(status)
}

F77_SUBROUTINE(subpar_def1l)( INTEGER(id), INTEGER(nvals), LOGICAL_ARRAY(x),
                              INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(nvals)
   GENPTR_LOGICAL_ARRAY(x)
   GENPTR_INTEGER(status)
}

F77_SUBROUTINE(subpar_defnc)( int status )
{DEBDUM("Called dummy defnc\n");}
F77_SUBROUTINE(subpar_defnd)( int status )
{DEBDUM("Called dummy defnd\n");}
F77_SUBROUTINE(subpar_defni)( int status )
{DEBDUM("Called dummy defni\n");}
F77_SUBROUTINE(subpar_defnl)( int status )
{DEBDUM("Called dummy defnl\n");}
F77_SUBROUTINE(subpar_defnr)( int status )
{DEBDUM("Called dummy defnr\n");}

F77_SUBROUTINE(subpar_maxc)( int status )
{DEBDUM("Called dummy maxc\n");}
F77_SUBROUTINE(subpar_maxd)( int status )
{DEBDUM("Called dummy maxd\n");}
F77_SUBROUTINE(subpar_maxi)( int status )
{DEBDUM("Called dummy maxi\n");}
F77_SUBROUTINE(subpar_maxl)( int status )
{DEBDUM("Called dummy maxl\n");}
F77_SUBROUTINE(subpar_maxr)( int status )
{DEBDUM("Called dummy maxr\n");}

F77_SUBROUTINE(subpar_minc)( int status )
{DEBDUM("Called dummy minc\n");}
F77_SUBROUTINE(subpar_mind)( int status )
{DEBDUM("Called dummy mind\n");}
F77_SUBROUTINE(subpar_mini)( int status )
{DEBDUM("Called dummy mini\n");}
F77_SUBROUTINE(subpar_minl)( int status )
{DEBDUM("Called dummy minl\n");}
F77_SUBROUTINE(subpar_minr)( int status )
{DEBDUM("Called dummy minr\n");}

F77_SUBROUTINE(subpar_promt)( int status )
{DEBDUM("Called dummy promt\n");}

/* The subpar_put0x routines set the value of the identified Parameter to
*  a scalar value of the type indicated by x (c,d,i,l,r).  The given value
*  must be valid for the type of the Parameter.
*  Arguments:
*     id    (Given)  the Parameter id
*     x     (Given)  the value to be set
*     status (Given and Returned) the global status
*/
F77_SUBROUTINE(subpar_put0c)( INTEGER(id), CHARACTER(x), INTEGER(status)
  TRAIL(x) ) {
   GENPTR_INTEGER(id)
   GENPTR_CHARACTER(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID putString;
   jobject param;
   char *cstring;
   jstring string;

   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
       
/* Get parameter class */
DEB("put0c: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );

   if( *status == SAI__OK ) {       
DEB1("put0c: get putString method\n", *id);
      putString = (*parenv)->GetMethodID(parenv, cls, "putString",
      "(Ljava/lang/String;)V");

      if (putString = 0) {
/* Create a Java String */
DEB1("put0c: string length is: %d\n", x_length);
      cstring = cnfCreim( x, x_length ); 
DEB1("put0c: C string is: %s\n", cstring);
      string = (*parenv)->NewStringUTF(parenv, cstring);
/*   cnfFree( cstring );*/

/* Now invoke the putString method */
DEB1("put0c: Putting param ID %d as a String\n", *id);
      (*parenv)->CallVoidMethod(parenv, param, putString, string );    
   
/* Check for null or abort status */
   subpar1_checkexc( id, status );

      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_PUT0C_2",
          "SUBPAR1_PUT0C: Can't find parameter putString() method.",
          status );
      }
   }
}

F77_SUBROUTINE(subpar_put0d)( INTEGER(id), DOUBLE(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_DOUBLE(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID putDouble;
   jobject param;

   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
       
/* Get parameter class */
DEB("put0d: Get parameter class\n");
    cls = subpar1_getObjectClass( param, status );

    if( *status == SAI__OK ) {       
DEB1("put0d: get putDouble method\n", *id);
      putDouble = (*parenv)->GetMethodID(parenv, cls, "putDouble", "(D)V");

      if (putDouble != 0) {
DEB1("put0d: Putting param ID %d as a double\n", *id);
         (*parenv)->CallVoidMethod(parenv, param, putDouble, (jdouble)*x );    
DEB1("put0d: Param is: %f\n", *x);
   
/* Check for null or abort status */
         subpar1_checkexc( id, status );

      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_PUT0D_2",
          "SUBPAR1_PUT0D: Can't find parameter putDouble() method.",
          status );
      }

   }
}

F77_SUBROUTINE(subpar_put0i)( INTEGER(id), INTEGER(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID putInt;
   jobject param;

   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
       
/* Get parameter class */
DEB("put0i: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );

   if( *status == SAI__OK ) {       
DEB1("put0i: get putInt method\n", *id);
      putInt = (*parenv)->GetMethodID(parenv, cls, "putInt", "(I)V");

      if (putInt != 0) {
DEB1("put0i: Putting param ID %d as an int\n", *id);
         (*parenv)->CallVoidMethod(parenv, param, putInt, *x );    
DEB1("put0i: Param is: %d\n", *x);
   
/* Check for null or abort status */
         subpar1_checkexc( id, status );

      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_PUT0I_2",
          "SUBPAR1_PUT0I: Can't find parameter putInt() method.",
          status );
      }

   }
}

F77_SUBROUTINE(subpar_put0l)( INTEGER(id), LOGICAL(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_LOGICAL(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID putBoolean;
   jobject param;
   jboolean bValue = JNI_FALSE;

   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
       
/* Get parameter class */
DEB("put0l: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );

   if( *status == SAI__OK ) {       
DEB1("put0l: get putBoolean method\n", *id);
      putBoolean = (*parenv)->GetMethodID(parenv, cls, "putBoolean", "(Z)V");

      if (putBoolean != 0) {

         if ( F77_ISTRUE(*x) ) bValue = JNI_TRUE;
DEB1("put0l: Putting param ID %d as an int\n", *id);
         (*parenv)->CallVoidMethod(parenv, param, putBoolean, F77_ISTRUE(*x) );    
DEB1("put0l: Param is: %d\n", *x);
   
/* Check for null or abort status */
         subpar1_checkexc( id, status );

      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_PUT0L_2",
          "SUBPAR1_PUT0L: Can't find parameter putBoolean() method.",
          status );
      }

   }
}

F77_SUBROUTINE(subpar_put0r)( INTEGER(id), REAL(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_REAL(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID putFloat;
   jobject param;

   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
       
/* Get parameter class */
DEB("put0r: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );

   if( *status == SAI__OK ) {       
DEB1("put0r: get putFloat method\n", *id);
      putFloat = (*parenv)->GetMethodID(parenv, cls, "putFloat", "(F)V");

      if (putFloat != 0) {
DEB1("put0r: Putting param ID %d as a float\n", *id);
         (*parenv)->CallVoidMethod(parenv, param, putFloat, *x );    
DEB1("put0r: Param is: %d\n", *x);
   
/* Check for null or abort status */
         subpar1_checkexc( id, status );

      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_PUT0R_2",
          "SUBPAR1_PUT0R: Can't find parameter putFloat() method.",
          status );
      }

   }
}

F77_SUBROUTINE(subpar_put1c)
  ( INTEGER(id), INTEGER(nval), CHARACTER_ARRAY(x), INTEGER(status) TRAIL(x) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(nval)
   GENPTR_CHARACTER_ARRAY(x)
   GENPTR_INTEGER(status)
   
   char *carr;

   jclass cls;
   jmethodID putArray;
   jobject param;
   jobject apval;
   
   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
       
/* Get parameter class */
DEB("put1c: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );

   if( *status == SAI__OK ) {
                  
DEB1("put1c: get putArray method\n", *id);
      putArray = (*parenv)->GetMethodID(parenv, cls, "putArray",
        "(Luk/ac/starlink/jpcs/ArrayParameterValue;)V");

      if (putArray != 0) {
DEB("put1c: create ArrayParameterValue\n");
         carr = (char *)malloc( *nval * (x_length+1) );
         if( carr != NULL ) {
/* Create the ArrayParameterValue */
            F77_IMPORT_CHARACTER_ARRAY( x, x_length, carr, x_length+1, *nval );
DEB1("Imported: %s\n",carr);
            apval = subpar1_creArrPValC( 1, nval, carr, x_length+1, status );
         
/* and put it as the Parameter value */
            (*parenv)->CallVoidMethod(parenv, param, putArray, apval ); 
            subpar1_checkexc( id, status );

/* free the C array memory */            
            cnfFree( carr );
            
         } else {
            *status = SUBPAR__ERROR;
            emsRep( "SUP_PUT1C_1",
             "SUBPAR1_PUT1C: Failed to obtain memory",
             status );
         }  
   
      } else {
DEB("put1c: failed to get putArray method\n");
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_PUT1C_2",
          "SUBPAR1_PUT1C: Can't find parameter putArray() method.",
          status );
      }
   }
}


F77_SUBROUTINE(subpar_put1d)
  ( INTEGER(id), INTEGER(nval), DOUBLE_ARRAY(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(nval)
   GENPTR_DOUBLE_ARRAY(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID putArray;
   jobject param;
   jobject apval;
   
   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
       
/* Get parameter class */
DEB("put1d: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );

   if( *status == SAI__OK ) {
                  
DEB1("put1d: get putArray method\n", *id);
      putArray = (*parenv)->GetMethodID(parenv, cls, "putArray",
        "(Luk/ac/starlink/jpcs/ArrayParameterValue;)V");

      if (putArray != 0) {
DEB("put1d: create ArrayParameterValue\n");
/* Create the ArrayParameterValue */
         apval = subpar1_creArrPValD( 1, nval, x, status );
/* and put it as the Parameter value */
         (*parenv)->CallVoidMethod(parenv, param, putArray, apval ); 
         subpar1_checkexc( id, status );   
   
      } else {
DEB("put1d: failed to get putArray method\n");
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_PUT1D_2",
          "SUBPAR1_PUT1D: Can't find parameter putArray() method.",
          status );
      }
   }
}

F77_SUBROUTINE(subpar_put1i)
  ( INTEGER(id), INTEGER(nval), INTEGER_ARRAY(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(nval)
   GENPTR_INTEGER_ARRAY(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID putArray;
   jobject param;
   jobject apval;
   
   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
       
/* Get parameter class */
DEB("put1i: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );

   if( *status == SAI__OK ) {
                  
DEB1("put1i: get putArray method\n", *id);
      putArray = (*parenv)->GetMethodID(parenv, cls, "putArray",
        "(Luk/ac/starlink/jpcs/ArrayParameterValue;)V");

      if (putArray != 0) {
DEB("put1i: create ArrayParameterValue\n");
/* Create the ArrayParameterValue */
         apval = subpar1_creArrPValI( 1, nval, x, status );
/* and put it as the Parameter value */
         (*parenv)->CallVoidMethod(parenv, param, putArray, apval ); 
         subpar1_checkexc( id, status );   
   
      } else {
DEB("put1i: failed to get putArray method\n");
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_PUT1I_2",
          "SUBPAR1_PUT1I: Can't find parameter putArray() method.",
          status );
      }
   }
}

F77_SUBROUTINE(subpar_put1l)
  ( INTEGER(id), INTEGER(nval), LOGICAL_ARRAY(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(nval)
   GENPTR_LOGICAL_ARRAY(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID putArray;
   jobject param;
   jobject apval;
   
   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
       
/* Get parameter class */
DEB("put1l: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );

   if( *status == SAI__OK ) {
                  
DEB1("put1l: get putArray method\n", *id);
      putArray = (*parenv)->GetMethodID(parenv, cls, "putArray",
        "(Luk/ac/starlink/jpcs/ArrayParameterValue;)V");

      if (putArray != 0) {
DEB("put1l: create ArrayParameterValue\n");
/* Create the ArrayParameterValue */
         apval = subpar1_creArrPValB( 1, nval, x, status );
/* and put it as the Parameter value */
         (*parenv)->CallVoidMethod(parenv, param, putArray, apval ); 
         subpar1_checkexc( id, status );   
   
      } else {
DEB("put1l: failed to get putArray method\n");
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_PUT1L_2",
          "SUBPAR1_PUT1L: Can't find parameter putArray() method.",
          status );
      }
   }
}

F77_SUBROUTINE(subpar_put1r)
  ( INTEGER(id), INTEGER(nval), REAL_ARRAY(x), INTEGER(status) ) {
   GENPTR_INTEGER(id)
   GENPTR_INTEGER(nval)
   GENPTR_REAL_ARRAY(x)
   GENPTR_INTEGER(status)

   jclass cls;
   jmethodID putArray;
   jobject param;
   jobject apval;
   
   if ( *status != SAI__OK ) return;
        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
       
/* Get parameter class */
DEB("put1r: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );

   if( *status == SAI__OK ) {
                  
DEB1("put1r: get putArray method\n", *id);
      putArray = (*parenv)->GetMethodID(parenv, cls, "putArray",
        "(Luk/ac/starlink/jpcs/ArrayParameterValue;)V");

      if (putArray != 0) {
DEB("put1r: create ArrayParameterValue\n");
/* Create the ArrayParameterValue */
         apval = subpar1_creArrPValF( 1, nval, x, status );
/* and put it as the Parameter value */
         (*parenv)->CallVoidMethod(parenv, param, putArray, apval ); 
         subpar1_checkexc( id, status );   
   
      } else {
DEB("put1r: failed to get putArray method\n");
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_PUT1R_2",
          "SUBPAR1_PUT1R: Can't find parameter putArray() method.",
          status );
      }
   }
}

F77_SUBROUTINE(subpar_putnc)( int status )
{DEBDUM("Called dummy putnc\n");}
F77_SUBROUTINE(subpar_putnd)( int status )
{DEBDUM("Called dummy putnd\n");}
F77_SUBROUTINE(subpar_putni)( int status )
{DEBDUM("Called dummy putni\n");}
F77_SUBROUTINE(subpar_putnl)( int status )
{DEBDUM("Called dummy putnl\n");}
F77_SUBROUTINE(subpar_putnr)( int status )
{DEBDUM("Called dummy putnr\n");}

F77_SUBROUTINE(subpar_putvc)( int status )
{DEBDUM("Called dummy putvc\n");}
F77_SUBROUTINE(subpar_putvd)( int status )
{DEBDUM("Called dummy putvd\n");}
F77_SUBROUTINE(subpar_putvi)( int status )
{DEBDUM("Called dummy putvi\n");}
F77_SUBROUTINE(subpar_putvl)( int status )
{DEBDUM("Called dummy putvl\n");}
F77_SUBROUTINE(subpar_putvr)( int status )
{DEBDUM("Called dummy putvr\n");}

F77_SUBROUTINE(subpar_putloc)
( INTEGER(id), CHARACTER(loc), INTEGER(status) TRAIL(loc) )
{DEBDUM("Called dummy putloc\n");}
F77_SUBROUTINE(subpar_putfloc)
( INTEGER(id), CHARACTER(loc), INTEGER(status) TRAIL(loc) )
{DEBDUM("Called dummy putfloc\n");}

F77_SUBROUTINE(subpar_unset)( int status )
{DEBDUM("Called dummy unset\n");}

F77_SUBROUTINE(subpar_curval)
  ( INTEGER(id), CHARACTER(value), INTEGER(status) TRAIL(value) ) {
   jclass cls;
   jmethodID toString;
   jobject param;
   jobject jstr;
   const char *ctmpstr; 
   
DEB("curval: Entered\n" );
   
   if ( *status != SAI__OK ) return;

DEB("curval: Status OK\n" );        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
DEB1("curval: getParameter returns status %d\n",*status);

/* Get the class of this parameter */
DEB("curval: Get parameter class\n");
   cls = subpar1_getObjectClass( param, status );

   if( *status == SAI__OK ) {       
DEB("curval: Getting toString method\n" );
      toString = (*parenv)->GetMethodID(parenv, cls, "toString",
       "()Ljava/lang/String;");

      if (toString != 0) {
DEB("curval: Getting param value as a String\n" );
         jstr = (*parenv)->CallObjectMethod(parenv, param, toString );

/* Convert the returned Java String to UTF and then to Fortran */
DEB("curval: Converting to C string\n" );
         if( 
          ( ctmpstr = (*parenv)->GetStringUTFChars(parenv, jstr, 0) ) == 0 ) {
            *status = SUBPAR__ERROR;
         } else {
            cnfExprt( ctmpstr, value, value_length );
      
/* Free the UTF string */    
DEB("curval: Releasing UTF string\n" );
            (*parenv)->ReleaseStringUTFChars(parenv, jstr, ctmpstr);
         }

/* Check for Exception */
         subpar1_checkexc( id, status );
         
      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_CURVAL_2",
          "SUBPAR1_CURVAL: Can't find parameter toString() method.",
          status );
      }
   }
} 
  
F77_SUBROUTINE(subpar_parname)( INTEGER(id), CHARACTER(name), INTEGER(namelen),
 INTEGER(status) TRAIL(name) ) {
   GENPTR_INTEGER(id)
   GENPTR_CHARACTER(str)
   GENPTR_INTEGER(status)
   
   char *cstr;

/* Return the keyword of the Parameter identified by id in the current 
 * ParameterList.
 */
   jclass cls;
   jmethodID getName;
   jobject param;
   jobject jstr;
   const char *ctmpstr; 
   
DEB("parname: Entered\n" );
   
   if ( *status != SAI__OK ) return;

DEB("parname: Status OK\n" );        
/* Get the specified  parameter */
   param = subpar1_getParameter( *id, status );
DEB1("parname: getParameter returns status %d\n",*status);

/* Get the class of this parameter */
DEB("getKey: Get parameter class\n");
    cls = subpar1_getObjectClass( param, status );

    if( *status == SAI__OK ) {       
DEB("parname: Getting getName method\n" );
      getName = (*parenv)->GetMethodID(parenv, cls, "getName",
       "()Ljava/lang/String;");

      if (getName != 0) {
DEB("parname: Getting param name\n" );
         jstr = (*parenv)->CallObjectMethod(parenv, param, getName );

/* Convert the returned Java String to UTF and then to Fortran */
DEB("parname: Converting to C string\n" );
         if( 
          ( ctmpstr = (*parenv)->GetStringUTFChars(parenv, jstr, 0) ) == 0 ) {
            *status = SUBPAR__ERROR;
         } else {
            *namelen = strlen( ctmpstr );
            cnfExprt( ctmpstr, name, name_length );
      
/* Free the UTF string */    
DEB("parname: Releasing UTF string\n" );
            (*parenv)->ReleaseStringUTFChars(parenv, jstr, ctmpstr);
         }

      } else {
         subpar1_checkexc( id, status );
         if( *status == SAI__OK ) *status = SUBPAR__ERROR;
         emsRep( "SUP_PARNAME_2",
          "SUBPAR1_PARNAME: Can't find parameter getName() method.",
          status );
      }
   }
} 

F77_SUBROUTINE(subpar_index)( INTEGER(id), INTEGER(status) ) {
GENPTR_INTEGER(id)
GENPTR_INTEGER(status)

   if ( *status != SAI__OK ) return;
DEB2("index: id in %d, plistSixze %d\n",*id,plistSize);   
   if ( ( *id <= 0 ) || ( *id > plistSize-1 ) ) {
      *id = 1;
      
   } else if ( *id == plistSize-1 ) {
      *id = 0;
      
   } else {
      *id = *id + 1;

   }
DEB1("index: id out %d\n",*id)
}

F77_LOGICAL_FUNCTION(subpar_gref)(INTEGER(id), CHARACTER(refstr),
                                  INTEGER(reflen) TRAIL(refstr) ) {
GENPTR_INTEGER(id)
GENPTR_CHARACTER(refstr)
GENPTR_INTEGER(reflen)

   int status;
   DECLARE_LOGICAL(retval);

DEB("gref: entered\n")
   
   emsMark();
   status = SAI__OK;
   F77_CALL(subpar_get0c)( INTEGER_ARG(id), CHARACTER_ARG(refstr),
     INTEGER_ARG(&status) TRAIL_ARG(refstr) );
     
   if( status == SAI__OK ) {
DEB("gref: status OK\n");
      *reflen = cnfLenf( refstr, refstr_length );
      retval = F77_TRUE;
      
   } else {
DEB1("gref: bad status %d\n",status);
      cnfExprt( " ", refstr, refstr_length );
      *reflen = 1;
      retval = F77_FALSE;
   }

   emsRlse();   

DEB1("gref: return %d\n",retval);
   return retval;
}

F77_SUBROUTINE(subpar_admus)( int status )
{DEBDUM("Called dummy admus\n");}
F77_SUBROUTINE(subpar_putname)( int status )
{DEBDUM("Called dummy putname\n");}

F77_SUBROUTINE(subpar_updat)( int status )
{DEBDUM("Called dummy updat\n");}
F77_SUBROUTINE(subpar_datdef)( int status )
{DEBDUM("Called dummy datdef\n");}
F77_SUBROUTINE(subpar_exist)( int status )
{DEBDUM("Called dummy exist\n");}
F77_SUBROUTINE(subpar_creat)( int status )
{DEBDUM("Called dummy creat\n");}
F77_SUBROUTINE(subpar_assoc)( int status )
{DEBDUM("Called dummy assoc\n");}
F77_SUBROUTINE(subpar_init)( int status )
{DEBDUM("Called dummy init\n");}

void task_get_name_(int status){}


