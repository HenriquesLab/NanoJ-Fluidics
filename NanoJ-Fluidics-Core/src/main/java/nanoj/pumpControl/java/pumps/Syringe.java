package nanoj.pumpControl.java.pumps;

public enum  Syringe {
    PERISTALTIC( 1, "Peristaltic", 3.098428844486803),
    BD50( 50, "BD", 26.594),
    BD20( 20, "BD", 19.05),
    BD10( 10, "BD", 14.427),
    BD5( 5,"BD",  11.989),
    BD2( 2, "BD", 8.7),
    BD1( 1, "BD", 4.699),
    TER60( 60, "Terumo", 29.7),
    TER30( 30, "Terumo", 23.1),
    TER20( 20, "Terumo", 20.15),
    TER10( 10, "Terumo", 15.8),
    TER5( 5,"Terumo",  13),
    TER1( 1, "Terumo", 4.699);

    public static final String VOLUME_UNITS = "ml";
    public final double volume;
    public final String brand;
    public final double diameter;

    Syringe(double givenVolume, String givenBrand, double givenDiameter) {
        this.volume = givenVolume;
        this.brand = givenBrand;
        this.diameter = givenDiameter;
    }

    public String getVolumeWUnits() {
        String vol = (volume < 1)? "" + volume: "" + (int) volume;
        return vol + " " + VOLUME_UNITS;
    }

    public String getBrandedName() {
        if (this.equals(PERISTALTIC))
            return brand;
        else return brand + ", " + getVolumeWUnits();
    }

    public static String[] getAllBrandedNames() {
        String[] allBrandedNames = new String[Syringe.values().length];

        for (Syringe syringe: Syringe.values())
            allBrandedNames[syringe.ordinal()] = syringe.getBrandedName();

        return allBrandedNames;
    }


}
