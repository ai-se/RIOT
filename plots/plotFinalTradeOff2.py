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
from utils import read_all_data
import matplotlib.pyplot as plt
import pylab
import numpy as np
import matplotlib
import math
import pdb


def jitter(x, y, drawed):
    for p in drawed:
        a = p[0]
        b = p[1]
        if abs(math.log10(x) - math.log10(a)) < 0.01 and abs(math.log10(y) - math.log10(b)) < 0.01:
            return math.pow(10, (math.log10(a) + 0.01)), math.pow(10, (math.log10(b) + 0.01))
    return x, y


AllExps = read_all_data()
models = set([i.model for i in AllExps])
algs = ['HC', 'SA', 'RIOT']
colors = ['blue', 'darkmagenta', 'darkgreen']  # https://matplotlib.org/examples/color/named_colors.html
markers = ['x', 'p', '^', 'd']  # https://matplotlib.org/examples/lines_bars_and_markers/marker_reference.html

fontdict = {'weight': 'normal', 'size': 10}
matplotlib.rc('font', **fontdict)

for model in models:
    plt.clf()
    plt.figure(figsize=(2.4, 2.4))
    ax = plt.gca()
    drawed = list()
    ymax = -1

    for alg, color, mk in zip(algs, colors, markers):
        points = filter(lambda exp: exp.alg == alg and exp.model == model, AllExps)
        for repeat in points:
            repeat.objs.sort(key=lambda i: i[0])
            x = [t[0] for t in repeat.objs]
            y = [t[1] for t in repeat.objs]

            # checking each x, y. jitter if necessary. avoid overwrite
            for i in range(len(x)):
                m, n = jitter(x[i], y[i], drawed)
                x[i] = m
                y[i] = n
                drawed.append((m, n))

        tmp0 = zip(*drawed)[0]
        tmp0 = tmp0[:len(tmp0) - len(x)]
        currentM = max(tmp0) if tmp0 else -1

        tmp1 = zip(*drawed)[1]
        tmp1 = tmp1[:len(tmp1) - len(y)]
        currentN = max(tmp1) if tmp1 else -1
        ax.plot(x, y, c=color, label=alg, marker=mk, markersize=4.0, ls='--', linewidth=1.0, alpha=0.8)

        if alg != "MOHEFT":
            ymax = max(ymax, max(y))

    ax.set_xscale("log")
    ax.get_xaxis().get_major_formatter().labelOnlyBase = False

    # ax.set_xlabel('makespan')
    # ax.set_ylabel('cost')
    ax.set_aspect(1.5)
    ax.set_ylim([-ymax * 0.1, ymax * 1.1])
    # major_ticks = ax.get_yticks()[::2]
    # major_ticks[0] = 0
    # ax.set_yticks(major_ticks)
    plt.legend(loc=0)
    plt.title(model.replace('_', ' '), fontdict=fontdict)
    plt.tight_layout()
    # plt.show()
    plt.savefig('../results/tradeOffPF2/' + model + '.png')

