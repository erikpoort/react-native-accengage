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

static NSString *const ERROR_LOADING_INBOX = @"loading_inbox_failed";
static NSString *const ERROR_LOADING_MESSAGE = @"loading_message_failed";
static NSString *const ERROR_ALREADY_LOADING = @"already_loading";
static NSString *const ERROR_GENERAL = @"general_error";

@implementation RNAccengageModule {
    BMA4SInBox *_inbox;
    NSMutableArray *_messages;
    NSMutableArray *_loadedMessages;
    NSUInteger _numLoadedMessages;
}

RCT_EXPORT_MODULE();

#pragma mark - Permissions

RCT_EXPORT_METHOD(
            hasPermissions:
            (RCTResponseSenderBlock) callback
) {
    UNUserNotificationCenter *notificationCenter = [UNUserNotificationCenter currentNotificationCenter];
    if (notificationCenter) {
        [notificationCenter getNotificationSettingsWithCompletionHandler:^(UNNotificationSettings *settings) {
            callback(@[@(settings.authorizationStatus == UNAuthorizationStatusAuthorized)]);
        }];
    } else {
        BOOL hasPermissions = [[UIApplication sharedApplication] isRegisteredForRemoteNotifications];
        callback(@[@(hasPermissions)]);
    }
}

RCT_EXPORT_METHOD(
            updatePermissions:
            (BOOL) request
            userAction:
            (BOOL) userAction
) {
    [self hasPermissions:^(NSArray <NSNumber *> *response) {
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
            trackEvent:
            (NSUInteger) key
) {
    [Accengage trackEvent:key];
}

RCT_EXPORT_METHOD(
            trackEventWithCustomData:
            (NSUInteger) key
            customData:
            (NSDictionary *) customData
) {
    if (!customData || [customData count] == 0) {
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
            trackLead:
            (NSString *) leadLabel
            value:
            (NSString *) leadValue
) {
    if (!leadLabel || [leadLabel isEqualToString:@""]) {
        NSLog(@"%@: No label was supplied", kRejectCode);
        return;
    }
    if (!leadValue || [leadValue isEqualToString:@""]) {
        NSLog(@"%@: No value was supplied", kRejectCode);
        return;
    }

    [Accengage trackLead:leadLabel value:leadValue];
}

#pragma mark - Get Inbox Messages

RCT_EXPORT_METHOD(
            getInboxMessages:(RCTPromiseResolveBlock) promise
            rejecter:(RCTPromiseRejectBlock) reject
) {
    [self getInboxMessagesWithPageIndex:0 limit:20 successCallback:^(NSArray *response) {
        promise(response);
    }                          rejecter:^(NSString *code, NSString *message, NSError *error) {
        reject(code, message, error);
    }];
}

RCT_EXPORT_METHOD(
            getInboxMessagesWithPageIndex:(NSUInteger) pageIndex
            limit:(NSUInteger) limit
            successCallback:(RCTPromiseResolveBlock) promise
            rejecter:(RCTPromiseRejectBlock) reject
) {
    //Get Accengage Inbox
    [self getAccengageInboxWithSuccess:^(BMA4SInBox *inbox) {
        _inbox = inbox;
        //Get Accengage Messsages From Index with limit
        [self getMessagesFromIndex:pageIndex limit:limit messageListCallback:^(NSArray *response) {
            promise(response);
        }                 rejecter:^(NSString *code, NSString *message, NSError *error) {
            reject(code, message, error);
        }];

    }                          failure:^(BMA4SInBoxLoadingResult result) {
        NSString *operation = (result == BMA4SInBoxLoadingResultCancelled ? @"Cancelled" : @"Failed");
        NSString *errorMessage = [NSString stringWithFormat:@"Inbox loading result has been %@", operation];
        reject(ERROR_LOADING_INBOX, errorMessage, nil);
    }];
}

- (void)getAccengageInboxWithSuccess:(void (^)(BMA4SInBox *inbox))success failure:(void (^)(BMA4SInBoxLoadingResult result))failure {
    [BMA4SInBox obtainMessagesWithCompletionHandler:^(BMA4SInBoxLoadingResult result, BMA4SInBox *inbox) {
        if (result != BMA4SInBoxLoadingResultLoaded) {
            failure(result);
        } else {
            success(inbox);
        }
    }];
}

- (void)getMessagesFromIndex:(NSUInteger)pageIndex limit:(NSUInteger)limit messageListCallback:(RCTPromiseResolveBlock)callback rejecter:(RCTPromiseRejectBlock)reject {
    if (_loadedMessages != nil) {
        reject(ERROR_ALREADY_LOADING, @"There's already messages being loaded", nil);
        return;
    }

    if (_inbox == nil) {
        reject(ERROR_GENERAL, @"Inbox was null", nil);
        return;
    }

    if (_messages == nil) {
        _messages = [NSMutableArray new];
    }

    NSUInteger startIndex = pageIndex * limit;
    NSUInteger leni = MIN(_inbox.size, limit);

    _loadedMessages = [NSMutableArray new];
    _numLoadedMessages = leni;

    for (NSUInteger i = 0; i < leni; i++) {
        NSUInteger currentIndex = startIndex + i;

        //In order to avoid index out of bounds, we check that our messages array has, at least, the value we're looking for.
        if (_messages.count >= currentIndex + 1) {
            if (_messages[currentIndex] != nil) {
                BMA4SInBoxMessage *cachedMessage = _messages[currentIndex];
                _loadedMessages[currentIndex] = cachedMessage;

                //Increase the number of loaded messages
                _numLoadedMessages--;

                [self resolvePromiseIfReadyWithPageIndex:pageIndex limit:limit messageCallback:callback rejecter:^(NSString *code, NSString *message, NSError *error) {
                    reject(code, message, error);
                }];

                return;
            }
        }

        [_inbox obtainMessageAtIndex:currentIndex loaded:^(BMA4SInBoxMessage *message, NSUInteger requestedIndex) {
            if (_inbox == nil) {
                return;
            }

            _loadedMessages[requestedIndex] = message;
            _numLoadedMessages--;

            [self resolvePromiseIfReadyWithPageIndex:pageIndex limit:limit messageCallback:callback rejecter:^(NSString *code, NSString *rejectMessage, NSError *error) {
                reject(code, rejectMessage, error);
            }];
        }                    onError:^(NSUInteger requestedIndex) {
            if (_inbox == nil) {
                return;
            }

            //remove number of loaded messages when the service call had failed, without changing indexes
            _loadedMessages[requestedIndex] = [NSNull null];
            _numLoadedMessages--;

            [self resolvePromiseIfReadyWithPageIndex:pageIndex limit:limit messageCallback:callback rejecter:^(NSString *code, NSString *message, NSError *error) {
                reject(code, message, error);
            }];

        }];
    }

    if (leni == 0) {
        [self resolvePromiseIfReadyWithPageIndex:pageIndex limit:limit messageCallback:callback rejecter:^(NSString *code, NSString *message, NSError *error) {
            reject(code, message, error);
        }];
    }
}

RCT_EXPORT_METHOD(
            resolvePromiseIfReadyWithPageIndex:(NSUInteger) pageIndex
            limit:(NSUInteger) limit
            messageCallback:(RCTPromiseResolveBlock) promise
            rejecter:(RCTPromiseRejectBlock) reject
) {
    if (_numLoadedMessages == 0) {
        NSUInteger startIndex = pageIndex * limit;
        NSUInteger leni = MIN(_inbox.size, startIndex + limit);

        NSMutableArray *messageList = [NSMutableArray new];

        for (NSUInteger i = 0; i < leni; i++) {
            NSUInteger currentIndex = startIndex + i;

            BMA4SInBoxMessage *loadedMessage = _loadedMessages[currentIndex];
            if ([loadedMessage isKindOfClass:[BMA4SInBoxMessage classForCoder]]) {

                _messages[currentIndex] = loadedMessage;
                NSDictionary *messageData = [self getMessageDictionary:currentIndex message:loadedMessage withLimitBody:true];
                [messageList addObject:messageData];
            } else {
                //if get message call failed
                NSDictionary *errorMessageData = @{
                        @"type": @"error",
                        @"index": @(currentIndex),
                };
                [messageList addObject:errorMessageData];
            }
        }

        promise(messageList);
    }
}

- (NSDictionary *)getMessageDictionary:(NSUInteger)index message:(BMA4SInBoxMessage *)message withLimitBody:(BOOL)isLimitBody {
    NSString *text = message.text;

    if (isLimitBody && message.text.length > 140) {
        text = [text substringToIndex:140];
    }

    //Create Message Dictionary
    NSDictionary *messageData = @{
            @"type": @"message",
            @"index": @(index),
            @"title": message.title,
            @"body": text,
            @"timestamp": [NSString stringWithFormat:@"%lli", [@(floor(message.date.timeIntervalSince1970)) longLongValue]],
            @"category": message.category,
            @"sender": message.from,
            @"read": @(message.isRead),
            @"archived": @(message.isArchived),
            @"customParameters": message.customParams
    };

    return messageData;
}

RCT_EXPORT_METHOD(
            getMessageAtIndex:(NSUInteger) index
            messageCallback:(RCTPromiseResolveBlock) promise
            rejecter:(RCTPromiseRejectBlock) reject
) {
    //See if we have a cached message for that index and return it if so
    if (_messages != nil && _messages.count >= index) {
        if (_messages[index] != nil) {
            NSDictionary *messageData = [self getMessageDictionary:index message:_messages[index] withLimitBody:YES];
            promise(messageData);
            return;
        }
    }

    if (_inbox == nil) {
        reject(ERROR_GENERAL, @"Inbox doesn't exist anymore", nil);
        return;
    }

    if (_messages == nil) {
        reject(ERROR_GENERAL, @"Messages disappeared", nil);
        return;
    }

    if (_loadedMessages == nil) {
        reject(ERROR_ALREADY_LOADING, @"Messages are already being loaded", nil);
        return;
    }

    if (index < 0 || index >= _inbox.size) {
        reject(ERROR_LOADING_MESSAGE, @"Requested index is out of bounds", nil);
        return;
    }

    NSUInteger nsi = (NSUInteger) index;
    [_inbox obtainMessageAtIndex:nsi loaded:^(BMA4SInBoxMessage *message, NSUInteger requestedIndex) {
        NSDictionary *messageData = [self getMessageDictionary:requestedIndex message:message withLimitBody:YES];
        promise(messageData);
    }                    onError:^(NSUInteger requestedIndex) {
        NSString *errorMessage = [NSString stringWithFormat:@"Error loading message with index %i", requestedIndex];
        reject(ERROR_LOADING_MESSAGE, errorMessage, nil);
    }];
}

RCT_EXPORT_METHOD(
            markMessageAsRead:(NSUInteger) index
            read:(BOOL) read
            callback:(RCTPromiseResolveBlock) promise
            rejecter:(RCTPromiseRejectBlock) reject
) {

    if (_inbox == nil) {
        reject(ERROR_GENERAL, @"Inbox doesn't exist anymore", nil);
        return;
    }

    if (_messages == nil) {
        reject(ERROR_GENERAL, @"Messages disappeared", nil);
        return;
    }


    BMA4SInBoxMessage *message = _messages[index];

    if (message == nil) {
        reject(ERROR_GENERAL, @"Couldn't find the message to mark", nil);
        return;
    }

    if (read) {
        [message markAsRead];
    } else {
        [message markAsUnread];
    }

    NSDictionary *messageData = [self getMessageDictionary:index message:message withLimitBody:NO];
    promise(messageData);
}

RCT_EXPORT_METHOD(
            markMessageAsArchived:(NSUInteger) index
            archived:(BOOL) archived
            callback:(RCTPromiseResolveBlock) promise
            rejecter:(RCTPromiseRejectBlock) reject
) {

    if (_inbox == nil) {
        reject(ERROR_GENERAL, @"Inbox doesn't exist anymore", nil);
        return;
    }

    if (_messages == nil) {
        reject(ERROR_GENERAL, @"Messages disappeared", nil);
        return;
    }

    BMA4SInBoxMessage *message = _messages[index];

    if (message == nil) {
        reject(ERROR_GENERAL, @"Couldn't find the message to mark", nil);
        return;
    }

    if (archived) {
        [message archive];
    } else {
        [message unarchive];
    }

    NSDictionary *messageData = [self getMessageDictionary:index message:message withLimitBody:NO];
    promise(messageData);
}

#pragma mark - Device info

RCT_EXPORT_METHOD(
            updateDeviceInfo:(NSDictionary *) object
) {
    if (!object || object.count == 0) {
        NSLog(@"No fields were added");
        return;
    }

    [Accengage updateDeviceInfo:object];
}

#pragma mark - Clear Messages

RCT_EXPORT_METHOD(
        clearMessages
) {
    _messages = nil;
    _loadedMessages = nil;
    _inbox = nil;
}

@end

