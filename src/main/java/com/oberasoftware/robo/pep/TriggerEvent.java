package com.oberasoftware.robo.pep;

/**
 * @author Renze de Vries
 */
public class TriggerEvent implements RoboEvent {
    private final boolean on;
    private final String source;

    public TriggerEvent(boolean on, String source) {
        this.on = on;
        this.source = source;
    }

    @Override
    public String getSource() {
        return source;
    }

    public boolean isOn() {
        return on;
    }

    @Override
    public String toString() {
        return "TriggerEvent{" +
                "on=" + on +
                ", source='" + source + '\'' +
                '}';
    }
}
