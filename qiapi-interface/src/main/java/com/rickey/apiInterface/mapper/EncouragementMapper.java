package com.rickey.apiInterface.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rickey.apiInterface.model.entity.Encouragement;

/**
 * 接口信息
 */
public interface EncouragementMapper extends BaseMapper<Encouragement> {

    Encouragement getRandomEncouragement();
}




