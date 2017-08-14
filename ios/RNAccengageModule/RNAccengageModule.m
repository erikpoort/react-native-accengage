//
//  RNAccengageModule.m
//  RNAccengageModule
//
//  Created by Erik Poort on 28/07/2017.
//  Copyright (c) 2017 MediaMonks. All rights reserved.
//

#import "RNAccengageModule.h"
#import <Accengage/Accengage.h>

static NSString *const kRejectCode = @"RNAccengageModule.h";
static NSString *const kPushRequested = @"pushRequested";

@implementation RNAccengageModule
RCT_EXPORT_MODULE();

#pragma mark - Permissions

RCT_EXPORT_METHOD(
	hasPermissions:(RCTResponseSenderBlock)callback
) {
	UNUserNotificationCenter *notificationCenter = [UNUserNotificationCenter currentNotificationCenter];
	if (notificationCenter) {
		[notificationCenter getNotificationSettingsWithCompletionHandler:^(UNNotificationSettings *settings)
		{
			callback(@[@(settings.authorizationStatus == UNAuthorizationStatusAuthorized)]);
		}];
	} else {
		BOOL hasPermissions = [[UIApplication sharedApplication] isRegisteredForRemoteNotifications];
		callback(@[@(hasPermissions)]);
	}
}

RCT_EXPORT_METHOD(
	requestPermissions
) {
	[self hasPermissions:^(NSArray <NSNumber *> *response)
	{
		BOOL hasPermissions = response.firstObject.boolValue;

		if (!hasPermissions) {
			if ([[NSUserDefaults standardUserDefaults] boolForKey:kPushRequested]) {
				NSURL *url = [NSURL URLWithString:UIApplicationOpenSettingsURLString];
				[[UIApplication sharedApplication] openURL:url];
			} else {
				[[NSUserDefaults standardUserDefaults] setBool:YES forKey:kPushRequested];
				ACCNotificationOptions options = (ACCNotificationOptionSound | ACCNotificationOptionBadge | ACCNotificationOptionAlert | ACCNotificationOptionCarPlay);
				[[Accengage push] registerForUserNotificationsWithOptions:options];
			}
		}
	}];
}

#pragma mark - Tracking

RCT_EXPORT_METHOD(
    trackEvent:(NSUInteger)key
) {
    [Accengage trackEvent:key];
}

RCT_EXPORT_METHOD(
    trackEventWithCustomData:(NSUInteger)key
    customData:(NSDictionary *)customData
) {
    if (!customData) {
        [Accengage trackEvent:key];
        return;
    }
    
    NSError *error = nil;
    NSData *data = [NSJSONSerialization dataWithJSONObject:customData options:0 error:&error];
    
    if (error) {
        NSLog(@"Custom data is sent in unsuported type and ignored");
        [Accengage trackEvent:key];
        return;
    }
    
    NSString *jsonString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    [Accengage trackEvent:key withParameters:@[jsonString]];
}

RCT_EXPORT_METHOD(
	trackLead:(NSString *)label
	value:(NSString *)value
) {
	if (!label || [label isEqualToString:@""]) {
		NSLog(@"%@: No label was supplied", kRejectCode);
		return;
	}
	if (!value || [value isEqualToString:@""]) {
		NSLog(@"%@: No value was supplied", kRejectCode);
		return;
	}

	[Accengage trackLead:label value:value];
}

#pragma mark - Device info

RCT_EXPORT_METHOD(
		updateDeviceInfo:(NSDictionary *)fields
) {
	if (!fields || fields.count == 0) {
		NSLog(@"No fields were added");
		return;
	}

	[Accengage updateDeviceInfo:fields];
}

@end
