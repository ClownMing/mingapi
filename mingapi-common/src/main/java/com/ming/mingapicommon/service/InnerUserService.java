package com.ming.mingapicommon.service;


import com.ming.mingapicommon.model.entity.User;


/**
 * 用户服务
 *
 * @author yupi
 */
public interface InnerUserService{

    /**
     * 从数据库中查询是否已分配给用户秘钥
     * @param accessKey
     * @return
     */
    User getInvokeUser(String accessKey);
}
