package org.ker.ker_plugin_2nd;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public final class Ker_plugin_2nd extends JavaPlugin implements Listener {
    private FileConfiguration config;
    private boolean monitoringEnabled = true;
    private boolean opsfi = true;
    private boolean crossServerCommandMonitoring = true;
    private Set<String> excludedPlayers = new HashSet<>();
    private Set<String> blockedCommands = new HashSet<>();
    private Map<String, Boolean> opNotificationSettings = new HashMap<>();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void onEnable() {
        // 确保插件数据文件夹存在
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // 初始化配置文件
        createDefaultConfig();
        config = getConfig();

        // 加载配置
        loadConfig();

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(this, this);


        getLogger().info(ChatColor.AQUA + "KerPlugin2.命令监控_插件已启动！Kp系列插件Created By Zhangker");

        getLogger().warning(Color.RED + "该插件可能涉及隐私信息收集，因本插件收集信息而可能导致的一切问题与本插件无关，请使用者自行负责！");
        getLogger().warning(Color.GREEN + "如果您决定停用该插件请立即删除或加载完成后使用指令/kp2-icmd set off");
        getLogger().warning(Color.GREEN + "插件作者Zhangker，请勿倒卖或贩售！");

        getCommand("kp2-icmd").setTabCompleter(new CommandCompleter());
        getCommand("kp2-reload").setTabCompleter(new CommandCompleter());
        getCommand("kp2-iplayer").setTabCompleter(new CommandCompleter());
        getCommand("kp2-query").setTabCompleter(new CommandCompleter());
        getCommand("kp2-help").setTabCompleter(new CommandCompleter());
    }
    @Override
    public void onDisable() {
        getLogger().info(ChatColor.RED + "插件已禁用!");
    }
    private boolean HelpCommandLine(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "======KerPlugin2帮助菜单=======");
        sender.sendMessage(ChatColor.AQUA + "/kp2-icmd set on/off 全局启停");
        sender.sendMessage(ChatColor.AQUA + "/kp2-icmd player 设置玩家是否受监督");
        sender.sendMessage(ChatColor.AQUA + "/kp2-iplayer 设置单个在线OP玩家是否收反馈信息");
        sender.sendMessage(ChatColor.AQUA + "/kp2-reload 重新加载（内存泄漏概不负责）");
        sender.sendMessage(ChatColor.AQUA + "/kp2-query 查询历史使用命令记录");
        sender.sendMessage(ChatColor.AQUA + "___|----按时间/玩家查找");
        sender.sendMessage(ChatColor.AQUA + "=============================");
        return true;
    }
    private void createDefaultConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try (InputStream in = getResource("config.yml")) {
                if (in == null) {
                    // 如果资源不存在，创建默认配置
                    configFile.createNewFile();
                    config = YamlConfiguration.loadConfiguration(configFile);
                    config.set("monitoring-enabled", true);
                    config.set("ops-commandFilter-enabled", true);
                    config.set("cross-server-command-monitoring", true);
                    config.set("excluded-players", new ArrayList<>());
                    config.set("blocked-commands", Arrays.asList("login", "register", "l", "reg", "changepassword", "cpw"));
                    config.save(configFile);
                } else {
                    Files.copy(in, configFile.toPath());
                }
            } catch (IOException e) {
                getLogger().severe("无法创建默认配置文件: " + e.getMessage());
            }
        }
    }

    private void loadConfig() {
        // 加载不监控的玩家列表
        excludedPlayers = new HashSet<>(config.getStringList("excluded-players"));

        // 加载阻止记录的命令列表
        blockedCommands = new HashSet<>(config.getStringList("blocked-commands"));
        //加载是否启用OP命令过滤
        opsfi = config.getBoolean("ops-commandFilter-enabled");
        
        // 加载跨服命令监控选项
        crossServerCommandMonitoring = config.getBoolean("cross-server-command-monitoring", true);

        // 加载OP玩家通知设置
        File opSettingsFile = new File(getDataFolder(), "op-settings.yml");
        if (opSettingsFile.exists()) {
            FileConfiguration opSettingsConfig = YamlConfiguration.loadConfiguration(opSettingsFile);
            for (String key : opSettingsConfig.getKeys(false)) {
                opNotificationSettings.put(key, opSettingsConfig.getBoolean(key));
            }
        }

        // 加载监控总开关
        monitoringEnabled = config.getBoolean("monitoring-enabled", true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommandMonitorSupervise(PlayerCommandPreprocessEvent event) {
        if (!monitoringEnabled) return;

        Player sender = event.getPlayer();
        String command = event.getMessage();
        
        // 检查是否为AzSPlugin跨服转发命令
        boolean isCrossServerCommand = isAzsCrossServerCommand(command);
        
        // 如果跨服命令监控被禁用，且当前命令是跨服命令，则不记录
        if (!crossServerCommandMonitoring && isCrossServerCommand) {
            return;
        }

        // 检查是否在不监控列表中
        if (excludedPlayers.contains(sender.getName())) {
            return;
        }

        // 检查是否是敏感命令
        String commandLower = command.toLowerCase().substring(1); // 去掉开头的斜杠
        for (String blockedCmd : blockedCommands) {
            if (commandLower.startsWith(blockedCmd.toLowerCase() + " ") ||
                    commandLower.equals(blockedCmd.toLowerCase())) {
                return; // 如果是敏感命令，不记录
            }
        }

        // 如果op命令过滤为false，就不判断全部输出
        if(!opsfi){
            // 构建通知消息
            String notification = ChatColor.GOLD + "[KP2(OP)] " +
                    ChatColor.AQUA + sender.getName() +
                    ChatColor.WHITE + " 使用命令:" +
                    ChatColor.YELLOW + command;

            // 如果是跨服命令，添加标识
            if (isCrossServerCommand) {
                notification += ChatColor.RED + " [跨服]";
            }

            // 发送给所有在线OP玩家（根据他们的通知设置）
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.isOp() &&
                        opNotificationSettings.getOrDefault(onlinePlayer.getName(), true)) {
                    onlinePlayer.sendMessage(notification);
                }
            }

            // 记录到控制台
            String logMessage = "[KP2命令监控] 成员 " + sender.getName() + " 使用命令: " + command;
            if (isCrossServerCommand) {
                logMessage += " [跨服]";
            }
            getLogger().info(logMessage);

            // 记录到数据文件
            String dateStr = DATE_FORMAT.format(new Date());
            File dateFile = new File(getDataFolder(), "logs/" + dateStr + ".yml");

            try {
                if (!dateFile.getParentFile().exists()) {
                    dateFile.getParentFile().mkdirs();
                }

                if (!dateFile.exists()) {
                    dateFile.createNewFile();
                }

                FileConfiguration dateConfig = YamlConfiguration.loadConfiguration(dateFile);
                List<String> commands = dateConfig.getStringList(sender.getName());
                String logEntry = System.currentTimeMillis() + ": " + command;
                if (isCrossServerCommand) {
                    logEntry += " [跨服]";
                }
                commands.add(logEntry);
                dateConfig.set(sender.getName(), commands);
                dateConfig.save(dateFile);
            } catch (IOException e) {
                getLogger().severe("无法保存命令日志: " + e.getMessage());
            }
        } else if (!sender.isOp() || isCrossServerCommand) { // 如果是跨服命令，即使是OP也要记录
            // 构建通知消息
            String notification = ChatColor.GOLD + "[KP2] " +
                    ChatColor.AQUA + sender.getName() +
                    ChatColor.WHITE + " 使用命令:" +
                    ChatColor.YELLOW + command;

            // 如果是跨服命令，添加标识
            if (isCrossServerCommand) {
                notification += ChatColor.RED + " [跨服]";
            }

            // 发送给所有在线OP玩家（根据他们的通知设置）
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.isOp() &&
                        opNotificationSettings.getOrDefault(onlinePlayer.getName(), true)) {
                    onlinePlayer.sendMessage(notification);
                }
            }

            // 记录到控制台
            String logMessage = "[KP2命令监控] 玩家 " + sender.getName() + " 使用命令: " + command;
            if (isCrossServerCommand) {
                logMessage += " [跨服]";
            }
            getLogger().info(logMessage);

            // 记录到数据文件
            String dateStr = DATE_FORMAT.format(new Date());
            File dateFile = new File(getDataFolder(), "logs/" + dateStr + ".yml");

            try {
                if (!dateFile.getParentFile().exists()) {
                    dateFile.getParentFile().mkdirs();
                }

                if (!dateFile.exists()) {
                    dateFile.createNewFile();
                }

                FileConfiguration dateConfig = YamlConfiguration.loadConfiguration(dateFile);
                List<String> commands = dateConfig.getStringList(sender.getName());
                String logEntry = System.currentTimeMillis() + ": " + command;
                if (isCrossServerCommand) {
                    logEntry += " [跨服]";
                }
                commands.add(logEntry);
                dateConfig.set(sender.getName(), commands);
                dateConfig.save(dateFile);
            } catch (IOException e) {
                getLogger().severe("无法保存命令日志: " + e.getMessage());
            }
        }
    }

    /**
     * 检查是否为AzSPlugin跨服转发命令
     * @param command 命令字符串
     * @return 是否为跨服命令
     */
    private boolean isAzsCrossServerCommand(String command) {
        // AzSPlugin使用/xcmd命令进行跨服转发
        // 格式为: /xcmd <服务器> <命令>
        if (command.startsWith("/xcmd ")) {
            return true;
        }
        
        // 检查是否为通过插件消息接收的命令
        // 这种情况下，命令会通过Bukkit.dispatchCommand执行
        // 我们无法直接检测到这种命令，但可以在日志中添加标识
        
        // 检查是否包含跨服命令的标识（如果之前已经标记过）
        if (command.contains("[跨服]")) {
            return true;
        }
        
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName().toLowerCase()) {
            case "kp2-icmd":
                return handleICmdCommand(sender, args);
            case "kp2-reload":
                return handleReloadCommand(sender);
            case "kp2-iplayer":
                return handleIPlayerCommand(sender, args);
            case "kp2-query":
                return handleQueryCommand(sender, args);
            case "kp2-help":
                return HelpCommandLine(sender);
            default:
                return false;
        }
    }

    private boolean handleICmdCommand(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "用法: /kp2-icmd set <on/off> 或 /kp2-icmd player <add/remove> <玩家名>");
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "用法: /kp2-icmd set <on/off>");
                return true;
            }

            boolean newState = args[1].equalsIgnoreCase("on");
            monitoringEnabled = newState;
            config.set("monitoring-enabled", newState);
            saveConfig();

            sender.sendMessage(ChatColor.GREEN + "命令监控已" + (newState ? "开启" : "关闭"));
            return true;
        }

        if (args[0].equalsIgnoreCase("player")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "用法: /kp2-icmd player <add/remove> <玩家名>");
                return true;
            }

            String action = args[1];
            String playerName = args[2];

            if (action.equalsIgnoreCase("add")) {
                excludedPlayers.add(playerName);
                sender.sendMessage(ChatColor.GREEN + "已添加 " + playerName + " 到不监控玩家列表");
            } else if (action.equalsIgnoreCase("remove")) {
                excludedPlayers.remove(playerName);
                sender.sendMessage(ChatColor.GREEN + "已从不监控玩家列表中移除 " + playerName);
            } else {
                sender.sendMessage(ChatColor.RED + "无效操作");
                return true;
            }

            config.set("excluded-players", new ArrayList<>(excludedPlayers));
            saveConfig();
            return true;
        }

        sender.sendMessage(ChatColor.RED + "未知子命令");
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
            return true;
        }

        reloadConfig();
        config = getConfig();
        loadConfig();

        sender.sendMessage(ChatColor.GREEN + "插件配置已重载");
        return true;
    }

    private boolean handleIPlayerCommand(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /kp2-iplayer <on/off> <玩家名>");
            return true;
        }

        boolean newState = args[0].equalsIgnoreCase("on");
        String playerName = args[1];

        Player target = Bukkit.getPlayer(playerName);
        if (target == null || !target.isOp()) {
            sender.sendMessage(ChatColor.RED + "指定的玩家不是OP或不在线！");
            return true;
        }

        opNotificationSettings.put(playerName, newState);

        // 保存OP通知设置到单独的文件
        File opSettingsFile = new File(getDataFolder(), "op-settings.yml");
        FileConfiguration opSettingsConfig = YamlConfiguration.loadConfiguration(opSettingsFile);
        opSettingsConfig.set(playerName, newState);

        try {
            opSettingsConfig.save(opSettingsFile);
        } catch (IOException e) {
            getLogger().severe("无法保存OP设置: " + e.getMessage());
            sender.sendMessage(ChatColor.RED + "保存设置时出错！");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "已" + (newState ? "开启" : "关闭") + " " + playerName + " 的命令监控通知");
        return true;
    }

    private boolean handleQueryCommand(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "用法: /kp2-query <日期> [玩家名]");
            sender.sendMessage(ChatColor.RED + "日期格式: yyyy-MM-dd (例如: 2025-07-30)");
            sender.sendMessage(ChatColor.RED + "使用 'today' 查询今天的数据");
            return true;
        }

        String dateStr = args[0].equalsIgnoreCase("today") ? DATE_FORMAT.format(new Date()) : args[0];
        String playerName = args.length > 1 ? args[1] : null;

        File dateFile = new File(getDataFolder(), "logs/" + dateStr + ".yml");

        if (!dateFile.exists()) {
            sender.sendMessage(ChatColor.RED + "没有找到 " + dateStr + " 的命令日志");
            return true;
        }

        FileConfiguration dateConfig = YamlConfiguration.loadConfiguration(dateFile);

        if (playerName != null) {
            // 查询特定玩家的命令
            if (!dateConfig.contains(playerName)) {
                sender.sendMessage(ChatColor.RED + "没有找到玩家 " + playerName + " 在 " + dateStr + " 的命令记录");
                return true;
            }

            List<String> commands = dateConfig.getStringList(playerName);
            sender.sendMessage(ChatColor.GOLD + "===== " + playerName + " 在 " + dateStr + " 的命令记录 =====");

            for (String cmd : commands) {
                sender.sendMessage(ChatColor.WHITE + "- " + cmd);
            }

            sender.sendMessage(ChatColor.GOLD + "===== 共 " + commands.size() + " 条记录 =====");
        } else {
            // 查询所有玩家的命令
            sender.sendMessage(ChatColor.GOLD + "===== " + dateStr + " 的命令记录 =====");
            int totalCommands = 0;

            for (String key : dateConfig.getKeys(false)) {
                List<String> commands = dateConfig.getStringList(key);
                sender.sendMessage(ChatColor.AQUA + "玩家 " + key + " (" + commands.size() + " 条命令):");

                for (String cmd : commands) {
                    sender.sendMessage(ChatColor.WHITE + "- " + cmd);
                }

                totalCommands += commands.size();
            }

            sender.sendMessage(ChatColor.GOLD + "===== 共 " + dateConfig.getKeys(false).size() + " 位玩家, " + totalCommands + " 条记录 =====");
        }

        return true;
    }
}