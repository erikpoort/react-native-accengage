package com.mediamonks.rnaccengage;

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
import com.facebook.react.bridge.WritableMap;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by erik on 28/07/2017.
 * MediaMonks 2017
 */

class RNAccengageModule extends ReactContextBaseJavaModule {
    private static final String ACCENGAGE = "RNAccengageModule";

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

    // TODO useful errors
    // TODO Sync errors with iOS version
    // TODO add mark as read
    // TODO add mark as archive

    private Inbox _inbox;
    private SparseArray<Message> _messages; // TODO primitives
    private SparseArray<Message> _loadedMessages; // TODO primitives
    private int _numLoadedMessages;

    @ReactMethod
    public void getInboxMessages(final Promise promise) {
        getInboxMessages(0, 10, promise);
    }

    @ReactMethod
    public void getInboxMessages(final int pageIndex, final int limit, final Promise promise) {
        if (_inbox == null) {
            // If inbox doesn't exist, create a new one
            A4S.get(getReactApplicationContext()).getInbox(new A4S.Callback<Inbox>() {
                @Override public void onResult(Inbox inbox) {
                    _inbox = inbox;
                    _getInboxMessages(pageIndex, limit, promise);
                }

                @Override public void onError(int i, String s) {
                    promise.reject(ACCENGAGE, s);
                }
            });
        } else {
            // If inbox does exist, do the messages call
            _getInboxMessages(pageIndex, limit, promise);
        }
    }

    private void _getInboxMessages(final int pageIndex, final int limit, final Promise promise) {
        if (_loadedMessages != null) {
            promise.reject(ACCENGAGE, "There's already messages being loaded");
            return;
        }
        if (_inbox == null) {
            promise.reject(ACCENGAGE, "Inbox was null");
            return;
        }

        // Only create a new cache if it doesn't exist right now.
        if (_messages == null) {
            _messages = new SparseArray<>();
        }

        final int startIndex = pageIndex * limit;
        final int leni = Math.max(_inbox.countMessages(), startIndex + limit);

        _loadedMessages = new SparseArray<>();
        _numLoadedMessages = leni;

        for (int i = 0; i < leni; ++i) {
            final int currentIndex = startIndex + i;

            Message cachedMessage = _messages.get(currentIndex);
            if (cachedMessage != null) {
                _loadedMessages.put(currentIndex, cachedMessage);
                _numLoadedMessages--;

                resolvePromiseIfReady(pageIndex, limit, promise);
                return;
            }

            _inbox.getMessage(currentIndex, new A4S.MessageCallback() {
                @Override public void onResult(Message message, int loadedMessageIndex) {
                    if (_inbox == null) {
                        // clearMessages was called and thus these calls are canceled and can be ignored.
                        if (promise != null) {
                            promise.reject(ACCENGAGE, "Canceled");
                        }
                    }

                    _loadedMessages.put(loadedMessageIndex, message);
                    _numLoadedMessages--;

                    resolvePromiseIfReady(pageIndex, limit, promise);
                }

                @Override public void onError(int failedMessageIndex, String s) {
                    if (_inbox == null) {
                        // clearMessages was called and thus these calls are canceled and can be ignored.
                        if (promise != null) {
                            promise.reject(ACCENGAGE, "Canceled");
                        }
                    }

                    _numLoadedMessages--;

                    resolvePromiseIfReady(pageIndex, limit, promise);
                }
            });
        }
    }

    private void resolvePromiseIfReady(int pageIndex, int limit, Promise promise) {
        // Check if all messages are loaded or have failed
        if (_numLoadedMessages == 0) {

            final int startIndex = pageIndex * limit;
            final int leni = Math.min(_inbox.countMessages(), startIndex + limit);

            List<WritableMap> messageList = new ArrayList<>();

            for (int i = 0; i < leni; ++i) {
                int currentIndex = startIndex + i;
                Message loadedMessage = _loadedMessages.get(currentIndex);

                if (loadedMessage != null) {
                    // Merge to cache
                    _messages.put(currentIndex, loadedMessage);

                    // Handle for callback
                    messageList.add(transformMessageToMap(loadedMessage, true));
                }
                // else {
                //      TODO unloaded / failed messages
                // }
            }

            promise.resolve(messageList);
        }
    }

    private WritableMap transformMessageToMap(Message message, boolean limitBody) {
        WritableMap map = Arguments.createMap();
        String body = message.getBody();
        if (limitBody && body.length() > 140) {
            body = body.substring(0, 140);
        }
        map.putString("title", message.getTitle());
        map.putString("body", body);
        map.putDouble("timestamp", message.getSendDate().getTime());
        map.putString("category", message.getCategory());
        map.putString("sender", message.getSender());
        map.putBoolean("read", message.isRead());
        map.putBoolean("archived", message.isArchived());
        // TODO loadedMessage.getCustomParameters() / HashMap to WritableMap
        return map;
    }

    @ReactMethod
    public void getMessage(final int index, final Promise promise) {
        // See if we have a cached message for that index and return it if so.
        if (_messages != null && _messages.get(index) != null) {
            promise.resolve(transformMessageToMap(_messages.get(index), false));
            return;
        }

        // Do validation before we load a missing message
        if (_inbox == null) {
            promise.reject(ACCENGAGE, "Inbox doesn't exist");
            return;
        }
        if (_messages == null) {
            promise.reject(ACCENGAGE, "There's no messages left");
            return;
        }
        if (_loadedMessages != null) {
            promise.reject(ACCENGAGE, "There's already messages being loaded");
            return;
        }

        // If the message was lost or never loaded, re-load it.
        _inbox.getMessage(index, new A4S.MessageCallback() {
            @Override public void onResult(Message message, int loadedMessageIndex) {
                _messages.put(loadedMessageIndex, message);
                promise.resolve(transformMessageToMap(message, false));
            }

            @Override public void onError(int failedMessageIndex, String s) {
                promise.reject(ACCENGAGE, s);
            }
        });
    }

    @ReactMethod
    public void clearMessages() {
        _messages = null;
        _loadedMessages = null;
        _inbox = null;
    }
}
