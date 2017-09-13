package com.mediamonks.rnaccengage;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by erik on 28/07/2017.
 * MediaMonks 2017
 */

public class RNAccengagePackage implements ReactPackage
{
	@Override
	public List<NativeModule> createNativeModules(ReactApplicationContext reactContext)
	{
		List<NativeModule> modules = new ArrayList<>();
		modules.add(new RNAccengageModule(reactContext));
		return modules;
	}

	@Override
	public List<ViewManager> createViewManagers(ReactApplicationContext reactContext)
	{
		return Collections.emptyList();
	}
}
