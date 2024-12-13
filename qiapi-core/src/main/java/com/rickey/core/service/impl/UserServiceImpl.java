package com.rickey.core.service.impl;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rickey.common.common.ErrorCode;
import com.rickey.common.exception.BusinessException;
import com.rickey.common.model.entity.User;
import com.rickey.common.service.InnerEmailService;
import com.rickey.core.constant.UserConstant;
import com.rickey.core.mapper.UserMapper;
import com.rickey.core.model.vo.UserDevKeyVO;
import com.rickey.core.service.UserService;
import com.rickey.core.utils.RedisUtil;
import com.sun.xml.internal.messaging.saaj.packaging.mime.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;


/**
 * 用户服务实现类
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @DubboReference
    private InnerEmailService emailService;

    private final UserMapper userMapper;

    private final RedisUtil redisUtil;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "rickey";

    @Autowired
    public UserServiceImpl(UserMapper userMapper, RedisUtil redisUtil) {
        this.userMapper = userMapper;
        this.redisUtil = redisUtil;
    }

    @Override
    public long userRegister(String email, String userPassword, String checkPassword, String code) {
        String userAccount = email;
        String redisKey = email + ":code";
        // 1. 校验
        if (StringUtils.isAnyBlank(email, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (!Validator.isEmail(email)) {
            log.info("email = {}", email);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        String codeFromRedis = (String) redisUtil.get(redisKey);
        log.info("codeFromRedis = {}", codeFromRedis);
        log.info("code = {}", code);
        if (!codeFromRedis.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
        synchronized (email.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", email);
            long count = userMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }

            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 分配 accessKey, secretKey
            String accessKey = DigestUtil.md5Hex(SALT + userAccount + RandomUtil.randomNumbers(5));
            String secretKey = DigestUtil.md5Hex(SALT + userAccount + RandomUtil.randomNumbers(8));
            // 4. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            user.setAccessKey(accessKey);
            user.setSecretKey(secretKey);
            user.setEmail(email);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public User userLogin(String userAccount, String userPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        return user;
    }


    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && UserConstant.ADMIN_ROLE.equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 移除登录态
        return true;
    }

    /**
     * @param token
     * @return
     */
    @Override
    public User getUserByToken(String token) {
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 查询 Redis 中的用户数据
        String userJson = (String) redisUtil.get("session:" + token);
        if (StringUtils.isBlank(userJson)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return JSONUtil.toBean(userJson, User.class);
    }

    /**
     * 发送短信服务
     *
     * @param email
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    @Override
    public void sendEmail(String email) throws MessagingException, UnsupportedEncodingException {
        String emailCodeKey = email + ":code";
        // 发送短信，存入redis
        String newCode = emailService.sendEmail(email);
        redisUtil.set(emailCodeKey, newCode, 180);
    }

    /**
     * 重新生成ak，sk
     *
     * @param request
     * @return
     */
    @Override
    public UserDevKeyVO genKey(HttpServletRequest request) {
        String userId = request.getHeader("userId");
        if (userId == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        UserDevKeyVO userDevKeyVO = this.genKey(userId);
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId);
        updateWrapper.set("accessKey", userDevKeyVO.getAccessKey());
        updateWrapper.set("secretKey", userDevKeyVO.getSecretKey());
        this.update(updateWrapper);
        //重置登录用户的ak,sk信息
        return userDevKeyVO;
    }

    private UserDevKeyVO genKey(String userAccount) {
        String accessKey = DigestUtil.md5Hex(SALT + userAccount + RandomUtil.randomNumbers(5));
        String secretKey = DigestUtil.md5Hex(SALT + userAccount + RandomUtil.randomNumbers(8));
        UserDevKeyVO userDevKeyVO = new UserDevKeyVO();
        userDevKeyVO.setAccessKey(accessKey);
        userDevKeyVO.setSecretKey(secretKey);
        return userDevKeyVO;
    }

}




