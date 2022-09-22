package com.imooc.controller;

import com.imooc.enums.OperatorFriendRequestTypeEnum;
import com.imooc.enums.SearchFriendsStatusEnum;
import com.imooc.pojo.FriendsRequest;
import com.imooc.pojo.Users;
import com.imooc.pojo.bo.UsersBO;
import com.imooc.pojo.vo.FriendRequestVO;
import com.imooc.pojo.vo.MyFriendsVO;
import com.imooc.pojo.vo.UsersVO;
import com.imooc.service.UserService;
import com.imooc.utils.FastDFSClient;
import com.imooc.utils.FileUtils;
import com.imooc.utils.IMoocJSONResult;
import com.imooc.utils.MD5Utils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping("u")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private FastDFSClient fastDFSClient;

    @PostMapping("/registOrLogin")
    //  等同于@RequestMapping(value = "/registOrLogin", method = POST)
    public IMoocJSONResult registOrLogin(@RequestBody Users user) throws Exception {

//        0.用户名和密码不能为空
        if (StringUtils.isBlank(user.getUsername()) || StringUtils.isBlank(user.getPassword())) {
            return IMoocJSONResult.errorMsg("用户名或密码不能为空");
        }
//        1.判断用户名是否存在，存在就登陆，不存在就注册
        boolean usernameIsExist = userService.queryUsernameIsExist(user.getUsername());
        Users userResult = null;
        if (usernameIsExist) {
//          1.1 登陆
            userResult = userService.queryUserForLogin(user.getUsername(),
                    MD5Utils.getMD5Str(user.getPassword()));
            if (userResult == null) {
                return IMoocJSONResult.errorMsg("用户名或密码不正确");
            }
        } else {
//          1.2 注册
            user.setNickname(user.getUsername());
            user.setFaceImage("");
            user.setFaceImageBig("");
            user.setPassword(MD5Utils.getMD5Str(user.getPassword()));
            userResult = userService.saveUser(user);
        }
        UsersVO usersVO = new UsersVO();
        BeanUtils.copyProperties(userResult, usersVO);
        return IMoocJSONResult.ok(usersVO); // 返回信息到前端
    }
    @RequestMapping(path = "/uploadFaceBase64", method = POST, consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public IMoocJSONResult uploadFaceBase64(@ModelAttribute UsersBO userBO) throws Exception {
        MultipartFile faceFile = userBO.getFaceData();
        // 上传文件到fastdfs
        String url = fastDFSClient.uploadBase64(faceFile);
        System.out.println("faceFile   " + url);

//		"dhawuidhwaiuh3u89u98432.png"
//		"dhawuidhwaiuh3u89u98432_80x80.png"
        String thump = "_80x80.";
        String arr[] = url.split("\\.");
        String thumpImgUrl = arr[0] + thump + arr[1];
//      更新数据库的用户头像
        Users user = new Users();
        user.setId(userBO.getUserId());
        user.setFaceImage(thumpImgUrl);
        user.setFaceImageBig(url);

        Users result = userService.updateUserInfo(user);
        return IMoocJSONResult.ok(result);
    }

    @PostMapping("/setNickname")
    public IMoocJSONResult setNickname(@RequestBody UsersBO usersBO) throws Exception{
//      更新数据库的用户昵称
        Users user = new Users();
        user.setId(usersBO.getUserId());
        user.setNickname(usersBO.getNickname());

        Users result = userService.updateUserInfo(user);
        return IMoocJSONResult.ok(result);
    }

//    搜索好友接口，根据账号做匹配查询而不是模糊查询
    @PostMapping("/searchUser")
    public IMoocJSONResult searchUser(String myUserId, String friendUsername) throws Exception{
        // 0.判断myUserId friendUsername不能为空
        if (StringUtils.isBlank(myUserId) || StringUtils.isBlank(friendUsername)) {
            return IMoocJSONResult.errorMsg("账号不能为空");
        }

        //  前置条件- 1. 搜索用户如果不存在，返回【无此用户】
        //  前置条件- 2. 搜索用户如果为自己，返回【不能添加你自己】
        //  前置条件- 3. 搜索用户已经是好友，返回【该用户已经是你的好友】
        int status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            Users user = userService.queryUserInfoByUsername(friendUsername);
            UsersVO usersVO = new UsersVO();
            BeanUtils.copyProperties(user, usersVO);
            return IMoocJSONResult.ok(usersVO);
        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByStatus(status);
            return IMoocJSONResult.errorMsg(errorMsg);
        }
    }

    //    发送添加好友请求
    @PostMapping("/addFriendRequest")
    public IMoocJSONResult addFriendRequest(String myUserId, String friendUsername) throws Exception{
        // 0.判断myUserId friendUsername不能为空
        if (StringUtils.isBlank(myUserId) || StringUtils.isBlank(friendUsername)) {
            return IMoocJSONResult.errorMsg("账号不能为空");
        }

        //  前置条件- 1. 搜索用户如果不存在，返回【无此用户】
        //  前置条件- 2. 搜索用户如果为自己，返回【不能添加你自己】
        //  前置条件- 3. 搜索用户已经是好友，返回【该用户已经是你的好友】
        int status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            userService.saveFriendsRequest(myUserId, friendUsername);
            return IMoocJSONResult.ok();
        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByStatus(status);
            return IMoocJSONResult.errorMsg(errorMsg);
        }
    }

    //    查询用户接收到的朋友申请
    @PostMapping("/queryFriendRequests")
    public IMoocJSONResult queryFriendRequests(String userId)
            throws Exception {
        // 0. 判断userId不能为空
        if (StringUtils.isBlank(userId)) {
            return IMoocJSONResult.errorMsg("账号不能为空");
        }
        //  1. 查询用户接收到的朋友申请
        List<FriendRequestVO> result= userService.queryFriendRequestList(userId);

        return IMoocJSONResult.ok(result);
    }

    //    接收方 通过/忽略好友申请
    @PostMapping("/operatorFriendRequest")
    public IMoocJSONResult operFriendRequest(String acceptUserId,String sendUserId, Integer operType)
            throws Exception {
        // 0. 判断userId不能为空
        if (StringUtils.isBlank(acceptUserId) && StringUtils.isBlank(sendUserId) && operType == null) {
            return IMoocJSONResult.errorMsg("不能为空");
        }

        // 1. 如果operType没有对应的枚举值，则抛出空错误信息
        if (StringUtils.isBlank(OperatorFriendRequestTypeEnum.getMsgByType(operType))) {
            return IMoocJSONResult.errorMsg("不能为空");
        }

        //  2. 如果忽略朋友申请，则删除friendsRequest表记录
        if (operType == OperatorFriendRequestTypeEnum.IGNORE.type) {
            userService.deleteFriendRequest(sendUserId, acceptUserId);
        } else if (operType == OperatorFriendRequestTypeEnum.PASS.type) {
            //  3. 如果通过朋友申请， 则互相增加好友记录到数据库对应的表，再删除friendsRequest表记录
            userService.passFriendRequest(sendUserId, acceptUserId);
        }
        // 4. 查询数据库
        List<MyFriendsVO> myFriends = userService.queryMyFriends(acceptUserId);
        return IMoocJSONResult.ok(myFriends);
    }

    //    查询我的好友列表
    @PostMapping("/myFriends")
    public IMoocJSONResult myFriends(String userId)
            throws Exception {
        // 0. 判断userId不能为空
        if (StringUtils.isBlank(userId)) {
            return IMoocJSONResult.errorMsg("不能为空");
        }
        // 1. 查询数据库
        List<MyFriendsVO> myFriends = userService.queryMyFriends(userId);

        return IMoocJSONResult.ok(myFriends);
    }

    //    用户手机端获取未签收的消息列表
    @PostMapping("/getUnReadMsgList")
    public IMoocJSONResult getUnReadMsgList(String acceptUserId)
            throws Exception {
        // 0. 判断userId不能为空
        if (StringUtils.isBlank(acceptUserId)) {
            return IMoocJSONResult.errorMsg("不能为空");
        }
        // 查询列表
        List<com.imooc.pojo.ChatMsg> unReadMsgList = userService.getUnReadMsgList(acceptUserId);
        return IMoocJSONResult.ok(unReadMsgList);
    }
}
