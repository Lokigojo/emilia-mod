package com.example.emilia;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EmiliaMod.MODID);

    /** Has the player taken Puck's contract (Emilia's powers)? Survives death. */
    public static final Supplier<AttachmentType<Boolean>> AWAKENED = ATTACHMENTS.register("awakened",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    /** Selected spirit art index. */
    public static final Supplier<AttachmentType<Integer>> SPELL = ATTACHMENTS.register("spell",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());

    public static final Supplier<AttachmentType<Float>> MANA = ATTACHMENTS.register("mana",
            () -> AttachmentType.builder(() -> 100.0F).serialize(Codec.FLOAT).copyOnDeath().build());

    /** Winter's Presence toggle. Resets on death. */
    public static final Supplier<AttachmentType<Boolean>> AURA = ATTACHMENTS.register("aura",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    /** Remaining Ice Blade ticks. Transient. */
    public static final Supplier<AttachmentType<Integer>> BLADE = ATTACHMENTS.register("blade",
            () -> AttachmentType.builder(() -> 0).build());

    private ModAttachments() {}
}
