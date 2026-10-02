# BarMC bridge

Shows your Minecraft server on a [BUSY Bar](https://busy.bar). It connects to the `vane-busybar`
plugin, draws on the Bar through the official [busylib](https://pypi.org/project/busylib/) SDK,
and sends the Bar's controls back to the server.

```
Paper + vane-busybar ──events──▶ barmc (this) ──local HTTP──▶ BUSY Bar
                     ◀─requests──              ◀─buttons────
```

Run it on a computer on the same network as the Bar (or with the Bar plugged in over USB).
It only makes outgoing connections, so it works behind a home router.

The bridge lives in the vane repository next to the plugin, `vane-busybar/bridge`, and is
released with the same version: bridge 1.23 goes with vane-busybar 1.23. Each GitHub release has
it as `barmc-<version>.tar.gz` and a wheel.

## Setup

Needs Python 3.10 or newer. From a release:

```sh
python3 -m venv ~/.local/share/barmc
~/.local/share/barmc/bin/pip install barmc-<version>-py3-none-any.whl
```

Or from a checkout of the vane repository:

```sh
cd vane-busybar/bridge
python3 -m venv .venv
.venv/bin/pip install -e .
```

## Try it with the vane dev server

1. Start the server:

   ```sh
   # from the root of the vane repository
   ./gradlew runServer --no-daemon --warning-mode all
   ```

2. Join `localhost` in Minecraft and run `/busybar link`. It prints a pairing string such as
   `<token>@<ip>:9123#sha256=<fingerprint>`. Click it to copy it; it is shown only once.

3. Preview in the terminal, no Bar needed:

   ```sh
   .venv/bin/barmc --pair '<pairing string>' --terminal
   ```

   Keys: `o`/Enter = OK, `x`/Backspace = BACK, `[` `]` or arrows = wheel, `b` = move the
   selector to or from BUSY.

4. On the real Bar:

   ```sh
   # USB
   .venv/bin/barmc --pair '<pairing string>' --bar 10.0.4.20
   # Wi-Fi: the Bar's IP and the API password set in its web interface
   .venv/bin/barmc --pair '<pairing string>' --bar 192.168.1.20 --bar-token 1234
   ```

Options can also come from the environment: `BARMC_PAIR`, `BUSYBAR_ADDR`, `BUSYBAR_TOKEN`
(and `BARMC_SERVER`, `BARMC_TOKEN`, see below). Add `-v` to log every event.

## Security

The pairing string holds everything the bridge needs: the server address, your token and the
fingerprint of the server's TLS key. The bridge only talks to a server presenting that exact key,
and checks it before sending anything, so the token never reaches an impostor. This works without
a domain or a certificate authority; the server's certificate is self-signed on purpose.

Treat the pairing string like a password: anyone holding it can read your events and switch your
focus mode on that server (not log into Minecraft as you). Command-line arguments are visible to
every user of the machine and land in your shell history, so for a permanent setup keep it in a
file only you can read:

```sh
install -m 600 /dev/null ~/.config/barmc.env
cat > ~/.config/barmc.env <<'ENV'
BARMC_PAIR='<token>@<ip>:9123#sha256=<fingerprint>'
BUSYBAR_ADDR=192.168.1.20
BUSYBAR_TOKEN=1234
ENV
```

and run it as a systemd user service, `~/.config/systemd/user/barmc.service`. Replace the
`ExecStart` path with your own: `~/.local/share/barmc/bin/barmc` for a release install, or what
`realpath .venv/bin/barmc` prints inside `vane-busybar/bridge` for a checkout:

```ini
[Unit]
Description=BarMC bridge
After=network-online.target

[Service]
EnvironmentFile=%h/.config/barmc.env
ExecStart=/home/you/vane/vane-busybar/bridge/.venv/bin/barmc
Restart=on-failure
RestartPreventExitStatus=2 3

[Install]
WantedBy=default.target
```

```sh
systemctl --user enable --now barmc
loginctl enable-linger "$USER"   # keep it running while logged out, e.g. on a Raspberry Pi
```

When the bridge stops on its own, its exit code says why:

| Exit | Meaning | Fix |
|------|---------|-----|
| 2 | The server rejected the token: it was revoked, or replaced by a newer `/busybar link`. | Run `/busybar link` and update the pairing string. |
| 3 | The server's TLS key does not match the pairing string. Nothing was sent to it. | If an admin ran `/busybar rotatekey`, link again. Otherwise someone may be intercepting the connection. |

Other connection problems are retried with backoff. `RestartPreventExitStatus` keeps systemd from
retrying 2 and 3, which only a new pairing string fixes.

**Servers without the fingerprint.** A pairing string without `#sha256=` means the admin set the
plugin's `Tls` to `keystore` (a certificate your system already trusts, validated as usual) or
`off`. With `off`, the string starts with `http://` and the bridge warns if the server is not on
your local network: the token then crosses the internet unencrypted. `--server` and `--token`
(`BARMC_SERVER`, `BARMC_TOKEN`) remain for setting the two parts separately.

## macOS

The Python that comes with the Xcode command line tools is 3.9, too old. Install a current one
with Homebrew (`brew install python`) or from python.org, then:

```sh
python3 -m venv ~/.local/share/barmc
~/.local/share/barmc/bin/pip install barmc-<version>-py3-none-any.whl
```

Running it and the private env file work as on Linux (see [Security](#security)): put
`BARMC_PAIR`, `BUSYBAR_ADDR` and `BUSYBAR_TOKEN` in `~/.config/barmc.env`, readable only by you.

The preview uses 256 colours in Terminal.app, which garbles 24-bit colour before macOS 26, and
24-bit colour in iTerm2, Ghostty and other terminals. `--colors 24bit` or `--colors 256` overrides
the choice.

To start the bridge at login, save this as `~/Library/LaunchAgents/org.oddlama.vane.barmc.plist`,
with your user name in the log path, since launchd does not expand `~` there:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>org.oddlama.vane.barmc</string>
  <key>ProgramArguments</key>
  <array>
    <string>/bin/sh</string>
    <string>-c</string>
    <string>set -a; . "$HOME/.config/barmc.env"; exec "$HOME/.local/share/barmc/bin/barmc"</string>
  </array>
  <key>RunAtLoad</key>
  <true/>
  <!-- Restart after a crash, but not after exit codes 2 and 3, which need a new pairing string. -->
  <key>KeepAlive</key>
  <dict>
    <key>Crashed</key>
    <true/>
  </dict>
  <key>StandardErrorPath</key>
  <string>/Users/you/Library/Logs/barmc.log</string>
</dict>
</plist>
```

```sh
launchctl bootstrap gui/$(id -u) ~/Library/LaunchAgents/org.oddlama.vane.barmc.plist   # start now and at login
launchctl bootout gui/$(id -u)/org.oddlama.vane.barmc                                   # stop and disable
```

Its messages end up in `~/Library/Logs/barmc.log`, which Console.app also shows. Like on Windows,
it reconnects by itself and only stops for exit codes 2 and 3, so it is not restarted for those.

## Windows

Install Python from python.org (tick "Add python.exe to PATH"), then in PowerShell:

```powershell
py -m venv $env:LOCALAPPDATA\barmc
& $env:LOCALAPPDATA\barmc\Scripts\pip install barmc-<version>-py3-none-any.whl
```

Try it with the preview first. Use double quotes around the pairing string in `cmd`; PowerShell
takes either:

```powershell
& $env:LOCALAPPDATA\barmc\Scripts\barmc --pair "<token>@<ip>:9123#sha256=<fingerprint>" --terminal
```

The preview looks best in Windows Terminal, the default on Windows 11; the older console works
too. The keys are the same as on Linux.

For a permanent setup, keep the pairing string and the Bar's password out of the command line by
storing them in your user environment, which other users of the PC cannot read:

```powershell
setx BARMC_PAIR "<token>@<ip>:9123#sha256=<fingerprint>"
setx BUSYBAR_ADDR "192.168.1.20"
setx BUSYBAR_TOKEN "1234"
```

`setx` only affects programs started afterwards, so open a new window before testing. To start the
bridge when you log in, press Win+R, open `shell:startup`, and create `barmc.cmd` there:

```bat
start "barmc" /min "%LOCALAPPDATA%\barmc\Scripts\barmc.exe"
```

There is no need to restart it on failure: it reconnects by itself after network problems and
server restarts, and only stops for exit codes 2 and 3 above, which a new pairing string fixes.
Ctrl+C in its window stops it (exit code 130).

## What the Bar shows

Idle screens rotate every 5 seconds; the wheel flips through them.

- **Players**: the server icon, players online and TPS (TPS only for players with
  `vane.busybar.receive.server`).
- **World clock**: in-game time in 10-minute steps, day or night.
- **Bedtime**: who is asleep and how many more are needed, while anyone is.
- **Autostop**: a countdown while the empty server is about to stop. With
  `vane.busybar.action.autostop_abort`, OK cancels it.
- **Region** (with vane-regions): the region you are standing in, and whether it is your land, a
  safe zone without PvP, or whose it is.

**Visitors** (with vane-regions and `vane.busybar.receive.region_visitors`, ops only by
default): when a stranger walks into one of your regions, the Bar shows their face and where, even
while you are offline. Several visitors show as `ALEX +2`, a crowd as `7 VISITORS`; the server
reports a region's first visitor at once and the rest at most once a minute.

Events such as joins, portals, night skips and lag appear as alerts on top, most important first.
BACK dismisses one. Join and leave alerts show the player's own face from their skin, at 16x16;
on offline-mode servers, or until the face has downloaded, a generic head stands in.

**Focus mode**: move the selector to BUSY. The server tags you `[Busy]` in the tab list, the Bar
shows a 25-minute focus timer, and only critical alerts (autostop, server stop) get through.
Moving the selector away shows how many alerts were held.

## Not verified on a device yet

The bridge has been tested against the vane dev server and a fake Bar that records requests, not
a real BUSY Bar. Things to check on the device:

- Text placement uses busylib's own two-line layout (`small` font, top line at y=0, bottom
  line anchored to y=16). Tune `TEXT_X`, `FONT` and the y values in
  `barmc/display/busybar.py` if lines clip.
- Lines longer than about 9 characters scroll, following busylib's rule of thumb. Short lines
  like `TIME 07:30` may fit without scrolling on the device.
- Whether the Bar keeps showing the bridge's screen when the selector is on BUSY, since the
  firmware has its own busy screen. `--priority` (1-100) sets how strongly the bridge claims
  the display.
- On macOS: the tests run there in CI, but the LaunchAgent and Terminal.app's colours have not
  been tried on a real Mac. The Bar's USB connection likely works without a driver, since macOS
  supports USB network adapters out of the box, but is untested too.
- On Windows: the tests run there in CI, but the preview's keys, colours and Ctrl+C have only
  been simulated, not used on a real Windows PC. Whether Windows sets up the Bar's USB network
  connection (`--bar 10.0.4.20`) without a driver is also open; Wi-Fi does not depend on it.

## Development

```sh
.venv/bin/pip install -e '.[test]'
.venv/bin/python -m pytest
```

- `barmc/state.py`: screens, alert priorities, focus mode and inputs. Display-independent.
- `barmc/server.py`: the plugin's event stream, requests and server icon.
- `barmc/pairing.py`: pairing strings and pinning of the server's TLS key.
- `barmc/display/busybar.py`: drawing on the BUSY Bar and reading its controls.
- `barmc/display/terminal.py`: the terminal preview.
- `barmc/sprites.py`, `barmc/icons.py`: pixel art and icon conversion.
