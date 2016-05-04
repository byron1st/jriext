package edu.kaist.salab.byron1st.jriext.instrumentation;

/**
 * Created by byron1st on 2016. 1. 12..
 */
public class C {
    public static final int SSIZE = 3;
    public static final String VIRTUAL = "+V+";
    public static final String STATIC = "+S+";
    public static final String ENTER = "+E+";
    public static final String EXIT = "+X+";
    public static final String DDELIM = ","; //Default delimiter
    public static final String VDELIM = ":"; //Delimiter in monitoring values
    public static final String VO = "{"; //Open monitoring values
    public static final String VC = "}"; //Close monitoring values
    public static final String VFIELD = "!"; //Field in monitoring values
    public static final String VPARAMETER = "@"; //Followed by an integer. (e.g. @0:Ljava/lang/Object;: ...)
    public static final String VRETURN = "#";
}
