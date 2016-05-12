package darkevilmac.archimedes.common.network;

public enum HelmClientAction {
    UNKNOWN, ASSEMBLE, MOUNT, UNDOCOMPILE;

    public int toInt() {
        return HelmClientAction.toInt(this);
    }

    public static int toInt(HelmClientAction action) {
        switch (action) {
            case ASSEMBLE:
                return (byte) 1;
            case MOUNT:
                return (byte) 2;
            case UNDOCOMPILE:
                return (byte) 3;
            default:
                return (byte) 0;
        }
    }

    public static HelmClientAction fromInt(int actionInt) {
        switch (actionInt) {
            case 1:
                return ASSEMBLE;
            case 2:
                return MOUNT;
            case 3:
                return UNDOCOMPILE;
            default:
                return UNKNOWN;
        }
    }
}
