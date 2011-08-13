package tzer0.PayDay;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.iConomy.iConomy;
import com.iConomy.system.Holdings;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

// Before anyone mentions it: 
// Yes, I know this code includes a stupid hack when it comes to iConomy.

public class PayDay extends JavaPlugin {
    public PermissionHandler permissions;
    PluginDescriptionFile pdfFile;
    private Configuration conf;
    Payer payer;
    Calendar cal;
    int relative;
    long mode;
    Calendar prev;
    public iConomy iConomy = null;
    LinkedList<String> onlineLastPeriod;
    int failedSoFar;
    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

    @Override
    public void onEnable() {
        pdfFile = this.getDescription();
        conf = getConfiguration();
        conf.save();
        failedSoFar = conf.getInt("failedSoFar", 0);
        prev = Calendar.getInstance();
        String []args = new String[3];
        args[0] = "";
        args[1] = conf.getString("modetype", "d");
        args[2] = conf.getString("mode", "");
        setRecur(null, args);
        setupPermissions();
        payer = new Payer();
        getServer().getScheduler().scheduleAsyncRepeatingTask(this, payer, 0L, 450L);
        getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, new PayDayPlayerListener(this), Priority.Normal, this);
        onlineLastPeriod = new LinkedList<String>();
        System.out.println(pdfFile.getName() + " version "
                + pdfFile.getVersion() + " is enabled!");
    }

    public boolean onCommand(CommandSender sender, Command cmd,
            String commandLabel, String[] uargs) {
        // Keeping everything lower-case except for account names.
        if (sender instanceof Player) {
            Player pl = (Player)sender;
            if ((permissions == null && !pl.isOp()) || (permissions != null && !permissions.has(pl, "payday.admin"))) {
                timeToPayDay(pl);
                return true;
            }
        }
        if (uargs.length != 0) {
            String fullCommand = uargs[0];
            for (int i = 1; i < uargs.length; i++) {
                fullCommand += " " + uargs[i];
            }
            String specialSplit[] = fullCommand.split("\\\"");
            if (specialSplit.length != 1) {
                LinkedList<String> out = new LinkedList<String>();
                for (int i = 0; i < specialSplit.length; i++) {
                    if (specialSplit[i].equalsIgnoreCase("") || specialSplit[i].equalsIgnoreCase(" ")) {
                        continue;
                    }
                    if (i%2 == 1) {
                        out.add(specialSplit[i]);
                    } else {
                        for (String split : specialSplit[i].split(" ")) {
                            if (split.equalsIgnoreCase("")) {
                                continue;
                            }
                            out.add(split);
                        }
                    }
                }
                uargs = new String[1];
                uargs = out.toArray(uargs);
            }
        }
        String[] args = new String[uargs.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = uargs[i].toLowerCase();
        }
        int l = args.length;
        if (l == 0 || (l >= 1 && args[0].equalsIgnoreCase("help"))) {
            int page = 1;
            if (l == 2) {                
                page = toInt(args[1], sender);
            }
            sender.sendMessage(ChatColor.GREEN+"PayDay " + pdfFile.getVersion() + " by TZer0");
            if (page == 1) {
                sender.sendMessage(ChatColor.YELLOW+"Help (all commands start with /pd):");
                sender.sendMessage(ChatColor.YELLOW+"checkerrors " + ChatColor.GREEN + "- checks for errors in the config-file");
                sender.sendMessage(ChatColor.YELLOW+"players [#] " + ChatColor.GREEN + "- shows number # page in the list of players");
                sender.sendMessage(ChatColor.YELLOW+"groups [#] " + ChatColor.GREEN + "- shows number # page in the list of groups");
                sender.sendMessage(ChatColor.YELLOW+"payday [#] " + ChatColor.GREEN + "- initiates a pay day");
                sender.sendMessage(ChatColor.YELLOW+"message [#] " + ChatColor.GREEN + "- set or displays pay day message");
                sender.sendMessage(ChatColor.YELLOW+"set group name value " + ChatColor.GREEN + "- creates a group");
                sender.sendMessage(ChatColor.YELLOW+"set player name groupname " + ChatColor.GREEN + "- assigns a player to a group");
                sender.sendMessage(ChatColor.YELLOW+"move groupname1 groupname2 " + ChatColor.GREEN + "- moves all players");
                sender.sendMessage(ChatColor.YELLOW+"delete player/group name " + ChatColor.GREEN + "- deletes a group/player");
                sender.sendMessage(ChatColor.YELLOW+"searchdelete player/group name " + ChatColor.GREEN + "- wildcard delete");
                sender.sendMessage(ChatColor.YELLOW+"sync [overwrite] " + ChatColor.GREEN + "- imports players and groups");
                sender.sendMessage(ChatColor.YELLOW+"onlinemode [f/t] " + ChatColor.GREEN + "- only online players get paid");
                sender.sendMessage(ChatColor.YELLOW+"lastperiodmode [f/t] " + ChatColor.GREEN + "- only pay players online in last period");
                sender.sendMessage(ChatColor.YELLOW+"time " + ChatColor.GREEN + "- shows how long it is to the next payday");
                sender.sendMessage(ChatColor.YELLOW+"interest [value] " + ChatColor.GREEN + "- sets or shows interest");
                sender.sendMessage(ChatColor.YELLOW+"worldconf [worldname] " + ChatColor.GREEN + "- sets or shows selected world");
                sender.sendMessage(ChatColor.YELLOW+"help 2 for aliases (very useful), help 3 for sched, 4 for essentials");
            } else if (page == 2) {
                sender.sendMessage(ChatColor.YELLOW+"Aliases:");
                sender.sendMessage(ChatColor.YELLOW+"player = pl, players = pl, groups = gr");
                sender.sendMessage(ChatColor.YELLOW+"group = gr, checkerrors = ce, payday = pd");
                sender.sendMessage(ChatColor.YELLOW+"set = s, delete = d, move = mv, t = time");
                sender.sendMessage(ChatColor.YELLOW+"sync = sy, overwrite = ow, searchdelete = sd");
                sender.sendMessage(ChatColor.YELLOW+"message = msg, essentials = ess");
                sender.sendMessage(ChatColor.YELLOW+"interest = i, worldconf = wc");
                sender.sendMessage(ChatColor.YELLOW+"recurring = rec, onlinemode = om, lastperiodmode = lpm");
                sender.sendMessage(ChatColor.YELLOW+"Example usage:");
                sender.sendMessage(ChatColor.YELLOW+"/pd s gr epicgroup 10000");
                sender.sendMessage(ChatColor.YELLOW+"/pd s pl TZer0 epicgroup");
                sender.sendMessage(ChatColor.YELLOW+"/pd pd");
                sender.sendMessage(ChatColor.YELLOW+"/pd d gr epicgroup");

            } else if (page == 3) {
                sender.sendMessage(ChatColor.YELLOW+"Recurring paydays:");
                sender.sendMessage(ChatColor.YELLOW+"Command: /pd recurring <arguments>");
                sender.sendMessage(ChatColor.YELLOW+"There are 3 modes");
                sender.sendMessage(ChatColor.YELLOW+"d = deactivated, usage: /pd rec d");
                sender.sendMessage(ChatColor.YELLOW+"r = relative, usage: /pd rec r minute:hours:days");
                sender.sendMessage(ChatColor.YELLOW+"s = static, usage: /pd rec s (h,d,w,m)");
                sender.sendMessage(ChatColor.YELLOW+"h = hour, d = day, w = week, m = month");
                sender.sendMessage(ChatColor.YELLOW+"s will always happen on the first minute of the hour, month,");
                sender.sendMessage(ChatColor.YELLOW+"day or week.");
            } else if (page == 4) {
                sender.sendMessage(ChatColor.RED + "WARNING: THIS IS NOT DONE, YOU MAY ENCOUNTER ISSUES!");
                sender.sendMessage(ChatColor.YELLOW + "To eanble essentials-support, run this command:");
                sender.sendMessage(ChatColor.YELLOW + "/pd essentials true");
                sender.sendMessage(ChatColor.YELLOW + "To disable:");
                sender.sendMessage(ChatColor.YELLOW + "/pd essentials false");
                sender.sendMessage(ChatColor.RED + "Note: while essentials-support is enabled, sync is unavailable!");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "No such help-page!");
            }
            return true;

        } else if ((args[0].equalsIgnoreCase("essentials") || (args[0].equalsIgnoreCase("ess")))) {
            if (l >= 2) {
                conf.setProperty("essentials", args[1].equalsIgnoreCase("t") || 
                       args[1].equalsIgnoreCase("true") );
            }
            sender.sendMessage(ChatColor.GREEN + "Essentials is set to " + conf.getBoolean("essentials", false));
        } else if ((args[0].equalsIgnoreCase("time") || (args[0].equalsIgnoreCase("t")))) {
            timeToPayDay(sender);
        } else if ((args[0].equalsIgnoreCase("message") || (args[0].equalsIgnoreCase("msg")))) {
            if (l != 1) {
                conf.setProperty("message", uargs[1]);
                conf.save();
            }
            sender.sendMessage(ChatColor.GREEN + "Current pay day-message: " + 
                    ChatColor.GOLD + conf.getString("message", "It is pay day!"));
        } else if ((args[0].equalsIgnoreCase("sync") || (args[0].equalsIgnoreCase("sy")))) {
            if (conf.getBoolean("essentials", false)) {
                sender.sendMessage(ChatColor.RED + "Using essentials: unable to sync.");
                return true;
            } else if (permissions == null) {
                sender.sendMessage(ChatColor.RED + "Permissions unavailable - aborting.");
            } else { 
                boolean overwrite = (l == 2 && (args[1].equalsIgnoreCase("overwrite") || args[1].equalsIgnoreCase("ow")));
                if (overwrite) {
                    conf.removeProperty("players.");
                }
                for (String key: iConomy.Accounts.ranking(iConomy.Accounts.values().size()).keySet()) {
                    key = key.replaceAll("\\.","<dot>");
                    if (key.contains("town-") || key.contains("nation-")) {
                        continue;
                    }
                    if (conf.getString("players."+key) == null || overwrite) {
                        boolean found = false;
                        if (!overwrite) {
                            if (conf.getKeys("players.") != null) {
                                for (String check : conf.getKeys("players.")) {
                                    if (check.equalsIgnoreCase(key)) {
                                        found = true;
                                    }
                                }
                            }
                        }
                        if (!found) {
                            String worldname = conf.getString("worldconf", "world");
                            conf.setProperty("players."+key, permissions.getPrimaryGroup(worldname, key).toLowerCase());
                            if (conf.getString("groups."+permissions.getPrimaryGroup(worldname, key).toLowerCase()) == null) {
                                conf.setProperty("groups."+permissions.getPrimaryGroup(worldname, key).toLowerCase(), 0);
                            }
                        }
                    }
                }
                conf.save();
                sender.sendMessage(ChatColor.GREEN+"Done!");
            }
            return true;
        } else if ((args[0].equalsIgnoreCase("checkerrors") || args[0].equalsIgnoreCase("ce"))) {
            // Utility - checks for errors while not running payday.
            if (!checkErrors(sender)) {
                sender.sendMessage(ChatColor.GREEN+"No errors found.");
            } else {
                sender.sendMessage(ChatColor.RED + "Errors found, fix them before running payday");
            }
        } else if ((args[0].replace("players", "player").equalsIgnoreCase("player") || args[0].equalsIgnoreCase("pl"))) {
            // Lists players
            int page = 0;
            if (l == 2) {
                page = toInt(args[1], sender);
            }
            page(page, sender, "Player");
        } else if ((args[0].replace("groups", "group").equalsIgnoreCase("group") || args[0].equalsIgnoreCase("gr"))) {
            // Lists groups
            int page = 0;
            if (l == 2) {
                page = toInt(args[2], sender);
            }
            page(page, sender, "Group");
        } else if ((args[0].equalsIgnoreCase("onlinemode") || args[0].equalsIgnoreCase("om"))) {
            // Attempts to pay out the predefined amounts of cash, fails before paying out anything if
            // the config is incorrect
            if (l == 2) {
                conf.setProperty("onlinemode", args[1].equalsIgnoreCase("t") || args[1].equalsIgnoreCase("true"));
                conf.save();
            } 
            String state = "";
            if (conf.getBoolean("onlinemode", false)) {
                state = "on.";
            } else {
                state = "off.";
            }
            sender.sendMessage(ChatColor.GREEN + "Online-mode is " + state);
            return true;
        } else if ((args[0].equalsIgnoreCase("worldconf") || args[0].equalsIgnoreCase("wc"))) {
            if (l >= 2) {
                conf.setProperty("worldconf", args[1]);
            }
            sender.sendMessage(ChatColor.GREEN + String.format("Worldconf is set to %s.", conf.getString("worldconf", "world")));
        } else if ((args[0].equalsIgnoreCase("interest") || args[0].equalsIgnoreCase("i"))) {
            if (l >= 2) {
                double newval = 0.0;
                try {
                    newval = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid format!");
                }
                conf.setProperty("interest", newval);
                conf.save();
            }
            sender.sendMessage(ChatColor.GREEN + String.format("Interest is set to %.5f.", conf.getDouble("interest",0.0)));
        }else if ((args[0].equalsIgnoreCase("lastperiodmode") || args[0].equalsIgnoreCase("lpm"))) {

            // Attempts to pay out the predefined amounts of cash, fails before paying out anything if
            // the config is incorrect
            if (l == 2) {
                conf.setProperty("lastperiod", args[1].equalsIgnoreCase("t") || args[1].equalsIgnoreCase("true"));
                conf.save();
            } 
            String state = "";
            if (conf.getBoolean("lastperiod", false)) {
                state = "on.";
            } else {
                state = "off.";
            }
            sender.sendMessage(ChatColor.GREEN + "LPM-mode is " + state);
            return true;
        } else if ((args[0].equalsIgnoreCase("payday") || args[0].equalsIgnoreCase("pd"))) {
            // Attempts to pay out the predefined amounts of cash, fails before paying out anything if
            // the config is incorrect
            int times = 1;
            if (l == 2) {
                try {
                    times = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid number.");
                    return true;
                }
            } else if (l > 2) {
                sender.sendMessage(ChatColor.RED + "Only one additional parameter is allowed");
            }
            payDay(sender, args, times);
            return true;
        } else if ((args[0].equalsIgnoreCase("recurring") || args[0].equalsIgnoreCase("rec"))) {
            setRecur(sender, args);
        } else if ((args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("s"))) {
            // sets either a group's income or a player's group
            if (l == 4) {
                if (args[1].equalsIgnoreCase("group") || args[1].equalsIgnoreCase("gr")) {
                    if (checkInt(args[3]) && !args[2].equalsIgnoreCase("none")) {
                        conf.setProperty("groups."+args[2], Integer.parseInt(args[3]));
                        conf.save();
                        sender.sendMessage(ChatColor.GREEN + "Done.");
                    } else {
                        sender.sendMessage("Invalid value.");
                    }
                } else if (args[1].equalsIgnoreCase("player") || args[1].equalsIgnoreCase("pl")) {
                    uargs[2] = uargs[2].replaceAll("\\.", "<dot>");
                    if (conf.getKeys("players.") != null) {
                        for (String pl : conf.getKeys("players.")) {
                            if (pl.equalsIgnoreCase(uargs[2])) {
                                conf.removeProperty("players."+pl);
                                conf.save();
                            }
                        }
                    }
                    if (conf.getString("groups."+args[3]) != null) {
                        conf.setProperty("players."+uargs[2], args[3]);
                        conf.save();
                        sender.sendMessage(ChatColor.GREEN + "Done");
                    } else {
                        sender.sendMessage(ChatColor.RED + "No such group");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED+ String.format("Unknown type %s!", uargs[2]));
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.RED+"Invalid format, see help");
            }

        } else if ((args[0].equalsIgnoreCase("move") || args[0].equalsIgnoreCase("mv"))) {
            // Moves all players from one group to another - even if the group you're moving from does
            // no longer exist
            if (l == 3) {
                List<String> groups = conf.getKeys("groups.");
                if (groups == null) {
                    sender.sendMessage(ChatColor.RED + "No groups found");
                    return true;
                }
                if (!groups.contains(args[2])) {
                    sender.sendMessage(ChatColor.RED + String.format("No such group %s", args[2]));
                } else {
                    List<String> players = conf.getKeys("players.");
                    for (String pl : players) {
                        if (conf.getString("players."+pl).equalsIgnoreCase(args[1])) {
                            conf.setProperty("players."+pl, args[2]);
                        }
                        conf.save();
                    }
                }
            }
        } else if ((args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("d"))) {
            // deletes either a player or a group
            if (l == 3) {
                if (args[1].equalsIgnoreCase("group") || args[1].equalsIgnoreCase("gr")) {
                    if (conf.getString("groups."+args[2]) != null) {
                        conf.removeProperty("groups."+uargs[2]);
                        conf.save();
                    } else {
                        sender.sendMessage(ChatColor.RED+"No such group: " + args[2]);
                        return true;
                    }
                } else if (args[1].equalsIgnoreCase("player") || args[1].equalsIgnoreCase("pl")) {
                    uargs[2] = uargs[2].replaceAll("\\.", "<dot>");
                    if (conf.getString("players."+uargs[2]) != null) {
                        conf.removeProperty("players."+uargs[2]);
                        conf.save();
                    } else {
                        boolean found = false;
                        if (conf.getKeys("players.") != null) {
                            for (String pl : conf.getKeys("players.")) {
                                if (pl.equalsIgnoreCase(uargs[2])) {
                                    conf.removeProperty("players."+pl);
                                    conf.save();
                                    found = true;
                                }
                            }
                        }
                        if (!found) {
                            sender.sendMessage(ChatColor.RED+"No such player: " + uargs[2]);
                            return true;
                        }
                    }
                } else {
                    sender.sendMessage(ChatColor.RED+ String.format("Unknown type %s!", args[1]));
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Done");
            } else {
                sender.sendMessage(ChatColor.RED+"Incorrect format, see help");
            }
        } else if (l  >= 1 && (args[0].equalsIgnoreCase("searchdelete") || args[0].equalsIgnoreCase("sd"))) {
            int i = 0;
            if (l == 3) {
                if (args[1].equalsIgnoreCase("group") || args[1].equalsIgnoreCase("gr")) {
                    for (String key : conf.getKeys("groups.")) {
                        if (key.contains(args[2].toLowerCase())) {
                            i += 1;
                            conf.removeProperty("groups."+key);
                        }
                    }
                    conf.save();
                } else if (args[1].equalsIgnoreCase("player") || args[1].equalsIgnoreCase("pl")) {
                    for (String key : conf.getKeys("players.")) {
                        if (key.contains(uargs[2].toLowerCase())) {
                            i += 1;
                            conf.removeProperty("players."+key);
                        }
                    }
                    conf.save();
                } else {
                    sender.sendMessage(ChatColor.RED+ String.format("Unknown type %s!", args[1]));
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + String.format("Done, %d entries removed", i));
            } else {
                sender.sendMessage(ChatColor.RED+"Incorrect format, see help");
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + "No such command, see help!");
        }
        return true;
    }


    public void timeToPayDay(CommandSender sender) {
        Calendar tmp = Calendar.getInstance();
        String prePart = "PayDay will occur";
        if (sender instanceof Player) {
            prePart = String.format("You will get %d money", 
                    conf.getInt("groups."+conf.getString("players."+((Player) sender).getName(), "none"),0));
        }
        if (mode == 0){
            sender.sendMessage(ChatColor.RED + "No recurring paydays set up :(");
        } else if (mode > 0) {
            sender.sendMessage(ChatColor.GREEN + String.format("%s in %d seconds", prePart, 
                    mode - (tmp.getTimeInMillis()-prev.getTimeInMillis())/1000 ));
        } else {
            String unit = "";
            if (mode == -1) {
                unit = "hour.";
            } else if (mode == -2) {
                unit = "day.";
            } else if (mode == -3) {
                unit = "week.";
            } else if (mode == -4) {
                unit = "month.";
            }
            sender.sendMessage(ChatColor.GREEN + String.format("%s on the beginning of the next %s", prePart, unit));
        }
    }

    public void setRecur(CommandSender sender, String []args) {
        boolean sendMsg = sender != null;
        int l = args.length;
        if (l >= 2) {
            if (args[1].equalsIgnoreCase("d") || args[1].equalsIgnoreCase("deactivated")) {
                if (sendMsg) {
                    sender.sendMessage(ChatColor.GREEN+"Recurring paydays deactivated");
                }
                prev = Calendar.getInstance();
                conf.setProperty("modetype", "d");
                conf.setProperty("mode", "");
                conf.save();
                mode = 0;
            } else if (args[1].equalsIgnoreCase("r") || args[1].equalsIgnoreCase("relative")) {
                if (l == 3) {
                    mode = 0;
                    String []split = args[2].split(":");
                    int v;
                    for (int i = 0; i < Math.min(split.length, 3); i++) {
                        v = toInt(split[i], sender);
                        if (v < 0) {
                            if (sendMsg) {
                                sender.sendMessage(ChatColor.RED+"Invalid format, deactivating paydays.");
                            }
                            mode = 0;
                            conf.setProperty("modetype", "d");
                            conf.setProperty("mode", "");
                            conf.save();
                            return;
                        }
                        if (i == 0) {
                            mode += v*60;
                        } else if (i == 0) {
                            mode += v*3600;
                        } else if (i == 1) {
                            mode += v*3600*24;
                        }
                    }
                    if (sendMsg) {
                        sender.sendMessage(ChatColor.GREEN + "Done.");
                    }
                    prev = Calendar.getInstance();
                    conf.setProperty("mode", args[2]);
                    conf.setProperty("modetype", "r");
                    conf.save();
                } else {
                    if (sendMsg) {
                        sender.sendMessage(ChatColor.RED+"Invalid format, deactivating paydays.");
                    }
                    conf.setProperty("modetype", "d");
                    conf.setProperty("mode", "");
                    conf.save();
                }
            } else if (args[1].equalsIgnoreCase("s") || args[1].equalsIgnoreCase("static")) {
                if (l == 3) {
                    if (args[2].equalsIgnoreCase("h") || args[2].equalsIgnoreCase("hour")) {
                        conf.setProperty("mode", "h");
                        mode = -1;
                    } else if (args[2].equalsIgnoreCase("d") || args[2].equalsIgnoreCase("day")) {
                        conf.setProperty("mode", "d");
                        mode = -2;
                    } else if (args[2].equalsIgnoreCase("w") || args[2].equalsIgnoreCase("week")) {
                        conf.setProperty("mode", "w");
                        mode = -3;
                    } else if (args[2].equalsIgnoreCase("m") || args[2].equalsIgnoreCase("month")) {
                        conf.setProperty("mode", "m");
                        mode = -4;
                    } else {
                        if (sendMsg) {
                            sender.sendMessage(ChatColor.RED+"Invalid format, deactivating paydays.");
                        }
                        conf.setProperty("modetype", "d");
                        conf.setProperty("mode", "");
                        conf.save();
                        return;
                    }
                    if (sendMsg) {
                        sender.sendMessage(ChatColor.GREEN + "Done.");
                    }
                    prev = Calendar.getInstance();
                    conf.setProperty("modetype", "s");
                    conf.save();
                } else {
                    if (sendMsg) {
                        sender.sendMessage(ChatColor.RED+"Invalid format, deactivating paydays.");
                    }
                    conf.setProperty("modetype", "d");
                    conf.setProperty("mode", "");
                    conf.save();
                }
            } 
        } else {
            if (sendMsg) {
                sender.sendMessage(ChatColor.GREEN+String.format("Current setting: %s - %s", 
                        conf.getString("modetype", "d"), conf.getString("mode", "")));
            }
        }
    }


    public void payDay(CommandSender sender, String []args, int times) {
        Essentials ess = (Essentials) getServer().getPluginManager().getPlugin("Essentials");
        int l = args.length;
        String[] uargs = new String[l];
        boolean sendMsg = sender != null;
        for (int i = 0; i < l; i++) {
            uargs[i] = args[i].toLowerCase();
        }
        if (checkErrors(sender)) {
            if (sendMsg) {
                sender.sendMessage(ChatColor.RED + "Errors found, fix them before running payday");
            } else {
                conf.setProperty("lastFailed", true);
                failedSoFar += times;
                conf.setProperty("failedSoFar", failedSoFar);

                conf.save();
            }
            return;
        } else {
            failedSoFar = 0;
            conf.setProperty("failedSoFar", failedSoFar);
            conf.save();
        }
        //Removed for now - may be re-used at some point.
        /*List<String> pay = null;
        if (l == 3) {
            pay = new LinkedList<String>();
            List<String> full = conf.getKeys("players.");
            if (args[1].equalsIgnoreCase("group") || args[1].equalsIgnoreCase("gr")) {
                for (String name : full) {
                    if (conf.getString("players."+name).equalsIgnoreCase(args[2])) {
                        pay.add(name);
                    }
                }
            } else if (args[1].equalsIgnoreCase("player") || args[1].equalsIgnoreCase("pl")) {
                if (full.contains(uargs[2])) {
                    pay.add(uargs[2]);
                } else {
                    if (sendMsg) {
                        sender.sendMessage(ChatColor.RED + "No such player!");
                    }
                    return;
                }
            } else {
                if (sendMsg) {
                    sender.sendMessage(ChatColor.RED + "Invalid 3rd parameter, must be group or player");
                }
                return;
            }
       } else if (l == 1) {*/
        List<String> pay = conf.getKeys("players.");
        //}

        String timesString = "";
        if (times != 1) {
            timesString = "x"+times;
        }
        getServer().broadcastMessage(ChatColor.GOLD+ 
                conf.getString("message", "It is Pay Day!")+timesString);

        if (l <= 3 || l == 1) {
            if (conf.getBoolean("lastperiod", false)) {
                addOnlinePlayers();
                pay = onlineLastPeriod;
                onlineLastPeriod = new LinkedList<String>();
                addOnlinePlayers();
            } else if (conf.getBoolean("onlinemode", false)) {
                LinkedList<String> filtered = new LinkedList<String>();
                for (Player online : getServer().getOnlinePlayers()) {
                    boolean found = false;
                    for (String pl : pay) {
                        if (online.getName().equalsIgnoreCase(pl)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        filtered.add(online.getName());
                    }
                }
                pay = filtered;
            }
            for (String pl : pay) {
                int payment = times * conf.getInt("groups."+conf.getString("players."+pl, "none"),0);
                if (conf.getBoolean("essentials", false)) {
                    User us = ess.getOfflineUser(pl.replaceAll("<dot>", "."));
                    us.giveMoney(payment);
                    if (us.getMoney() < 0) {
                        us.setMoney(0);
                    }
                } else {
                    Holdings acc = iConomy.getAccount(pl.replaceAll("<dot>", ".")).getHoldings();
                    acc.add(payment);
                    if (acc.balance() < 0) {
                        acc.set(0);
                    }
                }
            }
        } else {
            if (sendMsg) {
                sender.sendMessage(ChatColor.RED+"Incorrect format, see help");
            }
            return;
        }
        if (sendMsg) {
            sender.sendMessage(ChatColor.GREEN+"Payday complete");
        }
    }

    void addOnlinePlayers() {
        for (Player pl : getServer().getOnlinePlayers()) {
            if (!onlineLastPeriod.contains(pl.getName())) {
                onlineLastPeriod.add(pl.getName());
            }
        }
    }

    /**
     * Checks the configuration for errors - true if errors are found.
     * 
     * @param sender The one who will receive the error-messages
     * @param ic iConomy-bank
     * @return
     */
    public boolean checkErrors(CommandSender sender) {
        Essentials ess = (Essentials) getServer().getPluginManager().getPlugin("Essentials");
        boolean useEss = conf.getBoolean("essentials", false);
        boolean sendMsg = sender != null;
        conf.setProperty("lastFailed", false);
        conf.save();
        if (sendMsg) {
            sender.sendMessage(ChatColor.YELLOW+"Checking for errors.");
        }
        boolean failed = false;
        if (conf.getString("failed.") != null) {
            conf.removeProperty("failed.");
        }
        List<String> keys = conf.getKeys("players.");
        List<String> groups = conf.getKeys("groups.");
        if (keys == null || groups == null) {
            if (sendMsg) {
                sender.sendMessage(ChatColor.RED + "No configuration (groups or players)!");
            }
            return true;
        }
        for (String pl : keys) {
            String rpl = pl.replaceAll("<dot>", ".");
            if (pl.contains("town-") || pl.contains("nation-")) {
                if (sendMsg) {
                    sender.sendMessage(ChatColor.YELLOW + rpl + " may be a town or a nation.");
                }
            }
            if ((useEss && ess.getOfflineUser(rpl) == null) || (!useEss && !iConomy.hasAccount(rpl))) {
                boolean notFound = true;
                String corr = "";
                if (sendMsg) {
                    sender.sendMessage(ChatColor.RED+String.format("%s doesn't have an account!", rpl));
                }
                if (!useEss) {
                    for (String acc : iConomy.Accounts.ranking(iConomy.Accounts.values().size()).keySet()) {
                        if (acc.replaceAll("<dot>",".").equalsIgnoreCase(rpl)) {
                            conf.setProperty("players."+acc, conf.getProperty("players."+pl));
                            conf.removeProperty("players."+pl);
                            conf.save();
                            corr = acc.replaceAll("<dot>", ".");
                            notFound = false;
                            if (sendMsg) {
                                sender.sendMessage(ChatColor.GREEN + String.format("Corrected case for %s to %s.", rpl, corr));
                            }
                            pl = acc;
                            break;
                        }
                    }
                } else {
                    if (sendMsg) {
                        sender.sendMessage(ChatColor.YELLOW + "Warning: Using essentials. Unable to check for case mismatch!");
                    }
                }
                if (notFound) {
                    failed = true;
                }
            }
            if (!groups.contains(conf.getString("players."+pl))) {
                if (sendMsg) {
                    sender.sendMessage(ChatColor.RED+String.format("%s belongs to an invalid group - %s", 
                            rpl, conf.getString("players."+pl).replaceAll("<dot>", ".")));
                }
                failed = true;
            }
        }
        return failed;

    }
    /**
     * Displays information about either groups or players.
     * @param page Page to view
     * @param sender Who gets the output
     * @param node either group or player - decides what is shown.
     */
    public void page(int page, CommandSender sender, String node) {
        List<String> items = conf.getKeys(node.toLowerCase()+"s.");

        if (items != null && page*10 < items.size()) {
            sender.sendMessage(String.format("Listing %ss, page %d of %d", node, page, (items.size()-1)/10+1));
            for (int i = page*10; i < Math.min(items.size(), page*10+10); i++) {
                sender.sendMessage(items.get(i).replaceAll("<dot>", ".") + " - " + 
                        conf.getString(node.toLowerCase()+"s."+items.get(i), "error"));
            }
            if (items.size() > page*10+10) {
                sender.sendMessage(String.format("/pd %ss %d for next page", node, page+1));
            }
        } else {
            sender.sendMessage("No more items.");
        }
    }
    /**
     * Converts to int if valid, if not: returns 0
     * @param in
     * @param sender
     * @return
     */
    public int toInt(String in, CommandSender sender) {
        int out = 0;
        if (checkInt(in)) {
            out = Integer.parseInt(in);
        } else {
            return -1;
        }
        return out;
    }
    /**
     * Checks if a string is valid as a representation of an unsigned int.
     */
    public boolean checkInt(String in) {
        char chars[] = in.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (!(Character.isDigit(chars[i]) || (i == 0 && chars[i] == '-'))) {
                return false;
            }
        }
        return true;
    }
    /**
     * Basic Permissions-setup, see more here: https://github.com/TheYeti/Permissions/wiki/API-Reference
     */
    private void setupPermissions() {
        Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

        if (this.permissions == null) {
            if (test != null) {
                this.permissions = ((Permissions) test).getHandler();
            } else {
                System.out.println(ChatColor.YELLOW
                        + "Permissons not detected - defaulting to OP!");
            }
        }
    }
    class Payer extends Thread {
        public void run() {
            Essentials ess = (Essentials) getServer().getPluginManager().getPlugin("Essentials");
            boolean useEss = conf.getBoolean("essentials", false);
            if (mode == 0) {
                return;
            }
            int pay = 0;
            Calendar tmp = Calendar.getInstance();
            if (mode == -1) {
                if (prev.get(Calendar.HOUR) != tmp.get(Calendar.HOUR)) {
                    pay++;
                }
            } else if (mode == -2) {
                if (prev.get(Calendar.DAY_OF_MONTH) != tmp.get(Calendar.DAY_OF_MONTH)) {
                    pay++;
                }
            } else if (mode == -3) {
                if (prev.get(Calendar.WEEK_OF_YEAR) != tmp.get(Calendar.WEEK_OF_YEAR)) {
                    pay++;
                }
            } else if (mode == -4) {
                if (prev.get(Calendar.MONTH) != tmp.get(Calendar.MONTH)) {
                    pay++;
                }
            } else {
                while (tmp.getTimeInMillis()-prev.getTimeInMillis() > mode*1000) {
                    pay++;
                    prev.setTimeInMillis(prev.getTimeInMillis() + mode*1000);
                }
            }
            if (pay > 0) {
                String []args = new String[1];
                args[0] = "pd";
                payDay(null, args, pay);
                if (mode < 0) {
                    prev = tmp;
                }
            }
            double interest = conf.getDouble("interest", 0.0);
            if (failedSoFar == 0 && Math.abs(interest) > 0.00000001) {
                String keys[] = null;
                LinkedHashMap<String, Double> econ = null;
                if (useEss) {
                    keys = conf.getStringList("players.", null).toArray(keys);
                } else {
                    econ = iConomy.Accounts.ranking(iConomy.Accounts.values().size());
                    keys = econ.keySet().toArray(keys);
                }
                for ( String pl : keys ) {
                    pl = pl.replaceAll("<dot>", ".");
                    if (useEss) {
                        ess.getOfflineUser(pl).giveMoney(ess.getOfflineUser(pl).getMoney()*(Math.pow(1+interest/100, pay) -1));
                    } else {
                        iConomy.getAccount(pl).getHoldings().add(econ.get(pl)*(Math.pow(1+interest/100,pay) - 1));
                    }
                }
            }
            if (failedSoFar > 0) {
                String msg = ChatColor.RED + String.format("The last %d payday(s) failed, see /pd ce for more information", failedSoFar);
                System.out.println(msg);
                for (Player pl : getServer().getOnlinePlayers()) {
                    if ((permissions == null && !pl.isOp()) || (permissions != null && !permissions.has(pl, "payday.admin"))) {
                        return;
                    } else {
                        pl.sendMessage(msg);
                    }
                }
            }
        }
    }
}
