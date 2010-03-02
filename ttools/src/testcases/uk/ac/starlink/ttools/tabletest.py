import stilts
import java

messier = stilts.tread(testdir + "/messier.xml")

class TableTest(unittest.TestCase):
    def runTest(self):
        cols = messier.columns()
        self.assertEquals('Name', str(cols[0]))
        self.assertEquals('ImageURL', cols[-1].getName())
        colNgc = cols[2]
        self.assertEquals('NGC', str(colNgc))

        self.assertEquals('messier.xml', messier.parameters()['DemoLoc'])
        mp = messier.cmd_setparam('-type', 'float',
                                  '-unit', 'pint',
                                  'volume', '3.5')
        self.assertEquals(3.5, mp.parameters()['volume'])
        volparam = mp.getParameterByName('volume')
        self.assertEquals('pint', volparam.getInfo().getUnitString())
        self.assertEquals(java.lang.Float(0).getClass(),
                          volparam.getInfo().getContentClass())

        self.assert_(messier.isRandom())
        self.assertEquals(110, len(messier))
        ir = 0
        for row in messier:
            self.assertEquals(row, messier[ir])
            ic = 0
            for cell in row:
                self.assertEquals(cell, messier[ir][ic])
                ic += 1
            self.assertEquals('M', row[0][0])
            self.assertEquals('M', row['Name'][0])
            self.assertEquals('.jpg', row[-1][-4:])
            self.assertEquals('.jpg', row['ImageURL'][-4:])
            ir += 1
        head1 = messier.cmd_head(1)
        tail1 = messier.cmd_tail(1)
        self.assertEquals(1, len(head1))
        self.assertEquals(1, len(tail1))
        self.assertEquals(messier[0], head1[0])
        self.assertEquals(messier[-1], tail1[0])

        self.assertEquals(99, int(stilts.calc('100-1')))

        self.assertEqualData(3*messier, messier+messier+messier)
        self.assertEqualData(3*messier, messier*3)

        self.assertEquals('M23', messier[22][0])
        self.assertEquals('M23', messier[22]['Name'])
        self.assertEquals(6494, int(messier[22]['NGC']))
        self.assertEquals(6494, int(messier[22][colNgc]))
        self.assertEquals('.jpg', messier[5]['ImageURL'][-4:])
        self.assertEquals('.jpg', messier[5][-1][-4:])

        ands = (messier.cmd_select('equals("And",CON)')
                       .cmd_sort('-down', 'ID')
                       .cmd_keepcols('NAME'))
        self.assertEquals(['M110','M32','M31'],
                          [str(row[0]) for row in ands])

    def assertEqualData(self, t1, t2):
        try:
            self.assertEquals(len(t1), len(t2))
        except TypeError:
            pass
        it1 = t1.__iter__()
        it2 = t2.__iter__()
        ir = 0;
        while (True):
            end1 = False
            try:
                row1 = it1.next()
            except StopIteration:
                end1 = True
            end2 = False
            try:
                row2 = it2.next()
            except StopIteration:
                end2 = True
            self.assertEquals(end1, end2)
            if (end1):
                break;
            self.assertEquals(row1, row2, "row %d" % ir)
            ir += 1


TableTest().runTest()
