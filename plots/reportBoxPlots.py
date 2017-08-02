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

from __future__ import division
from utils import read_all_data
from lxml import etree

AllExps = read_all_data()
models = set([i.model for i in AllExps])
algorithms = ['SWAY', 'EMSC-NSGAII', 'EMSC-SPEA2', 'EMSC-MOEA/D', 'MOHEFT']  # pls put MOHEFT at the end


def get_spread(exps):
    return [12.11, 23, 2.2, 0.122]


def get_hv(exps):
    return [2, 23, 2.2, 0.122]


def get_igd(exps):
    return [1, 1, 1]


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
            ss = get_spread(exps)
            ss = [str(i) for i in ss]

            cchild = etree.Element("sec")
            cchild.set("alg", alg)
            cchild.set("algRunTime", " ".join(ss))

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
            ss = get_igd(exps)
            ss = [str(i) for i in ss]

            cchild = etree.Element("sec")
            cchild.set("alg", alg)
            cchild.set("algRunTime", " ".join(ss))

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
            ss = get_hv(exps)
            ss = [str(i) for i in ss]

            cchild = etree.Element("sec")
            cchild.set("alg", alg)
            cchild.set("algRunTime", " ".join(ss))

            child.append(cchild)

            hvs.append(child)

    reports.append(hvs)
    with open('../results/combined/reports.xml', 'w') as f:
        f.write(etree.tostring(reports, pretty_print=True))


if __name__ == '__main__':
    report()
