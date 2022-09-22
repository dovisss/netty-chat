package com.imooc.mapper;

import com.imooc.pojo.MyFriends;
import com.imooc.pojo.Users;
import com.imooc.pojo.vo.FriendRequestVO;
import com.imooc.pojo.vo.MyFriendsVO;
import com.imooc.utils.MyMapper;

import java.util.List;

public interface UsersMapperCustom extends MyMapper<Users> {
    List <FriendRequestVO> queryFriendRequestList(String acceptUserId);
    List<MyFriendsVO> queryMyFriends(String userId);
    void batchUpdateMsgSigned(List<String> msgIdList);
}

