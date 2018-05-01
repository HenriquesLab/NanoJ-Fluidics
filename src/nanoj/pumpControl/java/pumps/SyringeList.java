package nanoj.pumpControl.java.pumps;

public class SyringeList {
    private static final Syringe[] syringeList = new Syringe[] {
            new Syringe(50, "BD", 26.594),
            new Syringe(20, "BD", 19.05),
            new Syringe(10, "BD", 14.427),
            new Syringe( 5,"BD",  11.989),
            new Syringe( 2, "BD", 8.7),
            new Syringe( 1, "BD", 4.699),
            new Syringe(60, "Terumo", 29.7),
            new Syringe(30, "Terumo", 23.1),
            new Syringe(20, "Terumo", 20.15),
            new Syringe(10, "Terumo", 15.8),
            new Syringe( 5,"Terumo",  13),
            new Syringe( 1, "Terumo", 4.699)
    };

    private SyringeList() {}

    public static Syringe[] getSyringes() {
        return syringeList;
    }

    public static String[] getNames() {
        String[] names = new String[syringeList.length];
        for (int n=0; n<syringeList.length; n++) {
            names[n] = syringeList[n].getVolumeWUnits();
        }
        return names;
    }
    public static String[] getBrandedNames(int chars) {
        String[] brandedNames = new String[syringeList.length];
        for (int n=0; n<syringeList.length; n++) {
            String brand = syringeList[n].getBrand();
            if (chars > 0 && brand.length() > chars) brand = brand.substring(0,chars);
            brandedNames[n] = brand + ", " + syringeList[n].getVolumeWUnits();
        }
        return brandedNames;
    }

    public static double getVolume(int syringe) {
        return syringeList[syringe].getVolume();
    }

    public static String getVolumeWUnits(int syringe) {
        return syringeList[syringe].getVolumeWUnits();
    }

    public static double getDiameter(int syringe) {
        return syringeList[syringe].getDiameter();
    }

    public static String getBrand(int syringe) {
        return syringeList[syringe].getBrand();
    }

}