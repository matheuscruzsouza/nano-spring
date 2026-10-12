package com.github.matheuscruzsouza.nanospring.battery;

import java.io.Serializable;

/**
 * Immutable snapshot representing the battery state of an Android device.
 */
public class BatteryStatus implements Serializable {

    private final int level;
    private final int scale;
    private final int percentage;
    private final boolean charging;
    private final boolean pluggedAc;
    private final boolean pluggedUsb;
    private final double temperatureCelsius;
    private final int health;
    private final boolean low;
    private final long timestamp;

    public BatteryStatus(int level, int scale, int percentage, boolean charging,
                         boolean pluggedAc, boolean pluggedUsb, double temperatureCelsius,
                         int health, boolean low, long timestamp) {
        this.level = level;
        this.scale = scale;
        this.percentage = percentage;
        this.charging = charging;
        this.pluggedAc = pluggedAc;
        this.pluggedUsb = pluggedUsb;
        this.temperatureCelsius = temperatureCelsius;
        this.health = health;
        this.low = low;
        this.timestamp = timestamp;
    }

    public int getLevel() {
        return level;
    }

    public int getScale() {
        return scale;
    }

    public int getPercentage() {
        return percentage;
    }

    public boolean isCharging() {
        return charging;
    }

    public boolean isPluggedAc() {
        return pluggedAc;
    }

    public boolean isPluggedUsb() {
        return pluggedUsb;
    }

    public boolean isPlugged() {
        return pluggedAc || pluggedUsb;
    }

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public int getHealth() {
        return health;
    }

    public boolean isLow() {
        return low;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "BatteryStatus{" +
                "percentage=" + percentage + "%" +
                ", charging=" + charging +
                ", pluggedAc=" + pluggedAc +
                ", pluggedUsb=" + pluggedUsb +
                ", temperatureCelsius=" + temperatureCelsius +
                ", low=" + low +
                '}';
    }
}
