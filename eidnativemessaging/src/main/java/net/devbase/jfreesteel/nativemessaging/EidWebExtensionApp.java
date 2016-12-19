package net.devbase.jfreesteel.nativemessaging;

import net.devbase.jfreesteel.EidCard;
import net.devbase.jfreesteel.EidInfo;
import net.devbase.jfreesteel.Utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.*;
import javax.smartcardio.CardTerminals.State;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;


public class EidWebExtensionApp {

    private final static Logger logger = LoggerFactory.getLogger(EidWebExtensionApp.class);

    private InputStream in;
    private OutputStream out;

    private CardTerminals terminals;
    private List<String> terminalNames;

    public static void main(String[] args) {

        logger.info("Starting web extensions native messaging background app...");

        new EidWebExtensionApp();
    }

    static private final int ERROR_NO_INPUT_STREAM = 1;
    static private final int ERROR_UNKNOWN_COMMAND = 2;
    static private final int ERROR_NO_TERMINAL_FACTORY = 3;
    static private final int ERROR_TERMINALS_EXCEPTION = 4;
    static private final int ERROR_NO_TERMINALS = 5;
    static private final int ERROR_CARD_EXCEPTION = 6;
    static private final int ERROR_CARD_UNKNOWN = 7;

    private EidWebExtensionApp() {
        out = System.out;
        in = null;

        // Test for input stream
        try {
            System.in.available();
            in = System.in;
        } catch (IOException e) {
            sendError("No input stream", ERROR_NO_INPUT_STREAM, e);
        }

        initTerminals();
        observeTerminals(terminals);
        if (in != null) {
            observeInput();
        }
    }

    private void initTerminals() {
        TerminalFactory factory = TerminalFactory.getDefault();

        // Check for none
        String type = factory.getType();
        if (type.equals("None")) {
            sendError("Terminal factory not found", ERROR_NO_TERMINAL_FACTORY);
            System.exit(ERROR_NO_TERMINAL_FACTORY);
        }

        logger.info(String.format("Using terminal factory type %s", factory.getType()));

        // Get terminals
        terminals = factory.terminals();

        terminalNames = new LinkedList<>();
        try {
            for (CardTerminal terminal : terminals.list(State.ALL)) {
                terminalNames.add(terminal.getName());
            }
        } catch (CardException e) {
            sendError("Error listing terminals", ERROR_TERMINALS_EXCEPTION, e);
        }

        if (terminalNames.size() == 0) {
            sendError("Card terminal not found", ERROR_NO_TERMINALS);
            System.exit(ERROR_NO_TERMINALS);
        }
    }

    private void observeInput() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                while (true) {

                    // Read message
                    String msg;
                    try {
                        msg = receiveMessage();
                    } catch (IOException e) {
                        logger.error("Error receiving message", e);
                        continue;
                    }

                    if (msg == null) {
                        logger.info("Closing input stream");
                        break;
                    }

                    if (msg.length() > 2) {
                        /* primitive "json decode" for string commands */
                        msg = msg.substring(1, msg.length() - 1);
                    }

                    // Command switch
                    switch (msg) {
                        case "":
                            break;
                        case "terminals":
                            sendTerminals();
                            break;
                        case "quit":
                            logger.info("Quiting host app on quit command");
                            System.exit(0);
                            break;
                        default:
                            sendError(String.format("Unknown command '%s'", msg), ERROR_UNKNOWN_COMMAND);
                    }
                }
            }
        });
        thread.start();
    }

    private void observeTerminals(final CardTerminals terminals) {
        Thread thread = new Thread(new Runnable() {
            public void run() {

                Set<String> connectedTerminalNames = new HashSet<>();

                while (true) {
                    try {
                        for (CardTerminal terminal : terminals.list(State.CARD_INSERTION)) {
                            String terminalName = terminal.getName();
                            try {
                                EidCard eidcard = EidCard.fromCard(terminal.connect("*"));
                                inserted(eidcard, terminalName);
                                connectedTerminalNames.add(terminalName);
                            } catch (IllegalArgumentException e) {
                                sendError("Unknown card", ERROR_CARD_UNKNOWN, terminalName, e);
                            } catch (CardException e) {
                                sendError("Card error", ERROR_CARD_EXCEPTION, terminalName, e);
                            }
                        }
                        for (CardTerminal terminal : terminals.list(State.CARD_REMOVAL)) {
                            String terminalName = terminal.getName();
                            if (connectedTerminalNames.contains(terminalName)) {
                                connectedTerminalNames.remove(terminalName);
                                removed(terminalName);
                            }
                        }
                        terminals.waitForChange();
                    } catch (CardException e) {
                        sendError("Error observing terminals", ERROR_TERMINALS_EXCEPTION, e);
                    }
                }
            }
        });
        thread.start();
    }

    private String receiveMessage() throws IOException {
        byte[] lenBytes = new byte[4];
        int count = in.read(lenBytes);
        if (count == -1) {
            logger.info("End of input stream");
            return null;
        }

        int length = Utils.bytes2Int(lenBytes);

        logger.info(String.format("Receiving message size %d (%s)", length, Utils.bytes2HexStringCompact(lenBytes)));

        byte[] msgBytes = new byte[length];
        count = in.read(msgBytes);
        if (count == -1) {
            logger.error("Unexpected end of input stream");
            return null;
        }

        String msg = Utils.bytes2UTF8String(msgBytes);

        logger.info(String.format("%s\n%s", msg, Utils.bytes2HexString(msgBytes)));

        return msg;
    }

    private void sendError(String error, int code) {
        sendError(error, code, null, null);
    }

    private void sendError(String error, int code, Exception ex) {
        sendError(error, code, null, ex);
    }

    private void sendError(String error, int code, String terminalName, Exception ex) {

        logger.error(String.format("%s (code %d)", error, code), ex);

        JSONObject obj = new JSONObject();
        obj.put("error", error);
        obj.put("code", code);
        if (terminalName != null)
            obj.put("terminal", terminalName);

        sendMessage(obj.toString());
    }

    private synchronized void sendMessage(String msg) {

        try {
            byte[] msgBytes = msg.getBytes();

            int length = msgBytes.length;
            byte[] lenBytes = Utils.int2Bytes(length);

            logger.info(String.format("Sending message size %d (%s)", length, Utils.bytes2HexStringCompact(lenBytes)));
            logger.info(String.format("%s\n%s", msg, Utils.bytes2HexString(msgBytes)));

            out.write(lenBytes);
            out.write(msgBytes);

            out.flush();
        } catch (IOException e) {
            logger.error("Error sending message", e);
        }
    }

    private void sendTerminals() {
        sendMessage(JSONArray.toJSONString(terminalNames));
    }

    private void inserted(final EidCard card, String terminalName) {
        logger.info("Card inserted");

        try {
            JSONObject obj = new JSONObject();

            EidInfo info = card.readEidInfo();
            obj.put("info", info.toJSON());

            Image image = card.readEidPhoto();
            String photo = Utils.image2Base64String(image);
            obj.put("photo", photo);

            obj.put("terminal", terminalName);

            sendMessage(obj.toString());
        } catch (CardException e) {
            sendError("Error reading card info", ERROR_CARD_EXCEPTION, terminalName, e);
        }
    }

    private void removed(String terminalName) {
        logger.info("Card removed");

        JSONObject obj = new JSONObject();
        obj.put("event", "removed");
        obj.put("terminal", terminalName);
        sendMessage(obj.toString());
    }
}
