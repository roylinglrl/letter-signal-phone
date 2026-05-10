package net.royling.lsp.mail;



import net.royling.lsp.LetterSignalPhone;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.royling.lsp.mail.network.MailPayloads;
import net.royling.lsp.registry.ModBlocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MailSavedData extends SavedData {
    private static final String SEP = "\\|";

    public static final Codec<MailSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("mailboxes").forGetter(data -> data.mailboxes),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).fieldOf("inbox").forGetter(data -> data.inbox),
            Codec.STRING.listOf().fieldOf("pending").forGetter(data -> data.pending)
    ).apply(instance, MailSavedData::new));

    public static final SavedDataType<MailSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LetterSignalPhone.MODID, "mail"),
            MailSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<String, String> mailboxes = new HashMap<>();
    private final Map<String, List<String>> inbox = new HashMap<>();
    private final List<String> pending = new ArrayList<>();

    public MailSavedData() {
    }

    private MailSavedData(Map<String, String> mailboxes, Map<String, List<String>> inbox, List<String> pending) {
        this.mailboxes.putAll(mailboxes);
        this.inbox.putAll(inbox);
        this.pending.addAll(pending);
    }

    public static MailSavedData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean registerMailbox(UUID owner, String ownerName, BlockPos pos) {
        String ownerId = owner.toString();
        if (mailboxes.containsKey(ownerId)) {
            return false;
        }
        mailboxes.put(ownerId, ownerName.toLowerCase(Locale.ROOT) + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ());
        setDirty();
        return true;
    }

    public void unregisterMailbox(UUID owner, BlockPos pos) {
        MailboxInfo info = mailbox(owner);
        if (info != null && info.pos().equals(pos)) {
            mailboxes.remove(owner.toString());
            setDirty();
        }
    }

    public void unregisterMailboxAt(BlockPos pos) {
        boolean changed = mailboxes.entrySet().removeIf(entry -> {
            MailboxInfo info = parseMailbox(entry.getValue());
            return info != null && info.pos().equals(pos);
        });
        if (changed) {
            setDirty();
        }
    }

    public void unregisterMailboxAt(MinecraftServer server, BlockPos pos) {
        List<UUID> removedOwners = new ArrayList<>();
        boolean changed = mailboxes.entrySet().removeIf(entry -> {
            MailboxInfo info = parseMailbox(entry.getValue());
            if (info == null || !info.pos().equals(pos)) {
                return false;
            }
            try {
                removedOwners.add(UUID.fromString(entry.getKey()));
            } catch (IllegalArgumentException ignored) {
            }
            return true;
        });
        if (changed) {
            removedOwners.forEach(owner -> sendMailboxState(server, owner, null, false));
            setDirty();
        }
    }

    public Optional<UUID> findOwnerByName(String name) {
        String needle = name.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : mailboxes.entrySet()) {
            MailboxInfo info = parseMailbox(entry.getValue());
            if (info != null && info.ownerName().equals(needle)) {
                try {
                    return Optional.of(UUID.fromString(entry.getKey()));
                } catch (IllegalArgumentException ignored) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    public Optional<BlockPos> findMailboxByName(MinecraftServer server, String name) {
        Optional<UUID> owner = findOwnerByName(name);
        if (owner.isEmpty()) {
            return Optional.empty();
        }
        refreshMailboxBinding(server, owner.get());
        return Optional.ofNullable(mailbox(owner.get())).map(MailboxInfo::pos);
    }

    public boolean hasMailbox(UUID owner) {
        return mailboxes.containsKey(owner.toString());
    }

    public boolean refreshMailboxBinding(MinecraftServer server, UUID owner) {
        MailboxInfo info = mailbox(owner);
        if (info == null) {
            sendMailboxState(server, owner, null, false);
            return false;
        }

        ServerLevel level = server.overworld();
        BlockState state = level.getBlockState(info.pos());
        if (!state.is(ModBlocks.MAILBOX.get()) || !owner.equals(ownerAt(info.pos()).orElse(null))) {
            mailboxes.remove(owner.toString());
            sendMailboxState(server, owner, null, false);
            setDirty();
            return true;
        }

        boolean hasMail = inboxCount(owner) > 0;
        if (state.getValue(MailboxBlock.HAS_MAIL) != hasMail) {
            level.setBlock(info.pos(), state.setValue(MailboxBlock.HAS_MAIL, hasMail), 3);
        }
        sendMailboxState(server, owner, info.pos(), hasMail);
        return false;
    }

    public boolean isMailboxOwner(UUID owner, BlockPos pos) {
        MailboxInfo info = mailbox(owner);
        return info != null && info.pos().equals(pos);
    }

    public int inboxCount(UUID owner) {
        return inbox.getOrDefault(owner.toString(), List.of()).size();
    }

    public List<String> inboxSnapshot(UUID owner) {
        return new ArrayList<>(inbox.getOrDefault(owner.toString(), List.of()));
    }

    public void replaceInbox(UUID owner, List<String> mails) {
        String ownerId = owner.toString();
        if (mails.isEmpty()) {
            inbox.remove(ownerId);
        } else {
            inbox.put(ownerId, new ArrayList<>(mails));
        }
        setDirty();
    }

    public void send(UUID recipient, String mail, long dueTick) {
        pending.add(recipient + "|" + dueTick + "|" + mail);
        setDirty();
    }

    public List<String> collect(UUID owner) {
        return collect(owner, Integer.MAX_VALUE);
    }

    public List<String> collect(UUID owner, int limit) {
        String ownerId = owner.toString();
        List<String> stored = inbox.getOrDefault(ownerId, List.of());
        int count = Math.min(stored.size(), limit);
        List<String> delivered = new ArrayList<>(stored.subList(0, count));
        if (count >= stored.size()) {
            inbox.remove(ownerId);
        } else {
            inbox.put(ownerId, new ArrayList<>(stored.subList(count, stored.size())));
        }
        if (count > 0) {
            setDirty();
        }
        return delivered;
    }

    public void tick(MinecraftServer server) {
        // 鍒扮偣鐨勫緟鎶曢€掗偖浠惰浆鍏ユ敹浠剁锛屽苟鍚屾淇＄浜捣鐘舵€併€?
        long now = server.getTickCount();
        boolean changed = pending.removeIf(encoded -> {
            PendingMail mail = parsePending(encoded);
            if (mail == null || mail.dueTick() > now) {
                return false;
            }
            inbox.computeIfAbsent(mail.ownerId(), key -> new ArrayList<>()).add(mail.mail());
            setMailboxHasMail(server, mail.ownerId(), true);
            return true;
        });
        if (changed) {
            setDirty();
        }
    }

    public void refreshMailboxState(MinecraftServer server, UUID owner) {
        setMailboxHasMail(server, owner.toString(), inboxCount(owner) > 0);
    }

    public void syncMailboxState(MinecraftServer server, UUID owner) {
        MailboxInfo info = mailbox(owner);
        if (info == null) {
            sendMailboxState(server, owner, null, false);
        } else {
            sendMailboxState(server, owner, info.pos(), inboxCount(owner) > 0);
        }
    }

    private void setMailboxHasMail(MinecraftServer server, String ownerId, boolean hasMail) {
        MailboxInfo info = parseMailbox(mailboxes.get(ownerId));
        if (info == null) {
            return;
        }
        ServerLevel level = server.overworld();
        BlockState state = level.getBlockState(info.pos());
        if (state.is(ModBlocks.MAILBOX.get()) && state.getValue(MailboxBlock.HAS_MAIL) != hasMail) {
            level.setBlock(info.pos(), state.setValue(MailboxBlock.HAS_MAIL, hasMail), 3);
        }
        try {
            sendMailboxState(server, UUID.fromString(ownerId), info.pos(), hasMail);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void sendMailboxState(MinecraftServer server, UUID owner, BlockPos pos, boolean hasMail) {
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player == null) {
            return;
        }
        if (pos == null) {
            PacketDistributor.sendToPlayer(player, new MailPayloads.MailboxNoticePayload(0, 0, 0, false, false));
        } else {
            PacketDistributor.sendToPlayer(player, new MailPayloads.MailboxNoticePayload(pos.getX(), pos.getY(), pos.getZ(), true, hasMail));
        }
    }

    private MailboxInfo mailbox(UUID owner) {
        return parseMailbox(mailboxes.get(owner.toString()));
    }

    private Optional<UUID> ownerAt(BlockPos pos) {
        for (Map.Entry<String, String> entry : mailboxes.entrySet()) {
            MailboxInfo info = parseMailbox(entry.getValue());
            if (info != null && info.pos().equals(pos)) {
                try {
                    return Optional.of(UUID.fromString(entry.getKey()));
                } catch (IllegalArgumentException ignored) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private static MailboxInfo parseMailbox(String encoded) {
        if (encoded == null) {
            return null;
        }
        String[] parts = encoded.split(SEP, -1);
        if (parts.length != 4) {
            return null;
        }
        try {
            return new MailboxInfo(parts[0], new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static PendingMail parsePending(String encoded) {
        // 閭欢姝ｆ枃鍙兘鍖呭惈鍒嗛殧绗︼紝鎵€浠ヨ繖閲屽彧鍒囨垚涓夋銆?
        String[] parts = encoded.split(SEP, 3);
        if (parts.length != 3) {
            return null;
        }
        try {
            return new PendingMail(parts[0], Long.parseLong(parts[1]), parts[2]);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record MailboxInfo(String ownerName, BlockPos pos) {
    }

    private record PendingMail(String ownerId, long dueTick, String mail) {
    }
}
