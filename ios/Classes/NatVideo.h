//
//  NatVideo.h
//
//  Created by huangyake on 17/1/7.
//  Copyright © 2017 Instapp. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <MediaPlayer/MediaPlayer.h>



@interface NatVideo : NSObject

typedef void (^NatCallback)(id error, id result);


+ (NatVideo *)singletonManger;
//播放视频
- (void)play:(NSString *)path :(NatCallback)callback;
//暂停视频
- (void)pause:(NatCallback)callback;
//停止视频
- (void)stop:(NatCallback)callback;

@end
