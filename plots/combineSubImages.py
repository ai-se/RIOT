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

import glob
import pdb
from PIL import Image

# TODO SETTING HERE...
folder = '../results/objCmprImg/'
saveat = '../results/combined/objCmpr.png'

ifs = glob.glob(folder + '*.png')
ifs = [i for i in ifs if 'combine' not in i]
images = map(Image.open, ifs)
widths, heights = zip(*(i.size for i in images))

# fetch all models
models = set()
ifiles = list()
for i in ifs:
    t = i[len(folder):-4]  # name
    ifiles.append(t)
    if t.split('_')[0] not in models:
        models.add(t.split('_')[0])

new_im = Image.new('RGB', ((max(widths) + 5) * 4, (max(heights) + 10) * 5))  # TODO ATTENTION: grid size set here

x_offset = 0
y_offset = 0

for m in models:
    ENT = list()

    for im, ifile in zip(images, ifiles):
        if m not in ifile:
            continue
        ENT.append((im, ifile, int(ifile.split('_')[1])))
    ENT.sort(key=lambda i: i[2])

    for im, ifile, nn in ENT:
        new_im.paste(im, (x_offset, y_offset))
        x_offset += im.size[0] + 5

    y_offset += im.size[1] + 10
    x_offset = 0

new_im.save(saveat)
