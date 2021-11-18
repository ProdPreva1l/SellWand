package me.zachary.sellwand.listeners;

import de.tr7zw.changeme.nbtapi.NBTItem;
import me.zachary.sellwand.Sellwand;
import me.zachary.zachcore.utils.*;
import me.zachary.zachcore.utils.hooks.EconomyManager;
import me.zachary.zachcore.utils.hooks.ShopManager;
import me.zachary.zachcore.utils.items.ItemBuilder;
import me.zachary.zachcore.utils.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class RightClickListener implements Listener {
    private Sellwand plugin;

    public RightClickListener(Sellwand plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRightClick(PlayerInteractEvent event){
        if(event.isCancelled())
            return;
        if(event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if(!ReflectionUtils.getVersion().contains("1_8"))
            if(event.getHand() == EquipmentSlot.OFF_HAND)
                return;
        Player player = event.getPlayer();
        NBTItem item = null;
        double amount = 0D;
        int itemAmount = 0;
        if(event.getItem() != null)
            item = new NBTItem(event.getItem());
        if(item != null && item.getBoolean("Is a sell wand")){
            Inventory contents = StorageUtils.getStorageContents(event.getClickedBlock());
            if(contents == null || contents.getContents() == null)
                return;
            CooldownBuilder.createCooldown("Use cooldown");
            if(!player.hasPermission("sellwand.use")){
                MessageUtils.sendMessage(player, plugin.getMessage().getString("No permission"));
                return;
            }
            int uses = item.getInteger("Uses");
            if(uses == 0)
                return;
            event.setCancelled(true);
            if(CooldownBuilder.isCooldown("Use cooldown", player.getUniqueId())){
                MessageUtils.sendMessage(player, plugin.getMessage().getString("Player cooldown").replace("%seconds%", String.valueOf(CooldownBuilder.getCooldown("Use cooldown", player.getUniqueId()) / 1000)));
                return;
            }
            double multiplier = item.getDouble("Multiplier");
            for (int i = 0; i < contents.getContents().length; i++){
                ItemStack chestItem = contents.getItem(i);
                Double price = 0D;
                if(chestItem != null)
                    price = ShopManager.getSellPrice(player, chestItem, chestItem.getAmount());
                if(price != -1.0 && chestItem != null){
                    contents.setItem(i, new ItemBuilder(XMaterial.AIR.parseMaterial()).build());
                    itemAmount += chestItem.getAmount();
                    amount += price;
                }
            }
            amount = amount * multiplier;
            if(plugin.getConfig().getBoolean("Log sell with sell wand in console") && amount != 0D)
                plugin.getLog().log("Player " + player.getName() + " sell " + itemAmount + " items for a total of " + EconomyManager.formatEconomy(amount));
            if(amount != 0D){
                EconomyManager.deposit(player, amount);
                if(uses != -1){
                    uses = uses - 1;
                    if(plugin.getConfig().getBoolean("Destroy wand") && uses == 0)
                        PlayerInventoryUtils.setInMainHand(player, null);
                    else
                        PlayerInventoryUtils.setInMainHand(player, plugin.getSellWandBuilder().getSellWand(1, multiplier, uses));
                }
                MessageUtils.sendMessage(player, plugin.getMessage().getString("Amount give")
                        .replace("%price%", EconomyManager.formatEconomy(amount))
                        .replace("%item_amount%", String.valueOf(itemAmount)));
                int cooldown = PermissionUtils.getNumberFromPermission(player, "sellwand.cooldown", false, 0);
                CooldownBuilder.addCooldown("Use cooldown", player.getUniqueId(), cooldown);
            }
            else
                MessageUtils.sendMessage(player, plugin.getMessage().getString("No item to sell in chest"));
        }
    }
}
