package cn.blockforge.generated.generatedmod.alchemy.batch;

import java.util.Objects;
import java.util.UUID;

/** Result of one ordinary-furnace admission, not a retry token or a second batch authority. */
public record BatchStartResult(Reason reason, CommitStage stage, UUID batchId, String detail) {
    /** Knowledge at return time. Journal confirmation does not imply chunk/player atomicity. */
    public enum CommitStage { NOT_ATTEMPTED, UNCONFIRMED, JOURNAL_CONFIRMED, WORLD_APPLIED, COMPLETED }
    public enum Reason {
        STARTED, SERVER_REQUIRED, FURNACE_OCCUPIED, INVALID_PLAYER, DEVICE_WORN,
        STRUCTURE_INCOMPLETE, FURNACE_REALM_REQUIRED, FIRE_REQUIRED, FIRE_REALM_REQUIRED,
        KNOWLEDGE_REQUIRED, LEGACY_JOB_PENDING, PLAYER_BUSY, RECIPE_TIER_REQUIRED,
        RECIPE_REALM_REQUIRED, RECIPE_FIRE_REQUIRED, RECIPE_KNOWLEDGE_REQUIRED,
        MATERIALS_REQUIRED, PREPARATION_FAILED, RECOVERY_REQUIRED
    }
    public BatchStartResult {
        Objects.requireNonNull(reason); Objects.requireNonNull(stage); Objects.requireNonNull(detail);
        if ((stage == CommitStage.NOT_ATTEMPTED) != (batchId == null))
            throw new IllegalArgumentException("An attempted commit requires its original batch identity");
        if ((reason == Reason.STARTED) != (stage == CommitStage.COMPLETED))
            throw new IllegalArgumentException("Only a completed admission is started");
        if ((reason == Reason.RECOVERY_REQUIRED) !=
                (stage != CommitStage.NOT_ATTEMPTED && stage != CommitStage.COMPLETED))
            throw new IllegalArgumentException("Incomplete publication requires recovery");
    }
    public static BatchStartResult rejected(Reason reason) {
        return new BatchStartResult(reason, CommitStage.NOT_ATTEMPTED, null, "");
    }
    public boolean started() { return reason == Reason.STARTED; }
    public boolean needsRecovery() { return reason == Reason.RECOVERY_REQUIRED; }
}
