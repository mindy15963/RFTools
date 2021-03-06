package mcjty.rftools.blocks.storage;

import mcjty.lib.container.DefaultSidedInventory;
import mcjty.lib.container.InventoryHelper;
import mcjty.lib.tileentity.LogicTileEntity;
import mcjty.lib.bindings.DefaultValue;
import mcjty.lib.bindings.IValue;
import mcjty.lib.typed.Key;
import mcjty.lib.typed.Type;
import mcjty.lib.varia.WorldTools;
import mcjty.rftools.blocks.screens.ScreenSetup;
import mcjty.rftools.blocks.storagemonitor.StorageScannerTileEntity;
import mcjty.rftools.varia.RFToolsTools;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class LevelEmitterTileEntity extends LogicTileEntity implements DefaultSidedInventory, ITickable {

    public static final Key<Integer> VALUE_AMOUNT = new Key<>("amount", Type.INTEGER);
    public static final Key<Boolean> VALUE_OREDICT = new Key<>("oredict", Type.BOOLEAN);
    public static final Key<Boolean> VALUE_STARRED = new Key<>("starred", Type.BOOLEAN);

    @Override
    public IValue<?>[] getValues() {
        return new IValue[] {
                new DefaultValue<>(VALUE_AMOUNT, this::getAmount, this::setAmount),
                new DefaultValue<>(VALUE_OREDICT, this::isOreDict, this::setOreDict),
                new DefaultValue<>(VALUE_STARRED, this::isStarred, this::setStarred),
        };
    }

    private InventoryHelper inventoryHelper = new InventoryHelper(this, LevelEmitterContainer.factory, 2);

    private int amount = 1;
    private boolean oreDict = false;
    private boolean starred = false;

    private int checkCounter = 0;

    @Override
    public void update() {
        if (getWorld().isRemote) {
            return;
        }

        checkCounter--;
        if (checkCounter > 0) {
            return;
        }
        checkCounter = 10;

        int count = getCurrentCount();
        setRedstoneState(count >= amount ? 15 : 0);
    }

    public int getCurrentCount() {
        ItemStack module = inventoryHelper.getStackInSlot(LevelEmitterContainer.SLOT_MODULE);
        int count = -1;
        if (!module.isEmpty()) {
            ItemStack matcher = inventoryHelper.getStackInSlot(LevelEmitterContainer.SLOT_ITEMMATCH);
            if (matcher.isEmpty()) {
                return count;
            }
            int dimension = RFToolsTools.getDimensionFromModule(module);
            BlockPos scannerPos = RFToolsTools.getPositionFromModule(module);
            WorldServer world = DimensionManager.getWorld(dimension);

            if (WorldTools.chunkLoaded(world, scannerPos)) {
                TileEntity te = world.getTileEntity(scannerPos);
                if (te instanceof StorageScannerTileEntity) {
                    StorageScannerTileEntity scannerTE = (StorageScannerTileEntity) te;
                    count = scannerTE.countItems(matcher, starred, oreDict);
                }
            }
        }
        return count;
    }


    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        boolean module = !inventoryHelper.getStackInSlot(LevelEmitterContainer.SLOT_MODULE).isEmpty();

        super.onDataPacket(net, packet);

        if (getWorld().isRemote) {
            // If needed send a render update.
            boolean newmodule = !inventoryHelper.getStackInSlot(LevelEmitterContainer.SLOT_MODULE).isEmpty();
            if (newmodule != module) {
                getWorld().markBlockRangeForRenderUpdate(getPos(), getPos());
            }
        }
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        // Clear the oredict cache
        inventoryHelper.setInventorySlotContents(this.getInventoryStackLimit(), index, stack);
        if (!getWorld().isRemote) {
            // Make sure we update client-side
            markDirtyClient();
        } else {
            getWorld().markBlockRangeForRenderUpdate(getPos(), getPos());
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        powerOutput = tagCompound.getBoolean("rs") ? 15 : 0;
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        readBufferFromNBT(tagCompound, inventoryHelper);
        amount = tagCompound.getInteger("amount");
        oreDict = tagCompound.getBoolean("oredict");
        starred = tagCompound.getBoolean("starred");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("rs", powerOutput > 0);
        return tagCompound;
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        writeBufferToNBT(tagCompound, inventoryHelper);
        tagCompound.setInteger("amount", amount);
        tagCompound.setBoolean("oredict", oreDict);
        tagCompound.setBoolean("starred", starred);
    }


    @Override
    protected boolean needsCustomInvWrapper() {
        return true;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == LevelEmitterContainer.SLOT_MODULE && stack.getItem() != ScreenSetup.storageControlModuleItem) {
            return false;
        }
        if (index == LevelEmitterContainer.SLOT_ITEMMATCH) {
            return false;
        }
        return true;
    }

    @Override
    public InventoryHelper getInventoryHelper() {
        return inventoryHelper;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return canPlayerAccess(player);
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
        markDirty();
    }

    public boolean isOreDict() {
        return oreDict;
    }

    public void setOreDict(boolean oreDict) {
        this.oreDict = oreDict;
        markDirty();
    }

    public boolean isStarred() {
        return starred;
    }

    public void setStarred(boolean starred) {
        this.starred = starred;
        markDirty();
    }
}
