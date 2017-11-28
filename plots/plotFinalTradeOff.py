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
algs = ['RIOT', 'EMSC-NSGAII', 'EMSC-SPEA2', 'EMSC-MOEA/D', 'HC']  # pls put MOHEFT at the end
colors = ['darkgreen', 'red', 'blue', 'darkmagenta',
          'orange']  # https://matplotlib.org/examples/color/named_colors.html
markers = ['d', 's', 'x', 'p', '^']  # https://matplotlib.org/examples/lines_bars_and_markers/marker_reference.html

for model in models:
    plt.clf()
    ax = plt.gca()
    drawed = list()

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

        if alg != 'MOHEFT' or not x or np.mean(x) <= math.log10(
                currentM):  # dont show if extremely bad or no results in MOHEFT
            ax.plot(x, y, c=color, label=alg if alg != 'RIOT' else 'Our method', marker=mk, ls='--', linewidth=0.8)
            if not x and alg == 'MOHEFT':
                plt.text((currentM) * 0.8, currentN * 0.8, 'Model too large for MOHEFT', fontsize=9,
                         color='gray')
        else:
            ax.plot([], [], c=color, label=alg, marker=mk, ls='--', linewidth=0.8)
            plt.text(math.log10(currentM) * 0.8, currentN * 0.8, 'MOHEFT out of bound', fontsize=9, color='gray')

        ax.set_xscale("log")
        ax.get_xaxis().get_major_formatter().labelOnlyBase = False

        ax.set_xlabel('makespan')
        ax.set_ylabel('cost')
        ax.set_aspect(1.5)

        plt.legend(loc=0)
        plt.title(model)
        # plt.show()
        plt.savefig('../results/tradeOffPF/' + model + '.png')
