package com.telmomenezes.synthetic;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;


public class DRMap {

    private double[] data;
    private int binNumber;
    private double minValHor;
    private double maxValHor;
    private double minValVer;
    private double maxValVer;

    public DRMap(int binNumber, double minValHor, double maxValHor,
            double minValVer, double maxValVer) {
        this.binNumber = binNumber;
        this.minValHor = minValHor;
        this.maxValHor = maxValHor;
        this.minValVer = minValVer;
        this.maxValVer = maxValVer;
        setData(new double[binNumber * binNumber]);

        clear();
    }

    public void clear() {
        Arrays.fill(getData(), 0);
    }

    public void setValue(int x, int y, double val) {
        getData()[(y * binNumber) + x] = val;
    }

    public void incValue(int x, int y) {
        getData()[(y * binNumber) + x] += 1;
    }

    public double getValue(int x, int y) {
        return getData()[(y * binNumber) + x];
    }

    public void logScale() {
        for (int x = 0; x < binNumber; x++) {
            for (int y = 0; y < binNumber; y++) {
                if (getData()[(y * binNumber) + x] > 0) {
                    getData()[(y * binNumber) + x] = Math
                            .log(getData()[(y * binNumber) + x]);
                }
            }
        }
    }

    public void normalizeMax() {
        double m = max();
        if (m <= 0) {
            return;
        }
        // normalize by max
        for (int x = 0; x < binNumber; x++) {
            for (int y = 0; y < binNumber; y++) {
                getData()[(y * binNumber) + x] = getData()[(y * binNumber) + x]
                        / m;
            }
        }
    }

    public void normalizeTotal() {
        double t = total();
        if (t <= 0) {
            return;
        }
        // normalize by max
        for (int x = 0; x < binNumber; x++) {
            for (int y = 0; y < binNumber; y++) {
                getData()[(y * binNumber) + x] = getData()[(y * binNumber) + x]
                        / t;
            }
        }
    }

    public void binary() {
        for (int x = 0; x < binNumber; x++) {
            for (int y = 0; y < binNumber; y++) {
                if (getData()[(y * binNumber) + x] > 0) {
                    getData()[(y * binNumber) + x] = 1;
                }
            }
        }
    }

    public double total() {
        double total = 0;

        for (int x = 0; x < binNumber; x++) {
            for (int y = 0; y < binNumber; y++) {
                total += getData()[(y * binNumber) + x];
            }
        }

        return total;
    }

    double max() {
        double max = 0;

        for (int x = 0; x < binNumber; x++) {
            for (int y = 0; y < binNumber; y++) {
                if (getData()[(y * binNumber) + x] > max) {
                    max = getData()[(y * binNumber) + x];
                }
            }
        }

        return max;
    }
    
    public void draw(Graphics2D g, double side) {
        double binSide = ((double)side) / ((double)binNumber);

        // colors
        Color gridColor = new Color(255, 255, 0);

        // font
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        FontMetrics metrics = g.getFontMetrics(font);
        int textHeight = metrics.getHeight();
        g.setFont(font);
        
        // draw cells
        for (int x = 0; x < binNumber; x++) {
            for (int y = 0; y < binNumber; y++) {
                double val = getValue(x, y);
                Color color = new Color(0, 150, 200);
                if (val > 0.0) {
                    color = new Color((int)(255.0 * val), 0, 0);
                }
                g.setPaint(color);
                g.fill(new Rectangle2D.Double(x * binSide,
                        side - ((y + 1) * binSide),
                        binSide,
                        binSide));
            }
        }
                

        // draw grid
        double center = side / 2.0;
        g.setPaint(gridColor);
        g.draw(new Line2D.Double(center, 0, center, side));
        g.draw(new Line2D.Double(0, center, side, center));

        // TODO: configure limit
        double limit = 7.0;
        
        int divs = ((int)limit) - 1;
        for (int j = 0; j < divs; j++) {
            double y = center - ((center / limit) * (j + 1));
            g.draw(new Line2D.Double(center - 5, y, center + 5, y));
            g.drawString(Integer.toString(j + 1), (float)(center + 10), (float)(y + (textHeight / 2) - 2));
            
            y = center + ((center / limit) * (j + 1));
            g.draw(new Line2D.Double(center - 5, y, center + 5, y));
            g.drawString("-" + Integer.toString(j + 1), (float)(center + 10), (float)(y + (textHeight / 2) - 2));

            double x = center - ((center / limit) * (j + 1));
            g.draw(new Line2D.Double(x, center - 5, x, center + 5));
            g.drawString("-" + Integer.toString(j + 1), (float)(x - 10), (float)(center + 20));

            x = center + ((center / limit) * (j + 1));
            g.draw(new Line2D.Double(x, center - 5, x, center + 5));
            g.drawString(Integer.toString(j + 1), (float)(x - 3), (float)(center + 20));
        }

        g.drawString("R", (float)(center - 15), 15);
        g.drawString("D", (float)(side - 15), (float)(center - 10));
    }
    
    public void draw(String filename, int side) {
        BufferedImage img = new BufferedImage(side, side, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = img.createGraphics();
        draw(g, side);
        
        try {
            File outputfile = new File(filename);
            ImageIO.write(img, "png", outputfile);
        }
        catch (IOException e) {
           e.printStackTrace();
        }
    }

    double simpleDist(DRMap map) {
        double dist = 0;
        for (int x = 0; x < binNumber; x++) {
            for (int y = 0; y < binNumber; y++) {
                dist += Math.abs(getData()[(y * binNumber) + x]
                        - map.getData()[(y * map.binNumber) + x]);
            }
        }
        return dist;
    }

    /*
    double emdDist(DRMap map) {
        //printf("totals-> %f; %f\n", total(), map->total());
 
        double infinity = 9999999999.9;

        if (total() <= 0) {
            return infinity;
        }
        if (map.total() <= 0) {
            return infinity;
        }

        int[] hist1 = toHist();
        int[] hist2 = map.toHist();
        int[] dimensions = {binNumber, binNumber};

        return EMDL1.distance(hist1, hist2, dimensions);
    }*/

    @Override
    public String toString() {
        String str = "";
        for (int y = 0; y < binNumber; y++) {
            for (int x = 0; x < binNumber; x++) {
                str += getValue(x, y) + "\t";
            }
            str += "\n";
        }
        return str;
    }

    double[] getData() {
        return data;
    }

    void setData(double[] data) {
        this.data = data;
    }

    int getBinNumber() {
        return binNumber;
    }

    void setBinNumber(int binNumber) {
        this.binNumber = binNumber;
    }

    double getMinValHor() {
        return minValHor;
    }

    void setMinValHor(double minValHor) {
        this.minValHor = minValHor;
    }

    double getMaxValHor() {
        return maxValHor;
    }

    void setMaxValHor(double maxValHor) {
        this.maxValHor = maxValHor;
    }

    double getMinValVer() {
        return minValVer;
    }

    void setMinValVer(double minValVer) {
        this.minValVer = minValVer;
    }

    double getMaxValVer() {
        return maxValVer;
    }

    void setMaxValVer(double maxValVer) {
        this.maxValVer = maxValVer;
    }
}