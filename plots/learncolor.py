import matplotlib.pyplot as plt
from mpl_toolkits.axes_grid1 import make_axes_locatable
import numpy as np

fig = plt.figure()
ax = plt.gca()
# im = ax.imshow(np.arange(100).reshape((10,10)), aspect=0.5)

# create an axes on the right side of ax. The width of cax will be 5%
# of ax and the padding between cax and ax will be fixed at 0.05 inch.
# divider = make_axes_locatable(ax)
# cax = divider.append_axes("right", size="5%", pad=0.05, aspect=10)
#cax = fig.add_axes([0.85, 0.3, 0.04, 0.4])
# plt.colorbar(im, cax=cax)

plt.show()