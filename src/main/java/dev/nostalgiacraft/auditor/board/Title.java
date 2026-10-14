package dev.nostalgiacraft.auditor.board;

public enum Title {
    NONE(0),
    AUDITED(1),
    SUPPLIER(5),
    INDUSTRIAL(15),
    MAGNATE(40),
    MONOPOLIST(Integer.MAX_VALUE);

    public static final int MONOPOLY_RECORDS = 5;
    private final int seals;

    Title(int seals) {
        this.seals = seals;
    }

    public static Title of(int seals, int records) {
        if (records >= MONOPOLY_RECORDS) {
            return MONOPOLIST;
        }
        Title earned = NONE;
        for (Title title : values()) {
            if (seals >= title.seals) {
                earned = title;
            }
        }
        return earned;
    }
}
