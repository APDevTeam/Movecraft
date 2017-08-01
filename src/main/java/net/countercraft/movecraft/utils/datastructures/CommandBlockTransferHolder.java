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

package net.countercraft.movecraft.utils.datastructures;

public class CommandBlockTransferHolder extends TransferData {
    private final String commandText;
    private final String commandName;

    public CommandBlockTransferHolder(byte data, String commandText, String commandName) {
        super(data);
        this.commandText = commandText;
        this.commandName = commandName;
    }

    public String getText() {
        return this.commandText;
    }

    public String getName() {
        return this.commandName;
    }
}
