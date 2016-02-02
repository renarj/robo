package com.oberasoftware.robo.pep;

/**
 * @author Renze de Vries
 */
public class NumberEvent implements RoboEvent {
    private final String source;
    private final int number;

    public NumberEvent(String source, int number) {
        this.source = source;
        this.number = number;
    }

    @Override
    public String getSource() {
        return source;
    }

    public int getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return "NumberEvent{" +
                "source='" + source + '\'' +
                ", number=" + number +
                '}';
    }
}
