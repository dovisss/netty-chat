package com.imooc.netty;

import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户id和channel的关联关系处理
 */
public class UserChannelRel {

    private static HashMap<String, Channel> manager = new HashMap<>();

    public static void put(String senderId, Channel channel) {
        manager.put(senderId, channel);
    }

    public static Channel get(String senderId) {
        return manager.get(senderId);
    }

    public static void output() {
        System.out.println("UserChannelRel键值对关系：");
        for (Map.Entry<String, Channel> entry: manager.entrySet()) {
            System.out.println("userId: " + entry.getKey() +
                    ",  channelId: " + entry.getValue().id().asLongText());
        }
    }
}
