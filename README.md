# JSettlers

This project intends to create a remake of the famous strategy game "The Settlers 3" published by Blue Byte in 1998. The project is developed in Java and runs on PC (Windows/Linux), Mac and Android. More information can be found on the project's website at [www.settlers-android-clone.com](http://www.settlers-android-clone.com)

### Warning: Alpha Status
The game is currently in an **alpha** status! Therefore bugs, frequent changes making saved games invalid and server abortions need to be expected. Nevertheless we will try to minimize trouble.

### Found a Bug? Report it!
If you experience troubles / find a bug, help us fix it. The JSettlers game creates log files of your games that are essential for debugging. Therefore always include the following information in a bug report:
1. What OS and JRE are you using? If it is Android, please state your Android version.
2. Revision of your build:
  1. In the PC/Mac version this can be found in the head of the window as "JSettlers - commit: XXXXXXX".
  2. In the Android version, the info is displayed on the start screen in the lower right corner as "build: XXXXXXX".
3. In the game's folder, there is a subfolder ```resources/logs/``` containing a folder of log files for every game you played. Please package the folder belonging to your game where you experienced the bug (identifiable by date and map name). This folder contains the following files:
  1. *_out.log: This is the console / debugging output of the game.
  2. *_replay.log: This file contains all game relevant actions you and other players did in the game (e.g. place a building, send soldiers somewhere). With this game, we can recalculate your game and debug it to find the exact source of your trouble.
4. Specify the game time when you first experienced the bug (the game time can be found in the upper right corner while playing). 
5. If you were playing a custom map, please attach the map. Without it, replaying the game won't be possible.
6. Describe what problem you experienced so that we can easily understand it.

Many thanks in advance for helping to improve this game!


## Playing JSettlers

In order to play the game, you need to have the "GFX" and "SND" folders of the original version of "The Settlers 3" as obtained by installing the original "The Settlers 3" game (DEMO version also works).

After that, follow the detailed installation instructions for you platform.

### Windows, Linux, Mac OS
1. Install "The Settlers III" or a demo version of it. Don't worry, if it is not running on your OS, we only need the graphics and sound files.
2. Download the newest stable release of JSettlers*.zip / JSettlers*.tar.bz2 (this also includes the MapEditor).
3. Unpack the downloaded archive to wherever you want JSettlers' installation to be.
4. Open the unpacked file named "config.prp" with a text editor. Update the line starting with 

   ```settlers-folder=. ``` 

   to 

   `settlers-folder=<SETTLERS-3-INSTALLATION-PATH>` 

   where ```<SETTLERS-3-INSTALLATION-PATH>``` is the path to your original "The Settlers 3" installation. (Meaning a folder containing the original's "GFX" and "SND" folders.) **Important:** Use slashes as path delimiters. 
5. Run the "JSettlers.jar" file and enjoy the game.
6. Please have a look at the [manual](https://github.com/jsettlers/settlers-remake/wiki/JSettlers-Manual). The current state of the game still lacks many controls known from the original, but also contains new ways to do things, which you shouldn't miss. 

### Android
1. Enable installation of Apps from "Unknown Sources".
2. Copy the "GFX" and the "SND" folders of your original "The Settlers 3" installation into a folder called "JSettlers" on your device. The "JSettlers" folder must be located in the root directory of your internal storage (alongside folders like "Download" or "DCIM") or your external storage (e.g. memory card).
3. Download the newest stable JSettlers-Android_v*.apk onto your Android device.
4. Install JSettlers by running the downloaded file.

## Developer's Guide

This project can be built using the [Eclipse IDE](http://www.eclipse.org). The development process follows the [Fork & Pull](https://help.github.com/articles/using-pull-requests/#fork--pull) principle. 

### A Word in Advance
The code of this project has grown over years. Furthermore, I never expected this project to get so far. However, I want to make clear that I know there is still a lot to do and that some parts of the code need to evolve. Therefore I'm glad you want to help. If you find ugly code, you know a better way to do something or you think the architecture needs to be changed, feel free to tell me. Feel free to open tickets for these proposals, so everybody can have a look on them and share their opinion.

Moreover, if you have any questions or additions to this guide, feel free to write me. As the documentation on this project is rather small, I'm also looking forward to improvements to it.

### Development Process
In order to assist development, follow these steps:

1. Fork this repository.
2. Clone the forked repository to your local machine.
3. Import all projects in the cloned repository into your Eclipse Workspace (via "File" -> "Import" -> "Existing Projects into Workspace"). As this repository contains multiple Eclipse Projects, we recommend creating a new workspace for it. If you have trouble building the projects, the following steps might help:
  - Clean and build all the projects.
  - Make sure the project jsettlers.common has a source folder called "gen". If not, refresh the project. If it still isn't there, try to explicitly build the project (right click on the project -> "Build Project").
  - After that, rebuild all projects (without a clean!). You can do this with "CTRL + B".
  - If there is still a project refusing to build, try to explicitly build that one via right click on the project -> "Build Project". 
  - If you know how to fix this trouble, I'm looking forward to your hints and proposed changes ;).
4. When developing changes, create a new branch off of the project's master and give it a speaking name. 
5. Regularly commit your changes and push them to the new branch in your forked repository. 
6. If you have questions, need help, or want to see the test results of your code, create a [Pull Request](https://help.github.com/articles/using-pull-requests/) to this repository.
7. After your changes have been reviewed and all is fine, we will merge them into the master.

### Development Guidelines
In order to ease collaboration and reduce unnecessary overhead, developers should follow these guidelines.

#### Eclipse Auto Formatter
This project uses a common [Eclipse Auto Formatter configuration](/docs/eclipse-setup/formatterConfiguration.xml). To configure Eclipse correctly, open the workspace containing the project and select "Window" -> "Preferences" in the top bar. In the search field of the dialog, type "save actions" and select the Java save actions as shown in the picture below.

![configuring save actions](/docs/eclipse-setup/01_configuringSaveActions.png)

Adjust your settings to match the ones of the figure before clicking on "Formatter" and then "Import".

![importing formatter](/docs/eclipse-setup/02_importingFormatter.png)

The [formatter configuration](/docs/eclipse-setup/formatterConfiguration.xml) can be found in the repository at "docs/eclipse-setup/formatterConfiguration.xml".

#### Further Plugins
- Android Development Tools for Eclipse: Required for building, running and developing the Android version of JSettlers in Eclipse. You furthermore require the Android SDK installed on your machine. Even if you don't want to develop the Android version, this is recommended, as without it, you will have compile errors in the Android projects, making it hard to find real compile errors introduced by your changes.
- FindBugs: Runs a static code analysis to detect potential bugs and bad code.

These plugins can be downloaded via the "Eclipse Marketplace". 


### Tips & Tricks
The following tips can help you getting started with developing on this project.

#### Recommended Run Configurations
To ease development, JSettlers supports several command line parameters. Amongst others, we find the following combinations helpful.

- `jsettlers.main.swing.SwingManagedJSettlers --control-all --console-output --activate-all-players`
  - `--control-all` allows you to control and see the settlers of all players, instead of only your own ones.
  - `--console-output` prints the console / logging outputs to the console, instead of writing them to a file.
  - `--activate-all-players` ensures that all players on a map are positioned at start. This is especially useful when testing development with the single player mode, as otherwise you would only have one player on the map.
- `jsettlers.main.swing.SwingManagedJSettlers --control-all --console-output --activate-all-players --targetTime=10 --replayFile="2015-02-27_13-23-03_new_map_replay.log`
  - This combination of parameters is very helpful for analysing bugs experienced during a game. With the `--replayFile` parameter, it is possible to specify a replay file that is then used to rerun the exact same game (by using the same map, random seed, and user inputs). 
  - `--targetTime=<X>` specifies the game time of the savegame. This time is specified in minutes.
  - `--replayFile=<PATH TO FILE>` gives the path to the replay file used as source.
- `jsettlers.main.swing.SwingManagedJSettlers --control-all --console-output --activate-all-players --localhost`
  - `--localhost` ensures that localhost is used as multiplayer server, making it useful for local multiplayer development / debugging. **NOTE:** Of course you need to have a Dedicated JSettlers Server running on localhost (see below).
- `jsettlers.network.server.DedicatedServerApp`
  - Starts a local dedicated server.
- `jsettlers.main.replay.ReplayToolApp --targetTime=10 --replayFile="2015-02-27_13-23-03_new_map_replay.log"`
  - This tool lets you create a savegame from a replay file at a specified target time. Any remaining tasks of the replay that happen after the target time, will be saved to a new replay file `replayForSavegame.log`. This helps when searching a bug that happened after some hours of playing with a replay file. Instead of needing to replay the whole game every time you want to start the debugging over, you can create a savegame close to the time the bug happens and start replaying from there.
  - `--targetTime=<X>` specifies the game time of the savegame. This time is specified in minutes.
  - `--replayFile=<PATH TO FILE>` gives the path to the replay file used as source.

#### Known Issues
*  The project "jsettlers.main.android" displays a classpath error (red exclamation mark on the project symbol)
  * Right click the project, select "Run As" -> "Android Application". This causes the missing dependency to be build. Please note, the first run may fail, with the message that there are errors in the project. After that, simply wait until Eclipse detects that the errors are no longer valid. Then you can run the project as Android Application and install it on your device.

#### Project Structure Overview
The JSettlers code is divided into multiple projects. In the following, there main purposes will be described.

##### jsettlers.graphics
This is the main graphics project. It contains everything needed to draw the in game map.

##### jsettlers.graphics.androidui
This is the GUI overlay used on Android.

##### jsettlers.graphics.swing
This is the GUI overlay used on PC. This does not really require swing any more, it also works with native gl.

##### go.graphics
This project contains the base graphics abstraction interface. Contains all code used on all plattforms

##### go.graphics.android
The Android graphics implementation.

##### go.graphics.nativegl
_Experimental_: A native OpenGL implementation for Linux. No external libraries are needed here. Still needs some work (e.g. has no text support so far). Target is to bypass the overhead of JOGL and remove the need for external libraries.

##### go.graphics.swing
The default Linux/Windows drawing library. It uses Swing to create a GL drawing window.


##### jsettlers.common
This project contains all the abstractions needed by most other modules. This includes the map interfaces, building specifications, further resources, image tables and some utility functions. One main purpose is to create a lose coupling of the game logic and the graphics layer.

##### jsettlers.logic
The core game logic. Including the main grid, settlers, buildings, algorithms for path finding and management.

##### jsettlers.network
This project contains the network logic as well as the dedicated server implementation and is only used by the jsettlers.logic** project.

##### jsettlers.main.android
The Android launch code.

##### jsettlers.main.swing
The Linux/Windows/Mac launch code using a Swing UI.

##### jsettlers.mapcreator
A standalone map creator app that lets you design your own maps.

##### jsettlers.tests
In this project all test cases are collected. These are not included in a build of the game. Unit tests are run by Travis automatically. It also contains many helper classes that are no unit tests but contain main methods for manual debugging / testing. 

##### jsettlers.buildingcreator
A building editor. We use this to help implement new buildings. It presents a UI to specify blocked and protected tiles of a building as well as the stacks of required material. This code is not included in a build.
