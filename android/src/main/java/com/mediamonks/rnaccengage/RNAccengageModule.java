package com.mediamonks.rnaccengage;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;

import com.ad4screen.sdk.A4S;
import com.ad4screen.sdk.Inbox;
import com.ad4screen.sdk.Message;
import com.ad4screen.sdk.analytics.Lead;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableNativeMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by erik on 28/07/2017.
 * MediaMonks 2017
 */

class RNAccengageModule extends ReactContextBaseJavaModule {
    private static final String ACCENGAGE = "RNAccengageModule";

    private static final String ERROR_LOADING_INBOX = "loading_inbox_failed";
    private static final String ERROR_LOADING_MESSAGE = "loading_message_failed";
    private static final String ERROR_ALREADY_LOADING = "already_loading";
    private static final String ERROR_GENERAL = "general_error";

    RNAccengageModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return ACCENGAGE;
    }

    @ReactMethod
    public void trackEvent(int key) {
        A4S.get(getReactApplicationContext()).trackEvent(key);
    }

    @ReactMethod
    public void trackEventWithCustomData(int key, ReadableMap customData) {
        if (customData == null) {
            this.trackEvent(key);
            return;
        }
        if (!(customData instanceof ReadableNativeMap)) {
            Log.w(ACCENGAGE, "Custom data is sent in unsuported type and ignored");
            this.trackEvent(key);
            return;
        }

        ReadableNativeMap nativeMap = (ReadableNativeMap) customData;
        HashMap hashMap = nativeMap.toHashMap();
        JSONObject jsonObject = new JSONObject(hashMap);

        A4S.get(getReactApplicationContext()).trackEvent(key, jsonObject.toString());
    }

    @ReactMethod
    public void trackLead(String leadLabel, String leadValue) {
        if (leadLabel == null || leadLabel.isEmpty()) {
            Log.w(ACCENGAGE, "No label was supplied");
            return;
        }
        if (leadValue == null || leadValue.isEmpty()) {
            Log.w(ACCENGAGE, "No value was supplied");
            return;
        }

        Lead lead = new Lead(leadLabel, leadValue);
        A4S.get(getReactApplicationContext()).trackLead(lead);
    }

    @ReactMethod
    public void updateDeviceInfo(ReadableMap object) {
        if (object == null) {
            Log.w(ACCENGAGE, "No object was supplied");
            return;
        }
        if (!(object instanceof ReadableNativeMap)) {
            Log.w(ACCENGAGE, "Object is sent in unsupported type");
            return;
        }

        ReadableNativeMap nativeMap = (ReadableNativeMap) object;
        HashMap<String, Object> hashMap = nativeMap.toHashMap();

        Bundle bundle = new Bundle();
        for (String key : hashMap.keySet()) {
            Object value = hashMap.get(key);
            if (value instanceof String) {
                bundle.putString(key, (String) value);
            } else {
                Log.w(ACCENGAGE, "Value for key " + key + " is not a string and ignored.");
            }
        }

        A4S.get(getReactApplicationContext()).updateDeviceInfo(bundle);
    }

    private Inbox _inbox;
    private SparseArray<Message> _messages;
    private SparseArray<MessageResult> _loadedMessages;
    private int _numLoadedMessages;

    @ReactMethod
    public void getInboxMessages(final Promise promise) {
        getInboxMessagesPaginated(0, 20, promise);
    }

    @ReactMethod
    public void getInboxMessagesPaginated(final int pageIndex, final int limit, final Promise promise) {
        if (_inbox == null) {
            // If inbox doesn't exist, create a new one
            A4S.get(getReactApplicationContext()).getInbox(new A4S.Callback<Inbox>() {
                @Override public void onResult(Inbox inbox) {
                    _inbox = inbox;
                    _getInboxMessages(pageIndex, limit, promise);
                }

                @Override public void onError(int i, String s) {
                    promise.reject(ERROR_LOADING_INBOX, s);
                }
            });
        } else {
            // If inbox does exist, do the messages call
            _getInboxMessages(pageIndex, limit, promise);
        }
    }

    private void _getInboxMessages(final int pageIndex, final int limit, final Promise promise) {
        if (_loadedMessages != null) {
            promise.reject(ERROR_ALREADY_LOADING, "There's already messages being loaded");
            return;
        }
        if (_inbox == null) {
            promise.reject(ERROR_GENERAL, "Inbox was null");
            return;
        }

        // Only create a new cache if it doesn't exist right now.
        if (_messages == null) {
            _messages = new SparseArray<>();
        }

        final int startIndex = pageIndex * limit;
        final int leni = Math.min(_inbox.countMessages(), limit);

        _loadedMessages = new SparseArray<>();
        _numLoadedMessages = leni;

        for (int i = 0; i < leni; ++i) {
            final int currentIndex = startIndex + i;

            Message cachedMessage = _messages.get(currentIndex);
            if (cachedMessage != null) {
                _loadedMessages.put(currentIndex, new MessageResult(currentIndex, cachedMessage));
                _numLoadedMessages--;

                resolvePromiseIfReady(pageIndex, limit, promise);
                return;
            }

            _inbox.getMessage(currentIndex, new A4S.MessageCallback() {
                @Override public void onResult(Message message, int loadedMessageIndex) {
                    if (_inbox == null) {
                        // clearMessages was called and thus these calls are canceled and can be ignored.
                        return;
                    }

                    _loadedMessages.put(loadedMessageIndex, new MessageResult(loadedMessageIndex, message));
                    _numLoadedMessages--;

                    resolvePromiseIfReady(pageIndex, limit, promise);
                }

                @Override public void onError(int failedMessageIndex, String s) {
                    if (_inbox == null) {
                        // clearMessages was called and thus these calls are canceled and can be ignored.
                        return;
                    }

                    _loadedMessages.put(failedMessageIndex, new MessageResult(failedMessageIndex, s));
                    _numLoadedMessages--;

                    resolvePromiseIfReady(pageIndex, limit, promise);
                }
            });
        }

        if (leni == 0) {
            resolvePromiseIfReady(pageIndex, limit, promise);
        }
    }

    private void resolvePromiseIfReady(int pageIndex, int limit, Promise promise) {
        // Check if all messages are loaded or have failed
        if (_numLoadedMessages == 0) {

            final int startIndex = pageIndex * limit;
            final int leni = Math.min(_inbox.countMessages(), limit);

            WritableArray messageList = Arguments.createArray();

            for (int i = 0; i < leni; ++i) {
                int currentIndex = startIndex + i;
                MessageResult messageResult = _loadedMessages.get(currentIndex);

                if (messageResult == null) {
                    if (promise != null) {
                        promise.reject(ERROR_GENERAL, "A result was null.");
                    }
                    return;
                }

                Message loadedMessage = messageResult.getMessage();

                if (loadedMessage != null) {
                    // Merge to cache
                    _messages.put(currentIndex, loadedMessage);

                    // Handle for callback
                    messageList.pushMap(transformMessageToMap(currentIndex, loadedMessage, true));
                } else {
                    WritableMap map = Arguments.createMap();
                    map.putString("type", "error");
                    map.putInt("index", messageResult.getIndex());
                    map.putString("error", messageResult.getError());
                    messageList.pushMap(map);
                }
            }

            _loadedMessages = null;

            promise.resolve(messageList);
        }
    }

    private WritableMap transformMessageToMap(int index, Message message, boolean limitBody) {
        WritableMap map = Arguments.createMap();

        String text = message.getText();
        if (limitBody && text.length() > 140) {
            text = text.substring(0, 140);
        }

        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : message.getCustomParameters().entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }

        map.putString("type", "message");
        map.putInt("index", index);
        map.putString("subject", message.getTitle());
        map.putString("category", message.getCategory());
        map.putString("summary", text);
        map.putDouble("timestamp", message.getSendDate().getTime() / 1000);
        map.putString("sender", message.getSender());
        map.putBoolean("read", message.isRead());
        map.putBoolean("archived", message.isArchived());

        WritableMap customParameters = Arguments.fromBundle(bundle);
        map.putMap("customParameters", customParameters);

        String contentType = null;
        switch (message.getContentType()) {
            case Text:
                contentType = "text";
                break;
            case Web:
                contentType = "web";
                break;
            default:
                break;
        }

        if (contentType != null) {
            WritableMap contentMap = Arguments.createMap();

            contentMap.putString("type", contentType);
            contentMap.putString("body", message.getBody());

            WritableArray buttons = Arguments.createArray();
            for (int i = 0; i < message.countButtons(); ++i) {
                Message.Button button = message.getButton(i);
                WritableMap buttonMap = Arguments.createMap();
                buttonMap.putInt("index", i);
                buttonMap.putString("title", button.getTitle());
                buttons.pushMap(buttonMap);
            }
            contentMap.putArray("buttons", buttons);

            map.putMap("content", contentMap);
        }

        return map;
    }

    private void handleMessageResolver(final int index, final Promise promise) {
        Message message = _messages.get(index);
        if (message != null) {
            final boolean hasDetails = message.getContentType() == Message.MessageContentType.Text;

            message.display(getReactApplicationContext(), new A4S.Callback<Message>() {
                @Override public void onResult(Message displayedMessage) {
                    if (displayedMessage != null) {
                        _messages.put(index, displayedMessage);
                    }
                    if (hasDetails) {
                        WritableMap messageData = transformMessageToMap(index, displayedMessage, false);
                        promise.resolve(messageData);
                    }
                }

                @Override public void onError(int i, String s) {
                    promise.reject(ERROR_GENERAL, s);
                }
            });

            if (!hasDetails) {
                promise.resolve(null);
            }
        } else {
            promise.reject(ERROR_LOADING_MESSAGE, "Message was not loaded");
        }
    }

    @ReactMethod
    public void getMessageAtIndex(final int index, final Promise promise) {
        // See if we have a cached message for that index and return it if so.
        if (_messages != null && _messages.get(index) != null) {
            handleMessageResolver(index, promise);
            return;
        }

        // Do validation before we load a missing message
        if (_inbox == null) {
            promise.reject(ERROR_GENERAL, "Inbox doesn't exist anymore");
            return;
        }
        if (_messages == null) {
            promise.reject(ERROR_GENERAL, "Messages disappeared");
            return;
        }
        if (_loadedMessages != null) {
            promise.reject(ERROR_ALREADY_LOADING, "Messages are already being loaded");
            return;
        }
        if (index < 0 || index >= _inbox.countMessages()) {
            promise.reject(ERROR_LOADING_MESSAGE, "Requested index is out of bounds");
            return;
        }

        // If the message was lost or never loaded, re-load it.
        _inbox.getMessage(index, new A4S.MessageCallback() {
            @Override public void onResult(Message message, int loadedMessageIndex) {
                handleMessageResolver(loadedMessageIndex, promise);
            }

            @Override public void onError(int failedMessageIndex, String s) {
                promise.reject(ERROR_LOADING_MESSAGE, s);
            }
        });
    }

    @ReactMethod
    public void interactWithButton(final int buttonIndex, final int index, final Promise promise) {
        if (_inbox == null) {
            promise.reject(ERROR_GENERAL, "Inbox doesn't exist anymore");
            return;
        }
        if (_messages == null) {
            promise.reject(ERROR_GENERAL, "Messages disappeared");
            return;
        }

        Message message = _messages.get(index);

        if (message == null) {
            promise.reject(ERROR_GENERAL, "Couldn't find the message to interact with");
            return;
        }

        if (buttonIndex >= message.countButtons()) {

            promise.reject(ERROR_GENERAL, "Couldn't find the button in this content");
            return;
        }

        final Message.Button button = message.getButton(buttonIndex);

        if (button == null) {

            promise.reject(ERROR_GENERAL, "Couldn't find the button in this content");
            return;
        }

        button.click(getReactApplicationContext());

        promise.resolve(transformMessageToMap(index, message, false));
    }

    @ReactMethod
    public void markMessageAsRead(final int index, final boolean read, final Promise promise) {
        if (_inbox == null) {
            promise.reject(ERROR_GENERAL, "Inbox doesn't exist anymore");
            return;
        }
        if (_messages == null) {
            promise.reject(ERROR_GENERAL, "Messages disappeared");
            return;
        }

        Message message = _messages.get(index);

        if (message == null) {
            promise.reject(ERROR_GENERAL, "Couldn't find the message to mark");
            return;
        }

        message.setRead(read);
        A4S.get(getReactApplicationContext()).updateMessages(_inbox);

        promise.resolve(transformMessageToMap(index, message, false));
    }

    @ReactMethod
    public void markMessageAsDisplayed(final int index, final boolean diplayed, final Promise promise) {
        if (_inbox == null) {
            promise.reject(ERROR_GENERAL, "Inbox doesn't exist anymore");
            return;
        }
        if (_messages == null) {
            promise.reject(ERROR_GENERAL, "Messages disappeared");
            return;
        }

        Message message = _messages.get(index);

        if (message == null) {
            promise.reject(ERROR_GENERAL, "Couldn't find the message to mark");
            return;
        }

        message.setDisplayed(diplayed);
        A4S.get(getReactApplicationContext()).updateMessages(_inbox);

        promise.resolve(transformMessageToMap(index, message, false));
    }

    @ReactMethod
    public void markMessageAsArchived(final int index, final boolean archived, final Promise promise) {
        if (_inbox == null) {
            promise.reject(ERROR_GENERAL, "Inbox doesn't exist anymore");
            return;
        }
        if (_messages == null) {
            promise.reject(ERROR_GENERAL, "Messages disappeared");
            return;
        }

        Message message = _messages.get(index);

        if (message == null) {
            promise.reject(ERROR_GENERAL, "Couldn't find the message to mark");
            return;
        }

        message.setArchived(archived);
        A4S.get(getReactApplicationContext()).updateMessages(_inbox);

        promise.resolve(transformMessageToMap(index, message, false));
    }

    @ReactMethod
    public void trackDisplay(final int index, final Promise promise) {
        if (_inbox == null) {
            promise.reject(ERROR_GENERAL, "Inbox doesn't exist anymore");
            return;
        }
        if (_messages == null) {
            promise.reject(ERROR_GENERAL, "Messages disappeared");
            return;
        }

        Message message = _messages.get(index);

        if (message == null) {
            promise.reject(ERROR_GENERAL, "Couldn't find the message to track");
            return;
        }

        message.hasBeenDisplayedToUser(getReactApplicationContext());

        promise.resolve(transformMessageToMap(index, message, false));
    }

    @ReactMethod
    public void trackOpening(final int index, final Promise promise) {
        if (_inbox == null) {
            promise.reject(ERROR_GENERAL, "Inbox doesn't exist anymore");
            return;
        }
        if (_messages == null) {
            promise.reject(ERROR_GENERAL, "Messages disappeared");
            return;
        }

        Message message = _messages.get(index);

        if (message == null) {
            promise.reject(ERROR_GENERAL, "Couldn't find the message to track");
            return;
        }

        message.hasBeenOpenedByUser(getReactApplicationContext());

        promise.resolve(transformMessageToMap(index, message, false));
    }

    @ReactMethod
    public void clearMessages() {
        _messages = null;
        _loadedMessages = null;
        _inbox = null;
    }

    private class MessageResult {

        private final int _index;

        private Message _message;
        private String _error;

        MessageResult(int index, Message message) {
            _index = index;
            _message = message;
        }

        MessageResult(int index, String error) {
            _index = index;
            _error = error;
        }

        int getIndex() {
            return _index;
        }

        Message getMessage() {
            return _message;
        }

        String getError() {
            return _error;
        }
    }
}
