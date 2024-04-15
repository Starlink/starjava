import numpy as np
import pandas as pd
import pyarrow as pa
import pyarrow.parquet as pq

# Creates some files with different compression types.
#
# One way to examine these is to run
# java -classpath '$PCT/parquet-cli-1.13.1.jar:$PCT/dependency/*' \
#                 org.apache.parquet.cli.Main meta \
#                 <file>.parquet
# where $PCT is the directory parquet-mr/parquet-cli/target in a built
# https://github.com/apache/parquet-mr.

df = pd.DataFrame({
        'ints': [1, 2, 3],
        'fps': [2.5, np.nan, 99],
        'logs': [True, None, False],
        'strs': ['foo', None, 'baz'],
        'iarrs': [[11, 12, 13, 14], [21, 22, 23, 24], [31, 32, 33, 34]]
     })
table = pa.Table.from_pandas(df,
                             columns=['ints', 'fps', 'logs', 'strs', 'iarrs'])

for compress in ['none', 'gzip', 'snappy', 'brotli', 'lz4', 'zstd']:
   file = 'example-%s.parquet' % compress
   print(file)
   pq.write_table(table, file, compression=compress)

