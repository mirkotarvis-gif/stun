# Stun Practice (Fabric, Minecraft 1.21.11)

Hit a player who is blocking with a shield while holding an axe -> your hit disables the shield,
then the mod presses your REAL left mouse button again after `delay` ms, so the next hit lands on the player.

- No packets are built or sent by this mod. It clicks the mouse at the OS level
  (Windows: user32, macOS/Linux: java.awt.Robot) and Minecraft handles the click like any normal click.
- It won't click if a menu/chat is open or the game window isn't focused.

## Build
Needs JDK 21. In this folder run:

    gradlew build          (Windows: gradlew.bat build)

The jar is in `build/libs/stunpractice-1.0.0+1.21.11.jar`. Put it in `.minecraft/mods` together with Fabric API.

Different Minecraft version? Change the versions in `gradle.properties` (get them from https://fabricmc.net/develop)
and `"minecraft"` in `src/main/resources/fabric.mod.json`.

## Turning it off
Press **K** (shows ON/OFF above your hotbar). Change the key in Options > Controls > Key Binds > Miscellaneous.
You can also use `/stunpractice toggle` or the Enabled button in the Mod Menu screen.

## Settings
With **Mod Menu** installed: Mods -> Stun Practice -> config button. Without it, use the file or commands:

File: `.minecraft/config/stunpractice.properties` (created on first launch), or in game:

| Command | What it does | Default |
|---|---|---|
| `/stunpractice` | show settings | |
| `/stunpractice toggle` | on/off | on |
| `/stunpractice delay <ms>` | wait after your hit before the auto click | 60 |
| `/stunpractice hold <ms>` | how long the button is held per click | 15 |
| `/stunpractice clicks <n>` | number of auto clicks after your hit | 1 |
| `/stunpractice gap <ms>` | gap between auto clicks if clicks > 1 | 50 |
| `/stunpractice cooldown <ms>` | ignore new triggers for this long | 300 |
| `/stunpractice reload` | re-read the config file | |

Tip: the 2nd hit right after an axe hit does reduced damage because of attack cooldown.
Raise `delay` (e.g. 300-600) if you want the 2nd hit to do more damage, lower it to hit before they re-raise the shield.
