package in.ishdemon.mrlate.yuga.utils;

public class L {

    private L() {

    }

    public static void msg(Object str) {
        System.out.println(str);
    }

    public static void error(Exception e) {
        e.printStackTrace();
    }
}