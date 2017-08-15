#!/usr/bin/env python
# -*- coding: utf-8 -*-
# Copyright (C) 2017, Jianfeng Chen <jchen37@ncsu.edu>
# vim: set ts=4 sts=4 sw=4 expandtab smartindent:
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
#  of this software and associated documentation files (the "Software"), to deal
#  in the Software without restriction, including without limitation the rights
#  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
#  copies of the Software, and to permit persons to whom the Software is
#  furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
#  all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
#  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
#  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
#  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
#  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
#  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
#  THE SOFTWARE.


from __future__ import division, print_function
from lxml import etree
import xml.etree.ElementTree as ET
import numpy
import sys
import pdb
import debug

algorithms = ['SWAY', 'EMSC-NSGAII', 'EMSC-SPEA2', 'EMSC-MOEA/D', 'SANITY']
modelref = ['Montage', 'Epigenomics', 'Inspiral', 'CyberShake', 'Sipht']


def median(lst):
    return numpy.median(numpy.array(lst))


def get_data_by_tag(tag):
    tree = ET.parse('../results/combined/reports.xml')
    subtree = filter(lambda t: t.tag == tag, tree.getroot().getchildren())[0]
    return subtree


def get_runtime_csv():
    # f1 = open('../results/combined/runtime.csv', 'w+')
    f1 = sys.stdout
    data = get_data_by_tag("runtimes")
    models = [i.get('name') for i in data.getchildren()]
    models = sorted(models, key=lambda n: (modelref.index(n.split('_')[0]), int(n.split('_')[1])))
    header = ['model'] + algorithms
    # print(','.join(header), file=f1)

    for model in models:
        d = [i for i in data.getchildren() if i.get('name') == model][0]
        times = list()
        for alg in algorithms:
            time = [i for i in d.getchildren() if i.get('alg') == alg][0].get('algRunTime').split(' ')
            if len(time) == 0 or (int(time[0]) < 3 and alg == 'MOHEFT'):
                times.append('N/A')
            else:
                time = [int(i) for i in time]
                times.append((max(time)))

        if times[0] == 0:
            times[0] = 1
        times.append(int(min(times[1:]) / times[0]))

        times = [str(i) for i in times]

        print(' & '.join([model.replace('_', ' ')] + times) + '\\\\', file=f1)
        if models.index(model) % 4 == 3:
            print('\\midrule', file=f1)


def get_hv_csv():
    f1 = open('../results/combined/hv.csv', 'w+')
    data = get_data_by_tag("hypervolumes")
    models = [i.get('name') for i in data.getchildren()]
    models = sorted(models, key=lambda n: (n.split('_')[0], int(n.split('_')[1])))

    header = ['model'] + algorithms
    print(','.join(header), file=f1)

    for model in models:
        d = [i for i in data.getchildren() if i.get('name') == model][0]
        times = list()
        for alg in algorithms:
            time = [i for i in d.getchildren() if i.get('alg') == alg][0].get('hypervolume').split(' ')
            if len(time) == 0 or (float(time[0]) < 0.2 and alg == 'MOHEFT'):
                times.append('N/A')
            else:
                time = [float(i) for i in time]
                times.append(str(median(time)))
        print(','.join([model] + times), file=f1)


def get_spread_csv():
    f1 = open('../results/combined/spread.csv', 'w+')
    data = get_data_by_tag("spreads")
    models = [i.get('name') for i in data.getchildren()]
    models = sorted(models, key=lambda n: (n.split('_')[0], int(n.split('_')[1])))

    header = ['model'] + algorithms
    print(','.join(header), file=f1)

    for model in models:
        d = [i for i in data.getchildren() if i.get('name') == model][0]
        times = list()
        for alg in algorithms:
            time = [i for i in d.getchildren() if i.get('alg') == alg][0].get('spread').split(' ')
            if len(time) == 0 or (float(time[0]) < 0.1 and alg == 'MOHEFT'):
                times.append('N/A')
            else:
                time = [float(i) for i in time]
                times.append(str(median(time)))
        print(','.join([model] + times), file=f1)


def get_igd_csv():
    f1 = open('../results/combined/igd.csv', 'w+')
    data = get_data_by_tag("igds")
    models = [i.get('name') for i in data.getchildren()]
    models = sorted(models, key=lambda n: (n.split('_')[0], int(n.split('_')[1])))

    header = ['model'] + algorithms
    print(','.join(header), file=f1)

    for model in models:
        d = [i for i in data.getchildren() if i.get('name') == model][0]
        times = list()
        for alg in algorithms:
            time = [i for i in d.getchildren() if i.get('alg') == alg][0].get('igd').split(' ')
            if len(time) == 0 or (float(time[0]) > 1 and alg == 'MOHEFT'):
                times.append('N/A')
            else:
                time = [float(i) for i in time]
                times.append(str(median(time)))
        print(','.join([model] + times), file=f1)


def rq2_in_latex():
    hv_data = get_data_by_tag("hypervolumes")
    spread_data = get_data_by_tag("spreads")
    igd_data = get_data_by_tag("igds")

    models = [i.get('name') for i in hv_data.getchildren()]
    models = sorted(models, key=lambda n: (modelref.index(n.split('_')[0]), int(n.split('_')[1])))

    for model in models:
        # hvs
        d = [i for i in hv_data.getchildren() if i.get('name') == model][0]
        hvr = list()
        for alg in algorithms:
            hvx = [i for i in d.getchildren() if i.get('alg') == alg][0].get('hypervolume').split(' ')
            hvx = [float(i) for i in hvx]
            hvr.append(median(hvx))
        if hvr[-1] < 0.1:
            hvr[-1] = 0.1
        # tmp_h = [str(round(i / hvr[-1], 2)) for i in hvr[:-1]]
        # hvr = tmp_h
        hvr = [str(round(i, 2)) for i in hvr]

        # spreads
        d = [i for i in spread_data.getchildren() if i.get('name') == model][0]
        spr = list()
        for alg in algorithms:
            spx = [i for i in d.getchildren() if i.get('alg') == alg][0].get('spread').split(' ')
            spx = [float(i) for i in spx]
            spr.append(median(spx))

        # tmp_s = [str(round(i / spr[-1], 2)) for i in spr[:-1]]
        # spr = tmp_s
        spr = [str(round(i, 2)) for i in spr]

        # igds
        d = [i for i in igd_data.getchildren() if i.get('name') == model][0]
        ir = list()
        for alg in algorithms:
            ix = [i for i in d.getchildren() if i.get('alg') == alg][0].get('igd').split(' ')
            ix = [float(i) for i in ix]
            ir.append(median(ix))

        # tmp_i = [str(round(i / ir[-1], 2)) for i in ir[:-1]]
        # ir = tmp_i
        ir = [str(round(i, 2)) for i in ir]

        printseq = [model.replace('_', ' '), hvr[0], '/'.join(hvr[1:4]), hvr[4], spr[0], '/'.join(spr[1:4]), spr[4],
                    ir[0],
                    '/'.join(ir[1:4]), ir[4]]

        print(' & '.join(printseq) + '\\\\')
        if models.index(model) % 4 == 3:
            print('\\midrule')


if __name__ == '__main__':
    # get_runtime_csv()
    rq2_in_latex()
