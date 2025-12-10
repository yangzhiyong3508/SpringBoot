package com.example.edog.controller;

import com.example.edog.entity.Command;
import com.example.edog.service.CommandService;
import com.example.edog.utils.OrderWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 命令控制器
 * 前端访问路径示例：
 *  GET    /api/commands/{account}
 *  POST   /api/commands
 *  PUT    /api/commands/update
 *  DELETE /api/commands/delete?account=123456&content=前进
 */
@RestController
@RequestMapping("/api/commands")
public class CommandController {

    @Autowired
    private CommandService commandService;

    /**
     * ✅ 获取指定账号下的所有命令
     */
    @GetMapping("/{account}")
    public ResponseEntity<Map<String, Object>> getAllCommandByAccount(@PathVariable String account) {
        Map<String, Object> res = new HashMap<>();
        List<Command> commands = commandService.getAllCommandByAccount(account);
        if (commands != null && !commands.isEmpty()) {
            res.put("success", true);
            res.put("msg", "查询成功");
            res.put("data", commands);
        } else {
            res.put("success", true);
            res.put("msg", "暂无命令数据");
            res.put("data", List.of());
        }
        return ResponseEntity.ok(res);
    }

    /**
     * ✅ 添加或更新命令（若存在相同 content，则自动更新）
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addOrUpdateCommand(@RequestBody Command command) {
        Map<String, Object> res = new HashMap<>();
        int result = commandService.addOrUpdateCommand(command);
        if (result > 0) {
            // ✅ 同步刷新 WebSocketHandler 的命令缓存
            OrderWebSocketHandler.refreshCommandList(String.valueOf(command.getAccount()));

            res.put("success", true);
            res.put("msg", "命令保存成功");
        } else {
            res.put("success", false);
            res.put("msg", "命令保存失败");
        }
        return ResponseEntity.ok(res);
    }

    /**
     * ✅ 修改指定命令（根据账号 + content）
     */
    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateMessageByAccountAndContent(@RequestBody Command command) {
        Map<String, Object> res = new HashMap<>();
        int result = commandService.updateMessageByAccountAndContent(
                String.valueOf(command.getAccount()), command.getContent(), command.getMessage());

        if (result > 0) {
            // ✅ 刷新 WebSocket 缓存
            OrderWebSocketHandler.refreshCommandList(String.valueOf(command.getAccount()));

            res.put("success", true);
            res.put("msg", "更新成功");
        } else {
            res.put("success", false);
            res.put("msg", "更新失败，未找到匹配命令");
        }
        return ResponseEntity.ok(res);
    }

    /**
     * ✅ 删除命令（根据账号 + content）
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteCommand(
            @RequestParam("account") String account,
            @RequestParam("content") String content) {

        Map<String, Object> res = new HashMap<>();
        int result = commandService.deleteByAccountAndContent(account, content);

        if (result > 0) {
            // ✅ 刷新 WebSocket 缓存
            OrderWebSocketHandler.refreshCommandList(account);

            res.put("success", true);
            res.put("msg", "删除成功");
        } else {
            res.put("success", false);
            res.put("msg", "删除失败或未找到命令");
        }
        return ResponseEntity.ok(res);
    }
}