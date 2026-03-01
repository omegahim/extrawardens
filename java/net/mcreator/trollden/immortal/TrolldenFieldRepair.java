package net.mcreator.trollden.immortal;

import net.mcreator.trollden.entity.TrolldenEntity;
import net.minecraft.core.BlockPos;

import java.lang.reflect.Field;
import java.util.UUID;

public class TrolldenFieldRepair {

    // ── Cached reflected fields ───────────────────────────────────────────────
    private static final Field F_X;
    private static final Field F_Y;
    private static final Field F_Z;
    private static final Field F_XO;
    private static final Field F_YO;
    private static final Field F_ZO;
    private static final Field F_XROT;
    private static final Field F_YROT;
    private static final Field F_REMOVAL;
    private static final Field F_UUID;
    private static final Field F_STRING_UUID;
    private static final Field F_CHUNK_POS;
    private static final Field F_ADDED;
    private static final Field F_CAN_UPDATE;
    private static final Field F_BLOCKPOS;
    private static final Field F_DEATH_TIME;
    private static final Field F_HURT_TIME;
    private static final Field F_LAST_HURT;

    // 🔥 NEW — internal LivingEntity "dead" flag
    private static final Field F_DEAD;

    static {
        F_X           = find("f_19854_");
        F_Y           = find("f_19855_");
        F_Z           = find("f_19856_");
        F_XO          = find("f_19790_");
        F_YO          = find("f_19791_");
        F_ZO          = find("f_19792_");
        F_XROT        = find("f_19859_");
        F_YROT        = find("f_19860_");
        F_REMOVAL     = find("f_146795_");
        F_UUID        = find("f_19820_");
        F_STRING_UUID = find("f_19821_");
        F_CHUNK_POS   = find("f_19848_");
        F_ADDED       = find("isAddedToWorld");
        F_CAN_UPDATE  = find("canUpdate");
        F_BLOCKPOS    = find("f_19825_");
        F_DEATH_TIME  = find("f_20919_");
        F_HURT_TIME   = find("f_20916_");
        F_LAST_HURT   = find("f_20920_");

        // 🔥 Added
        F_DEAD        = find("f_20890_");
    }

    private static Field find(String name) {
        Class<?> cls = net.minecraft.world.entity.LivingEntity.class;
        while (cls != null && cls != Object.class) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                cls = cls.getSuperclass();
            }
        }
        return null;
    }

    public static void repairIfNeeded(TrolldenEntity entity) {

        double sx = entity.lastGoodX;
        double sy = entity.lastGoodY;
        double sz = entity.lastGoodZ;

        if (Double.isNaN(sx)) sx = 0.0;
        if (Double.isNaN(sy)) sy = 64.0;
        if (Double.isNaN(sz)) sz = 0.0;

        repairDouble(entity, F_X,  sx);
        repairDouble(entity, F_Y,  sy);
        repairDouble(entity, F_Z,  sz);
        repairDouble(entity, F_XO, sx);
        repairDouble(entity, F_YO, sy);
        repairDouble(entity, F_ZO, sz);

        repairFloat(entity, F_XROT, 0.0f);
        repairFloat(entity, F_YROT, 0.0f);

        if (F_BLOCKPOS != null) {
            try {
                BlockPos pos = (BlockPos) F_BLOCKPOS.get(entity);
                boolean corrupt = pos == null
                        || Math.abs(pos.getX()) > 30_000_000
                        || pos.getY() < -2048 || pos.getY() > 4096
                        || Math.abs(pos.getZ()) > 30_000_000;
                if (corrupt) {
                    F_BLOCKPOS.set(entity, new BlockPos(
                            (int) Math.floor(sx),
                            (int) Math.floor(sy),
                            (int) Math.floor(sz)));
                }
            } catch (Exception ignored) {}
        }

        if (F_REMOVAL != null) {
            try {
                if (F_REMOVAL.get(entity) != null) {
                    F_REMOVAL.set(entity, null);
                }
            } catch (Exception ignored) {}
        }

        if (F_UUID != null) {
            try {
                if (F_UUID.get(entity) == null) {
                    F_UUID.set(entity, entity.storedUUID);
                }
            } catch (Exception ignored) {}
        }

        if (F_STRING_UUID != null) {
            try {
                Object current = F_STRING_UUID.get(entity);
                if ("SB".equals(current) || current == null) {
                    F_STRING_UUID.set(entity, entity.storedStringUUID);
                }
            } catch (Exception ignored) {}
        }

        if (F_CHUNK_POS != null) {
            try {
                if (F_CHUNK_POS.getInt(entity) == Integer.MIN_VALUE) {
                    F_CHUNK_POS.setInt(entity, 0);
                }
            } catch (Exception ignored) {}
        }

        resetForgeFlag(entity, F_ADDED);
        resetForgeFlag(entity, F_CAN_UPDATE);

        try {
            if (!entity.canUpdate()) entity.canUpdate(true);
        } catch (Exception ignored) {}

        if (entity.immuneCallback != null) {
            entity.setLevelCallback(entity.immuneCallback);
        }

        resetIntField(entity, F_DEATH_TIME, 0);
        resetIntField(entity, F_HURT_TIME,  0);
        resetFloatField(entity, F_LAST_HURT, 0.0f);

        // CRITICAL FIX — clear internal "dead" flag
        if (F_DEAD != null) {
            try {
                if (F_DEAD.getBoolean(entity)) {
                    F_DEAD.setBoolean(entity, false);
                }
            } catch (Exception ignored) {}
        }
    }

    public static void recordGoodPosition(TrolldenEntity entity) {
        double x = readDouble(entity, F_X);
        double y = readDouble(entity, F_Y);
        double z = readDouble(entity, F_Z);
        if (!Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(z)) {
            entity.lastGoodX = x;
            entity.lastGoodY = y;
            entity.lastGoodZ = z;
        }
    }

    private static double readDouble(Object obj, Field f) {
        if (f == null) return Double.NaN;
        try { return f.getDouble(obj); } catch (Exception e) { return Double.NaN; }
    }

    private static void repairDouble(Object obj, Field f, double safeVal) {
        if (f == null) return;
        try {
            if (Double.isNaN(f.getDouble(obj))) f.setDouble(obj, safeVal);
        } catch (Exception ignored) {}
    }

    private static void repairFloat(Object obj, Field f, float safeVal) {
        if (f == null) return;
        try {
            if (Float.isNaN(f.getFloat(obj))) f.setFloat(obj, safeVal);
        } catch (Exception ignored) {}
    }

    private static void resetForgeFlag(Object obj, Field f) {
        if (f == null) return;
        try {
            if (!f.getBoolean(obj)) f.setBoolean(obj, true);
        } catch (Exception ignored) {}
    }

    private static void resetIntField(Object obj, Field f, int target) {
        if (f == null) return;
        try {
            if (f.getInt(obj) != target) f.setInt(obj, target);
        } catch (Exception ignored) {}
    }

    private static void resetFloatField(Object obj, Field f, float target) {
        if (f == null) return;
        try {
            if (f.getFloat(obj) != target) f.setFloat(obj, target);
        } catch (Exception ignored) {}
    }
}