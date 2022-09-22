package com.imooc.service.impl;

import com.imooc.enums.MsgActionEnum;
import com.imooc.enums.MsgSignFlagEnum;
import com.imooc.enums.SearchFriendsStatusEnum;
import com.imooc.mapper.*;
import com.imooc.netty.ChatMsg;
import com.imooc.netty.DataContent;
import com.imooc.netty.UserChannelRel;
import com.imooc.pojo.FriendsRequest;
import com.imooc.pojo.MyFriends;
import com.imooc.pojo.Users;
import com.imooc.pojo.vo.FriendRequestVO;
import com.imooc.pojo.vo.MyFriendsVO;
import com.imooc.service.UserService;
import com.imooc.utils.FastDFSClient;
import com.imooc.utils.FileUtils;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.QRCodeUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.entity.Example.Criteria;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UsersMapper userMapper;
    @Autowired
    private MyFriendsMapper myFriendsMapper;
    @Autowired
    private FriendsRequestMapper friendsRequestMapper;
    @Autowired
    private UsersMapperCustom usersMapperCustom;
    @Autowired
    private ChatMsgMapper chatMsgMapper;
    @Autowired
    private Sid sid;
    @Autowired
    private QRCodeUtils qrCodeUtils;
    @Autowired
    private FastDFSClient fastDFSClient;

//    判断用户名是否存在
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public boolean queryUsernameIsExist(String username) {
        Users user = new Users();
        user.setUsername(username);
        Users result = userMapper.selectOne(user);
        return result != null ? true : false;
    }

//    用户登陆，查询用户是否存在
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserForLogin(String username, String pwd) {
        Example userExample = new Example(Users.class);
        Criteria criteria = userExample.createCriteria();

        criteria.andEqualTo("username",username);
        criteria.andEqualTo("password",pwd);

        Users result = userMapper.selectOneByExample(userExample);
        return result;
    }

//    用户注册
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users saveUser(Users user) {
        String userId = sid.nextShort();
//      为每个用户生成一个唯一的二维码
        String qrCodePath = "/Users/zxf/desktop/qrcode/" + userId + "qrcode.png"; // qrcode所在的path
        qrCodeUtils.createQRCode(qrCodePath, "qrcode:" + user.getUsername());
        MultipartFile qrCodeFile = FileUtils.fileToMultipart(qrCodePath);
        String qrCodeUrl = "";
//      上传到文件服务器
        try {
            qrCodeUrl = fastDFSClient.uploadQRCode(qrCodeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        user.setQrcode(qrCodeUrl);

        user.setId(userId);
        userMapper.insert(user);
        return user;
    }

    //    修改用户记录
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users updateUserInfo(Users user) {
        // 根据主键修改不为null的字段,如果为Null就忽略更新
        userMapper.updateByPrimaryKeySelective(user);
        return queryUserById(user.getId());
    }

    //    通过ID查询用户
    @Transactional(propagation = Propagation.SUPPORTS)
    public Users queryUserById(String userId) {
        return userMapper.selectByPrimaryKey(userId);
    }

    //  搜索朋友的前置条件
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public int preconditionSearchFriends(String myUserId, String friendUsername) {
        Users user = queryUserInfoByUsername(friendUsername);
//        1. 搜索用户如果不存在，返回【无此用户】
        if (user == null) {
            return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
        }
//        2. 搜索用户如果是自己，返回【不能添加自己】
        if (user.getId().equals(myUserId)) {
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }

//        3. 搜索用户已经是好友，返回【该用户已经是你的好友】
        Example myFriendsExample = new Example(MyFriends.class);
        Criteria criteria = myFriendsExample.createCriteria();
        criteria.andEqualTo("myUserId", myUserId);
        criteria.andEqualTo("myFriendUserId", user.getId());
        MyFriends myFriendsResult = myFriendsMapper.selectOneByExample(myFriendsExample);
        if (myFriendsResult != null) {
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }

        return SearchFriendsStatusEnum.SUCCESS.status;
    }

    //    通过用户名查询用户
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserInfoByUsername(String username) {
        Example userExample = new Example(Users.class);
        Criteria criteria = userExample.createCriteria();// 创建一个条件
        criteria.andEqualTo("username", username);

        return userMapper.selectOneByExample(userExample);
    }

    //    添加好友请求记录，保存到数据库
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void saveFriendsRequest(String myUserId, String friendUsername) {
    //    根据用户名查询朋友信息
        Users friend = queryUserInfoByUsername(friendUsername);

    //    1. 查询发送好友请求记录表
        Example frExample = new Example(FriendsRequest.class);
        Criteria frCriteria = frExample.createCriteria();
        frCriteria.andEqualTo("sendUserId", myUserId);
        frCriteria.andEqualTo("acceptUserId", friend.getId());
        FriendsRequest frResult = friendsRequestMapper.selectOneByExample(frExample);

    //    2. 如果该用户不是你的好友，且friendsRequest表中无记录，则新增好友请求记录
        if (frResult == null) {
            String requestId = sid.nextShort();
            FriendsRequest friendsRequest = new FriendsRequest();

            friendsRequest.setId(requestId);
            friendsRequest.setSendUserId(myUserId);
            friendsRequest.setAcceptUserId(friend.getId());
            friendsRequest.setRequestDateTime(new Date());
            friendsRequestMapper.insert(friendsRequest);
        }

    }

    //    查询好友请求列表，（FriendRequestVO:发送方信息）
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId) {
        return usersMapperCustom.queryFriendRequestList(acceptUserId);
    }


    //    删除好友请求的数据库表记录
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void deleteFriendRequest(String sendUserId, String acceptUserId) {
        Example frExample = new Example(FriendsRequest.class);
        Criteria frCriteria = frExample.createCriteria();
        frCriteria.andEqualTo("sendUserId", sendUserId);
        frCriteria.andEqualTo("acceptUserId", acceptUserId);

        friendsRequestMapper.deleteByExample(frExample);
    }

    //    通过好友请求
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {
        saveFriends(sendUserId, acceptUserId);
        saveFriends(acceptUserId, sendUserId);
        deleteFriendRequest(sendUserId, acceptUserId);

//      使用websocket主动推送消息到请求发起者，更新他的通讯录表为最新
        Channel senderChannel =  UserChannelRel.get(sendUserId);
        if (senderChannel != null) {
            DataContent dataContent = new DataContent();
            dataContent.setAction(MsgActionEnum.PULL_FRIEND.type);
            senderChannel.writeAndFlush(
                    new TextWebSocketFrame(
                            JsonUtils.objectToJson(dataContent)));
        }
    }
    // 新增好友
    @Transactional(propagation = Propagation.REQUIRED)
    private void saveFriends(String sendUserId, String acceptUserId) {
        MyFriends myFriends = new MyFriends();
        myFriends.setId(sid.nextShort());
        myFriends.setMyUserId(acceptUserId);
        myFriends.setMyFriendUserId(sendUserId);
        myFriendsMapper.insert(myFriends);

    }

    //    查询我的好友列表
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<MyFriendsVO> queryMyFriends(String userId) {


        return usersMapperCustom.queryMyFriends(userId);
    }

    //  保存聊天消息
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public String saveMsg(ChatMsg chatMsg) {
        com.imooc.pojo.ChatMsg msgDB = new com.imooc.pojo.ChatMsg();
        String msgId = sid.nextShort();
        msgDB.setId(msgId);
        msgDB.setAcceptUserId(chatMsg.getReceiverId());
        msgDB.setSendUserId(chatMsg.getSenderId());
        msgDB.setCreateTime(new Date());
        msgDB.setSignFlag(MsgSignFlagEnum.unsign.type);
        msgDB.setMsg(chatMsg.getMsg());

        chatMsgMapper.insert(msgDB);
        return msgId;
    }

    //  批量签收聊天消息
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateMsgSigned(List<String> msgIdList) {
        usersMapperCustom.batchUpdateMsgSigned(msgIdList);
    }

    //  获取未签收消息列表
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<com.imooc.pojo.ChatMsg> getUnReadMsgList(String acceptUserId) {
        Example chatExample = new Example(com.imooc.pojo.ChatMsg.class);
        Criteria chatCriteria = chatExample.createCriteria();
        chatCriteria.andEqualTo("signFlag", MsgSignFlagEnum.unsign);
        chatCriteria.andEqualTo("acceptUserId", acceptUserId);

        List<com.imooc.pojo.ChatMsg> result = chatMsgMapper.selectByExample(chatExample);
        return result;
    }

}
