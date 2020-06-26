package mcib3d.tracking_dev;

import ij.IJ;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.geom.Objects3DPopulationColocalisation;
import mcib3d.image3d.ImageHandler;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

public class TrackingAssociation {
    List<AssociationPair> finalAssociations;
    List<Object3D> finalOrphan1;
    List<Object3D> finalOrphan2;
    List<Mitosis> finalMitosis;
    private ImageHandler img1;
    private ImageHandler img2;
    private ImageHandler path = null;
    private ImageHandler tracked = null;
    private ImageHandler pathed = null;

    private boolean merge = false;

    public TrackingAssociation(ImageHandler img1, ImageHandler img2) {
        this.img1 = img1;
        this.img2 = img2;
    }

    public void setPathImage(ImageHandler path) {
        this.path = path;
    }

    public ImageHandler getTracked() {
        if (this.tracked == null) computeTracking();

        return this.tracked;
    }

    public ImageHandler getPathed() {
        if (this.path == null) return null;
        if (this.pathed == null) computeTracking();

        return this.pathed;
    }

    public void setImage1(ImageHandler img1) {
        this.img1 = img1;
        this.tracked = null;
    }

    public void setImage2(ImageHandler img2) {
        this.img2 = img2;
        this.tracked = null;
    }

    public void setMerge(boolean merge) {
        this.merge = merge;
        this.tracked = null;
    }

    private void computeTracking() {
        this.tracked = this.img1.createSameDimensions();
        if (this.path != null) this.pathed = this.img1.createSameDimensions();

        Objects3DPopulation population1 = new Objects3DPopulation(this.img1);
        Objects3DPopulation population2 = new Objects3DPopulation(this.img2);

        Association association = new Association(population1, population2, new CostColocalisation(new Objects3DPopulationColocalisation(population1, population2)));
        association.verbose = true;

        association.computeAssociation();

        MitosisDetector mitosisDetector = new MitosisDetector(this.img1, this.img2, association);

        if (this.merge) {
            this.img2 = mitosisDetector.detectAndMergeSplit();

            population2 = new Objects3DPopulation(this.img2);
            association = new Association(population1, population2, new CostColocalisation(new Objects3DPopulationColocalisation(population1, population2)));
            association.computeAssociation();
            mitosisDetector = new MitosisDetector(this.img1, this.img2, association);
        }

        association.drawAssociation(this.tracked);

        if (this.path != null) {
            association.drawAssociationPath(this.pathed, this.path, this.tracked);
        }

        TreeSet<Mitosis> treeSet = mitosisDetector.detectMitosis();
        List<Object3D> mito = new LinkedList<>();
        for (Mitosis mitosis : treeSet) {
            Object3D d1 = mitosis.getDaughter1();
            Object3D d2 = mitosis.getDaughter2();
            Object3D mo = mitosis.getMother();
            double coloc = mitosis.getColocMitosis();
            int valPath = (int) mo.getPixMeanValue(this.path);
            if (!mito.contains(d1) && !mito.contains(d2) && coloc > mitosisDetector.getMinColocMitosis()) {
                IJ.log("MITOSIS : " + d1.getValue() + " " + d2.getValue() + " " + mo.getValue() + " " + coloc + " " + valPath);
                mito.add(d1);
                mito.add(d2);

                if (this.path != null) {
                    d1.draw(this.pathed, valPath);
                    d2.draw(this.pathed, valPath);
                }
            }
        }
    }
}