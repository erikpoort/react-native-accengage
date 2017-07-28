package com.mediamonks.rnaccengage;

import android.util.Log;

import com.ad4screen.sdk.A4S;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableNativeMap;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by erik on 28/07/2017.
 * MediaMonks 2017
 */

class RNAccengageModule extends ReactContextBaseJavaModule
{
	public static final String ACCENGAGE = "RNAccengageModule";

	RNAccengageModule(ReactApplicationContext reactContext)
	{
		super(reactContext);
	}

	@Override
	public String getName()
	{
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
}
