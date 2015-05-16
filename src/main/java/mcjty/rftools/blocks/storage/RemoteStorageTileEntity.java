package mcjty.rftools.blocks.storage;

import mcjty.container.InventoryHelper;
import mcjty.entity.GenericEnergyReceiverTileEntity;
import mcjty.rftools.items.storage.StorageModuleItem;
import mcjty.varia.Coordinate;
import mcjty.varia.GlobalCoordinate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

public class RemoteStorageTileEntity extends GenericEnergyReceiverTileEntity implements ISidedInventory {

    private InventoryHelper inventoryHelper = new InventoryHelper(this, RemoteStorageContainer.factory, 8);

    private ItemStack[][] slots = new ItemStack[][] {
            new ItemStack[ModularStorageContainer.MAXSIZE_STORAGE],
            new ItemStack[ModularStorageContainer.MAXSIZE_STORAGE],
            new ItemStack[ModularStorageContainer.MAXSIZE_STORAGE],
            new ItemStack[ModularStorageContainer.MAXSIZE_STORAGE]
    };
    private int[] maxsize = { 0, 0, 0, 0 };


    public RemoteStorageTileEntity() {
        super(ModularStorageConfiguration.REMOTE_MAXENERGY, ModularStorageConfiguration.REMOTE_RECEIVEPERTICK);
    }

    private int timer = 0;

    @Override
    protected void checkStateServer() {
        timer--;
        if (timer > 0) {
            return;
        }
        timer = 5;

        RemoteStorageIdRegistry registry = RemoteStorageIdRegistry.getRegistry(worldObj);
        for (int i = 0 ; i < 4 ; i++) {
            if (inventoryHelper.containsItem(i)) {
                ItemStack stack = inventoryHelper.getStacks()[i];
                NBTTagCompound tagCompound = stack.getTagCompound();
                if (tagCompound != null && tagCompound.hasKey("id")) {
                    int id = tagCompound.getInteger("id");
                    registry.publishStorage(id, new GlobalCoordinate(new Coordinate(xCoord, yCoord, zCoord), worldObj.provider.dimensionId));
                }
            }
        }
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return RemoteStorageContainer.factory.getAccessibleSlots();
    }

    @Override
    public boolean canInsertItem(int index, ItemStack item, int side) {
        return RemoteStorageContainer.factory.isInputSlot(index);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack item, int side) {
        return RemoteStorageContainer.factory.isOutputSlot(index);
    }

    public boolean hasStorage(int index) {
        return inventoryHelper.containsItem(index);
    }

    @Override
    public int getSizeInventory() {
        return inventoryHelper.getStacks().length;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return inventoryHelper.getStacks()[index];
    }

    @Override
    public ItemStack decrStackSize(int index, int amount) {
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        return inventoryHelper.decrStackSize(index, amount);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        return null;
    }

    private void link(int index) {
        if (index >= RemoteStorageContainer.SLOT_LINKER) {
            index -= RemoteStorageContainer.SLOT_LINKER;
        }
        if (!inventoryHelper.containsItem(index)) {
            return;
        }
        if (!inventoryHelper.containsItem(index+4)) {
            return;
        }
        ItemStack source = inventoryHelper.getStacks()[index];
        ItemStack dest = inventoryHelper.getStacks()[index+4];
        if (dest.getItemDamage() != StorageModuleItem.STORAGE_REMOTE) {
            return;
        }

        NBTTagCompound tagCompound = source.getTagCompound();
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
            source.setTagCompound(tagCompound);
        }
        int id;
        if (tagCompound.hasKey("id")) {
            id = tagCompound.getInteger("id");
        } else {
            RemoteStorageIdRegistry registry = RemoteStorageIdRegistry.getRegistry(worldObj);
            id = registry.getNewId();
            registry.save(worldObj);
            tagCompound.setInteger("id", id);
        }

        tagCompound = dest.getTagCompound();
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
            dest.setTagCompound(tagCompound);
        }
        tagCompound.setInteger("id", id);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index < RemoteStorageContainer.SLOT_LINKER) {
            copyFromModule(stack, index);
        }
        inventoryHelper.setInventorySlotContents(getInventoryStackLimit(), index, stack);
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        if (!worldObj.isRemote) {
            link(index);
        }
    }

    @Override
    public String getInventoryName() {
        return "Remote Storage Inventory";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {

    }

    @Override
    public void closeInventory() {

    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    public ItemStack findStorageWithId(int id) {
        for (int i = 0 ; i < 4 ; i++) {
            if (inventoryHelper.containsItem(i)) {
                ItemStack stack = inventoryHelper.getStacks()[i];
                if (stack.getItemDamage() != StorageModuleItem.STORAGE_REMOTE) {
                    NBTTagCompound tagCompound = stack.getTagCompound();
                    if (tagCompound != null && tagCompound.hasKey("id")) {
                        if (id == tagCompound.getInteger("id")) {
                            return stack;
                        }
                    }
                }
            }
        }
        return null;
    }

    public ItemStack[] findStacksForId(int id) {
        for (int i = 0 ; i < 4 ; i++) {
            if (inventoryHelper.containsItem(i)) {
                ItemStack stack = inventoryHelper.getStacks()[i];
                if (stack.getItemDamage() != StorageModuleItem.STORAGE_REMOTE) {
                    NBTTagCompound tagCompound = stack.getTagCompound();
                    if (tagCompound != null && tagCompound.hasKey("id")) {
                        if (id == tagCompound.getInteger("id")) {
                            return slots[i];
                        }
                    }
                }
            }
        }
        return null;
    }


    public void copyToModule(int index) {
        ItemStack stack = inventoryHelper.getStacks()[index];
        if (stack == null || stack.stackSize == 0) {
            // Should be impossible.
            return;
        }
        if (stack.getItemDamage() == StorageModuleItem.STORAGE_REMOTE) {
            return;
        }
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
            stack.setTagCompound(tagCompound);
        }
        writeSlotsToNBT(tagCompound, index);

        for (int i = 0 ; i < ModularStorageContainer.MAXSIZE_STORAGE ; i++) {
            slots[index][i] = null;
        }
    }

    public void copyFromModule(ItemStack stack, int index) {
        for (int i = 0 ; i < ModularStorageContainer.MAXSIZE_STORAGE ; i++) {
            slots[index][i] = null;
        }

        if (stack == null || stack.stackSize == 0) {
            setMaxSize(index, 0);
            return;
        }
        if (stack.getItemDamage() == StorageModuleItem.STORAGE_REMOTE) {
            setMaxSize(index, 0);
            return;
        }

        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound != null) {
            readSlotsFromNBT(tagCompound, index);
        }

        setMaxSize(index, StorageModuleItem.MAXSIZE[stack.getItemDamage()]);
    }

    private void setMaxSize(int index, int ms) {
        maxsize[index] = ms;
    }


    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        readBufferFromNBT(tagCompound);
        for (int i = 0 ; i < 4 ; i++) {
            readSlotsFromNBT(tagCompound, i);
            maxsize[i] = tagCompound.getInteger("maxSize" + i);
        }
    }

    private void readBufferFromNBT(NBTTagCompound tagCompound) {
        NBTTagList bufferTagList = tagCompound.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0 ; i < bufferTagList.tagCount() ; i++) {
            NBTTagCompound nbtTagCompound = bufferTagList.getCompoundTagAt(i);
            inventoryHelper.getStacks()[i] = ItemStack.loadItemStackFromNBT(nbtTagCompound);
        }
    }

    private void readSlotsFromNBT(NBTTagCompound tagCompound, int index) {
        NBTTagList bufferTagList = tagCompound.getTagList("Slots" + index, Constants.NBT.TAG_COMPOUND);
        for (int i = 0 ; i < bufferTagList.tagCount() ; i++) {
            NBTTagCompound nbtTagCompound = bufferTagList.getCompoundTagAt(i);
            slots[index][i] = ItemStack.loadItemStackFromNBT(nbtTagCompound);
        }
    }


    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        writeBufferToNBT(tagCompound);
        for (int i = 0 ; i < 4 ; i++) {
            writeSlotsToNBT(tagCompound, i);
            tagCompound.setInteger("maxSize" + i, maxsize[i]);
        }
    }

    private void writeBufferToNBT(NBTTagCompound tagCompound) {
        NBTTagList bufferTagList = new NBTTagList();
        for (int i = 0 ; i < inventoryHelper.getCount() ; i++) {
            ItemStack stack = inventoryHelper.getStacks()[i];
            NBTTagCompound nbtTagCompound = new NBTTagCompound();
            if (stack != null) {
                stack.writeToNBT(nbtTagCompound);
            }
            bufferTagList.appendTag(nbtTagCompound);
        }
        tagCompound.setTag("Items", bufferTagList);
    }

    private void writeSlotsToNBT(NBTTagCompound tagCompound, int index) {
        NBTTagList bufferTagList = new NBTTagList();
        for (int i = 0 ; i < slots[index].length ; i++) {
            ItemStack stack = slots[index][i];
            NBTTagCompound nbtTagCompound = new NBTTagCompound();
            if (stack != null) {
                stack.writeToNBT(nbtTagCompound);
            }
            bufferTagList.appendTag(nbtTagCompound);
        }
        tagCompound.setTag("Slots" + index, bufferTagList);
    }

}
