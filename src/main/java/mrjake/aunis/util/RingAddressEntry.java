/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mrjake.aunis.util;
import mrjake.aunis.tileentity.TransportRingsTile;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
/**
 *
 * @author draks
 */
public class RingAddressEntry {
    
    private String address;
    private int x;
    private int y;
    private int z;
    private int dimension;
    private int frequency;
    
    @Override
    public String toString() {
        String s = ("Address: " + address + ", Pos: " + x + "," + y + "," + z + " on dimension: " + dimension + " with freq : " + frequency + " and ringExists: " + (getRings() != null));
        return s;
    }
    
    public RingAddressEntry(TransportRingsTile te, String address) {
        this.dimension = te.getWorld().provider.getDimension();
        this.address = address;
        this.x = te.getPos().getX();
        this.y = te.getPos().getY();
        this.z = te.getPos().getZ();
        this.frequency = RingMap.getAvailableFrequency(this.address);
    }
    
    public RingAddressEntry(String address, int dimension, int frequency, int posX, int posY, int posZ) {
        this.address = address;
        this.dimension = dimension;
        this.frequency = frequency;
        this.x = posX;
        this.y = posY;
        this.z = posZ;
    }
    
    public static RingAddressEntry fromNBT(NBTTagCompound tag) {
        try {
            return new RingAddressEntry(
                    tag.getString("address"), 
                    tag.getInteger("dimension"),
                    tag.getInteger("frequency"),
                    tag.getInteger("x"),
                    tag.getInteger("y"),
                    tag.getInteger("z")
            );
        } catch (Exception e ) {
            return null;
        }
    }
    
    public TransportRingsTile getRings() {
        return null;
    }
    
    public String getAddress() {
        return this.address;
    }
    public int getDimension() {
        return this.dimension;
    }
    
    public BlockPos getBlockPos() {
        return new BlockPos(this.x,this.y,this.z);
    }
    public int getX() {
        return this.x;
    }
    public int getY() {
        return this.y;
    }
    public int getZ() {
        return this.z;
    }
    public void setFrequency(int val) {
        this.frequency = val;
    }
    public int getFrequency() {
        return this.frequency;
    }
    
    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("address", this.address);
        tag.setInteger("dimension", this.dimension);
        tag.setInteger("x", this.x);
        tag.setInteger("y", this.y);
        tag.setInteger("z", this.z);
        tag.setInteger("frequency", this.frequency);
        return tag;
    }
    
    public boolean equals(RingAddressEntry entry) {
        if (!entry.getAddress().equals(this.getAddress())) return false;
        if (entry.getDimension() != this.getDimension()) return false;
        if (entry.getFrequency() != this.getFrequency()) return false;
        if (entry.getX() != this.getX()) return false;
        if (entry.getY() != this.getY()) return false;
        if (entry.getZ() != this.getZ()) return false;
        return true;
    }
}
