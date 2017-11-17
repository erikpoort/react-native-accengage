//
//  RNAccengageModule.m
//  RNAccengageModule
//
//  Created by Erik Poort on 28/07/2017.
//  Copyright (c) 2017 MediaMonks. All rights reserved.
//

#import "RNAccengageModule.h"
#import <Accengage/Accengage.h>
#import <React/RCTUtils.h>


static NSString *const kRejectCode = @"RNAccengageModule.h";
static NSString *const kPushRequested = @"pushRequested";

@implementation RNAccengageModule
    BMA4SInBox      *_inboxMessageList;
    NSMutableArray  *_messageList;

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
	updatePermissions:(BOOL)request userAction:(BOOL)userAction
) {
	[self hasPermissions:^(NSArray <NSNumber *> *response)
	{
		BOOL hasPermissions = response.firstObject.boolValue;

		if (userAction && !hasPermissions && [[NSUserDefaults standardUserDefaults] boolForKey:kPushRequested]) {
			// There's no permissions, the user was asked before and this call is triggered by user action
			NSURL *url = [NSURL URLWithString:UIApplicationOpenSettingsURLString];
			[[UIApplication sharedApplication] openURL:url];
		} else if (request || hasPermissions) {
			// There's permissions so we are updating, or we are requesting for the first time
			[[NSUserDefaults standardUserDefaults] setBool:YES forKey:kPushRequested];
			ACCNotificationOptions options = (ACCNotificationOptionSound | ACCNotificationOptionBadge | ACCNotificationOptionAlert | ACCNotificationOptionCarPlay);
			[[Accengage push] registerForUserNotificationsWithOptions:options];
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

#pragma mark - Get Inbox Messages

RCT_EXPORT_METHOD(
    getInboxMessages:(RCTResponseSenderBlock)callback
                     rejecter:(RCTPromiseRejectBlock)reject
){

    //Get Accengage Inbox
    [self getAccengageInboxWithSuccess:^(BMA4SInBox *inbox) {
        _inboxMessageList = inbox;
        
        //get message list
        [self getAccengageMessagesWithSuccess:^(NSMutableArray* messages) {
            callback(messages);
        } failure:^(NSString *code, NSString *message, NSError *error) {
            reject(code,message,error);
        }];
        
    } failure:^(BMA4SInBoxLoadingResult result) {
        NSString *operation = (result == BMA4SInBoxLoadingResultCancelled ? @"Cancelled" : @"Failed");
        NSString *errorMessage = [NSString stringWithFormat:@"Inbox loading result had been %@",operation];
        NSString *operationCode = [NSString stringWithFormat:@"%@",@(result)];
        reject(operationCode,errorMessage,nil);
    }];
}


//Get Accengage inbox
//@success BMA4SInBox
//@failure BMA4SInBoxLoadingResult
//
- (void)getAccengageInboxWithSuccess:(void (^)(BMA4SInBox *inbox))success failure:(void (^)(BMA4SInBoxLoadingResult result))failure
{
    [BMA4SInBox obtainMessagesWithCompletionHandler:^(BMA4SInBoxLoadingResult result, BMA4SInBox *inbox) {
        if(result != BMA4SInBoxLoadingResultLoaded)
        {
            failure(result);
        }else{
            success(inbox);
        }
    }];
}

//Get Message list
//@success return a maximun of 10 messages
//@failure BMA4SInBoxLoadingResult
//
- (void)getAccengageMessagesWithSuccess:(void (^)(NSMutableArray *messages))success failure:(RCTPromiseRejectBlock)failure
{
    _messageList = [NSMutableArray new];
    
    int maximum = MIN((int)_inboxMessageList.size, 10);
    
    for (int i = 0; i < maximum; i++)
    {
        [_messageList addObject:[NSNull null]];
        [self getInboxMessageAtIndex:i+1 messageCallback:^(NSArray *response) {
            //override object at index
            [_messageList setObject:response.firstObject atIndexedSubscript:i];
            
            //if the message is the last of the array
            if(_messageList.count == i)
            {
                success(_messageList);
            }
        } rejecter:^(NSString *code, NSString *message, NSError *error) {
            failure(code,message,error);
        }];
    }
    
    success([NSMutableArray new]);
}

RCT_EXPORT_METHOD(
                  getInboxMessageAtIndex:(int)index
                  messageCallback:(RCTResponseSenderBlock)callback
                  rejecter:(RCTPromiseRejectBlock)reject
){
    //
    //Check if the Inbox message list exists
    //
    if(_inboxMessageList != nil)
    {
        NSUInteger nsi = (NSUInteger) index;
        
        [_inboxMessageList obtainMessageAtIndex:nsi loaded:^(BMA4SInBoxMessage *message, NSUInteger requestedIndex) {
        
            //Create Message Dictionary
            NSDictionary *messageData = @{@"title"       : message.title,
                                          @"body"        : message.text,
                                          @"timestamp"   : message.date,
                                          @"category"    : message.category,
                                          @"sender"      : message.from,
                                          @"read"        : [NSNumber numberWithBool:message.isRead],
                                          @"archived"    : [NSNumber numberWithBool:message.isArchived]
                                          };
            
            callback(@[messageData]);
        } onError:^(NSUInteger requestedIndex) {
            NSString *errorMessage = @"the call to obtain message at index ";
            NSString *operationCode = [NSString stringWithFormat:@"%@",@(AccengageCallResultError)];
            reject(operationCode,errorMessage,nil);
        }];
    }else{
        NSString *errorMessage = @"You need to call to getInboxMessage before call getInboxMessageAtIndex";
        NSString *operationCode = [NSString stringWithFormat:@"%@",@(InboxMessageListNotExists)];
        reject(operationCode,errorMessage,nil);
    }
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
