package com.oberasoftware.robo.pep;

import java.util.List;

/**
 * @author Renze de Vries
 */
public class ListValueEvent implements RoboEvent {
    private final List<Object> values;
    private final String source;

    public ListValueEvent(List<Object> values, String source) {
        this.values = values;
        this.source = source;
    }

    public List<Object> getValues() {
        return values;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "ListValueEvent{" +
                "values=" + values +
                ", source='" + source + '\'' +
                '}';
    }
}
