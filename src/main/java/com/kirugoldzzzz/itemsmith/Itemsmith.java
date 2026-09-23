package com.kirugoldzzzz.itemsmith;

import com.foliagui.FoliaGUI;
import com.kirugoldzzzz.itemsmith.common.command.NexusCommand;
import com.kirugoldzzzz.itemsmith.common.config.ConfigFile;
import com.kirugoldzzzz.itemsmith.common.gui.Guis;
import com.kirugoldzzzz.itemsmith.common.scheduler.Scheduling;
import com.kirugoldzzzz.itemsmith.common.text.Messages;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Itemsmith extends JavaPlugin {

    @Override
    public void onEnable() {
        Scheduling.bind(this);
        FoliaGUI.init(this);
        Guis.installTheme();
        Messages.load(new ConfigFile(this, "messages.yml").load().get());
        ItemEditService service = new ItemEditService();
        bind("item", new ItemEditCommand(service, new ItemEditMenu(service)));
    }

    @Override
    public void onDisable() {
        FoliaGUI.shutdown();
    }

    private void bind(String name, NexusCommand executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("La commande " + name + " est absente du plugin.yml");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
