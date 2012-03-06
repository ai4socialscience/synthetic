#!/usr/bin/env python
# encoding: utf-8

__author__ = "Telmo Menezes (telmo@telmomenezes.com)"
__date__ = "Oct 2011"


import random
import math
import sys
from syn.core import *
from syn.drmap import *


class Evo:
    def __init__(self, targ_net, max_edges=10000, map_limit=7.0, bins=10):
        self.targ_net = targ_net
        self.population = []

        self.syn_net = targ_net.load_net()
        self.nodes = net_node_count(self.syn_net)
        self.edges = net_edge_count(self.syn_net) 

        # check if max edges is reached
        if self.edges > max_edges:
            self.edges = max_edges
            
            # node to edge ratio
            ne_ratio = float(net_node_count(self.syn_net)) / float(net_edge_count(self.syn_net))
            
            self.nodes = int(float(max_edges) * ne_ratio)
        
        self.map_limit = map_limit
        self.bins = bins

        seed_random()

        compute_pageranks(self.syn_net)
        self.targ_drmap = get_drmap_with_limits(self.syn_net, self.bins, -self.map_limit, self.map_limit, -self.map_limit, self.map_limit)
        drmap_log_scale(self.targ_drmap)
        drmap_normalize_max(self.targ_drmap)

        draw_drmap(self.syn_net, 'target.png', bins=self.bins, limit=self.map_limit)

    def __del__(self):
        destroy_drmap(self.targ_drmap)
        for p in self.population:
            destroy_gpgenerator(p)
        destroy_net(self.syn_net)