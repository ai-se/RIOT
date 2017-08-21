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


from __future__ import division
import matplotlib.pyplot as plt
import xml.etree.ElementTree as ET
from matplotlib.path import Path
import matplotlib.patches as patches
import numpy
import math
import random
import matplotlib.lines as lines
import sys
import pdb
import debug

from utils import read_all_data

AllExps = read_all_data()
models = set([i.model for i in AllExps])
modelref = ['Montage', 'Epigenomics', 'Inspiral', 'CyberShake', 'Sipht']
models = sorted(models, key=lambda n: (modelref.index(n.split('_')[0]), int(n.split('_')[1])))

markers = ['pentagon*', '10-pointed star', 'square*', 'triangle*', 'diamond*']


def jitter(x, y, drawed):
    for p in drawed:
        a = p[0]
        b = p[1]
        if abs(math.log10(x) - math.log10(a)) < 0.3 and abs(math.log10(y) - math.log10(b)) < 0.3:
            return math.pow(10, (math.log10(a) + random.uniform(0.2, 0.5))), math.pow(10, (math.log10(b) + random.uniform(0.2, 0.5)))
    return x, y


def get_true_pf(model):
    tree = ET.parse('../results/combined/trues.xml')
    subtree = filter(lambda t: t.get('name') == model, tree.getroot().getchildren())[0]
    res = list()
    for e in subtree.getchildren():
        res.append([float(e.get('makespan')), float(e.get('cost'))])

    return res


def median(lst):
    return numpy.median(numpy.array(lst))


def EOverhead(algorithm, color, marker, nn):
    WT = list()
    OH = list()

    for model in models:
        tmp = filter(lambda i: i.model == model and algorithm in i.alg, AllExps)
        for O in tmp[:1]:
            objs = sorted(O.objs, key=lambda i: i[0])

            tf = get_true_pf(model)
            tfc = [i[1] for i in tf]
            tfc = sorted(tfc)
            cursor = tfc[-1] * 0.5

            for o in objs:
                if o[1] > cursor:
                    break

            makespan = o[0]
            overhead = max(O.time, 2)
            n = int(model.split('_')[1])
            # if n < 31:
            #     size = 1.5
            # elif n < 61:
            #     size = 2.0
            # elif n < 101:
            #     size = 2.3
            # else:
            #     size = 2.7
            # marker = markers[modelref.index(model.split('_')[0])]

            print("%s %s  runtime: %d vs. optimization time: % d" % (nn, model, int(makespan), int (overhead)))

            makespan, overhead = jitter(makespan, overhead, drawed)
            drawed.append((makespan, overhead))
            # print("\\addplot[color=%s, mark=%s, mark options={mark size=%.2fpt}, style={solid, fill=%s!50}] coordinates{(%d, %d)};" % (
            #     color, marker, 2.1, color, int(makespan), int(overhead)))
            WT.append(makespan)
            OH.append(overhead)

    return WT, OH

drawed =list()

WT1, OH1 = EOverhead('SWAY', 'blue', '*', 'This paper')
WT2, OH2 = EOverhead('SANITY', 'red', '*', 'Random')
WT3, OH3 = EOverhead('NSGA', 'black', '*', 'Previous')

pdb.set_trace()
# WT2, OH2 = EOverhead('SWAY')
# plt.clf()
# ax = plt.gca()
# ax.plot(WT1, OH1, 'o', label="Stae-of-the-art", color='black')
# ax.plot(WT2, OH2, 'x', label="Our method", color='black')
# ax.set_xscale('log')
# ax.set_yscale('log')
# ax.set_xlabel('Runtime w/ median cost')
# ax.set_ylabel('Overhead')
# plt.plot([0,10000], [0, 1000])
# # plt.legend(loc=2)
#
# plt.show()
