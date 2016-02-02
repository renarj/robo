package com.oberasoftware.robo.pep;

import com.aldebaran.qi.CallError;
import com.aldebaran.qi.Session;
import com.aldebaran.qi.helper.proxies.ALMemory;
import com.oberasoftware.base.event.EventBus;
import com.oberasoftware.base.event.EventHandler;
import com.oberasoftware.base.event.impl.LocalEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Arrays.asList;

/**
 * @author Renze de Vries
 */
public class SensorManager {
    private static final Logger LOG = LoggerFactory.getLogger(SensorManager.class);

    private final ALMemory memory;
    private final EventBus eventBus;

    private List<Long> eventIds = new CopyOnWriteArrayList<>();

    public SensorManager(Session session) {
        try {
            this.memory = new ALMemory(session);
            this.eventBus = new LocalEventBus();
            eventBus.registerFilter((o, handlerEntry) -> {
                if(o instanceof RoboEvent) {
                    RoboEvent roboEvent = (RoboEvent) o;
                    Method eventMethod = handlerEntry.getEventMethod();
                    EventSource eventSource = eventMethod.getAnnotation(EventSource.class);
                    if(eventSource != null) {
                        Optional<String> supportedSource = asList(eventSource.value()).stream()
                                .filter(s -> s.equalsIgnoreCase(roboEvent.getSource()))
                                .findFirst();

                        if(!supportedSource.isPresent()) {
                            return true;
                        }
                    }
                }
                return false;
            });
        } catch (Exception e) {
            throw new RoboException("Unable to create Memory connection to NAO", e);
        }
    }

    public void listenToEvent(String event) {
        try {
            LOG.info("Subscribing to event: {}", event);
            eventIds.add(memory.subscribeToEvent(event, o -> {
                LOG.debug("Received event: {} for source: {}", o, event);
                if(o instanceof Float) {
                    eventBus.publish(new TriggerEvent(((Float)o > 0), event));
                } else if(o instanceof Integer) {
                    eventBus.publish(new NumberEvent(event, (Integer)o));
                } else if(o instanceof List) {
                    List<Object> values = (List<Object>)o;

                    eventBus.publish(new ListValueEvent(values, event));
                } else {
                    LOG.debug("Some unknown type was sent: {}", o);
                }

            }));
        } catch (Exception e) {
            throw new RoboException("Unable to subscribe to event: " + event, e);
        }
    }

    public void registerListener(EventHandler handler) {
        this.eventBus.registerHandler(handler);

        Method[] methods = handler.getClass().getMethods();
        asList(methods).forEach(m -> {
            EventSource eventSource = m.getAnnotation(EventSource.class);
            if(eventSource != null) {
                asList(eventSource.value()).forEach(this::listenToEvent);
            }
        });
    }

    public void shutdown() {
        eventIds.forEach(e -> {
            try {
                memory.unsubscribeToEvent(e);
            } catch (InterruptedException | CallError ex) {
                LOG.error("Unable to cleanly unsubscribe from event: ", ex.getMessage());
            }
        });
    }
}
