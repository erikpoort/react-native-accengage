//
//  RNAccengageModule.h
//  RNAccengageModule
//
//  Created by Erik Poort on 28/07/2017.
//  Copyright (c) 2017 MediaMonks. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTUtils.h>

@interface RNAccengageModule : NSObject <RCTBridgeModule>

typedef NS_ENUM(NSInteger, PlayerStateType) {
    /**
     * Use this type in the class/instance method call to signify the state of the Accengage Call state: Cancelled
     */
    AccengageCallResultCancelled,
    /**
     * Use this type in the class/instance method call to signify the state of the Accengage Call state: Failed
     */
    AccengageCallResultFailed,
    /**
     * Use this type in the class/instance method call to signify the state of the Accengage Call state: Error
     */
    AccengageCallResultError,
    /**
     * Use this type in the class/instance method call to signify the state of the Accengage Call state: Loading
     */
    AccengageCallIsLoading,
    /**
     * Use this type in the class/instance method call to signify the state of the Inbox Message List: Not Exists
     */
    InboxMessageListNotExists,
    /**
     * Use this type in the class/instance method call to signify the state of the Message: Not Exists
     */
    MessageNotExists,
    /**
     * Use this type in the class/instance method call to signify the state of the Inbox: Not Exists
     */
    InboxNotExists
};
@end
