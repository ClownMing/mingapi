package com.ming.mingapiinterface.mapper;

import com.ming.mingapiinterface.entity.ApiStory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author ClownMing
* @description 针对表【api_story】的数据库操作Mapper
*/
@Mapper
public interface ApiStoryMapper extends BaseMapper<ApiStory> {

    /**
     * 随机返回一条趣味故事
     * @return 趣味故事
     */
    String getOneByRandom();

}




