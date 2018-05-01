package nanoj.pumpControl.java.pumps;

public class Syringe {
    public static final String VOLUME_UNITS = "ml";
    private double volume = 0;
    private String brand = "";
    private double diameter = 1;

    public Syringe(double givenVolume, String givenBrand, double givenDiameter) {
        this.volume = givenVolume;
        this.brand = givenBrand;
        this.diameter = givenDiameter;
    }

    public double getVolume() {
        return volume;
    }

    public String getVolumeWUnits() {
        return (int) volume + " " + VOLUME_UNITS;
    }

    public String getBrand() {
        return brand;
    }

    public double getDiameter() {
        return diameter;
    }
}
