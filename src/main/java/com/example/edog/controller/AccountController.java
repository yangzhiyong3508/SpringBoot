package com.example.edog.controller;

import com.example.edog.configurer.TTSConfig;
import com.example.edog.entity.Account;
import com.example.edog.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    /** 获取所有账户 */
    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    /** 根据账号查询 */
    @GetMapping("/account/{account}")
    public ResponseEntity<List<Account>> getAccountsByAccount(@PathVariable Long account) {
        return ResponseEntity.ok(accountService.getAccountsByAccount(account));
    }

    /** 注册账号 */
    @PostMapping
    public ResponseEntity<Integer> addAccount(@RequestBody Account account) {
        int result = accountService.addAccount(account);
        if (result == -1) return ResponseEntity.badRequest().body(-1);
        return ResponseEntity.ok(result);
    }

    /** 更新密码 */
    @PostMapping("/updatePassword")
    public ResponseEntity<Integer> updatePassword(@RequestBody Account account) {
        return ResponseEntity.ok(accountService.updatePasswordByAccount(account));
    }

    /** 删除账号 */
    @DeleteMapping("/{account}")
    public ResponseEntity<Integer> deleteAccount(@PathVariable Long account) {
        return ResponseEntity.ok(accountService.deleteAccount(account));
    }

    /** ✅ 更新语音参数（不修改唤醒词） */
    @PostMapping("/updateVoice")
    public ResponseEntity<Integer> updateVoice(@RequestBody Account account) {
        int result = accountService.updateVoiceByAccount(account);
        if (result > 0) {
            Account updated = accountService.getAccountByAccount(account.getAccount());
            if (updated != null) {
                TTSConfig.setVoiceParams(updated.getPer(), updated.getSpd(), updated.getPid(), updated.getVol());
                System.out.println("🔄 已实时更新全局TTS参数: per=" + updated.getPer()
                        + ", spd=" + updated.getSpd()
                        + ", pid=" + updated.getPid()
                        + ", vol=" + updated.getVol());
            }
        }
        return ResponseEntity.ok(result);
    }

    /** 更新头像 */
    @PostMapping("/updateAvatar")
    public ResponseEntity<Integer> updateAvatar(@RequestBody Account account) {
        return ResponseEntity.ok(accountService.updateAvatarByAccount(account));
    }

    /** 更新用户名 */
    @PostMapping("/updateUsername")
    public ResponseEntity<Integer> updateUsername(@RequestBody Account account) {
        return ResponseEntity.ok(accountService.updateUsernameByAccount(account));
    }

    /** ✅ 单独更新唤醒词 */
    @PostMapping("/updateWakeWord")
    public ResponseEntity<Integer> updateWakeWord(@RequestBody Account account) {
        int result = accountService.updateWakeWordByAccount(account);
        if (result > 0) {
            Account updated = accountService.getAccountByAccount(account.getAccount());
            if (updated != null)
                System.out.println("🗣️ 唤醒词已更新为: " + updated.getWakeWord());
        }
        return ResponseEntity.ok(result);
    }
}