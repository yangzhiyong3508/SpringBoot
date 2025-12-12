package com.example.edog.service.impl;

import com.example.edog.entity.Account;
import com.example.edog.mapper.AccountMapper;
import com.example.edog.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

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

    @Override
    public int addAccount(Account account) {
        if (account == null || account.getAccount() == null || account.getPassword() == null) {
            System.out.println("注册失败：账号或密码为空");
            return -1;
        }

        if (account.getAvatarUrl() == null || account.getAvatarUrl().trim().isEmpty())
            account.setAvatarUrl("https://edog-avatar-1361772203.cos.ap-nanjing.myqcloud.com/uploads/default.png");
        
        if (account.getUsername() == null || account.getUsername().isEmpty())
            account.setUsername("用户名");
        
        if (account.getWakeWord() == null || account.getWakeWord().isEmpty())
            account.setWakeWord("小爱同学");

        // ✅ 默认值适配 SQL 定义
        if (account.getVol() == null) account.setVol(70); 
        if (account.getVoiceId() == null || account.getVoiceId().isEmpty()) {
            account.setVoiceId("7568423452617523254");
        }
        if (account.getSpeedRatio() == null) {
            account.setSpeedRatio(1.0);
        }

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