/*+
 *  Name:
 *     bdpthread

 *  Purpose:
 *     Brain dead pthreads implementation for JNIAST

 *  Description:
 *     These functions define a null implementation of the various
 *     pthread calls made in JNIAST. They assume no actual threading
 *     and just provide a service for mapping between a key and a value.

 *  Language:
 *     ANSI C.

 *  Authors:
 *     PWD: Peter W. Draper (JAC, Durham University)

 *  History:
 *     28-MAY-2009 (PWD):
 *        Original version.
 *-
 */

#include <stdio.h>
#include "jniast.h"

#if ! HAVE_PTHREADS
#include "bdpthread.h"

/*  Maximum number of keys, matched to POSIX. */
#define PTHREAD_KEYS_MAX  128

/*  Container for all keys */
static const void *keys[PTHREAD_KEYS_MAX];

/*  Current key index */
static int index = 0;

/*  Create a key. Just a simple next in the list allocation */
int pthread_key_create( pthread_key_t *key, void (*destr_function) (void *) )
{
    *key = index;
    index++;
    return 0;
}

/*  Get the pointer associated with a key */
const void *pthread_getspecific( pthread_key_t key )
{
    return keys[key];
}

/*  Set the pointer associated with a key */
int pthread_setspecific( pthread_key_t key, const void *pointer )
{
    keys[key] = pointer;
    return 0;
}

#else 
static void dummy() {}
#endif
