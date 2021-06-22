package mcib3d.geom2.measurements;


import mcib3d.geom2.Object3D;

public class MeasureCompactness extends MeasureAbstract {
    public final static String COMP_UNIT = "CompactnessPix";
    public final static String COMP_PIX = "CompactnessUnit";
    public final static String COMP_CORRECTED = "CompactnessCorrPix";
    public final static String COMP_DISCRETE = "compactnessDiscretePix";
    public final static String SPHER_UNIT = "SphericityPix";
    public final static String SPHER_PIX = "SphericityUnit";
    public final static String SPHER_CORRECTED = "SphericityCorrPix";


    public MeasureCompactness(Object3D object3D) {
        super(object3D);
    }

    @Override
    public String[] getNames() {
        return new String[]{COMP_UNIT, COMP_PIX, COMP_CORRECTED, COMP_DISCRETE, SPHER_PIX, SPHER_UNIT, SPHER_CORRECTED};
    }

    @Override
    public void computeAll() {
        MeasureVolume volume = new MeasureVolume(object3D);
        MeasureSurface surface = new MeasureSurface(object3D);

        double s3 = Math.pow(surface.getSurfaceContactPix(), 3);
        double v2 = Math.pow(volume.getVolumePix(), 2);
        double c = (v2 * 36.0 * Math.PI) / s3;
        keysValues.put(COMP_PIX, c);
        keysValues.put(SPHER_PIX, Math.pow(c, 1.0 / 3.0));

        s3 = Math.pow(surface.getSurfaceCorrectedPix(), 3);
        c = (v2 * 36.0 * Math.PI) / s3;
        keysValues.put(COMP_CORRECTED,c);
        keysValues.put(SPHER_CORRECTED, Math.pow(c, 1.0 / 3.0));

        // From Bribiesca 2008 Pattern Recognition
        // An easy measure of compactness for 2D and 3D shapes
        double v = volume.getVolumePix();
        double tmp = Math.pow(v, 2.0 / 3.0);
        keysValues.put(COMP_DISCRETE, ((v - surface.getSurfaceContactPix() / 6.0) / (v - tmp)));

        s3 = Math.pow(surface.getSurfaceContactUnit(), 3);
        v2 = Math.pow(volume.getVolumeUnit(), 2);
        c = (v2 * 36.0 * Math.PI) / s3;
        keysValues.put(COMP_UNIT, c);
        keysValues.put(SPHER_UNIT, Math.pow(c, 1.0 / 3.0));
    }
}
