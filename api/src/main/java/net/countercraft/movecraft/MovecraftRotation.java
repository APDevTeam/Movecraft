/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft;

import org.bukkit.event.block.Action;

public enum MovecraftRotation {
    CLOCKWISE, NONE, ANTICLOCKWISE;
	
	public static MovecraftRotation fromAction(Action clickType) {
        switch (clickType) {
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
                return ANTICLOCKWISE;
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                return CLOCKWISE;
            default:
                return NONE;
        }
    }
}
