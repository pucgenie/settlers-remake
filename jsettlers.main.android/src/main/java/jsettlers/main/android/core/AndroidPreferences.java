/*
 * Copyright (c) 2015 - 2017
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
 */
package jsettlers.main.android.core;

import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import jsettlers.common.CommonConstants;

public class AndroidPreferences {
	private static final String PREFS = "prefs";
	private static final String PREF_PLAYER_ID = "playerid";
	private static final String PREF_PLAYER_NAME = "playername";
	private static final String PREF_SERVER = "server";
	private static final String PREF_PLAYALL_MUSIC = "playall";

	private final SharedPreferences preferences;

	public AndroidPreferences(Context context) {
		this.preferences = context.getSharedPreferences(PREFS, 0);

		CommonConstants.PLAYALL_MUSIC = this::isPlayAllMusic;
	}

	public String getPlayerId() {
		String id = preferences.getString(PREF_PLAYER_ID, null);
		if (id == null) {
			id = UUID.randomUUID().toString();
			preferences.edit().putString(PREF_PLAYER_ID, id).apply();
		}
		return id;
	}

	public String getPlayerName() {
		String name = preferences.getString(PREF_PLAYER_NAME, null);
		if (name == null) {
			name = Build.MODEL;
		}
		return name;
	}

	public void setPlayerName(String name) {
		preferences.edit().putString(PREF_PLAYER_NAME, name).apply();
	}

	public String getServer() {
		return preferences.getString(PREF_SERVER, CommonConstants.DEFAULT_SERVER_ADDRESS);
	}

	public void setServer(String serverName) {
		preferences.edit().putString(PREF_SERVER, serverName).apply();
	}

	public boolean isPlayAllMusic() {
		return preferences.getBoolean(PREF_PLAYALL_MUSIC, false);
	}

	public void setPlayAllMusic(boolean playAll) {
		preferences.edit().putBoolean(PREF_PLAYALL_MUSIC, playAll).apply();
	}
}
