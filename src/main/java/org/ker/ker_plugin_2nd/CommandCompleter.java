package org.ker.ker_plugin_2nd;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommandCompleter implements TabCompleter {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.isOp()) {
            return completions;
        }

        switch (cmd.getName().toLowerCase()) {
            case "kp2-icmd":
                if (args.length == 1) {
                    completions.add("set");
                    completions.add("player");
                    completions.add("set是设置全局命令监控开关(不要选择此项)");
                    completions.add("player是设置单个玩家是否被监控(不要选择此项)");
                } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
                    completions.add("on");
                    completions.add("off");
                    completions.add("on是全局打开命令监控(不要选择此项)");
                    completions.add("off是全局关闭命令监控(不要选择此项)");
                } else if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
                    completions.add("add");
                    completions.add("remove");
                } else if (args.length == 3 && args[0].equalsIgnoreCase("player")) {
                    // 补全在线玩家名
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                }
                break;

            case "kp2-iplayer":
                if (args.length == 1) {
                    completions.add("on");
                    completions.add("off");
                    completions.add("on是开启命令监控聊天反馈(不要选择此项)");
                    completions.add("off是关命令监控聊天反馈(不要选择此项)");
                } else if (args.length == 2) {
                    // 补全在线OP玩家名
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.isOp()) {
                            completions.add("你只能选择在线的OP玩家↓");
                            completions.add(player.getName());
                        }
                    }
                }
                break;

            case "kp2-query":
                if (args.length == 1) {
                    completions.add("today");

                    // 补全已有的日志日期
                    File logsDir = new File(Bukkit.getPluginManager().getPlugin("Ker_plugin_2nd").getDataFolder(), "logs");
                    if (logsDir.exists()) {
                        File[] logFiles = logsDir.listFiles((dir, name) -> name.endsWith(".yml"));
                        if (logFiles != null) {
                            for (File logFile : logFiles) {
                                String fileName = logFile.getName();
                                completions.add(fileName.substring(0, fileName.length() - 4)); // 去掉 .yml 后缀
                            }
                        }
                    }
                } else if (args.length == 2) {
                    // 补全玩家名
                    File logsDir = new File(Bukkit.getPluginManager().getPlugin("Ker_plugin_2nd").getDataFolder(), "logs");
                    String dateStr = args[0].equalsIgnoreCase("today") ? DATE_FORMAT.format(new Date()) : args[0];
                    File dateFile = new File(logsDir, dateStr + ".yml");

                    if (dateFile.exists()) {
                        YamlConfiguration dateConfig = YamlConfiguration.loadConfiguration(dateFile);
                        completions.addAll(dateConfig.getKeys(false));
                    }
                }
                break;
        }

        // 过滤已输入的参数
        if (!args[args.length - 1].isEmpty()) {
            completions.removeIf(s -> !s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()));
        }

        return completions;
    }
}