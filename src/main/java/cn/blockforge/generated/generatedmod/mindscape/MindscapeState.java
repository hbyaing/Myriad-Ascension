package cn.blockforge.generated.generatedmod.mindscape;

public enum MindscapeState {
    NONE, UNSTABLE, MEDITATING, ENTERING, IN_MINDSCAPE, CHALLENGE_ACTIVE, COMPLETED, RETURNING;
    /** Shared visual timing; keeping these here avoids client loading the server manager. */
    public static final int PURIFICATION_TICKS = 48;
    public static final int VICTORY_DISPLAY_TICKS = PURIFICATION_TICKS + 24;
    public static MindscapeState read(String value){
        try{return valueOf(value);}catch(IllegalArgumentException exception){return NONE;}
    }
}
