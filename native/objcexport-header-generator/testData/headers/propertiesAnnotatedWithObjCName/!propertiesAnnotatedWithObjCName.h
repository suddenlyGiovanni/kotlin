#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

NS_ASSUME_NONNULL_BEGIN
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-warning-option"
#pragma clang diagnostic ignored "-Wincompatible-property-type"
#pragma clang diagnostic ignored "-Wnullability"

#pragma push_macro("_Nullable_result")
#if !__has_feature(nullability_nullable_result)
#undef _Nullable_result
#define _Nullable_result _Nullable
#endif

__attribute__((objc_subclassing_restricted))
@interface Foo : Base
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) int32_t prop __attribute__((swift_name("prop")));
@property (readonly) int32_t objCName __attribute__((swift_name("objCName")));
@property (readonly) int32_t objCNameWithSwiftName __attribute__((swift_name("swiftNameWithObjCName")));
@end

__attribute__((objc_subclassing_restricted))
@interface FooKt : Base
+ (int32_t)prop:(int32_t)receiver __attribute__((swift_name("prop(_:)")));
+ (int32_t)objCName:(int32_t)receiver __attribute__((swift_name("objCName(_:)")));
+ (int32_t)objCNameWithSwiftName:(int32_t)receiver __attribute__((swift_name("swiftNameWithObjCName(_:)")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
