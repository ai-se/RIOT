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
import pandas as pd
import matplotlib.pyplot as plt
import pdb

records = pd.read_csv('../results/exp1_random.csv', names=['dataset', 'makespan', 'configuration'])
minminrec = pd.read_csv('../results/minmin.csv', names=['dataset', 'makespan', 'configuration'])
minmaxrec = pd.read_csv('../results/minmax.csv', names=['dataset', 'makespan', 'configuration'])

models = set(records['dataset'])

for model in models:
    plt.clf()
    filtered = records[(records['dataset'] == model)]
    makespans = filtered['makespan'].tolist()
    minmin = minminrec[(minminrec['dataset'] == model)]['makespan'].tolist()[0]
    minmax = minmaxrec[(minmaxrec['dataset'] == model)]['makespan'].tolist()[0]

    plt.hist(makespans, bins=5, rwidth=0.8, color='black')
    plt.xlabel('makespan')
    plt.ylabel('frequency')
    plt.title(model + ' :: no optimization')
    plt.axvline(x=minmin, color='skyblue', label='MIN_MIN', linewidth=3)
    plt.axvline(x=minmax, color='tomato', label='MIN_MAX', linewidth=3)
    plt.legend(loc=0)

    plt.savefig('../results/static/'+model+'.png')
    # plt.show()