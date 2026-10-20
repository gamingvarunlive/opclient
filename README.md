# Op Client (in-game mod)

Small Fabric mod that Op Launcher installs into your profile automatically. It does two things:

1. Shows your own name tag in third person (F5).
2. Shows the Op logo in front of the name (name tag and Tab list) of other players who play with Op Launcher.
   For this the launcher gives the game the friend server address, and the mod sends the names in your Tab list
   to that server to ask who uses Op Launcher. Nothing else is sent. Without a friend server this part stays off.

## Build it (no programming needed)

1. Create a free GitHub repository and upload **everything inside this `mod` folder** (including the hidden `.github` folder).
2. Open the **Actions** tab, wait for "Build Op Client" to turn green (a few minutes).
3. Open the run, download the artifact `opclient`, unzip it and copy the jar (the one without `-sources`) to
   `op-launcher/assets/opclient/opclient.jar`.
4. Create `op-launcher/assets/opclient/opclient.json` with the versions this jar was built for:
   `{ "versions": ["1.21.11"] }`

If the build turns red, open the failed step and send me the red error text. The version numbers in
`gradle.properties` come from https://fabricmc.net/develop and may need a small update.

A jar only works with the Minecraft version it was built for. Op Launcher only installs it into profiles
whose version is listed in `opclient.json`. Op Client is always on: it is added to every new profile, shows as
"Required" in the Mods tab, and is put back on every launch if it was removed.
