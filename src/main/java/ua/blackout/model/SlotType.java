package ua.blackout.model;

public enum SlotType {
    DEFINITE("Definite"),
    NOT_PLANNED("NotPlanned"),
    UNKNOWN("");

    private final String apiName;

    SlotType(String apiName){
        this.apiName = apiName;
    }

    public static SlotType fromApi(String value){
        for (SlotType type : values()) {
            if (type.apiName.equals(value)){
                return type;
            }
        }
        return UNKNOWN;
    }
}