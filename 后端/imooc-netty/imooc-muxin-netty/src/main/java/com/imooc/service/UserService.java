package com.imooc.service;

import com.imooc.netty.ChatMsg;
import com.imooc.pojo.Users;
import com.imooc.pojo.vo.FriendRequestVO;
import com.imooc.pojo.vo.MyFriendsVO;

import java.util.List;

public interface UserService {

    //  判断用户名是否存在
    boolean queryUsernameIsExist(String username);

    //  用户登陆，查询用户是否存在
    Users queryUserForLogin(String username, String pwd);

    //  用户注册，保存到数据库
    Users saveUser(Users user);

    //  修改用户记录
    Users updateUserInfo(Users user);

////    通过ID查询用户
//    public Users queryUserById(String userId);

    //  搜索朋友的前置条件
    int preconditionSearchFriends(String myUserId, String friendUsername);

    //  通过username查询用户信息
    Users queryUserInfoByUsername(String username);

    //  添加好友请求记录，保存到数据库
    void saveFriendsRequest(String myUserId, String friendUsername);

    //  查询好友请求列表，（发送方信息）
    List<FriendRequestVO> queryFriendRequestList(String acceptUserId);

    //  删除（拒绝）好友申请
    void deleteFriendRequest(String sendUserId, String acceptUserId);

    //  同意好友申请
    void passFriendRequest(String sendUserId, String acceptUserId);

    //  查询我的好友列表
    List<MyFriendsVO> queryMyFriends(String userId);

    //  保存聊天消息
    String saveMsg(ChatMsg chatMsg);

    //  批量签收聊天消息
    void updateMsgSigned(List<String> msgIdList);

    //  获取未签收消息列表
    List<com.imooc.pojo.ChatMsg> getUnReadMsgList(String acceptUserId);
}
