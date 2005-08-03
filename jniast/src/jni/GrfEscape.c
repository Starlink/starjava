/*
*+
*  Name:
*     GrfEscape.c

*  Purpose:
*     JNI implementations of native methods of GrfEscape class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     13-FEB-2004 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_grf_GrfEscape.h"

/* Class methods. */

JNIEXPORT jboolean JNICALL Java_uk_ac_starlink_ast_grf_GrfEscape_escapes(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* Class object */
   jint newval           /* New value */
) {   
   int oldval;

   ASTCALL(
      oldval = astEscapes( (int) newval );
   )
   return oldval ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_uk_ac_starlink_ast_grf_GrfEscape_findEscape(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* Class object */
   jstring jText,        /* String to analyse */
   jintArray jResults    /* Repository for results */
) {
   const char *text;
   jint *results;
   int found;

   ENSURE_SAME_TYPE(int,jint)
   
   if ( jniastCheckNotNull( env, jText ) &&
        jniastCheckArrayLength( env, jResults, 3 ) ) {
      text = (*env)->GetStringUTFChars( env, jText, NULL );
      results = (*env)->GetIntArrayElements( env, jResults, NULL );
      ASTCALL(
         found = astFindEscape( text, results, results + 1, results + 2 );
      )
      (*env)->ReleaseIntArrayElements( env, jResults, results, 0 );
      (*env)->ReleaseStringUTFChars( env, jText, text );
   }
   return found ? JNI_TRUE : JNI_FALSE;
}

