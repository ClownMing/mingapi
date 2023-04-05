package com.ming.mingapicommon.service;

import com.ming.mingapicommon.model.entity.InterfaceInfo;

/**
* @author ClownMing
* @description 针对表【interface_info(接口信息)】的数据库操作Service
* @createDate 2023-03-28 18:56:40
*/
public interface InnerInterfaceInfoService{
    /**
     * 从数据库中查询模拟接口是否存在(请求路径、请求方法、请求参数)
     * @param path   请求路径
     * @param method  请求方法
     * @return
     */
    InterfaceInfo getInterfaceInfo(String path, String method);

}
