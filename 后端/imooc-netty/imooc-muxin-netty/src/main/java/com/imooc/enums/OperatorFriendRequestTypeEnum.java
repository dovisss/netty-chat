package com.imooc.enums;

//  忽略/通过 好友申请的枚举
public enum OperatorFriendRequestTypeEnum {
    IGNORE(0, "忽略"),
    PASS(1, "通过");
    public final Integer type;
    public final String msg;

    OperatorFriendRequestTypeEnum(Integer type, String msg) {
        this.type = type;
        this.msg = msg;
    }

    public Integer getType() {
        return type;
    }
    public static String getMsgByType(Integer type) {
        for (OperatorFriendRequestTypeEnum obj: OperatorFriendRequestTypeEnum.values()) {
            if (obj.getType() == type) {
                return obj.msg;
            }
        }
        return null;
    }
}
