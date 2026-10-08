# Steam Deck + couch co-op setup for the Forge 2HG fork

Goal: desktop PC runs Forge (does all the work), Steam Deck docked to the TV
is just a video/input relay, two controllers plugged/paired to the Deck each
act as independent mice for the two local players.

## Why this is plain Remote Play, not "Remote Play Together"

These are two different Steam features and it matters which one you use:

- **Remote Play Together** is for inviting *other people's Steam accounts* to
  join a game they don't own, without them needing to install it. Non-Steam
  games generally don't work with it, because the publisher has to
  specifically flag their game as RPT-enabled - something that's simply not
  possible for a game (like this fork) that was never published on Steam at
  all.
- **Plain Remote Play** is just streaming your own library (including
  non-Steam shortcuts) from one of your own devices to another of your own
  devices, under the same Steam account. No publisher flag needed. This is
  the one you want: the Deck is your second device, not a guest.
- Local co-op with two controllers on the *same* client device (the Deck)
  forwarding both inputs to the host is a standard, long-supported part of
  plain Remote Play - it works the same as local splitscreen/hotseat, just
  with the game actually running on the PC. This is exactly the hotseat flow
  Forge already supports (and that the dedicated-hand-cell fix cleaned up).

## One-time setup

### 1. Add Forge as a non-Steam game on the desktop

1. Open Steam on the desktop PC.
2. Library → **Add a Game** (bottom-left, or Games menu) → **Add a Non-Steam
   Game**.
3. Browse to `C:\Users\nhoff\dev\forge-run2\forge.exe` (or wherever you've
   extracted the latest built bundle - see the build notes below) and add it.
4. It'll now show up as a regular entry in your Steam library.

### 2. Enable Remote Play on the desktop

1. Steam → **Settings** → **Remote Play**.
2. Make sure Remote Play is **enabled**.
3. Expand **Advanced Host Options** and turn on **Play audio on host**
   (otherwise you may not get sound) and, if you want the resolution to
   adapt automatically, **change desktop resolution to match streaming
   client**.
4. Still in Big Picture Mode → Settings → Controller Settings, enable
   generic/Xbox/PlayStation/Switch Pro controller config support, so Steam
   Input is available for whatever controllers you're pairing.

### 3. Pair both controllers to the Steam Deck

Since both controllers are *local peripherals on the Deck* (not on separate
remote accounts), pair them to the Deck itself via Bluetooth (or plug in via
USB if wired) - same as pairing any controller to the Deck for its own games.

### 4. Stream from the Deck

1. Dock the Deck to the TV via HDMI.
2. On the Deck, open Steam, find Forge in your library (it should show up
   there since it's tied to your account's library across devices).
3. Select it → the play button should offer **Stream** (since it's a
   Windows exe, not a native SteamOS build) → choose your desktop PC.
4. The desktop's screen now shows on the TV via the Deck.

## Controller → mouse mapping (the part that actually matters for Forge)

Forge is a point-and-click desktop app with no native gamepad support, so
each controller needs a Steam Input **Desktop Configuration** (the generic
"use this controller as a mouse" profile) rather than a game-specific
gamepad binding - there's no gamepad binding to make here, since Forge never
reads controller input directly.

1. While streaming, open the Steam overlay (usually the Steam button on the
   controller) → **Controller Settings** for that specific device.
2. Pick the **Desktop Configuration** template (right stick/trackpad as
   mouse movement, a face button as left-click, another as right-click).
   Do this for *both* controllers.
3. Both controllers will drive the **same** system cursor - Steam Input has
   no concept of "player 2's own cursor." This sounds like a problem but
   isn't really one for 2HG: turns are still sequential at the
   priority-holding level (CR 805 lets teammates act in whatever order they
   like, it doesn't mean simultaneous clicking), so in practice only the
   player whose turn it is to act moves their stick; the other player just
   watches their own now-dedicated hand/field panel until it's their moment.
   If this turns out to be annoying in practice, say so after a playtest -
   there are fancier options (e.g. a third-party tool that gives each
   controller its own independent cursor) but they're not worth setting up
   preemptively.

## Rebuilding after code changes

Each time the Forge source changes, the runnable bundle needs to be rebuilt
and re-extracted - the bare jar under `target/` won't run on its own (it's
missing the `res/` folder). From `C:\Users\nhoff\dev\forge`:

```powershell
mvn -U -B clean -P windows-linux install -DskipTests
```

Then extract the fresh bundle (pick a new folder name each time to dodge
Windows file-lock flakiness with the previous extraction):

```powershell
tar -xjf forge-installer\target\forge-installer-2.0.16-SNAPSHOT.tar.bz2 -C ..\forge-run-N
```

`forge.exe` (or `forge.cmd`) in that folder is what you point the Steam
shortcut at. If you re-add the shortcut each time, un-add the old one first,
or just overwrite the exe at a fixed path instead of making a new folder
every time.
