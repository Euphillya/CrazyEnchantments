package com.badbones69.crazyenchantments.paper.api.builders.types.blacksmith;

import com.badbones69.crazyenchantments.paper.CrazyEnchantments;
import com.badbones69.crazyenchantments.paper.Methods;
import com.badbones69.crazyenchantments.paper.Starter;
import com.badbones69.crazyenchantments.paper.api.builders.InventoryBuilder;
import com.badbones69.crazyenchantments.paper.api.economy.Currency;
import com.badbones69.crazyenchantments.paper.api.economy.CurrencyAPI;
import com.badbones69.crazyenchantments.paper.api.enums.Messages;
import com.badbones69.crazyenchantments.paper.api.objects.BlackSmithResult;
import com.badbones69.crazyenchantments.paper.controllers.settings.EnchantmentBookSettings;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BlackSmithMenu extends InventoryBuilder {

    public BlackSmithMenu(Player player, int size, String title) {
        super(player, size, title);
    }

    @Override
    public InventoryBuilder build() {
        Inventory inventory = getInventory();

        for (int slot : new int[]{0, 7, 8, 9, 16, 18, 25, 26}) {
            inventory.setItem(slot, BlackSmithManager.getGrayGlass());
        }

        for (int slot : new int[]{1,2,3,4,5,6,10,12,13,15,19,20,21,22,23,24}) {
            inventory.setItem(slot, BlackSmithManager.getRedGlass());
        }

        inventory.setItem(17, BlackSmithManager.getExitButton());

        return this;
    }

    public static class BlackSmithListener implements Listener {

        private final int mainSlot = 10;
        private final int subSlot = 13;
        private final int outputSlot = 16;

        @NotNull
        private final CrazyEnchantments plugin = CrazyEnchantments.get();

        @NotNull
        private final Starter starter = this.plugin.getStarter();

        @NotNull
        private final Methods methods = this.starter.getMethods();

        @NotNull
        private final EnchantmentBookSettings settings = this.starter.getEnchantmentBookSettings();

        private static int[] resultBorder = new int[]{0, 7, 8, 9, 16, 18, 25, 26};

        @EventHandler(ignoreCancelled = true)
        public void onInventoryClick(InventoryClickEvent event) {
            Inventory inventory = event.getInventory();

            if (!(inventory.getHolder(false) instanceof BlackSmithMenu holder)) return;

            InventoryView view = holder.getInventoryView();

            event.setCancelled(true);

            ItemStack item = event.getCurrentItem();

            // If item is null, we return.
            if (item == null) return;

            // If item is not 1, return.
            if (item.getAmount() != 1) return;

            Player player = holder.getPlayer();

            if (this.settings.getEnchantments(item).isEmpty() || !this.settings.isEnchantmentBook(item)) return;

            // 10 is the main slot.
            ItemStack root = inventory.getItem(this.mainSlot);

            // If the click is in the player inventory.
            if (event.getClickedInventory() != view.getTopInventory()) {
                if (root == null) {
                    event.setCurrentItem(new ItemStack(Material.AIR));
                    inventory.setItem(this.mainSlot, item);
                    //todo() play sound.

                    ItemStack subItem = inventory.getItem(this.subSlot);

                    if (subItem != null) {
                        BlackSmithResult result = new BlackSmithResult(player, inventory.getItem(this.mainSlot), subItem);
                        setBorder(result, inventory);
                    }

                    return;
                }

                event.setCurrentItem(new ItemStack(Material.AIR));

                ItemStack subItem = inventory.getItem(this.subSlot);

                if (subItem != null) {
                    event.setCurrentItem(subItem);
                }

                inventory.setItem(this.subSlot, item);
                //todo() play sound.

                BlackSmithResult result = new BlackSmithResult(player, inventory.getItem(this.mainSlot), subItem);
                setBorder(result, inventory);

                return;
            }

            //todo() add menu clicks.
            if (event.getRawSlot() == this.mainSlot || event.getRawSlot() == this.subSlot) {
                event.setCurrentItem(new ItemStack(Material.AIR));
                this.methods.addItemToInventory(player, item);

                inventory.setItem(this.outputSlot, BlackSmithManager.getExitButton());

                for (int slot : resultBorder) {
                    inventory.setItem(slot, BlackSmithManager.getBlueGlass());
                }

                //todo() play sound.
            }

            if (event.getRawSlot() == this.outputSlot) {
                if (inventory.getItem(this.mainSlot) != null && inventory.getItem(this.subSlot) != null) {
                    BlackSmithResult result = new BlackSmithResult(player, inventory.getItem(this.mainSlot), inventory.getItem(this.subSlot));

                    if (result.getCost() > 0) {
                        Currency currency = BlackSmithManager.getCurrency();

                        if (currency != null && player.getGameMode() != GameMode.CREATIVE) {
                            CurrencyAPI currencyAPI = this.plugin.getStarter().getCurrencyAPI();

                            if (currencyAPI.canBuy(player, currency, result.getCost())) {
                                currencyAPI.takeCurrency(player, currency, result.getCost());
                            } else {
                                String needed = String.valueOf(result.getCost() - currencyAPI.getCurrency(player, currency));

                                this.methods.switchCurrency(player, currency, "%Money_Needed%", "%XP%", needed);
                                return;
                            }
                        }

                        this.methods.addItemToInventory(player, result.getResultItem());

                        inventory.setItem(this.mainSlot, new ItemStack(Material.AIR));
                        inventory.setItem(this.subSlot, new ItemStack(Material.AIR));

                        //todo() play sound

                        inventory.setItem(this.outputSlot, BlackSmithManager.getExitButton());

                        for (int slot : resultBorder) {
                            inventory.setItem(slot, BlackSmithManager.getRedGlass());
                        }
                        
                        return;
                    }

                    //todo() play sound
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);

                    return;
                }

                //todo() play sound
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onInventoryDrag(InventoryDragEvent event) {
            Inventory inventory = event.getInventory();

            if (!(inventory.getHolder(false) instanceof BlackSmithMenu)) return;

            event.setCancelled(true);
        }

        @EventHandler(ignoreCancelled = true)
        public void onInventoryClose(InventoryCloseEvent event) {
            Inventory inventory = event.getInventory();

            if (!(inventory.getHolder(false) instanceof BlackSmithMenu holder)) return;

            Player player = holder.getPlayer();

            for (int slot : new int[]{10, 13}) {
                ItemStack itemStack = inventory.getItem(slot);

                if (itemStack != null) {
                    if (itemStack.getType() != Material.AIR) {
                        this.methods.addItemToInventory(player, itemStack);
                    }
                }
            }

            inventory.clear();
        }

        private void setBorder(BlackSmithResult item, Inventory inventory) {
            if (item.getCost() > 0) {
                ItemStack result = item.getResultItem();

                String value = String.valueOf(item.getCost());
                String message = Messages.replacePlaceholders("%Cost%", value, BlackSmithManager.getItemCost());

                for (int slot : resultBorder) {
                    inventory.setItem(slot, BlackSmithManager.getBlueGlass());
                }

                inventory.setItem(this.outputSlot, this.methods.addLore(result, message));
                return;
            }

            inventory.setItem(this.outputSlot, BlackSmithManager.getExitButton());

            for (int slot : resultBorder) {
                inventory.setItem(slot, BlackSmithManager.getRedGlass());
            }
        }
    }
}