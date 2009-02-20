/*
*+
*  Name:
*     SwitchMap.c

*  Purpose:
*     JNI implementations of native methods of SwitchMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor

*  History:
*     20-FEB-2009 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdlib.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_SwitchMap.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_SwitchMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jFsmap,       /* Forward selector map */
   jobject jIsmap,       /* Inverse selector map */
   jobjectArray jRoutemaps /* Route mapping array */
) {
   AstPointer pointer;
   AstMapping *fsmap;
   AstMapping *ismap;
   AstMapping **routemaps = NULL;
   AstObject **astObjs = NULL;
   AstPointer routemapPointer;
   jobject jRoutemap;
   int i;
   int j;

   if ( jniastCheckNotNull( env, jRoutemaps ) ) {
      int nroute = (*env)->GetArrayLength( env, jRoutemaps );
      astObjs = jniastMalloc( env, ( 3 + nroute ) * sizeof( AstObject * ) );
      routemaps = jniastMalloc( env, nroute * sizeof( AstMapping * ) );
      if ( astObjs && routemaps ) {
         j = 0;
         for ( i = 0; i < nroute; i++ ) {
            jRoutemap = (*env)->GetObjectArrayElement( env, jRoutemaps, i );
            if ( jRoutemap != NULL ) {
               routemapPointer = jniastGetPointerField( env, jRoutemap );
               routemaps[ i ] = routemapPointer.Mapping;
               astObjs[ j++ ] = routemapPointer.AstObject;
            }
            else {
               routemaps[ i ] = NULL;
            }
         }
         fsmap = jFsmap ? jniastGetPointerField( env, jFsmap ).Mapping : NULL;
         ismap = jIsmap ? jniastGetPointerField( env, jIsmap ).Mapping : NULL;
         if ( fsmap ) {
            astObjs[ j++ ] = (AstObject *) fsmap;
         }
         if ( ismap ) {
            astObjs[ j++ ] = (AstObject *) ismap;
         }
         astObjs[ j++ ] = NULL;
         THASTCALL( astObjs,
            pointer.SwitchMap = astSwitchMap( fsmap, ismap, (jint) nroute,
                                              (void **) routemaps, "" );
         )
         jniastInitObject( env, this, pointer );
      }
      free( routemaps );
   }
}
