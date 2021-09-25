package jsettlers.graphics.sound;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java8.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.SneakyThrows;

import go.graphics.sound.SoundHandle;
import jsettlers.common.CommonConstants;
import jsettlers.common.player.ECivilisation;
import go.graphics.sound.SoundPlayer;

/**
 *
 * music management of different s3 versions, provides CIVILISATION specific music management
 *
 * TODO: validate music files and integrate exception handling
 *
 * @author MarviMarv
 */
public final class MusicManager implements Runnable {

	private final static String[][] ULTIMATE_EDITION_MUSIC_SET = { { "02", "03", "12" }, { "06", "07", "14" }, { "04", "05", "13" }, { "08", "09", "10" }, { "11" } };
	private final static String[][] HISTORY_EDITION_MUSIC_SET = { { "02", "03", "04" }, { "05", "06", "07" }, { "08", "09", "10" }, { "13", "14", "15" }, { "11", "12" } };
	
	private static final Logger LOG = LoggerFactory.getLogger(MusicManager.class);

	private static Path lookupPath = null;

	public static void setLookupPath(final Path lookupPath) {
		MusicManager.lookupPath = lookupPath;
	}

	private Thread musicThread = null;
	private final SoundPlayer soundPlayer;
	private final ECivilisation civilisation;
	private boolean paused = true;
	private SoundHandle activeTrack = null;
	private float volume;

	private final Semaphore waitMutex = new Semaphore(1);

	public MusicManager(SoundPlayer soundPlayer, ECivilisation civilisation) {
		this.soundPlayer = soundPlayer;
		this.civilisation = civilisation;

		volume = CommonConstants.MUSIC_VOLUME.get();
	}

	public boolean isRunning() {
		return !paused;
	}

	public void startMusic() {
		if (isRunning()) {
	return;
		}
		paused = false;
		musicThread = new Thread(this, "MusicThread");
		musicThread.setPriority(Thread.MIN_PRIORITY + 1);
		musicThread.setDaemon(true);
		musicThread.start();
	}

	@SneakyThrows(InterruptedException.class)
	public void stopMusic() {
		if (!isRunning()) {
	return;
		}
		if (activeTrack != null) {
			activeTrack.pause();
		}
		paused = true;

		waitMutex.release();

		musicThread.join();
	}

	public void setMusicVolume(float volume, boolean relative) {
		if (relative) {
			this.volume += volume;
		} else {
			this.volume = volume;
		}

		if (activeTrack == null || !isRunning()) {
	return;
		}
		activeTrack.setVolume(this.volume);
	}
	
	private void assembleMusicSet(final ECivilisation civilisation, final boolean playAll, final Consumer<URL> collector) throws IOException {
		// there are no music files available
		if (lookupPath == null || !Files.exists(lookupPath)) {
	return;
		}

		boolean somethingFound = false;
		// just take all mp3 and ogg files
		if (playAll) {
			//final Predicate<String> PAT_RECOG_MUSIC_FILEFORMATS = Pattern.compile(".*.(mp3|wav|ogg)", Pattern.CASE_INSENSITIVE).asMatchPredicate();
			try (DirectoryStream<Path> heuristicInGameMusicList = Files.newDirectoryStream(lookupPath, 
					//path -> PAT_RECOG_MUSIC_FILEFORMATS.test(path.toString())
					"*.{mp3,wav,ogg,MP3,WAV,OGG}" // pucgenie: spare us all the other permutations
					)) {
				for (Path file : heuristicInGameMusicList) {
					somethingFound = true;
					collector.accept(file.toUri().toURL());
				}
			}
		} else if(lookupPath.endsWith("Theme")) { // history edition
			final StringBuilder sb = new StringBuilder().append("Track");
			for (String fileName : HISTORY_EDITION_MUSIC_SET[civilisation.ordinal]) {
				sb.setLength(5);
				Path file = lookupPath.resolve(sb.append(fileName).append(".mp3").toString());
				if (!Files.exists(file)) {
					LOG.info(sb.append(" is missing but expected when using History Edition assets.").toString());
			continue;
				}
				somethingFound = true;
				collector.accept(file.toUri().toURL());
			}
		} else {
			// ultimate edition
			final StringBuilder sb = new StringBuilder().append("Track");
			for (String fileName : ULTIMATE_EDITION_MUSIC_SET[civilisation.ordinal]) {
				sb.setLength(5);
				Path file = lookupPath.resolve(sb.append(fileName).append(".ogg").toString());
				if (!Files.exists(file)) {
					LOG.info(sb.append(" is missing but expected when using Ultimate Edition assets.").toString());
			continue;
				}
				somethingFound = true;
				collector.accept(file.toUri().toURL());
			}
		}

		if (!somethingFound) {
			// this music folder is custom made
			//final File[] heuristicResultList = lookupPath.listFiles((file, name) -> name.matches(civilisation.name() + ".\\d*.(mp3|ogg|wav)"));
			try (DirectoryStream<Path> heuristigInGameMusicList = Files.newDirectoryStream(lookupPath, civilisation.name() + ".[0-9]*.{mp3,wav,ogg,MP3,WAV,OGG}")) {
				for (Path file : heuristigInGameMusicList) {
					collector.accept(file.toUri().toURL());
				}
			}
		}
	}

	@Override
	public void run() {
		int trackIndex = 0;
		ArrayList<SoundHandle> musicSet = new ArrayList<>(30);
		try {
			assembleMusicSet(civilisation, CommonConstants.PLAYALL_MUSIC.get(), musicFile -> musicSet.add(soundPlayer.openSound(musicFile)));
		} catch (IOException e1) {
			LOG.warn("", e1);
		}

		// nothing to do here
		if (musicSet.isEmpty()) {
	return;
		}
		Collections.shuffle(musicSet);
		if (Boolean.TRUE.toString().equals(System.getProperty("PUC_EXTENSIVE_COMPACTION"))) {
			musicSet.trimToSize();			
		}
		while (!paused) {
			activeTrack = musicSet.get(trackIndex);

			activeTrack.setVolume(volume);
			activeTrack.start();

			// wait until the track is completed or we are killed
			try {
				waitMutex.tryAcquire();
				waitMutex.tryAcquire(activeTrack.getPlaybackDuration(), TimeUnit.MILLISECONDS);
				waitMutex.release();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			activeTrack = null;

			trackIndex++;


			if (trackIndex == musicSet.size()) {
				trackIndex = 0;
			}
		}

		// cleanup music files
		for(SoundHandle handle : musicSet) {
			handle.dismiss();
		}
	}
}
