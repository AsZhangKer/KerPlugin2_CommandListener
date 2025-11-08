# KerPlugin2nd - AzSPlugin 跨服命令兼容性更新

## 更新概述

本次更新使KerPlugin2nd能够正确识别和记录AzSPlugin的跨服命令。

## 主要修改

### 1. 跨服命令检测
- 更新了`isAzsCrossServerCommand`方法以正确识别AzSPlugin的`/xcmd`命令
- 当检测到以`/xcmd`开头的命令时，将其标记为跨服命令

### 2. 新增配置选项
- 在config.yml中添加了`cross-server-command-monitoring`选项
- 允许用户控制是否记录跨服命令

### 3. 增强的日志记录
- 跨服命令在日志、控制台输出和OP通知中都会标记`[跨服]`标识
- 即使是OP玩家执行的跨服命令也会被记录（如果配置允许）

## 使用说明

### 配置文件
```yaml
# 是否记录跨服转发的命令
cross-server-command-monitoring: true
```

### 命令记录示例
当玩家执行`/xcmd serverB /say hello`时，将记录：
```
[KP2命令监控] 玩家 TestPlayer 使用命令: /xcmd serverB /say hello [跨服]
```

## 测试验证

1. 已验证代码能够成功编译
2. 跨服命令检测逻辑已根据AzSPlugin的实际实现进行调整
3. 新增的配置选项已正确集成

## 注意事项

1. 本插件记录的是在当前服务器上执行的`/xcmd`命令，而不是在目标服务器上实际执行的命令
2. 如果需要记录目标服务器上的实际命令执行情况，需要在目标服务器上也部署KerPlugin2nd
3. 用户可以通过配置文件控制是否记录跨服命令