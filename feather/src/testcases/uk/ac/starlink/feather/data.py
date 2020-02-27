import pandas
import pyarrow.feather as feather

d = {
   "ic": [-1,None,-3,4],
   "nc": [-1,2,-3,4],
   "dc": [10.,None,12.,13.],
   "bc": [True, None, False, True],
   "sc": ["red", None, "blue", "puce"],
}
cols = ["bcol", "icol", "dcol"];

df = pandas.DataFrame(data=d)

file = "data.fea";
feather.write_feather(df, file)

df1 = feather.read_feather(file)

print(df1)
