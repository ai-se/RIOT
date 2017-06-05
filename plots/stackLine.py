from __future__ import division
from matplotlib import gridspec
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import csv
import pdb


def loadCSV(filename):
    """
    it works for ga/aco/sa at this time
    :param filename:
    :return:
    """
    # note: don't use pd.read_csv for row preserving...
    ALG = {}
    for model in models:
        ALG[model] = list()
    with open(filename, 'rb') as csvfile:
        sareader = csv.reader(csvfile, delimiter=',')
        lstmodel = ''
        for row in sareader:
            if row[0] == '*==...':
                ALG[lstmodel].append(list())
                continue
            if row[0] == lstmodel:
                ALG[lstmodel][-1].append(float(row[2]))
                continue
            if row[0] not in models:
                continue
            if row[0] != lstmodel:
                lstmodel = row[0]
                ALG[lstmodel].append(list())
                ALG[lstmodel][-1].append(float(row[2]))

    for model in models:
        ALG[model] = [c for c in ALG[model] if len(c) > 0]
    return ALG


minminrec = pd.read_csv('../results/minmin.csv', names=['dataset', 'makespan', 'configuration'])
minmaxrec = pd.read_csv('../results/minmax.csv', names=['dataset', 'makespan', 'configuration'])
models = ["fmri", "eprotein", "j30_1", "j30_2", "j60_1", "j60_2",
          "j90_1", "j90_2", "j120_1", "j120_2"]
# models = ['fmri']
alg_name = ['SA', 'GA', 'ACO']
colors = ['green', 'blue', 'red']

flatten = lambda l: [item for sublist in l for item in sublist]

SA = loadCSV('../results/sa.csv')
GA = loadCSV('../results/ga.csv')
ACO = loadCSV('../results/aco.csv')
# pdb.set_trace()

for model in models:
    fig = plt.figure(1, figsize=(10, 3.5))
    gs = gridspec.GridSpec(1, 3, width_ratios=[1.5, 7.5, 1.5])
    axes = [plt.subplot(gs[i]) for i in range(3)]

    # getting all datas
    minmin = minminrec[(minminrec['dataset'] == model)]['makespan'].tolist()[0]
    minmax = minmaxrec[(minmaxrec['dataset'] == model)]['makespan'].tolist()[0]

    INIT = list()
    FINAL = list()

    for i, name, col, alg in zip(range(3), alg_name, colors, [SA, GA, ACO]):
        inits = [c[0] for c in alg[model]]
        finals = [c[-1] for c in alg[model]]
        INIT.append(inits)
        FINAL.append(finals)

        medi = np.argsort(finals)[len(finals) // 2]
        makespans = alg[model][medi]
        iters = range(len(makespans))

        axes[1].plot(iters, makespans, color=col, label=name)

    axes[1].axhline(y=minmin, color='black', linestyle='--', label='MIN_MIN')
    axes[1].axhline(y=minmax, color='black', label='MIN_MAX')

    bplot1 = axes[0].boxplot(INIT, patch_artist=True, showfliers=False, widths=0.5, labels=alg_name)
    bplot2 = axes[2].boxplot(FINAL, patch_artist=True, showfliers=False, widths=0.5, labels=alg_name)

    for (c, b1, b2) in zip(colors, axes[0].xaxis.get_ticklabels(), axes[2].xaxis.get_ticklabels()):
        b1.set_color(c)
        b2.set_color(c)

    for bplot in (bplot1, bplot2):
        for patch, color in zip(bplot['boxes'], colors):
            patch.set_facecolor(color)

    for ax in axes:
        ax.set_ylim([min(flatten(FINAL)) * 0.9, max(flatten(INIT)) * 1.0])
        ax.yaxis.grid(True)
    axes[1].set_yticklabels([])
    axes[2].yaxis.tick_right()
    axes[1].legend(loc=1)
    axes[1].set_xlim([-25, 500])

    axes[0].set_title('Init objective distribution\n(30 runs)', fontsize=8)
    axes[1].set_title(model + '  Convergence analysis', fontsize=12)
    axes[2].set_title('Final objective distribution\n(30 runs)', fontsize=8)

    axes[0].set_ylabel('Makespans')

    gs.tight_layout(fig)
    # plt.show()
    plt.savefig('../results/summary/' + model + '.png')
    plt.savefig('../results/summary/' + model + '.eps')
