/*
 * Copyright (C) 2010 Telmo Menezes.
 * telmo@telmomenezes.com
 */


#include "network.h"
#include "utils.h"
#include <cmath>
#include <string.h>
#include <stdio.h>
#include <iostream>


using std::isfinite;
using std::cout;
using std::endl;


namespace syn
{

static unsigned int SYN_CURID = 0;

Net::Net()
{
    node_count = 0;
    edge_count = 0;
    nodes = NULL;
    temporal = 0;
    min_ts = 0;
    max_ts = 0;
}


Net::~Net()
{
    Node* node = nodes;
    while (node) {
        Node* next_node = node->next;
        delete node;
        node = next_node;
    }
}


Node* Net::add_node(unsigned int type)
{
    node_count++;
    Node* node = new Node(type, SYN_CURID++);
    node->next = nodes;
    nodes = node;
    return node;
}

Node* Net::add_node_with_id(unsigned int nid, unsigned int type)
{
    node_count++;
    if (nid >= SYN_CURID) {
        SYN_CURID = nid + 1;
    }
    Node* node = new Node(type, nid);
    node->next = nodes;
    nodes = node;
    return node;
}

int Net::add_edge_to_net(Node* orig, Node* targ, unsigned long timestamp)
{
    if (orig->add_edge(targ, timestamp)) {
        edge_count++;

        if (timestamp > 0) {
            temporal = 1;
            if ((min_ts == 0) || (timestamp < min_ts)) {
                min_ts = timestamp;
            }
            if ((max_ts == 0) || (timestamp > max_ts)) {
                max_ts = timestamp;
            }
        }

        return 1;
    }
    return 0;
}

Node* Net::get_random_node()
{
    unsigned int pos = RANDOM_UINT(node_count);
    Node* curnode = nodes;
    for (int i = 0; i < pos; i++) {
        curnode = curnode->next;
    }
    return curnode;
}

DRMap* Net::get_drmap(unsigned int bin_number)
{
    return get_drmap_with_limits(bin_number, min_pr_in, max_pr_in, min_pr_out, max_pr_out);
}

DRMap* Net::get_drmap_with_limits(unsigned int bin_number, double min_val_hor,
                                        double max_val_hor, double min_val_ver, double max_val_ver)
{
    double interval_hor = (max_val_hor - min_val_hor) / ((double)bin_number);
    double interval_ver = (max_val_ver - min_val_ver) / ((double)bin_number);

    DRMap* map = new DRMap(bin_number, min_val_hor - interval_hor,
        max_val_hor, min_val_ver - interval_ver, max_val_ver);

    Node* node = nodes;
    while (node) {
        int x = 0;
        int y = 0;

        if (isfinite(node->get_pr_in())) {
            if (node->get_pr_in() <= min_val_hor) {
                x = 0;
            }
            else if (node->get_pr_in() >= max_val_hor) {
                x = bin_number - 1;
            }
            else {
                x = (unsigned int)floor((node->get_pr_in() - min_val_hor) / interval_hor);
            }
        }
        if (isfinite(node->pr_out)) {
            if (node->get_pr_out() <= min_val_ver) {
                y = 0;
            }
            else if (node->get_pr_out() >= max_val_ver) {
                y = bin_number - 1;
            }
            else {
                y = (unsigned int)floor((node->get_pr_out() - min_val_ver) / interval_ver);
            }
        }

        if ((x >= 0) && (y >= 0) && ((node->get_in_degree() != 0) || (node->get_out_degree() != 0))) {
            map->inc_value(x, y);
        }
        
        node = node->next;
    }

    return map;
}


void Net::compute_pageranks()
{
    // TODO: config
    unsigned int max_iter = 10;
    double drag = 0.999;

    Node* node = nodes;
    while (node) {
        node->pr_in_last = 1;
        node->pr_out_last = 1;
        node = node->next;
    }

    unsigned int i = 0;

    double delta_pr_in = 999;
    double delta_pr_out = 999;
    
    double zero_test = 0.0001;

    //while (((delta_pr_in > zero_test) || (delta_pr_out > zero_test)) && (i < max_iter)) {
    while (i < max_iter) {
        double acc_pr_in = 0;
        double acc_pr_out = 0;

        node = nodes;
        while(node) {
            node->pr_in = 0;
            Edge* origin = node->origins;
            while (origin) {
                node->pr_in += origin->orig->pr_in_last / ((double)origin->orig->out_degree);
                origin = origin->next_orig;
            }
            
            node->pr_in *= drag;
            node->pr_in += (1.0 - drag) / ((double)node_count);
            
            acc_pr_in += node->pr_in;

            node->pr_out = 0;
            Edge* target = node->targets;
            while (target) {
                node->pr_out += target->targ->pr_out_last / ((double)target->targ->in_degree);
                target = target->next_targ;
            }
            
            node->pr_out *= drag;
            node->pr_out += (1.0 - drag) / ((double)node_count);
            
            acc_pr_out += node->pr_out;
            
            node = node->next;
        }
        
        //printf("acc_pr_in: %f; acc_pr_out: %f\n", acc_pr_in, acc_pr_out);

        delta_pr_in = 0;
        delta_pr_out = 0;

        node = nodes;
        while (node) {
            node->pr_in /= acc_pr_in;
            node->pr_out /= acc_pr_out;
            delta_pr_in += fabs(node->pr_in - node->pr_in_last);
            delta_pr_out += fabs(node->pr_out - node->pr_out_last);    
            //printf("pr_in: %f; pr_in_last: %f; delta_pr_in: %f\n", node->pr_in, node->pr_in_last, delta_pr_in);
            //printf("pr_out: %f; pr_out_last: %f; delta_pr_out: %f\n", node->pr_out, node->pr_out_last, delta_pr_out);
            node->pr_in_last = node->pr_in;
            node->pr_out_last = node->pr_out;
            
            node = node->next;
        }

        //printf("delta in: %f; delta out: %f\n", delta_pr_in, delta_pr_out);
        i++;
    }

    // relative pr
    double base_pr = 1.0 / ((double)node_count);
    node = nodes;
    while (node) {
        node->pr_in = node->pr_in / base_pr;
        node->pr_out = node->pr_out / base_pr;
        node = node->next;
    }
    
    // use log scale
    node = nodes;
    while (node) {
        node->pr_in = log(node->pr_in);
        node->pr_out = log(node->pr_out);
        node = node->next;
    }

    // compute min/max EVC in and out
    min_pr_in = 0;
    min_pr_out = 0;
    max_pr_in = 0;
    max_pr_out = 0;
    int first = 1;
    node = nodes;
    while (node) {
        if (isfinite(node->pr_in) && (first || (node->pr_in < min_pr_in))) {
            min_pr_in = node->pr_in;
        }
        if (isfinite(node->pr_out) && (first || (node->pr_out < min_pr_out))) {
            min_pr_out = node->pr_out;
        }
        if (isfinite(node->pr_in) && (first || (node->pr_in > max_pr_in))) {
            max_pr_in = node->pr_in;
        }
        if (isfinite(node->pr_out) && (first || (node->pr_out > max_pr_out))) {
            max_pr_out = node->pr_out;
        }

        first = 0;
        
        node = node->next;
    }
}


void Net::write_pageranks(const char *file_path)
{
    FILE *f = fopen(file_path, "w");
   
    fprintf(f, "id, pr_in, pr_out, in_degree, out_degree\n");

    Node* node = nodes;
    while (node) {
        fprintf(f, "%d,%.10f,%.10f,%d,%d\n", node->id, node->pr_in, node->pr_out, node->in_degree, node->out_degree);
        node = node->next;
    }

    fclose(f);
}

void Net::print_net_info()
{
    printf("node number: %d\n", node_count);
    printf("edge number: %d\n", edge_count);
    printf("log(pr_in): [%f, %f]\n", min_pr_in, max_pr_in);
    printf("log(pr_out): [%f, %f]\n", min_pr_out, max_pr_out);
}

int Net::triad_type(Node* a, Node* b, Node* c)
{
    int type = -1;

    bool ab = a->edge_exists(b);
    bool ac = a->edge_exists(c);
    bool ba = b->edge_exists(a);
    bool bc = b->edge_exists(c);
    bool ca = c->edge_exists(a);
    bool cb = c->edge_exists(b);

    if      (ab && ac && !ba && !bc && !ca && !cb) type = 1;
    else if (!ab && !ac && ba && !bc && ca && !cb) type = 2;
    else if (!ab && !ac && !ba && bc && ca && !cb) type = 3;
    else if (!ab && ac && ba && !bc && ca && !cb)  type = 4;
    else if (ab && ac && ba && !bc && !ca && !cb)  type = 5;
    else if (ab && ac && ba && !bc && ca && !cb)   type = 6;
    else if (ab && ac && !ba && bc && !ca && !cb)  type = 7;
    else if (!ab && ac && ba && !bc && !ca && cb)  type = 8;
    else if (ab && ac && !ba && bc && !ca && cb)   type = 9;
    else if (!ab && !ac && ba && bc && ca && cb)   type = 10;
    else if (ab && ac && !ba && bc && ca && !cb)   type = 11;
    else if (!ab && ac && ba && bc && ca && cb)    type = 12;
    else if (ab && ac && ba && bc && ca && cb)     type = 13;

    return type;
}

void Net::update_triad_profile(Node** triad, unsigned long* profile)
{
    int type = triad_type(triad[0], triad[1], triad[2]);
    if (type < 0) type = triad_type(triad[0], triad[2], triad[1]);
    if (type < 0) type = triad_type(triad[1], triad[0], triad[2]);
    if (type < 0) type = triad_type(triad[1], triad[2], triad[0]);
    if (type < 0) type = triad_type(triad[2], triad[0], triad[1]);
    if (type < 0) type = triad_type(triad[2], triad[1], triad[0]);

    if (type < 0) {
        cout << "negative type!" << endl;
        return;
    }

    profile[type - 1]++;
}

bool Net::not_in_triad(Node* node, Node** triad, unsigned int depth)
{
    for (unsigned int i = 0; i <= depth; i++) {
        if (triad[i] == node)
            return false;
    }

    return true;
}

void Net::triad_profile_r(Node** triad, unsigned int depth, unsigned long* profile)
{
    if (depth == 2) {
        update_triad_profile(triad, profile);
        return;    
    }

    Node* node = triad[depth];

    Edge* orig = node->origins;
    while (orig) {
        Node* next_node = orig->orig;
        if (next_node->flag && not_in_triad(next_node, triad, depth)) {
            triad[depth + 1] = next_node;
            triad_profile_r(triad, depth + 1, profile);
        }
        orig = orig->next_orig;
    }

    Edge* targ = node->targets;
    while (targ) {
        Node* next_node = targ->targ;
        if (next_node->flag && not_in_triad(next_node, triad, depth)) {
            triad[depth + 1] = next_node;
            triad_profile_r(triad, depth + 1, profile);
        }
        targ = targ->next_targ;
    }
}

unsigned long* Net::triad_profile()
{
    Node* triad[3];
    unsigned long* profile = (unsigned long*)malloc(sizeof(unsigned long) * 13);

    for (unsigned int i = 0; i < 13; i++)
        profile[i] = 0;
    
    // set all node flags to true
    Node* node = nodes;
    while (node) {
        node->flag = true;
        node = node->next;
    }

    // search for triads starting on each node
    node = nodes;
    while (node) {
        triad[0] = node;
        triad_profile_r(triad, 0, profile);
        node->flag = false;
        node = node->next;
    }

    return profile;
}

unsigned int* Net::in_deg_seq()
{
    unsigned int* seq = (unsigned int*)malloc(sizeof(unsigned int) * node_count);
    Node* curnode = nodes;
    unsigned int i = 0;
    while (curnode) {
        seq[i] = curnode->in_degree;
        curnode = curnode->next;
        i++;
    }

    return seq;
}

unsigned int* Net::out_deg_seq()
{
    unsigned int* seq = (unsigned int*)malloc(sizeof(unsigned int) * node_count);
    Node* curnode = nodes;
    unsigned int i = 0;
    while (curnode) {
        seq[i] = curnode->out_degree;
        curnode = curnode->next;
        i++;
    }

    return seq;
}

void Net::gen_degree_seq(Net* ref_net)
{
    unsigned int* in_deg_seq = ref_net->in_deg_seq();
    unsigned int* out_deg_seq = ref_net->out_deg_seq();

    unsigned int total_degree = ref_net->edge_count;

    // create nodes
    Node* new_nodes[ref_net->node_count];
    for (unsigned int i = 0; i < ref_net->node_count; i++) {
        new_nodes[i] = add_node(0);
    }

    // create edges
    for (unsigned int i = 0; i < ref_net->edge_count; i++) {
        unsigned int orig_pos = RANDOM_UINT(total_degree);
        unsigned int targ_pos = RANDOM_UINT(total_degree);

        unsigned int curpos = 0;
        int orig_index = -1;
        while (curpos <= orig_pos) {
            orig_index++;
            curpos += out_deg_seq[orig_index];
        }
        out_deg_seq[orig_index]--;

        curpos = 0;
        int targ_index = -1;
        while (curpos <= targ_pos) {
            targ_index++;
            curpos += in_deg_seq[targ_index];
        }
        in_deg_seq[targ_index]--;

        add_edge_to_net(new_nodes[orig_index], new_nodes[targ_index], 0);

        total_degree--;
    }

    free(in_deg_seq);
    free(out_deg_seq);
}

}
