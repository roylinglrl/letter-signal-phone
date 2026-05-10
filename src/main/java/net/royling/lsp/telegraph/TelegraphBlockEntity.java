package net.royling.lsp.telegraph;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.royling.lsp.registry.ModBlockEntities;
import net.royling.lsp.registry.ModItems;
import net.royling.lsp.telegraph.menu.TelegraphMenu;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TelegraphBlockEntity extends BaseContainerBlockEntity {
    public static final int SLOT_FREQ_A = 0;
    public static final int SLOT_FREQ_B = 1;
    public static final int SLOT_SEND_INPUT = 2;
    public static final int SLOT_WORK = 3;
    public static final int SLOT_RECEIVE_INPUT = 4;
    public static final int SLOT_RECEIVE_OUTPUT_START = 5;
    public static final int RECEIVE_OUTPUT_COUNT = 6;
    public static final int SLOT_COUNT = SLOT_RECEIVE_OUTPUT_START + RECEIVE_OUTPUT_COUNT;

    private static final String DOT = "\u00b7";
    private static final Set<TelegraphBlockEntity> LOADED = new HashSet<>();
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private String frequency = "";
    private String message = "";
    private boolean composing;
    private UUID usingPlayer;

    public TelegraphBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TELEGRAPH.get(), pos, state);
    }

    @Override
    public void onLoad() {
        if (level != null && !level.isClientSide()) {
            LOADED.add(this);
        }
    }

    @Override
    public void onChunkUnloaded() {
        LOADED.remove(this);
    }

    @Override
    public void setRemoved() {
        LOADED.remove(this);
        super.setRemoved();
    }

    public void confirmFrequency() {
        frequency = frequencyOf(items.get(SLOT_FREQ_A), items.get(SLOT_FREQ_B));
        setChanged();
    }

    public void pressDot() {
        signal(DOT, true);
    }

    public void pressDash() {
        signal("-", true);
    }

    public void pressSpace() {
        if (!composing || message.endsWith(",") || message.isEmpty()) {
            return;
        }
        message += ",";
        updateWorkPaper();
    }

    public void start() {
        if (composing || !items.get(SLOT_WORK).isEmpty() || !items.get(SLOT_SEND_INPUT).is(Items.PAPER)) {
            return;
        }
        items.get(SLOT_SEND_INPUT).shrink(1);
        ItemStack paper = new ItemStack(ModItems.TELEGRAM_PAPER.get());
        message = "";
        composing = true;
        TelegraphData.setMessage(paper, message);
        items.set(SLOT_WORK, paper);
        setChanged();
    }

    public void finish() {
        if (!composing) {
            return;
        }
        composing = false;
        trimTrailingSeparator();
        updateWorkPaper();
        if (!message.isBlank()) {
            broadcastMessage(message);
        }
        setChanged();
    }

    public boolean isComposing() {
        return composing;
    }

    public String message() {
        return message;
    }

    public boolean beginUse(UUID playerId) {
        if (usingPlayer != null && !usingPlayer.equals(playerId)) {
            return false;
        }
        usingPlayer = playerId;
        return true;
    }

    public void endUse(UUID playerId) {
        if (usingPlayer != null && usingPlayer.equals(playerId)) {
            usingPlayer = null;
        }
    }

    private void signal(String token, boolean audible) {
        if (audible) {
            broadcastSound(token.equals(DOT));
        }
        if (composing) {
            message += token;
            updateWorkPaper();
        }
    }

    private void updateWorkPaper() {
        ItemStack paper = items.get(SLOT_WORK);
        if (!paper.isEmpty()) {
            TelegraphData.setMessage(paper, message);
        }
        setChanged();
    }

    private void trimTrailingSeparator() {
        while (message.endsWith(",")) {
            message = message.substring(0, message.length() - 1);
        }
    }

    private void broadcastSound(boolean dot) {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (frequency.isEmpty()) {
            playSound(dot);
            return;
        }
        for (TelegraphBlockEntity telegraph : LOADED) {
            if (telegraph.level == level && frequency.equals(telegraph.frequency)) {
                telegraph.playSound(dot);
            }
        }
    }

    private void broadcastMessage(String sentMessage) {
        if (level == null || level.isClientSide() || frequency.isEmpty()) {
            return;
        }
        for (TelegraphBlockEntity telegraph : LOADED) {
            if (telegraph.level == level && frequency.equals(telegraph.frequency)) {
                telegraph.receive(sentMessage);
            }
        }
    }

    private void receive(String sentMessage) {
        ItemStack input = items.get(SLOT_RECEIVE_INPUT);
        if (!input.is(Items.PAPER)) {
            return;
        }
        for (int i = SLOT_RECEIVE_OUTPUT_START; i < SLOT_COUNT; i++) {
            if (items.get(i).isEmpty()) {
                input.shrink(1);
                ItemStack paper = new ItemStack(ModItems.TELEGRAM_PAPER.get());
                TelegraphData.setMessage(paper, sentMessage);
                items.set(i, paper);
                setChanged();
                return;
            }
        }
    }

    private void playSound(boolean dot) {
        if (level != null) {
            level.playSound(null, worldPosition, dot ? SoundEvents.NOTE_BLOCK_HAT.value() : SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.8F, dot ? 1.8F : 0.8F);
        }
    }

    private static String frequencyOf(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return "";
        }
        String a = BuiltInRegistries.ITEM.getKey(first.getItem()).toString();
        String b = BuiltInRegistries.ITEM.getKey(second.getItem()).toString();
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("screen.letter_signal_phone.telegraph_machine");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        for (int i = 0; i < Math.min(this.items.size(), items.size()); i++) {
            this.items.set(i, items.get(i));
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new TelegraphMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        frequency = input.getStringOr("frequency", "");
        message = input.getStringOr("message", "");
        composing = input.getBooleanOr("composing", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putString("frequency", frequency);
        output.putString("message", message);
        output.putBoolean("composing", composing);
    }
}
