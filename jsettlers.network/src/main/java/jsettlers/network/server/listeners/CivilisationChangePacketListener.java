package jsettlers.network.server.listeners;

import java.io.IOException;

import jsettlers.network.NetworkConstants;
import jsettlers.network.common.packets.IntegerMessagePacket;
import jsettlers.network.infrastructure.channel.GenericDeserializer;
import jsettlers.network.infrastructure.channel.listeners.PacketChannelListener;
import jsettlers.network.server.IServerManager;
import jsettlers.network.server.match.Player;

public class CivilisationChangePacketListener extends PacketChannelListener<IntegerMessagePacket> {

	private final IServerManager serverManager;
	private final Player player;

	public CivilisationChangePacketListener(IServerManager serverManager, Player player) {
		super(NetworkConstants.ENetworkKey.CHANGE_CIVILISATION, new GenericDeserializer<>(IntegerMessagePacket.class));
		this.serverManager = serverManager;
		this.player = player;
	}
	@Override
	protected void receivePacket(NetworkConstants.ENetworkKey key, IntegerMessagePacket packet) throws IOException {
		serverManager.setCivilisationForPlayer(player, packet.getValue());
	}
}
