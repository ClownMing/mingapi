package com.ming.mingapicommon.service;


import com.ming.mingapicommon.model.entity.UserInterfaceInfo;

/**
* @author ClownMing
* @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Service
* @createDate 2023-03-31 20:49:02
*/
public interface InnerUserInterfaceInfoService{
    /**
     * 调用接口统计
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    boolean invokeCount(long interfaceInfoId, long userId);

}
