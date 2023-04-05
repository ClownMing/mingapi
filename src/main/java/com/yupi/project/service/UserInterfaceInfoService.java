package com.yupi.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ming.mingapicommon.model.entity.UserInterfaceInfo;

/**
 * @author ClownMing
 * @version 1.0
 * @description TODO
 * @date 2023/4/4 17:49
 */
public interface UserInterfaceInfoService extends IService<UserInterfaceInfo> {

    void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add);


    /**
     * 调用接口统计
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    boolean invokeCount(long interfaceInfoId, long userId);
}