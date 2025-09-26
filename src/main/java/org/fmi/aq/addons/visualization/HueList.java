/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.addons.visualization;

import java.util.ArrayList;

/**
 * This class is used the get a list of hue-values to set up the colors used by
 * FigureData. The progression can be from lower values to larger, however,
 * with inverted progression the values go from larger to smaller.
 * There are some other empirical/experimental modifiers at play: the goal
 * is to obtain visually pleasing hue sets. Brightness or saturation is not considered,
 * just the hue [0,1]
 * @author Lasse Johansson
 */
public class HueList {

    public static ArrayList<Double> getHuesAsList(double startHue, double endHue,
            int steps, boolean inverse, double stepsize) {
        ArrayList<Double> hues = new ArrayList<>();

        if (inverse) {
            for (int i = 0; i < steps; i++) {
                double hue = startHue - i * stepsize;
                if (hue >= 0) {
                    hues.add(hue);
                } else {
                    hues.add(hue + 1);
                }
            }
        } //huet normaalisti.
        else {
            for (int i = 0; i < steps; i++) {
                hues.add(startHue + i * stepsize);
            }
        }
        return hues;
    }

    public static ArrayList<Double> getHuesAsList_natural(double h1, double h2, int steps) {
        double hn1 = 0.28;
        double hn2 = 0.78;
        int nsteps = 1000;
        double stepsize = (hn1 + (1.0 - hn2)) / (double) nsteps;
        ArrayList<Double> natHues = getHuesAsList(hn1, hn2, nsteps, true, stepsize);//inversion is ON
        ArrayList<Double> hues = new ArrayList<>();

        for (int i = 0; i < steps; i++) {
            float frac = (float) i / (float) steps;
            int index = (int) (h1 * natHues.size());//base component
            index += frac * (h2 - h1) * natHues.size();
            if (index > natHues.size() - 1) index = natHues.size() - 1;
            hues.add(natHues.get(index));
        }
        return hues;
    }
}
