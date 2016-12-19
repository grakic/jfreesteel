var HOST_NAME = 'net.devbase.jfreesteel.eidnativemessaging';
var EXTENSION_NAME = HOST_NAME;

var nativePorts = {};
var contentPorts = {};

function openNativePort(portId) {
    var port = chrome.extension.connectNative(HOST_NAME);

    // receive from host, forward to content
    port.onMessage.addListener(function(message) {
        console.log("Background received from host", message);
        sendContent(message, portId);
    });

    // handle host disconnect
    port.onDisconnect.addListener(function() {
        var error = chrome.runtime.lastError;
        if (error) {
            console.error("Host error", error.message);
            sendContent({
                "error": error.message
            }, portId);
        }
        console.log("Host disconnected");
        delete nativePorts[portId];
    });

    console.log("New native port", portId);

    nativePorts[portId] = port;
}

function handleContentPort(port) {

    var portId = port.sender.tab.id;

    port.onMessage.addListener(function(command) {

        console.log("Background received from content", command);

        // send to host app
        sendNative(command, portId);
    });

    port.onDisconnect.addListener(function() {
        console.log("background port disconnected");
        try {
            sendNative("quit", portId);
        } catch (err) {}
    });

    console.log("New content port", portId);

    contentPorts[portId] = port;
}

function sendContent(message, portId) {
    try {
        contentPorts[portId].postMessage(message);
    } catch (err) {
        console.error(err);
    }
}

function sendNative(message, portId) {

    if (!nativePorts[portId])
        openNativePort(portId);

    if (message)
        nativePorts[portId].postMessage(message);
}

// receive connection from content script
chrome.runtime.onConnect.addListener(function(port){

    if (port.name != EXTENSION_NAME) {
        console.warn('Ignoring connections not from our port name');
        return;
    }

    if (port.sender.id !== chrome.runtime.id || !port.sender.tab) {
        console.warn('Ignoring message not from our extension');
        return;
    }

    handleContentPort(port);
});