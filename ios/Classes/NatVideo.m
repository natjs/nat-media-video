//
//  NatVideo.h
//
//  Created by huangyake on 17/1/7.
//  Copyright © 2017 Nat. All rights reserved.
//


#import "NatVideo.h"
#import <AVFoundation/AVFoundation.h>
#import <MobileCoreServices/MobileCoreServices.h>
#import "Reachability.h"


@interface NatVideo ()
@property(nonatomic, strong)MPMoviePlayerController *moviePlayer;
@property(nonatomic, strong)NatCallback playback;
@end

@implementation NatVideo

+ (NatVideo *)singletonManger{
    static id manager = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        manager = [[self alloc] init];
    });
    return manager;
}


- (void)play:(NSString *)path :(NatCallback)callback{
    self.playback = callback;
    if (!path || ![path isKindOfClass:[NSString class]]) {
        callback(nil,@{@"error":@{@"msg":@"MEDIA_SRC_NOT_SUPPORTED ",@"code":@110120}});
        return;
    }
    NSURL *url = [NSURL URLWithString:path];
    if ([url.scheme isEqual:@"nat"]) {
        NSString *str = [path substringFromIndex:19];
        url = [NSURL fileURLWithPath:str];
       NSString  *mimeType = [NatVideo getMimeTypeFromPath:url.absoluteString];
        if ([mimeType rangeOfString:@"video/"].location == NSNotFound){
            callback(nil,@{@"error":@{@"msg":@"MEDIA_FILE_TYPE_NOT_SUPPORTED",@"code":@110110}});
            return;
        }
    }else{
        if ([[NatVideo internetStatus] isEqualToString:@"notReachable"]) {
            callback(@{@"error":@{@"msg":@"MEDIA_NETWORK_ERROR",@"code":@110050}},nil);
            return;
        }
    }
    
    
    
//    if (self.moviePlayer && self.moviePlayer.playbackState == MPMoviePlaybackStatePlaying) {
//        
//    }
    
    UIView *view = [self getCurrentVC].view;
//    AVPlayerViewController *avplayer;
//    NSData *data = [NSData dataWithContentsOfURL:[NSURL URLWithString:path]];
//    self.moviePlayer = [[MPMoviePlayerViewController alloc] init];
//    AVPlayerItem * playerItem = [AVPlayerItem playerItemWithURL:url];
//    AVPlayer *avplay = [[AVPlayer alloc] initWithPlayerItem:playerItem];
    
    self.moviePlayer=[[MPMoviePlayerController alloc]initWithContentURL:url];
    self.moviePlayer.controlStyle =  MPMovieControlStyleFullscreen;
    if (self.moviePlayer == nil) {
        
        callback(@{@"error":@{@"msg":@"MEDIA_FILE_TYPE_NOT_SUPPORTED",@"code":@110110}},nil);
        return;
    }
    [self.moviePlayer prepareToPlay];
    [self addNotification];
    self.moviePlayer.view.frame=CGRectMake(0, view.frame.size.height, view.frame.size.width, view.frame.size.height);
    self.moviePlayer.view.autoresizingMask=UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
    [view addSubview:self.moviePlayer.view];
    

    [UIView animateWithDuration:0.3 animations:^{
        self.moviePlayer.view.frame = CGRectMake(0, 0, view.frame.size.width, view.frame.size.height);
    }completion:^(BOOL finished) {
//        [self.moviePlayer play];
    }];
    
    
}
-(void)addNotification{
    NSNotificationCenter *notificationCenter=[NSNotificationCenter defaultCenter];
    [notificationCenter addObserver:self selector:@selector(mediaPlayerPlaybackStateChange:) name:MPMoviePlayerPlaybackStateDidChangeNotification object:self.moviePlayer];
    [notificationCenter addObserver:self selector:@selector(mediaPlayerPlaybackFinished:) name:MPMoviePlayerPlaybackDidFinishNotification object:self.moviePlayer];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(exitFullScreen:) name: MPMoviePlayerDidExitFullscreenNotification object:self.moviePlayer];
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(moviePlayerLoadStateDidChange:) name:MPMoviePlayerScalingModeDidChangeNotification object:self.moviePlayer];
    
    //测试通知
    
//    [self addObserver:@selector(playDidFinished:) name:MPMovieMediaTypesAvailableNotification object:nil];
}


- (void)moviePlayerLoadStateDidChange:(NSNotification *)notification
{
    switch (self.moviePlayer.loadState)
    {
        case MPMovieLoadStatePlayable:
        {
            /** 可播放 */;
//            NSLog(@"可以播放");
            [self.moviePlayer play];
        }
            break;
        case MPMovieLoadStatePlaythroughOK:
        {
            /** 状态为缓冲几乎完成，可以连续播放 */;
            NSLog(@"状态为缓冲几乎完成，可以连续播放");
        }
            break;
        case MPMovieLoadStateStalled:
        {
            /** 缓冲中 */
            NSLog(@"缓冲中");
        }
            break;
        case MPMovieLoadStateUnknown:
        {
            /** 未知状态 */
//            NSLog(@"未知状态");
            
        }
            break;
    }
}
-(void)exitFullScreen:(NSNotification *)notification{
    [self close];
}
-(void)mediaPlayerPlaybackStateChange:(NSNotification *)notification{
    switch (self.moviePlayer.playbackState) {
//            self.moviePlayer.loadState
        case MPMoviePlaybackStatePlaying:
            NSLog(@"正在播放...");
            break;
        case MPMoviePlaybackStatePaused:
            NSLog(@"暂停播放.");
            break;
        case MPMoviePlaybackStateStopped:
            NSLog(@"停止播放.");
            [self close];
            break;
        case MPMoviePlaybackStateSeekingForward:
            NSLog(@"kuaijin播放.");
            break;
        case MPMoviePlaybackStateSeekingBackward:
            NSLog(@"kuaitui播放.");
            break;
        case MPMoviePlaybackStateInterrupted:
            NSLog(@"打断了播放.");
            self.playback(@{@"error":@{@"msg":@"MEDIA_ABORTED",@"code":@110090}},nil);
            [self.moviePlayer pause];
            break;
        default:
            NSLog(@"播放状态:%li",self.moviePlayer.playbackState);
            break;
    }
    
    
}

- (void)close{
    UIView *view = [self getCurrentVC].view;
    [UIView animateWithDuration:0.3 animations:^{
        self.moviePlayer.view.frame = CGRectMake(0, view.frame.size.height, view.frame.size.width, view.frame.size.height);
    }completion:^(BOOL finished) {
        [self.moviePlayer.view removeFromSuperview];
        self.moviePlayer = nil;
    }];

}

/**
 *  播放完成
 *
 *  @param notification 通知对象
 */
-(void)mediaPlayerPlaybackFinished:(NSNotification *)notification{
 if([notification.userInfo[@"MPMoviePlayerPlaybackDidFinishReasonUserInfoKey"] integerValue] ==2) {
            [self close];
    }else if([notification.userInfo[@"MPMoviePlayerPlaybackDidFinishReasonUserInfoKey"] integerValue] ==0){
    }
    
//    [self close];
}

- (void)pause:(NatCallback)callback{
    if (self.moviePlayer) {
        [self.moviePlayer pause];
        callback(nil,nil);
    }else{
       callback(@{@"error":@{@"msg":@"MEDIA_PLAYER_NOT_STARTED",@"code":@110100}},nil);
    }
    
}
- (void)stop:(NatCallback)callback{
    if (self.moviePlayer) {
        [self.moviePlayer stop];
        callback(nil,nil);
    }else{
        callback(@{@"error":@{@"msg":@"MEDIA_PLAYER_NOT_STARTED",@"code":@110100}},nil);
    }
}
-(void)dealloc{
    //移除所有通知监控
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}
+ (NSString*)getMimeTypeFromPath:(NSString*)fullPath
{
    NSString* mimeType = nil;
    
    if (fullPath) {
        CFStringRef typeId = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, (__bridge CFStringRef)[fullPath pathExtension], NULL);
        if (typeId) {
            mimeType = (__bridge_transfer NSString*)UTTypeCopyPreferredTagWithClass(typeId, kUTTagClassMIMEType);
            if (!mimeType) {
                // special case for m4a
                if ([(__bridge NSString*)typeId rangeOfString : @"m4a-audio"].location != NSNotFound) {
                    mimeType = @"audio/mp4";
                } else if ([[fullPath pathExtension] rangeOfString:@"wav"].location != NSNotFound) {
                    mimeType = @"audio/wav";
                } else if ([[fullPath pathExtension] rangeOfString:@"css"].location != NSNotFound) {
                    mimeType = @"text/css";
                }
            }
            CFRelease(typeId);
        }
    }
    return mimeType;
}
- (UIViewController *)getCurrentVC
{
    UIViewController *result = nil;
    
    UIWindow * window = [[UIApplication sharedApplication] keyWindow];
    if (window.windowLevel != UIWindowLevelNormal)
    {
        NSArray *windows = [[UIApplication sharedApplication] windows];
        for(UIWindow * tmpWin in windows)
        {
            if (tmpWin.windowLevel == UIWindowLevelNormal)
            {
                window = tmpWin;
                break;
            }
        }
    }
    
    UIView *frontView = [[window subviews] objectAtIndex:0];
    id nextResponder = [frontView nextResponder];
    
    if ([nextResponder isKindOfClass:[UIViewController class]])
        result = nextResponder;
    else
        result = window.rootViewController;
    
    return result;
}
+(NSString *)internetStatus{
    
    Reachability *reachability   = [Reachability reachabilityWithHostName:@"www.apple.com"];
    NetworkStatus internetStatus = [reachability currentReachabilityStatus];
    NSString *net = @"wifi";
    switch (internetStatus) {
        case ReachableViaWiFi:
        {
            net = @"wifi";
            break;
        }
        case ReachableViaWWAN:
        {
            net = @"wwan";
            break;
        }
        case NotReachable:
        {
            net = @"notReachable";
        }
        default:
            break;
    }
    
    return net;
}

@end
