package com.example.edog.service.impl;

import com.example.edog.entity.Account;
import com.example.edog.mapper.AccountMapper;
import com.example.edog.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Account 业务逻辑实现类
 * 自动补全默认值，防止数据库空值约束错误
 */
@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountMapper accountMapper;

    @Override
    public List<Account> getAllAccounts() {
        return accountMapper.selectAll();
    }

    @Override
    public List<Account> getAccountsByAccount(Long account) {
        return accountMapper.selectListByAccount(account);
    }

    @Override
    public Account getAccountByAccount(Long account) {
        return accountMapper.selectByAccount(account);
    }

    /** 注册账户*/
    @Override
    public int addAccount(Account account) {
        if (account == null || account.getAccount() == null || account.getPassword() == null) {
            System.out.println("注册失败：账号或密码为空");
            return -1;
        }
        // ===== 自动补全非空字段默认值 =====
        if (account.getAvatarUrl() == null || account.getAvatarUrl().trim().isEmpty())
            account.setAvatarUrl("https://edog-avatar-1361772203.cos.ap-nanjing.myqcloud.com/uploads/default.png"); // 默认头像
        if (account.getPer() == null) account.setPer(0);               // 默认音色
        if (account.getSpd() == null) account.setSpd(5);               // 默认语速
        if (account.getPid() == null) account.setPid(0);               // 默认语言
        if (account.getVol() == null) account.setVol(5);               // 默认音量
        if (account.getUsername() == null || account.getUsername().isEmpty())
            account.setUsername("用户名");                              // 默认用户名
        if (account.getWakeWord() == null || account.getWakeWord().isEmpty())
            account.setWakeWord("小爱同学");                               // 默认唤醒词

        System.out.println("正在插入账户: " + account);
        return accountMapper.insert(account);
    }

    @Override
    public int updatePasswordByAccount(Account account) {
        return accountMapper.updatePassword(account);
    }

    @Override
    public int deleteAccount(Long account) {
        return accountMapper.deleteByAccount(account);
    }

    @Override
    public int updateVoiceByAccount(Account account) {
        return accountMapper.updateVoice(account);
    }

    @Override
    public int updateAvatarByAccount(Account account) {
        if (account != null) {
            String avatarUrl = account.getAvatarUrl();
            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                account.setAvatarUrl("https://edog-avatar-1361772203.cos.ap-nanjing.myqcloud.com/uploads/default.png");
            }
        }
        return accountMapper.updateAvatar(account);
    }

    @Override
    public int updateUsernameByAccount(Account account) {
        return accountMapper.updateUsername(account);
    }

    @Override
    public int updateWakeWordByAccount(Account account) {
        return accountMapper.updateWakeWord(account);
    }
}