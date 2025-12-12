package com.example.edog.controller;

import com.example.edog.entity.Account;
import com.example.edog.service.AccountService;
import com.example.edog.service.WebSocketServer; // ✅ 引入 WebSocketServer
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/account/{account}")
    public ResponseEntity<List<Account>> getAccountsByAccount(@PathVariable Long account) {
        return ResponseEntity.ok(accountService.getAccountsByAccount(account));
    }

    @PostMapping
    public ResponseEntity<Integer> addAccount(@RequestBody Account account) {
        int result = accountService.addAccount(account);
        if (result == -1) return ResponseEntity.badRequest().body(-1);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/updatePassword")
    public ResponseEntity<Integer> updatePassword(@RequestBody Account account) {
        return ResponseEntity.ok(accountService.updatePasswordByAccount(account));
    }

    @DeleteMapping("/{account}")
    public ResponseEntity<Integer> deleteAccount(@PathVariable Long account) {
        return ResponseEntity.ok(accountService.deleteAccount(account));
    }

    /** * ✅ 修复后的 updateVoice 接口
     * 不再调用 getPer/getSpd 等不存在的方法
     * 改为调用 WebSocketServer 更新全局语音参数
     */
    @PostMapping("/updateVoice")
    public ResponseEntity<Integer> updateVoice(@RequestBody Account account) {
        int result = accountService.updateVoiceByAccount(account);
        if (result > 0) {
            Account updated = accountService.getAccountByAccount(account.getAccount());
            if (updated != null) {
                // 🔥 核心修复：调用 WebSocketServer 的静态方法更新参数
                WebSocketServer.setVoiceParams(updated);
                
                System.out.println("🔄 已实时更新全局语音参数: voiceId=" + updated.getVoiceId()
                        + ", speed=" + updated.getSpeedRatio()
                        + ", vol=" + updated.getVol());
            }
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/updateAvatar")
    public ResponseEntity<Integer> updateAvatar(@RequestBody Account account) {
        return ResponseEntity.ok(accountService.updateAvatarByAccount(account));
    }

    @PostMapping("/updateUsername")
    public ResponseEntity<Integer> updateUsername(@RequestBody Account account) {
        return ResponseEntity.ok(accountService.updateUsernameByAccount(account));
    }

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