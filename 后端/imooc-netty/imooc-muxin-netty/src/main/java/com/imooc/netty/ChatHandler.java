package com.imooc.netty;

import com.imooc.SpringUtil;
import com.imooc.enums.MsgActionEnum;
import com.imooc.service.UserService;
import com.imooc.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @Description: 处理消息的handler
 * TextWebSocketFrame： 在netty中，是用于为websocket专门处理文本的对象，frame是消息的载体
 */
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    // 用于记录和管理所有客户端的channle
    public static ChannelGroup users =
            new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg)
            throws Exception {

//        1. 获取客户端发来的消息
        String content = msg.text();
        System.out.println("content: " + content);

        Channel currentChannel = ctx.channel();
        DataContent dataContent = JsonUtils.jsonToPojo(content, DataContent.class);
        Integer action = dataContent.getAction();

//        2. 判断消息类型，各位那句不同类型来处理不同的业务
        if (action == MsgActionEnum.CONNECT.type) {
//          2.1 当websocket第一次open的时候，初始化channel，把用户的channel和userid关联起来
            String senderId = dataContent.getChatMsg().getSenderId();
            UserChannelRel.put(senderId, currentChannel);

//            测试
            System.out.println("users的channel");
            for (Channel c: users) {
                System.out.println(c.id().asLongText());
            }
            UserChannelRel.output();

        } else if (action == MsgActionEnum.CHAT.type) {
//          2.2 聊天类型的消息，把聊天记录保存到数据库，同时标记消息的签收状态[未签收]
            ChatMsg chatMsg = dataContent.getChatMsg();
            String senderId = chatMsg.getSenderId();
            String receiverId = chatMsg.getReceiverId();
            String msgText = chatMsg.getMsg();

//          保存消息到数据库，并且标记为未签收
            UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");  //  Service默认名字为第一个字母小些驼峰
            String msgId = userService.saveMsg(chatMsg);
            chatMsg.setMsgId(msgId);

            DataContent dataContentMsg = new DataContent();
            dataContentMsg.setChatMsg(chatMsg);

//          发送消息
//          从全局用户Channel关系中获取接收方的channel
            Channel receiverChannel = UserChannelRel.get(receiverId);
            if (receiverChannel == null) {
//              TODO channel为空代表用户离线，推送消息（第三方工具）
            } else {
//              当receiverChannel不为空，从ChannGroup的users去查找对应的channel是否存在
                Channel findChannel = users.find(receiverChannel.id());
                if (findChannel != null) {
//                  用户在线，实时推送，
                    receiverChannel.writeAndFlush(
                            new TextWebSocketFrame(
                                    JsonUtils.objectToJson(dataContentMsg)
                            ));
                    System.out.println("writeAndFlush发送成功");
                } else {
//                  TODO 用户离线
                }
            }

        } else if (action == MsgActionEnum.SIGNED.type) {
//          2.3 签收消息类型，针对具体的消息进行签收，修改数据库中对应消息的签收状态[已签收]
//
            UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");
            ChatMsg chatMsg = dataContent.getChatMsg();
            String msgIdStr = dataContent.getExtand();
            String msgIds[] = msgIdStr.split(",");

            List<String> msgIdList = new ArrayList<>();
            for (String msgId: msgIds) {
                if (StringUtils.isNotBlank(msgId)) {
                    msgIdList.add(msgId);
                }
            }
            //System.out.println(msgIdList.toString());
            if (msgIdList !=null && !msgIdList.isEmpty() && msgIdList.size() > 0) {
                userService.updateMsgSigned(msgIdList);
            }

        } else if (action == MsgActionEnum.KEEPALIVE.type){
//          2.4 心跳类型的消息
            System.out.println("收到来自channel为[" + currentChannel.id() + "]的心跳包....");
        }





        // 获取客户端传输过来的消息
//        String content = msg.text();
//		for (Channel channel: users) {
//			channel.writeAndFlush(
//				new TextWebSocketFrame(
//						"[服务器在]" + LocalDateTime.now()
//						+ "接受到消息, 消息为：" + content));
//		}
        // 下面这个方法，和上面的for循环，一致
//        users.writeAndFlush(
//                new TextWebSocketFrame(
//                        "[服务器在]" + LocalDateTime.now()
//                                + "接受到消息, 消息为：" + content));

    }

    /**
     * 当客户端连接服务端之后（打开连接）
     * 获取客户端的channle，并且放到ChannelGroup中去进行管理
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        users.add(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // 当触发handlerRemoved，ChannelGroup会自动移除对应客户端的channel
//		users.remove(ctx.channel());
        System.out.println("客户端断开，channle对应的长id为："
                + ctx.channel().id().asLongText());
        System.out.println("客户端断开，channle对应的短id为："
                + ctx.channel().id().asShortText());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
//      发生异常之后关闭连接（关闭channel），随后从ChannelGroup中移除
        ctx.channel().close();
        users.remove(ctx.channel());
    }
}
