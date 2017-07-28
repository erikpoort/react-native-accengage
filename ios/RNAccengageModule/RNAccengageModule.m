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

@implementation RNAccengageModule
RCT_EXPORT_MODULE();

#pragma mark - Tracking

RCT_EXPORT_METHOD(
    trackEvent:(NSInteger)key
) {
    [Accengage trackEvent:key];
}

RCT_EXPORT_METHOD(
    trackEventWithCustomData:(NSInteger)key
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

@end
