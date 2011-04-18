/*
 * Copyright (C) 2010 Telmo Menezes.
 * telmo@telmomenezes.com
 */


#include "histogram2D.h"
#include "utils.h"
#include <stdlib.h>
#include <strings.h>
#include <stdio.h>


syn_histogram2d *syn_histogram2d_create(unsigned int bin_number, double min_val_hor, double max_val_hor,
     double min_val_ver, double max_val_ver)
{
    syn_histogram2d *hist = (syn_histogram2d *)malloc(sizeof(syn_histogram2d));
    hist->bin_number = bin_number;
    hist->min_val_hor = min_val_hor;
    hist->max_val_hor = max_val_hor;
    hist->min_val_ver = min_val_ver;
    hist->max_val_ver = max_val_ver;
    hist->data = (double *)malloc(bin_number * bin_number * sizeof(double));

    syn_histogram2d_clear(hist);
    
    return hist;
}


void syn_histogram2d_destroy(syn_histogram2d *hist)
{
    free(hist->data);
    free(hist);
}


void syn_histogram2d_clear(syn_histogram2d *hist)
{
    bzero(hist->data, hist->bin_number * hist->bin_number * sizeof(double));
}


void syn_histogram2d_set_value(syn_histogram2d *hist, unsigned int x, unsigned int y, double val)
{
    hist->data[(y * hist->bin_number) + x] = val;
}


void syn_historgram2d_inc_value(syn_histogram2d *hist, unsigned int x, unsigned int y)
{
    hist->data[(y * hist->bin_number) + x] += 1;
}


double syn_histogram2d_get_value(syn_histogram2d *hist, unsigned int x, unsigned int y)
{
    return hist->data[(y * hist->bin_number) + x];
}

    
void syn_histogram2d_log_scale(syn_histogram2d *hist)
{
    unsigned int x, y;
    for (x = 0; x < hist->bin_number; x++)
        for (y = 0; y < hist->bin_number; y++)
            if(hist->data[(y * hist->bin_number) + x] > 0) 
                hist->data[(y * hist->bin_number) + x] = log(hist->data[(y * hist->bin_number) + x]);
}


double syn_histogram2d_simple_dist(syn_histogram2d *hist1, syn_histogram2d *hist2)
{
    double dist = 0;
    unsigned int x, y;
    for (x = 0; x < hist1->bin_number; x++)
        for (y = 0; y < hist1->bin_number; y++)
            dist += fabs(hist1->data[(y * hist1->bin_number) + x] - hist2->data[(y * hist2->bin_number) + x]);
    return dist;
}


signature_t* syn_histogram2d_get_emd_signature(syn_histogram2d *hist)
{
    double interval_hor = (hist->max_val_hor - hist->min_val_hor) / ((double)hist->bin_number);
    double interval_ver = (hist->max_val_ver - hist->min_val_ver) / ((double)hist->bin_number);
    
    unsigned int n = 0;
    unsigned int x, y;
    
    for (x = 0; x < hist->bin_number; x++) {
        for (y = 0; y < hist->bin_number; y++) {
            if (hist->data[(y * hist->bin_number) + x] > 0)
                n++;
        }
    }

    feature_t* features = (feature_t*)malloc(sizeof(feature_t) * n);
    double* weights = (double*)malloc(sizeof(double) * n);

    unsigned int i = 0;

    for (x = 0; x < hist->bin_number; x++) {
        for (y = 0; y < hist->bin_number; y++) {
            double val = hist->data[(y * hist->bin_number) + x];
            if (val > 0) {
                features[i].x = (x * interval_hor) + hist->min_val_hor;
                features[i].y = (y * interval_ver) + hist->min_val_ver;
                weights[i] = val;
                i++;
            }
        }
    }

    signature_t* signature = (signature_t*)malloc(sizeof(signature_t));
    signature->n = n;
    signature->Features = features;
    signature->Weights = weights;

    return signature;
}


double syn_histogram2d_emd_dist(syn_histogram2d *hist1, syn_histogram2d *hist2)
{
    signature_t* sig1 = syn_histogram2d_get_emd_signature(hist1);
    signature_t* sig2 = syn_histogram2d_get_emd_signature(hist2);
    
    double dist = emd(sig1, sig2, groundDist, NULL, NULL);
    
    free(sig1->Features);
    free(sig1->Weights);
    free(sig1);
    free(sig2->Features);
    free(sig2->Weights);
    free(sig2);
    
    return dist;
}


void syn_histogram2d_print(syn_histogram2d *hist)
{
    unsigned int x, y;

    for (y = 0; y < hist->bin_number; y++) {
        for (x = 0; x < hist->bin_number; x++) {
            printf("%f\t", syn_histogram2d_get_value(hist, x, y));
        }
        printf("\n");
    }
}
