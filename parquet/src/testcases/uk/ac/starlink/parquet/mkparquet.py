import numpy as np
import pandas as pd
import pyarrow as pa
import pyarrow.parquet as pq

df = pd.DataFrame({'ints': [1, 2, 3],
                   'fps': [2.5, np.nan, 99],
                   'logs': [True, None, False],
                   'strs': ['foo', None, 'baz']})
table = pa.Table.from_pandas(df, columns=['ints', 'fps', 'logs', 'strs'])

pq.write_table(table, 'example.parquet')

