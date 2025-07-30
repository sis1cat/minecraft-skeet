package sisicat.events;

import com.darkmagician6.eventapi.events.callables.EventCancellable;

import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class PacketReceiveEvent<T extends PacketListener> extends EventCancellable {
	
	private Packet<T> packet;
	
	public PacketReceiveEvent(Packet<T> packet) {
	    this.packet = packet;
	}
	  
	public Packet<T> getPacket() {
	    return this.packet;
	}
	  
	public void setPacket(Packet<T> packet) {
	    this.packet = packet;
	}
	
}
