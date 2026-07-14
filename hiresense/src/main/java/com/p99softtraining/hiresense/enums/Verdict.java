package com.p99softtraining.hiresense.enums;

public enum Verdict {

    GOOD(3),
    AVERAGE(2),
    POOR(1);

    private final int points;

    Verdict(int points) {
        this.points = points;
    }

    public int points() {
        return points;
    }
}
