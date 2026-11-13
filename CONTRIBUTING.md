# Contributing

Bug reports and pull requests are welcome. For visual bugs, include the Android
version, launcher, widget size and a screenshot with private information removed.

Keep changes focused and describe both the problem and the resulting behavior.
Run the builds and Android Lint from the [development guide](docs/DEVELOPMENT.md#checks). If a change
affects rendering, run the connected-device checks and inspect the generated
previews as well. State clearly when device testing was not available.

Java sources use Google Java Format style. Preserve sprite proportions, weekday
contrast, the transparent background and offline behavior. Do not add signing
keys, credentials, SDK installations, build output or private screenshots.

Original code contributions use the repository's MIT license. For any new
third-party artwork, document its source and terms in [ASSETS.md](ASSETS.md).
