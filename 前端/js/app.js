window.app = {
	// netty服务后端发布的url地址
	nettyServerUrl: "ws://192.168.1.8:8088/ws",
	// 后端服务发布的URL地址
	serverUrl: 'http://192.168.1.8:8080',
	// 图片服务发布的URL地址
	imgServerUrl: 'http://192.168.0.141:88/imooc/',
	
	isNotNull: function(str) {
		if (str != null && str != "" && str != undefined) {
			return true;
		}
		return false;
	},
	// 封装消息提示框
	showToast: function(msg, type) {
		plus.nativeUI.toast(msg, 
			{icon: "image/" + type + ".png", verticalAlign: "center"})
	},
	// 保存用户全局对象
	setUserGlobalInfo: function(user) {
		var userInfoStr = JSON.stringify(user);
		plus.storage.setItem("userInfo", userInfoStr);
	},
	// 获取用户全局对象
	getUserGlobalInfo: function() {
		var userInfostr = plus.storage.getItem("userInfo");
		return JSON.parse(userInfostr); // 字符串转换称JSON对象
	},
	/**
	 * 登出后，移除用户全局对象
	 */
	userLogout: function() {
		plus.storage.removeItem("userInfo");
	},
	// 保存用户联系人列表到本地缓存
	setContactList: function(contactList) {
		var contactListStr = JSON.stringify(contactList);
		plus.storage.setItem("contactList", contactListStr);
	},
	
	// 获取本地缓存中的联系人列表
	getContactList: function() {
		var contactListStr = plus.storage.getItem("contactList");
		
		if(!this.isNotNull(contactListStr)) { // 为空
			return [];
		}
		return JSON.parse(contactListStr);
	},
	
	// 根据用户id，从本地缓存（联系人列表）中获取朋友的信息
	getFriendFromContactList: function(friendId) {
		var contactList = this.getContactList();
		
		if (contactList.length > 0) {
			// 不为空，则把用户信息返回
			for (var i = 0; i < contactList.length; i++) {
				var friend = contactList[i];
				if (friend.friendUserId == friendId) {
					return friend;
					break;
				}
			}
		} else {
			// 如果为空，直接返回null
			return null;
		}
		
	},
	
	// 保存聊天记录到本地缓存, 
	// flag判断是我发送的还是朋友发送的。1:我，2:朋友
	saveUserChatHistory: function(myId, friendId, msg, flag) {
		var me = this;
		var chatKey = "chat-" + myId + "-" + friendId;
		
		// 从本地缓存获取聊天记录是否存在
		var chatHistoryListStr = plus.storage.getItem(chatKey);
		var chatHistoryList;
		if (me.isNotNull(chatHistoryListStr)) {
			chatHistoryList = JSON.parse(chatHistoryListStr);
		} else {
			chatHistoryList = [];
		}
		
		// 构建单个聊天记录对象
		var singleMsg = new me.ChatHistory(myId, friendId, msg, flag);
		
		chatHistoryList.push(singleMsg);
		plus.storage.setItem(chatKey, JSON.stringify(chatHistoryList));
	},
	
	// 获取本地聊天记录 
	getUserChatHistory: function(myId, friendId) {
		var me = this;
		var chatKey = "chat-" + myId + "-" + friendId;
		
		// 从本地缓存获取聊天记录是否存在
		var chatHistoryListStr = plus.storage.getItem(chatKey);
		var chatHistoryList;
		if (me.isNotNull(chatHistoryListStr)) {
			chatHistoryList = JSON.parse(chatHistoryListStr);
		} else {
			chatHistoryList = [];
		}
		
		return chatHistoryList;
	},
	
	// 删除用户聊天记录
	deleteUserChatHistory: function(myId, friendId) {
		var chatKey = "chat-" + myId + "-" + friendId;
		plus.storage.removeItem(chatKey);
	},
	
	// 保存聊天快照本地缓存,仅仅保存每次和朋友聊天的最后一条消息
	// isRead判断是已读未读，true，false
	saveUserChatSnapshot: function(myId, friendId, msg, isRead) {
		var me = this;
		var chatKey = "chat-Snapshot" + myId;
		
		// 从本地缓存获取聊天快照list
		var chatSnapshotListStr = plus.storage.getItem(chatKey);
		var chatSnapshotList;
		if (me.isNotNull(chatSnapshotListStr)) {
			chatSnapshotList = JSON.parse(chatSnapshotListStr);
			// 循环快照列表chatSnapshotList，判断每个元素friendId是否匹配friendId
			// 有则删除，无则不做处理
			for (var i = 0; i < chatSnapshotList.length; i++) {
				if (chatSnapshotList[i].friendId == friendId) {
					chatSnapshotList.splice(i, 1);
					break;
				}
			}
		} else {
			chatSnapshotList = [];
		}
		// 构建单个聊天快照对象
		var singleMsg = new me.ChatSnapshot(myId, friendId, msg, isRead);
		
		chatSnapshotList.unshift(singleMsg);
		plus.storage.setItem(chatKey, JSON.stringify(chatSnapshotList));
	},
	
	// 获取聊天快照本地缓存
	getUserChatSnapshot: function(myId) {
		var me = this;
		var chatKey = "chat-Snapshot" + myId;
		
		// 从本地缓存获取聊天快照list
		var chatSnapshotListStr = plus.storage.getItem(chatKey);
		var chatSnapshotList;
		if (me.isNotNull(chatSnapshotListStr)) {
			chatSnapshotList = JSON.parse(chatSnapshotListStr);
		} else {
			chatSnapshotList = [];
		}
		
		return chatSnapshotList;
	},
	
	// 删除聊天快照本地缓存
	deleteUserChatSnapshot: function(myId, friendId) {
		var me = this;
		var chatKey = "chat-Snapshot" + myId;
		
		// 从本地缓存获取聊天快照list
		var chatSnapshotListStr = plus.storage.getItem(chatKey);
		var chatSnapshotList;
		if (me.isNotNull(chatSnapshotListStr)) {
			chatSnapshotList = JSON.parse(chatSnapshotListStr);
			// 循环快照列表chatSnapshotList，判断每个元素friendId是否匹配friendId
			// 有则删除，无则不做处理
			for (var i = 0; i < chatSnapshotList.length; i++) {
				if (chatSnapshotList[i].friendId == friendId) {
					chatSnapshotList.splice(i, 1);
					break;
				}
			}
		} else {// 如果为空，不做处理
			return;
		}
		
		plus.storage.setItem(chatKey, JSON.stringify(chatSnapshotList));
	},
	
	// 未读消息标记为已读状态
	readUserChatSnapshot: function(myId, friendId) {
		var chatKey = "chat-Snapshot" + myId;
		var chatSnapshotList = this.getUserChatSnapshot(myId);
		for (var i = 0; i < chatSnapshotList.length; i++) {
			if (chatSnapshotList[i].friendId == friendId) {
				chatSnapshotList[i].isRead = true;
				break;
			}
		}
		// 替换原有的快照列表缓存
		plus.storage.setItem(chatKey, JSON.stringify(chatSnapshotList));
	},
	
	// 枚举类型
	CONNECT: 1, 	// 第一次（或重连）初始化连接
	CHAT: 2, 		// 聊天消息
	SIGNED: 3,  	// 消息签收
	KEEPALIVE: 4, 	// 客户端保持心跳,
	PULL_FRIEND: 5, // 重新拉取好友
	
	// 和后端的ChatMsg聊天模型对象保持一致
	ChatMsg: function(senderId, receiverId, msg, msgId) {
		this.senderId = senderId;
		this.receiverId = receiverId;
		this.msg = msg;
		this.msgId = msgId;
	},
	
	// 构建消息DataContent模型对象 
	DataContent: function(action, chatMsg, extand) {
		this.action = action;
		this.chatMsg = chatMsg;
		this.extand = extand;
	},
	
	// 单个聊天记录的对象
	// flag判断是我发送的还是朋友发送的。1:我，2:朋友
	ChatHistory: function(myId, friendId, msg, flag) {
		this.myId = myId;
		this.friendId = friendId;
		this.msg = msg;
		this.flag = flag;
	},
	
	// 快照对象
	ChatSnapshot: function(myId, friendId, msg, isRead) {
		this.myId = myId;
		this.friendId = friendId;
		this.msg = msg;
		this.isRead = isRead;
	}
	
}
