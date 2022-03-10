import unittest
import stilts
import java
import uk.ac.starlink
import os.path
import sys
import re
import io
import cStringIO

class TableTest(unittest.TestCase):
    def setUp(self, testdir = None):
        if testdir is None:
            testdir = os.path.dirname(__file__)
        self.messier = stilts.tread(os.path.join(testdir, "messier.xml"))

    def testObj(self):
        cols = self.messier.columns()
        self.assertEquals('Name', str(cols[0]))
        self.assertEquals('ImageURL', cols[-1].getName())
        colNgc = cols[2]
        self.assertEquals('NGC', str(colNgc))

        self.assertEquals('messier.xml', self.messier.parameters()['DemoLoc'])
        mp = self.messier.cmd_setparam('-type', 'float',
                                       '-unit', 'pint',
                                       'volume', '3.5')
        self.assertEquals(3.5, mp.parameters()['volume'])
        volparam = mp.getParameterByName('volume')
        self.assertEquals('pint', volparam.getInfo().getUnitString())
        self.assertEquals(java.lang.Float(0).getClass(),
                          volparam.getInfo().getContentClass())

        self.assert_(self.messier.isRandom())
        self.assertEquals(110, len(self.messier))
        self.assertEquals(110, self.messier.count_rows())
        self.assertEquals(110, self.messier.rowCount)
        self.assertEquals(110, self.messier.cmd_select("true").count_rows());
        for (ir, row) in enumerate(self.messier):
            self.assertEquals(row, self.messier[ir])
            for (ic, cell) in enumerate(row):
                self.assertEquals(cell, self.messier[ir][ic])
            self.assertEquals('M', row[0][0])
            self.assertEquals('M', row['Name'][0])
            self.assertEquals('.jpg', row[-1][-4:])
            self.assertEquals('.jpg', row['ImageURL'][-4:])
        head1 = self.messier.cmd_head(1)
        tail1 = self.messier.cmd_tail(1)
        self.assertEquals(1, len(head1))
        self.assertEquals(1, len(tail1))
        self.assertEquals(1, head1.count_rows())
        self.assertEquals(1, tail1.count_rows())
        self.assertEquals(self.messier[0], head1[0])
        self.assertEquals(self.messier[-1], tail1[0])

        self.assertEquals(5, len(self.messier[20:25]))
        self.assertEquals(self.messier[10], self.messier[10:12][0])

        names0 = ['M' + str(i+1) for i in xrange(0, len(self.messier))]
        names1 = self.messier.coldata('Name')
        names2 = tuple(self.messier.cmd_seqview().coldata(0))
        for key in (1, slice(0,4), -5, slice(0, 10, 10), slice(50, -50)):
            dat0 = tuple(names0.__getitem__(key))
            dat1 = tuple(names1.__getitem__(key))
            dat2 = tuple(names2.__getitem__(key))
            self.assertEquals(dat0, dat1)
            self.assertEquals(dat0, dat2)

        self.assertEquals(tuple(self.messier.coldata(-1)),
                          tuple(self.messier.coldata('ImageURL')))

        self.assertEqualTable(3*self.messier,
                              self.messier+self.messier+self.messier)
        self.assertEqualTable(3*self.messier, self.messier*3)

        self.assertEquals('M23', self.messier[22][0])
        self.assertEquals('M23', self.messier[22]['Name'])
        self.assertEquals(6494, int(self.messier[22]['NGC']))
        self.assertEquals(6494, int(self.messier[22][colNgc]))
        self.assertEquals('.jpg', self.messier[5]['ImageURL'][-4:])
        self.assertEquals('.jpg', self.messier[5][-1][-4:])

    def testFilters(self):
        ands = (self.messier.cmd_select('equals("And",CON)')
                            .cmd_sort('-down', 'ID')
                            .cmd_keepcols('NAME'))
        self.assertEquals(['M110','M32','M31'],
                          [str(row[0]) for row in ands])

        self.assertEquals('M101', self.messier[100]['Name'])
        self.assertEquals('M101',
                          self.messier.cmd_addcol('Name', '999')[100]['Name'])
        self.assertEquals(999,
                          self.messier.cmd_addcol('-before', '1', 'Name', 999)
                          [100]['Name'])

    def testScheme(self):
        self.assertEquals(10, len(stilts.tread(':loop:10')))
        cliff = stilts.tread(':attractor:11,clifford').cmd_cache()
        self.assertEquals(11, len(cliff))
        self.assertEquals(['x','y'], [str(c) for c in cliff.columns()])

    def testTasks(self):
        self.assertEquals(99, int(stilts.calc('100-1')))
        self.assertEquals(31,
                          int(stilts.calc(table=self.messier
                                          .cmd_setparam("number", "29"),
                                          expression='2 + param$number')))

        self.assertEqualTable(2*self.messier,
                              stilts.tcat([self.messier, self.messier]))
        self.assertEqualTable(2*self.messier,
                              stilts.tcatn(nin=2, in1=self.messier,
                                           in2=self.messier, countrows=True))

        m2 = stilts.tjoin(nin=2, in1=self.messier, in2=self.messier,
                          fixcols='all', suffix1='_A', suffix2='_B')
        self.assertEquals(len(m2.columns()), 2*len(self.messier.columns()))
        self.assertEqualData(m2.cmd_keepcols('*_A'), self.messier)

        self.assertEquals(['ID', 'lcol'],
                          [str(c) for c in
                               stilts.tcat(in_=[self.messier.cmd_keepcols(2)]*2,
                                           loccol='lcol').columns()])
        self.assertEquals(['ID'],
                          [str(c) for c in
                               stilts.tcat(in_=[self.messier.cmd_keepcols(2)]*2,
                                           loccol=None).columns()])

        self.assertRaises(SyntaxError, stilts.calc, '1+2', spurious='99')
        self.assertRaises(uk.ac.starlink.task.UsageException,
                          stilts.tmatchn)
        lop = stilts.tloop(start=50, end=100, step=10)
        self.assertEquals(99, len(stilts.tloop(99)))
        self.assertEquals(5, len(stilts.tloop(start=50, end=100, step=10)))

    def testIO(self):
        for fmt in ['csv', 'fits', 'ascii', 'votable']:
            self.ioRoundTrip(self.messier, fmt)

    def testMultiIO(self):
        for fmt in ['votable', 'fits-basic', 'fits-plus']:
            self.ioMultiRoundTrip([self.messier, self.messier.cmd_every(3)],
                                  fmt)

    def testModes(self):
        count = captureJavaOutput(self.messier.mode_count)
        ncol, nrow = map(int, re.compile(r': *(\w+)').findall(count))
        self.assertEquals(ncol, len(self.messier.columns()))
        self.assertEquals(nrow, len(self.messier))

        meta = captureJavaOutput(self.messier.mode_meta)
        lastmeta = meta.split('\n')[-2]
        index, name, clazz = re.compile(r'\w+').findall(lastmeta)[0:3]
        self.assertEquals(len(self.messier.columns()), int(index))
        self.assertEquals(self.messier.columns()[-1].getName(), name)

        discard = captureJavaOutput(self.messier.mode_discard)
        self.assert_(not discard)

    def testFuncs(self):
        self.assertEquals(42, stilts.Conversions.fromHex('2a'))
        self.assertEquals('2a', stilts.Conversions.toHex(42))
        self.assertEquals(51910, stilts.Times.isoToMjd('2001-01-01'))

    def ioRoundTrip(self, table, fmt):
        ofile = _UnclosedBytesIO()
        table.write(ofile, fmt=fmt)
        ifile = io.BytesIO(ofile.getvalue())
        table2 = stilts.tread(ifile, fmt=fmt)
        self.assertEqualTable(table, table2)

    def ioMultiRoundTrip(self, tables, fmt):
        ofile = _UnclosedBytesIO()
        stilts.twrites(tables, ofile, fmt=fmt)
        ifile = io.BytesIO(ofile.getvalue())
        tables2 = stilts.treads(ifile)
        for otable, itable in zip(tables, tables2):
           self.assertEqualTable(otable, itable)

    def assertEqualTable(self, t1, t2):
        self.assertEquals([str(col) for col in t1.columns()],
                          [str(col) for col in t2.columns()])
        self.assertEqualData(t1, t2)

    def assertEqualData(self, t1, t2):
        try:
            self.assertEquals(len(t1), len(t2))
        except (AttributeError, TypeError):
            pass
        for ir, rows in enumerate(map(None, t1, t2)):
            self.assertEquals(rows[0], rows[1], "row %d" % ir)

    def runTest(self, testdir):
        self.setUp(testdir)
        tests = [value for key, value in vars(TableTest).iteritems()
                       if key.startswith('test') and callable(value)]
        for test in tests:
            test(self)

def capturePythonOutput(callable, *args, **kwargs):
    sio = cStringIO.StringIO()
    sys.stdout = sio
    callable(*args, **kwargs)
    sys.stdout = sys.__stdout__
    return sio.getvalue()

def captureJavaOutput(callable, *args, **kwargs):
    buf = java.io.ByteArrayOutputStream()
    oldout = java.lang.System.out
    java.lang.System.setOut(java.io.PrintStream(buf))
    callable(*args, **kwargs)
    java.lang.System.setOut(oldout)
    return buf.toByteArray().tostring()
    

class _UnclosedBytesIO(io.BytesIO):
    def __init__(self):
        io.BytesIO.__init__(self)
    def close(self):
        pass

if "testdir" in globals():   # testdir was set from JyStiltsTest
    TableTest().runTest(testdir)
elif __name__ == '__main__': # standalone
    unittest.main()
else:                        # shouldn't happen
    raise RuntimeWarning('Tests not run')
