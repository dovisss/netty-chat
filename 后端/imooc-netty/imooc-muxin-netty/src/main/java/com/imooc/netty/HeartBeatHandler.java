package com.imooc.netty;

import com.imooc.SpringUtil;
import com.imooc.enums.MsgActionEnum;
import com.imooc.service.UserService;
import com.imooc.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @Description: 用于检测channel的心跳handler
 * 继承ChannelInboundHandlerAdapter，从而不需要实现channelRead0方法
 */
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//      判断evt是否是IdleStateEvent（用于触发用户事件，包含 读空闲/写空闲/读写空闲）
        if (evt instanceof IdleStateEvent) { // idle:空闲, IdleStateEvent:空闲状态检测事件
            IdleStateEvent event = (IdleStateEvent)evt;
            if (event.state() == IdleState.READER_IDLE) {
                System.out.println("进入读空闲...");
            } else if (event.state() == IdleState.WRITER_IDLE) {
                System.out.println("进入写空闲...");
            } else if (event.state() == IdleState.ALL_IDLE){
                System.out.println("进入读写空闲...");
                System.out.println("channel关闭前， users的数量为： " + ChatHandler.users.size());
                Channel channel = ctx.channel();
//              关闭无用的channel，以防资源浪费
                channel.close();
                System.out.println("channel关闭后， users的数量为： " + ChatHandler.users.size());
            }
        }

    }
}