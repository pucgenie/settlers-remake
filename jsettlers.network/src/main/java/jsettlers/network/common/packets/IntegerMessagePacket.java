package jsettlers.network.common.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import jsettlers.network.infrastructure.channel.packet.Packet;

public class IntegerMessagePacket extends Packet {

	private int value;

	public IntegerMessagePacket() {
	}

	public IntegerMessagePacket(int value) {
		this.value = value;
	}

	@Override
	public void serialize(DataOutputStream dos) throws IOException {
		dos.writeInt(value);
	}

	@Override
	public void deserialize(DataInputStream dis) throws IOException {
		value = dis.readInt();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + value;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntegerMessagePacket other = (IntegerMessagePacket) obj;
		return value == other.value;
	}

	public int getValue() {
		return value;
	}

}
