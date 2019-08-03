package net.countercraft.movecraft.utils;

import com.mewin.WGCustomFlags.WGCustomFlagsPlugin;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import net.countercraft.movecraft.Movecraft;
import org.bukkit.Location;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author mwkaicz <mwkaicz@gmail.com>
 */
public class WGCustomFlagsUtils {


    public StateFlag getNewStateFlag(String name, boolean def) {
        Constructor<?> cc = getStateFlagConstructor();
        if (cc == null) {
            return null;
        }
        Object o;
        try {
            o = cc.newInstance(name, def);
            return (StateFlag) o;
        } catch (InstantiationException | InvocationTargetException | IllegalArgumentException | IllegalAccessException ex) {
            return null;
        }
    }

    public Constructor<?> getStateFlagConstructor() {
        try {
            Class<?> c = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            return c.getConstructor(String.class, boolean.class);
        } catch (ClassNotFoundException | SecurityException | NoSuchMethodException ex) {
            return null;
        }
    }

    public boolean registerStageFlag(Object o) {
        WGCustomFlagsPlugin wgcf = Movecraft.getInstance().getWGCustomFlagsPlugin();
        if (wgcf != null) {
            Constructor<?> cc = getStateFlagConstructor();
            if (cc == null) {
                return false;
            }
            try {
                wgcf.addCustomFlag((Flag) o);
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }


    public void init() {
        if (Movecraft.FLAG_PILOT != null) {
            this.registerStageFlag(Movecraft.FLAG_PILOT);
        }
        if (Movecraft.FLAG_MOVE != null) {
            this.registerStageFlag(Movecraft.FLAG_MOVE);
        }
        if (Movecraft.FLAG_ROTATE != null) {
            this.registerStageFlag(Movecraft.FLAG_ROTATE);
        }
        if (Movecraft.FLAG_SINK != null) {
            this.registerStageFlag(Movecraft.FLAG_SINK);
        }
    }

    public boolean validateFlag(Location loc, Object flag) {
        if (flag != null) {
            StateFlag.State state = LegacyUtils.getFlag(Movecraft.getInstance().getWorldGuardPlugin(), loc, flag);
            return state != null && state == StateFlag.State.ALLOW;
        } else {
            return true;
        }
    }

    public boolean validateFlag(Location loc, Object flag, LocalPlayer lp) {
        if (flag != null) {
            StateFlag.State state = LegacyUtils.getFlag(Movecraft.getInstance().getWorldGuardPlugin(), loc, flag, lp);
            return state != null && state == StateFlag.State.ALLOW;
        } else {
            return true;
        }
    }


}
