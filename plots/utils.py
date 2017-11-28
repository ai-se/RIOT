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


def read_all_data():
    currentExpDatas = list()
    newe = None

    # Read all NSGAII results
    with open("../results/emsc-nsgaii.txt", 'r') as f:
        content = f.read()
        for line in content.split('\n'):
            if line.startswith('#'):
                if newe:
                    currentExpDatas.append(newe)
                continue
            if line.startswith('*'):
                d = line.split(' ')
                newe = Exp('EMSC-NSGAII', d[1], int(d[2]))
                continue
            if len(line) == 0:
                continue
            os = line.split(' ')
            newe.add_obj(float(os[0]), float(os[1]))

    # Read all SPEA2 results
    with open("../results/emsc-spea2.txt", 'r') as f:
        content = f.read()
        for line in content.split('\n'):
            if line.startswith('#'):
                if newe:
                    currentExpDatas.append(newe)
                continue
            if line.startswith('*'):
                d = line.split(' ')
                newe = Exp('EMSC-SPEA2', d[1], int(d[2]))
                continue
            if len(line) == 0:
                continue
            os = line.split(' ')
            newe.add_obj(float(os[0]), float(os[1]))

    # Read all MOEAD results
    with open("../results/emsc-moead.txt", 'r') as f:
        content = f.read()
        for line in content.split('\n'):
            if line.startswith('#'):
                if newe:
                    currentExpDatas.append(newe)
                continue
            if line.startswith('*'):
                d = line.split(' ')
                newe = Exp('EMSC-MOEA/D', d[1], int(d[2]))
                continue
            if len(line) == 0:
                continue
            os = line.split(' ')
            newe.add_obj(float(os[0]), float(os[1]))

    # # Read all MOHEFT results
    # with open("../results/moheft.txt", 'r') as f:
    #     content = f.read()
    #     for line in content.split('\n'):
    #         if line.startswith('#'):
    #             if newe:
    #                 currentExpDatas.append(newe)
    #             continue
    #         if line.startswith('*'):
    #             d = line.split(' ')
    #             newe = Exp('MOHEFT', d[1], int(d[2]))
    #             continue
    #         if len(line) == 0:
    #             continue
    #         os = line.split(' ')
    #         newe.add_obj(float(os[0]), float(os[1]))

    # Read all RIOT results
    with open("../results/riot.txt", 'r') as f:
        content = f.read()
        for line in content.split('\n'):
            if line.startswith('#'):
                if newe:
                    currentExpDatas.append(newe)
                continue
            if line.startswith('*'):
                d = line.split(' ')
                newe = Exp('RIOT', d[1], int(d[2]))
                continue
            if len(line) == 0:
                continue
            os = line.split(' ')
            newe.add_obj(float(os[0]), float(os[1]))

    # Read all RIOT-HC results
    with open("../results/riotHc.txt", 'r') as f:
        content = f.read()
        for line in content.split('\n'):
            if line.startswith('#'):
                if newe:
                    currentExpDatas.append(newe)
                continue
            if line.startswith('*'):
                d = line.split(' ')
                newe = Exp('HC', d[1], int(d[2]))
                continue
            if len(line) == 0:
                continue
            os = line.split(' ')
            newe.add_obj(float(os[0]), float(os[1]))

    # # Read all Sanity Check results
    # with open("../results/sanity.txt", 'r') as f:
    #     content = f.read()
    #     for line in content.split('\n'):
    #         if line.startswith('#'):
    #             if newe:
    #                 currentExpDatas.append(newe)
    #             continue
    #         if line.startswith('*'):
    #             d = line.split(' ')
    #             newe = Exp('SANITY', d[1], int(d[2]))
    #             continue
    #         if len(line) == 0:
    #             continue
    #         os = line.split(' ')
    #         newe.add_obj(float(os[0]), float(os[1]))

    return currentExpDatas
