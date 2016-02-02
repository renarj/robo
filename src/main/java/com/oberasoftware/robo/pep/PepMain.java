package com.oberasoftware.robo.pep;

import com.aldebaran.qi.Application;
import com.aldebaran.qi.CallError;
import com.aldebaran.qi.Session;
import com.aldebaran.qi.helper.proxies.*;
import com.oberasoftware.base.event.EventHandler;
import com.oberasoftware.base.event.EventSubscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Renze de Vries
 */
public class PepMain implements EventHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PepMain.class);

    private static final String PEP_URL = "tcp://peppy.local:9559";

    private final ALMotion motion;
    private final ALTextToSpeech textToSpeech;

    public PepMain(ALMotion motion, ALTextToSpeech textToSpeech) {
        this.motion = motion;
        this.textToSpeech = textToSpeech;
    }

    public static void main(String[] args) {
        LOG.info("Starting Pep the Robot");
        Application application = new Application(args, PEP_URL);
        try{
            application.start();

            Session session = application.session();

            ALMotion motion = new ALMotion(session);
            ALTextToSpeech tts = new ALTextToSpeech(session);

            PepMain pep = new PepMain(motion, tts);

            SensorManager sensorManager = new SensorManager(session);
            sensorManager.registerListener(pep);

//            List<Float> positions = asList(0f);
//            List<Float> times = asList(1f, 1f);
//            motion.angleInterpolation(new String[] {"HeadYaw", "HeadPitch"}, positions,
//                    times, true);
//            motion.setAngles("HeadYaw", 0f, 0.5f);


            ALSonar sonar = new ALSonar(session);


            ALRobotPosture posture = new ALRobotPosture(session);
            ALSpeechRecognition speechRecognition = new ALSpeechRecognition(session);
            speechRecognition.subscribe("renze");
            ALRedBallDetection ballDetection = new ALRedBallDetection(session);

            ALDarknessDetection darknessDetection = new ALDarknessDetection(session);
            darknessDetection.subscribe("renze");
            darknessDetection.setDarknessThreshold(40);

//            ALBarcodeReader barcodeReader = new ALBarcodeReader(session);

//            speechRecognition.setVocabulary(Lists.newArrayList("Maap", "Bun", "Kwaak", "Miep",
//                    "Hello", "World", "Who are you", "move", "left", "right"), false);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOG.info("Doing a safe shutdown");

                try {
//                    motion.rest();
                    speechRecognition.unsubscribe("renze");
                    ballDetection.unsubscribe("renze");
                    sonar.unsubscribe("renze");
                    darknessDetection.unsubscribe("renze");
//                    barcodeReader.unsubscribe("renze");

                    sensorManager.shutdown();
                } catch (CallError | InterruptedException callError) {
                    LOG.error("Unable to rest", callError.getMessage());
                }

                LOG.info("Shutdown complete");
            }));

//            posture.goToPosture("Stand", 0.5f);

            String language = tts.getLanguage();
            tts.say("Let's rule the world together in " + language);

            sonar.subscribe("renze");
//            speechRecognition.subscribe("renze");
            ballDetection.subscribe("renze");
//            barcodeReader.subscribe("renze");

            application.run();
        }  catch(Exception e) {
            LOG.error("Unable to start Application", e);
        }
    }

    private static void stopMove(ALMotion motion) throws InterruptedException, CallError {
        LOG.info("Killing the move");
        motion.moveToward(0.0f, 0.0f, 0.0f);
    }

    @EventSubscribe
    @EventSource({"FrontTactilTouched", "MiddleTactilTouched", "RearTactilTouched"})
    public void receive(TriggerEvent triggerEvent) {
        if(triggerEvent.isOn()) {
            LOG.info("Head was touched: {}", triggerEvent.getSource());
        }
    }

//    @EventSubscribe
//    @EventSource({"SonarLeftDetected", "SonarRightDetected"})
//    public void receiveSonar(TriggerEvent triggerEvent) {
//        if(triggerEvent.isOn()) {
//            LOG.info("Something was detected: {}", triggerEvent.getSource());
//        }
//    }
//
//    @EventSubscribe
//    @EventSource({"SonarLeftNothingDetected", "SonarRightNothingDetected"})
//    public void receivePartial(TriggerEvent triggerEvent) {
//        LOG.info("We have a partial detection, nothing found on: {}", triggerEvent.getSource());
//    }

    @EventSubscribe
    @EventSource({"DarknessDetection/DarknessDetected"})
    public void receiveBacklight(NumberEvent numberEvent) {
        LOG.info("Backlight detection: {} darkness: {}", numberEvent.getSource(), numberEvent.getNumber());

        try {
            if(numberEvent.getNumber() > 40) {
                textToSpeech.say("Man it is kind of dark here");
            } else {
                textToSpeech.say("That is better");
            }
        } catch (CallError | InterruptedException callError) {
            LOG.error("", callError);
        }
    }

    @EventSubscribe
    @EventSource("WordRecognized")
    public void receiveWords(ListValueEvent valueEvent) {
        List<Object> values = valueEvent.getValues();
        String word = (String)values.get(0);
        Float certainty = (Float)values.get(1);
        LOG.info("Word: {} recognized with certainty: {}", word, certainty);

        if(certainty * 100 > 40) {
            switch(word) {
                case "left":
                    LOG.info("Looking left");
                    try {
                        motion.setAngles("HeadYaw", 1.0f, 0.1f);
                    } catch (CallError | InterruptedException callError) {
                        LOG.error("", callError);
                    }

                    break;
                case "right":
                    LOG.info("Looking right");
                    try {
                        motion.setAngles("HeadYaw", -1.0f, 0.1f);
                    } catch (CallError | InterruptedException callError) {
                        LOG.error("", callError);
                    }
                default:
                    LOG.info("Unsupported movement word: {}", word);
            }
        }
    }

    @EventSubscribe
    @EventSource("BarcodeReader/BarcodeDetected")
    public void receiveQRData(ListValueEvent event) {
        List<Object> values = event.getValues();
        LOG.info("Barcodes detected: {}", values.size());

        for(Object barcode : values) {
            List<Object> data = (List<Object>)barcode;

            String barcodeValue = (String) data.get(0);
            LOG.info("Barcode detected: {} raw data: {}", barcodeValue, data);
        }
    }

}
