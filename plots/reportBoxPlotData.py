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
from pareto import eps_sort
from lxml import etree
import sys
import glob
import pdb

"""
Combining Pareto frontier
Reporting igd, spread, hypervolume, runtime
"""

frontier_DS = ['../results/', ]  # '../results/arxivR/07-30-17/'


class Exp:
    def __init__(self, alg, model, runtime):
        self.time = runtime
        if model.startswith('sci_'):  # removing the tag
            model = model[4:]
        self.model = model
        self.alg = alg
        self.objs = []

    def add_obj(self, obj1, obj2):
        self.objs.append([obj1, obj2])

    def __str__(self):
        return self.model + " " + str(len(self.objs))


def update_true_frontier():
    AllExps = list()

    files = list()
    for i in frontier_DS:
        files.extend(glob.glob(i + "*.txt"))

    for fileN in files:
        with open(fileN, 'r') as f:
            content = f.read()
            for line in content.split('\n'):
                if line.startswith('#'):
                    if newe:
                        AllExps.append(newe)
                    continue
                if line.startswith('*'):
                    d = line.split(' ')
                    newe = Exp('universal', d[1], int(d[2]))
                    continue
                if len(line) == 0:
                    continue
                os = line.split(' ')
                newe.add_obj(float(os[0]), float(os[1]))

    models = set()
    for d in AllExps:
        models.add(d.model)
    models = list(models)

    # calculating the frontier <two objectives. both to minimize>
    root = etree.Element('frontiers')

    for model in models:
        exps = filter(lambda i: i.model == model, AllExps)
        objs = list()
        for exp in exps:
            objs.extend(exp.objs)
        frontiers = eps_sort(objs)

        child = etree.Element("model")
        child.set("name", model)

        for f in frontiers:
            cchild = etree.Element("objs")
            cchild.set("makespan", str(f[0]))
            cchild.set("cost", str(f[1]))
            child.append(cchild)

        root.append(child)

    with open('../results/FRONTIER.xml', 'w') as f:
        f.write(etree.tostring(root, pretty_print=True))

    print("Frontiers Recreated!", file=sys.stderr)

if __name__ == '__main__':
    update_true_frontier()
