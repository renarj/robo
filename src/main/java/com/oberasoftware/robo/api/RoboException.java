package com.oberasoftware.robo.api;

/**
 * @author Renze de Vries
 */
public class RoboException extends RuntimeException {
    public RoboException(String message) {
        super(message);
    }

    public RoboException(String message, Throwable cause) {
        super(message, cause);
    }
}
