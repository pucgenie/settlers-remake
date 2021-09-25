/*******************************************************************************
 * Copyright (c) 2020
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.main.swing.menu.multiplayer;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import java8.util.stream.Collectors;
import jsettlers.common.menu.EProgressState;
import jsettlers.common.menu.IJoinPhaseMultiplayerGameConnector;
import jsettlers.common.menu.IJoiningGame;
import jsettlers.common.menu.IJoiningGameListener;
import jsettlers.common.menu.IMultiplayerConnector;
import jsettlers.graphics.localization.Labels;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.map.loading.list.MapList;
import jsettlers.main.swing.JSettlersFrame;
import jsettlers.main.swing.menu.joinpanel.JoinGamePanel;
import jsettlers.main.swing.menu.mainmenu.NetworkGameMapLoader;
import jsettlers.main.swing.menu.openpanel.OpenPanel;
import jsettlers.main.swing.settings.ServerEntry;
import jsettlers.network.client.IClientConnection;

import static java8.util.stream.StreamSupport.stream;

public class ServerConnectionPanel extends JPanel {

	private ServerEntry entry;
	private JTabbedPane root = new JTabbedPane();
	private RemoteMapDirectoryPanel maps = null;
	private OpenPanel newMatch = null;
	private OpenPanel joinMatch = null;
	private JSettlersFrame settlersFrame;

	private final OpenPanel openSinglePlayerPanel;

	public ServerConnectionPanel(ServerEntry entry, Runnable leave, JSettlersFrame settlersFrame, OpenPanel openSinglePlayerPanel) {
		this.openSinglePlayerPanel = openSinglePlayerPanel;
		this.settlersFrame = settlersFrame;
		this.entry = entry;

		JTextArea logText = new JTextArea();
		logText.setEditable(false);
		entry.setConnectionLogListener(logText::setText);
		root.addTab(Labels.getString("multiplayer-log-title"), new JScrollPane(logText));
		root.addTab(Labels.getString("multiplayer-log-settings"), new EditServerEntryPanel(leave, () -> root.setSelectedIndex(0), entry));

		setLayout(new BorderLayout());
		add(root, BorderLayout.CENTER);
	}

	public void update() {
		IClientConnection connection = entry.getConnection();
		boolean conMaps = connection.getMaps("/") != null;
		int i = 1;
		if(!conMaps && maps != null) {
			root.removeTabAt(i);
			maps = null;
		} else if(conMaps && maps == null) {
			root.insertTab(Labels.getString("multiplayer-mapslist-title"), null, maps = new RemoteMapDirectoryPanel(connection, openSinglePlayerPanel),  null, i);
		}
		if(maps != null) {
			maps.update();
			i++;
		}

		boolean conMatch = connection.isConnected() && connection instanceof IMultiplayerConnector;
		if(!conMatch && newMatch != null) {
			root.removeTabAt(i);
			root.removeTabAt(i);
			newMatch = null;
			joinMatch = null;
		} else if(conMatch && newMatch == null){
			newMatch = new OpenPanel(MapList.getDefaultList().getFreshMaps().getItems(), this::openNewMatch);
			SwingUtilities.updateComponentTreeUI(newMatch);
			root.insertTab(Labels.getString("multiplayer-newmatch-title"), null, newMatch, null, i);

			joinMatch = new OpenPanel(Collections.emptyList(), this::joinMatch);
			SwingUtilities.updateComponentTreeUI(joinMatch);

			((IMultiplayerConnector)connection).getJoinableMultiplayerGames()
					.setListener(networkGames -> {
						List<MapLoader> mapLoaders = stream(networkGames.getItems())
								.map(NetworkGameMapLoader::new)
								.collect(Collectors.toList());
						SwingUtilities.invokeLater(() -> joinMatch.setMapLoaders(mapLoaders));
					});

			root.insertTab(Labels.getString("multiplayer-joinmatch-title"), null, joinMatch, null, i+1);
		}

		if(newMatch != null) {
			i += 2;
		}
	}

	private void openNewMatch(MapLoader loader) {
		settlersFrame.showNewMultiPlayerGameMenu(loader, (IMultiplayerConnector)entry.getConnection());
	}

	private void joinMatch(MapLoader loader) {
		NetworkGameMapLoader networkGameMapLoader = (NetworkGameMapLoader) loader;
		IJoiningGame joiningGame = ((IMultiplayerConnector)entry.getConnection()).joinMultiplayerGame(networkGameMapLoader.getJoinableGame());
		joiningGame.setListener(new IJoiningGameListener() {
			@Override
			public void joinProgressChanged(EProgressState state, float progress) {
			}

			@Override
			public void gameJoined(IJoinPhaseMultiplayerGameConnector connector) {
				SwingUtilities.invokeLater(
						() -> settlersFrame.showJoinMultiplayerMenu(connector, MapList.getDefaultList().getMapById(networkGameMapLoader.getMapId()), ((IMultiplayerConnector)entry.getConnection()).getPlayerUUID()));
			}
		});
	}
}
