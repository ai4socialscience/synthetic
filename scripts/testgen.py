#!/usr/bin/env python
# encoding: utf-8

__author__ = "Telmo Menezes (telmo@telmomenezes.com)"
__date__ = "Nov 2011"


from syn.net import Net
from syn.core import *
import sys


if __name__ == '__main__':
    seed_random()

    gen = create_gpgenerator()
    load_gpgen(gen, sys.argv[1])

    net = Net(sys.argv[2]).load_net()
    nodes = net_node_count(net)
    edges = net_edge_count(net)
    max_cycles = edges * 100
    map_limit = 7.0
    bins = 10

    compute_pageranks(net)
    drmap1 = get_drmap_with_limits(net, bins, -map_limit, map_limit, -map_limit, map_limit)
    drmap_log_scale(drmap1)
    drmap_normalize_max(drmap1)

    syn_net = gpgen_run(gen, nodes, edges)

    compute_pageranks(syn_net)

    drmap2 = get_drmap_with_limits(syn_net, bins, -map_limit, map_limit, -map_limit, map_limit)
    drmap_log_scale(drmap2)
    drmap_normalize_max(drmap2)

    fit = drmap_emd_dist(drmap1, drmap2)

    print fit

    destroy_drmap(drmap1)
    destroy_drmap(drmap2)