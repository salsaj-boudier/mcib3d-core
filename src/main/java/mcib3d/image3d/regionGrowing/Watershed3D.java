/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mcib3d.image3d.regionGrowing;

import ij.IJ;
import ij.ImageStack;
import mcib3d.geom.ComparatorVoxel;
import mcib3d.geom.Voxel3D;
import mcib3d.geom.Voxel3DComparable;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.ImageLabeller;
import mcib3d.image3d.ImageShort;

import java.util.*;

/**
 * Copyright (C) Thomas Boudier
 * <p>
 * License: This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Class for 3D watershed implementation, using seeds
 *
 * @author thomas
 */
public class Watershed3D {

    private final int DAM = 1; // watershed label
    private final int QUEUE = 2;
    private final int seedsThreshold;
    private ImageHandler rawImage; // raw image
    private ImageHandler seedsImage; // positions of seeds
    private ImageInt watershedImage = null; // watershed from seeds
    private ImageInt labelQueueImage = null; // watershed from seeds
    private ImageInt damImage = null; // image for separation between objects
    private LinkedList<Voxel3DComparable> voxels = null; // voxels to compute watershed
    //private boolean okseeds = false;
    private boolean anim = false;
    private int rawThreshold;
    private boolean labelSeeds = true;
    private HashMap<Integer, Integer> seedsValue;

    /**
     * @param image raw image
     * @param seeds seeds image
     * @param noi   noise level for rw image
     * @param seth  noise level for seeds
     */
    public Watershed3D(ImageHandler image, ImageHandler seeds, int noi, int seth) {
        this.rawImage = image;
        this.seedsImage = seeds;
        this.rawThreshold = noi;
        this.seedsThreshold = seth;
        seedsValue = new HashMap<Integer, Integer>();
    }

    /**
     * Constructor for 3D Watershed
     *
     * @param image raw image
     * @param seeds seeds image
     * @param noi   noise level for rw image
     * @param seth  noise level for seeds
     */
    public Watershed3D(ImageStack image, ImageStack seeds, int noi, int seth) {
        this.rawImage = ImageHandler.wrap(image);
        this.seedsImage = ImageInt.wrap(seeds);
        this.rawThreshold = noi;
        this.seedsThreshold = seth;
        seedsValue = new HashMap<Integer, Integer>();
    }

    /**
     * Get the raw image used
     *
     * @return raw image
     */
    public ImageHandler getRawImage() {
        return rawImage;
    }

    /**
     * Sets the raw image
     *
     * @param image
     */
    public void setRawImage(ImageHandler image) {
        this.rawImage = image;
    }

    /**
     * Get the seeds image used
     *
     * @return seeds image
     */
    public ImageHandler getSeeds() {
        return seedsImage;
    }

    /**
     * set the seeds image used
     *
     * @param seeds image
     */
    public void setSeeds(ImageInt seeds) {
        this.seedsImage = seeds;
        seedsValue = new HashMap<Integer, Integer>();
        watershedImage = null;
    }

    public void setLabelSeeds(boolean labelSeeds) {
        this.labelSeeds = labelSeeds;
        watershedImage = null;
    }

    public void setAnim(boolean anim) {
        this.anim = anim;
    }

    public int getDamValue() {
        return DAM;
    }

    public ImageInt getWatershedImage3D() {
        if (watershedImage == null) {
            processWatershed();
        }
        return watershedImage;
    }

    public ImageInt getDamImage() {
        if (watershedImage == null) {
            processWatershed();
        }
        return damImage;
    }

    private void processWatershed() {
        long step = 100;
        createNeigList();
        long t0 = System.currentTimeMillis();
        if (anim) {
            watershedImage.show();
        }
        if (rawImage.getMin() > rawThreshold) {
            IJ.log("Setting minimum for raw image to " + rawImage.getMin());
            rawThreshold = (int) rawImage.getMin();
        }

        // tree set
        ComparatorVoxel comp = new ComparatorVoxel();
        TreeSet<Voxel3DComparable> tree = new TreeSet<Voxel3DComparable>(comp);
        int idx = 1;
        for (Voxel3DComparable V : voxels) {
            V.setMax(idx, 0);
            idx++;
            tree.add(V);
        }
        boolean newt = true;

        IJ.log("");
        while (newt) {
            newt = false;
            while (!tree.isEmpty()) {
                Voxel3DComparable V = tree.pollFirst();

                ArrayList<Voxel3D> Nei = watershedImage.getNeighborhood3x3x3ListNoCenter(V.getRoundX(), V.getRoundY(), V.getRoundZ());

                // if in DAM, do not process
                if (watershedImage.getPixel(V) == DAM) continue;

                watershedImage.setPixel(V, labelQueueImage.getPixel(V));

                // all free voxels around are put into queue
                for (Voxel3D N : Nei) {
                    int rawN = (int) rawImage.getPixel(N);
                    if (rawN > rawThreshold) {
                        // neighbor voxel not in queue yet
                        if ((N.getValue() == 0)) {
                            watershedImage.setPixel(N, QUEUE);
                            labelQueueImage.setPixel(N, (int) V.getLabel());
                            Voxel3DComparable Vnew = new Voxel3DComparable(N.getRoundX(), N.getRoundY(), N.getRoundZ(), rawN, V.getLabel());
                            Vnew.setMax(idx, 0);
                            idx++;
                            tree.add(Vnew);
                        }
                        // neighbor was already in Q by another label --> dam !
                        else if ((N.getValue() == QUEUE) && (labelQueueImage.getPixel(V) != labelQueueImage.getPixel(N))) {
                            watershedImage.setPixel(N, DAM);
                        }
                    }
                }

                if (System.currentTimeMillis() - t0 > step) {
                    IJ.log("\\Update:Voxels to process : " + Math.abs(tree.size()));
                    if (anim) {
                        watershedImage.updateDisplay();
                    }
                    t0 = System.currentTimeMillis();
                }
            }
        }
        IJ.log("\\Update:Voxels to process : " + Math.abs(tree.size()));
        IJ.log("Watershed completed.");

        damImage = (ImageInt) watershedImage.createSameDimensions();
        watershedImage.transfertPixelValues(damImage, 1, 255);

        // replace dam values with 0
        watershedImage.replacePixelsValue(1, 0);
        // back to original seeds value
        for (int val : seedsValue.keySet()) {
            watershedImage.replacePixelsValue(val, seedsValue.get(val));
        }
    }


    private void createNeigList() {
        voxels = new LinkedList<Voxel3DComparable>();
        int sx = rawImage.sizeX;
        int sy = rawImage.sizeY;
        int sz = rawImage.sizeZ;

        // watershed images
        watershedImage = new ImageShort("watershed", sx, sy, sz);
        labelQueueImage = new ImageShort("labelQ", sx, sy, sz);

        //okseeds = false;

        float pix;
        float se;

        // compute the seeds image
        // threshold, // TODO 32-bits seeds ?
        ImageInt seedsLabel = (ImageInt) seedsImage.duplicate();
        seedsLabel.thresholdCut(seedsThreshold, false, true);

        if ((labelSeeds)) {
            IJ.log("Labelling ");
            ImageLabeller labeller = new ImageLabeller();
            seedsLabel = labeller.getLabels(seedsLabel);
        }
        // since seeds Label starts at 1 and watershed at 2, replace values
        int max = (int) seedsLabel.getMax();
        if (seedsLabel.hasOneValueInt(QUEUE)) {
            seedsLabel.replacePixelsValue(QUEUE, max + 1);
            seedsValue.put(max + 1, QUEUE);
        }
        if (seedsLabel.hasOneValueInt(DAM)) {
            seedsLabel.replacePixelsValue(DAM, max + 2);
            seedsValue.put(max + 2, DAM);
        }
        if (!seedsValue.isEmpty())
            seedsLabel.resetStats(null);

        for (int z = sz - 1; z >= 0; z--) {
            IJ.showStatus("Processing watershed " + (z + 1));
            for (int y = sy - 1; y >= 0; y--) {
                for (int x = sx - 1; x >= 0; x--) {
                    pix = rawImage.getPixel(x, y, z);
                    se = seedsLabel.getPixel(x, y, z);
                    if (pix > rawThreshold) {
                        if (se > 0) {
                            watershedImage.setPixel(x, y, z, se);
                            //okseeds = true;
                            ArrayList<Voxel3D> list = watershedImage.getNeighborhood3x3x3ListNoCenter(x, y, z);
                            Collections.shuffle(list);
                            for (Voxel3D N : list) {
                                int vx = (int) N.getX();
                                int vy = (int) N.getY();
                                int vz = (int) N.getZ();
                                int raw = (int) rawImage.getPixel(vx, vy, vz);
                                if ((raw > rawThreshold) && (seedsLabel.getPixel(vx, vy, vz) == 0) && (watershedImage.getPixel(vx, vy, vz) != QUEUE)) {
                                    voxels.add(new Voxel3DComparable(vx, vy, vz, raw, se));
                                    watershedImage.setPixel(vx, vy, vz, QUEUE);
                                    labelQueueImage.setPixel(vx, vy, vz, se);
                                }
                            }
                        }
                    }
                }
            }
        }
        IJ.showStatus("Watershed...");
    }
}
