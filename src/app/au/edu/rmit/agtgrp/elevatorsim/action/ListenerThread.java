package au.edu.rmit.agtgrp.elevatorsim.action;

import au.edu.rmit.agtgrp.elevatorsim.NetworkHelper;
import au.edu.rmit.agtgrp.elevatorsim.WrapperModel;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Listens for and processes message from the client
 *
 * @author Joshua Richards
 */
public class ListenerThread extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    private NetworkHelper connection;
    private WrapperModel model;

    private boolean closed = false;

    private Set<Integer> processedActions = new HashSet<>();
    private Map<String, MessageHandler> specialTypes = new HashMap<>();

    public ListenerThread(NetworkHelper connection, WrapperModel model) {
        this.connection = connection;
        this.model = model;
    }

    @Override
    public void run() {
        while (true) {
            try {
                JSONObject actionJson = connection.receive();
                doAction(actionJson);
            } catch (IOException e) {
                if (!closed) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

    /**
     * Processes a message received from the client. If the type
     * field is a key in specialTypes the corresponding MessageHandler
     * is invoked. Otherwise an Action is generated and added to the
     * eventQueue.
     *
     * @param actionJson the json message received
     */
    private void doAction(JSONObject actionJson) {
        int actionId = actionJson.getInt("id");
        String type = actionJson.getString("type");
        MessageHandler handler = specialTypes.get(type);

        LOG.info("Incoming Action [{}:{}]", actionId, type);
        LOG.debug("\n{}", actionJson.toString(4));

        if (handler != null) {
            handler.handleMessage(actionJson);
            return;
        }

        JSONObject params = actionJson.getJSONObject("params");
        Action action;
        switch (type) {
            case "sendCar":
                action = new SendCarAction(actionId, model, params);
                break;
            case "changeNextDirection":
                action = new ChangeNextDirectionAction(actionId, model, params);
                break;
            case "reconnected":
                action = new ReconnectedAction(actionId, model, params);
                break;
            default:
                action = new ErrorAction(
                        actionId,
                        model.getEventQueue(),
                        "Invalid action type: " + type
                );
                break;
        }

        model.getEventQueue().addEvent(action);
    }

    /**
     * Used to signify that a certain type of message is special and
     * should not be treated as an action
     *
     * @param type    the type field of the messages that should be handled
     * @param handler a listener that will be called when a matching message
     *                is received
     */
    public void setMessageHandler(String type, MessageHandler handler) {
        specialTypes.put(type, handler);
    }

    public void close() {
        closed = true;
    }

    /**
     * Listener that should be associated with a special type of message
     * that should be handled as an action
     *
     * @author Joshua Richards
     */
    @FunctionalInterface
    public interface MessageHandler {
        public void handleMessage(JSONObject message);
    }

    private class ReconnectedAction extends Action {
        private JSONObject params;

        public ReconnectedAction(long actionId, WrapperModel model, JSONObject params) {
            super(actionId, model.getEventQueue());
            this.params = params;
        }

        @Override
        protected ProcessingStatus performAction() {
            JSONArray unprocessedActions = params.getJSONArray("unprocessedActions");

            for (int i = 0; i < unprocessedActions.length(); i++) {
                JSONObject unprocessedAction = unprocessedActions.getJSONObject(i);
                int actionId = unprocessedAction.getInt("id");

                if (!processedActions.contains(actionId)) {
                    doAction(unprocessedAction);
                }
            }

            return ProcessingStatus.COMPLETED;
        }

    }
}
