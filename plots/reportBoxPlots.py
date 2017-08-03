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

"""
Reporting igd, spread, hypervolume, runtime
"""

from __future__ import division, print_function

import math
import sys
import xml.etree.ElementTree as ET
import pdb
from lxml import etree

from assPkg.hv import HyperVolume
from utils import read_all_data

AllExps = read_all_data()
models = set([i.model for i in AllExps])

algorithms = ['SWAY', 'EMSC-NSGAII', 'EMSC-SPEA2', 'EMSC-MOEA/D', 'MOHEFT']  # pls put MOHEFT at the end


def format_obj_list(objs):
    # return objs
    return [[math.log10(i[0]), i[1]] for i in objs]


def get_true_pf(model):
    tree = ET.parse('../results/combined/trues.xml')
    subtree = filter(lambda t: t.get('name') == model, tree.getroot().getchildren())[0]
    res = list()
    for e in subtree.getchildren():
        res.append([float(e.get('makespan')), float(e.get('cost'))])

    return format_obj_list(res)


def normalization_list(truePF, toNormSingleObj):
    minO1, maxO1 = min(zip(*truePF)[0]) * 0.9, max(zip(*truePF)[0]) * 1.1
    minO2, maxO2 = min(zip(*truePF)[1]) * 0.9, max(zip(*truePF)[1]) * 1.1

    o1 = toNormSingleObj[0]
    o2 = toNormSingleObj[1]
    return [(o1 - minO1) / (maxO1 - minO1), (o2 - minO2) / (maxO2 - minO2)]


def euc_dist(sol1, sol2):
    sums = 0
    for i, j in zip(sol1, sol2):
        sums += (i - j) ** 2
    return math.sqrt(sums)


def get_spread(exps, modelName):
    res = list()
    for O in exps:
        objs = O.objs
        ''' Calc spread in a single experiment '''
        objs = sorted(objs, key=lambda i: i[0])
        objs = format_obj_list(objs)

        truePF = get_true_pf(modelName)
        truePF.sort(key=lambda i: i[0])  # sort by first objective

        dl, df = truePF[0], truePF[-1]

        # normalization
        dl = normalization_list(truePF, dl)
        df = normalization_list(truePF, df)
        objs = map(lambda i: normalization_list(truePF, i), objs)

        if len(objs) < 3:  # cant calc
            res.append(-1)
            continue

        dfcs = list()
        for i, j in zip(objs[:-1], objs[1:]):
            dfcs.append(euc_dist(i, j))

        dfc_avg = sum(dfcs) / len(dfcs)

        nominator = euc_dist(dl, objs[0]) + euc_dist(df, objs[-1]) + sum([abs(i - dfc_avg) for i in dfcs])
        denominator = euc_dist(dl, objs[0]) + euc_dist(df, objs[-1]) + sum(dfcs)

        res.append(nominator / denominator)

    print("Spread for " + modelName + " checked!", file=sys.stderr)
    return res


def get_hv(exps, modelName):
    res = list()
    for O in exps:
        objs = O.objs
        ''' Calc hyperVolume in a single experiment '''
        objs = sorted(objs, key=lambda i: i[0])
        objs = format_obj_list(objs)

        truePF = get_true_pf(modelName)
        truePF.sort(key=lambda i: i[0])  # sort by first objective

        # normalization
        objs = map(lambda i: normalization_list(truePF, i), objs)

        hv = HyperVolume([1, 1])
        volume = hv.compute(objs)
        res.append(volume)

    print("Hypervolume for " + modelName + " checked!", file=sys.stderr)
    return res


def get_igd(exps, modelName):
    def igd_metric(front, true_front):
        dimension = 2
        distances = []
        for opt_ind in true_front:
            distances.append(float("inf"))
            for ind in front:
                dist = euc_dist(ind, opt_ind)
                if dist < distances[-1]:
                    distances[-1] = dist

        return sum(distances) / len(distances)

    res = list()
    for O in exps:
        objs = O.objs
        ''' Calc igd in a single experiment '''
        objs = sorted(objs, key=lambda i: i[0])
        objs = format_obj_list(objs)

        truePF = get_true_pf(modelName)
        truePF.sort(key=lambda i: i[0])  # sort by first objective

        # normalization
        objs = map(lambda i: normalization_list(truePF, i), objs)
        trues = map(lambda i: normalization_list(truePF, i), truePF)

        res.append(igd_metric(objs, trues))

    print("IGD for " + modelName + " checked!", file=sys.stderr)
    return res


def report():
    reports = etree.Element('reports')

    """ algorithm_run_time"""

    runtimes = etree.Element('runtimes')
    for model in models:
        child = etree.Element("model")
        child.set("name", model)

        for alg in algorithms:
            exps = filter(lambda i: i.model == model and i.alg == alg, AllExps)
            times = [str(i.time) for i in exps]

            cchild = etree.Element("sec")
            cchild.set("alg", alg)
            cchild.set("algRunTime", " ".join(times))

            child.append(cchild)

        runtimes.append(child)

    reports.append(runtimes)

    """ spread """
    spreads = etree.Element('spreads')
    for model in models:
        child = etree.Element("model")
        child.set("name", model)

        for alg in algorithms:
            exps = filter(lambda i: i.model == model and i.alg == alg, AllExps)
            ss = get_spread(exps, model)
            ss = ['%.3f' % i for i in ss]

            cchild = etree.Element("sec")
            cchild.set("alg", alg)
            cchild.set("spread", " ".join(ss))

            child.append(cchild)

            spreads.append(child)

    reports.append(spreads)

    """ igd """
    igds = etree.Element('igds')
    for model in models:
        child = etree.Element("model")
        child.set("name", model)

        for alg in algorithms:
            exps = filter(lambda i: i.model == model and i.alg == alg, AllExps)
            ss = get_igd(exps, model)
            ss = ['%.3f' % i for i in ss]

            cchild = etree.Element("sec")
            cchild.set("alg", alg)
            cchild.set("igd", " ".join(ss))

            child.append(cchild)

            igds.append(child)

    reports.append(igds)

    """ hypervolume """
    hvs = etree.Element('hypervolumes')
    for model in models:
        child = etree.Element("model")
        child.set("name", model)

        for alg in algorithms:
            exps = filter(lambda i: i.model == model and i.alg == alg, AllExps)
            ss = get_hv(exps, model)
            ss = ['%.3f' % i for i in ss]

            cchild = etree.Element("sec")
            cchild.set("alg", alg)
            cchild.set("hypervolume", " ".join(ss))

            child.append(cchild)

            hvs.append(child)

    reports.append(hvs)

    with open('../results/combined/reports.xml', 'w') as f:
        f.write(etree.tostring(reports, pretty_print=True))


if __name__ == '__main__':
    report()
