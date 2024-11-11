package com.rickey.apiInterface.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.rickey.apiInterface.model.entity.Encouragement;

/**
 * @author RicKey
 * @description 针对表【encouragement(心灵鸡汤)】的数据库操作Service
 * @createDate 2024-10-22 22:08:31
 */
public interface EncouragementService extends IService<Encouragement> {

    Encouragement getRandomEncouragement();
}
