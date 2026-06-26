# Google Play Metadata

This folder is the source used by the `Sync Google Play Metadata` workflow.

When the Play Store listing needs an update, edit the files here, review the diff, run the metadata
validation workflow, and only then run the sync workflow.

Supported locale folders:

```text
en-US
fi-FI
sv-SE
de-DE
fr-FR
pl-PL
ru-RU
```

Text files usually live directly inside each locale folder:

```text
title.txt
short_description.txt
full_description.txt
changelogs/default.txt
```

Images live under each locale's `images/` folder:

```text
images/icon.png
images/featureGraphic.jpg
images/phoneScreenshots/
images/sevenInchScreenshots/
images/tenInchScreenshots/
```

Text changes are usually low risk. Screenshots and graphics are higher risk because syncing them can
replace the live Play Store assets for that locale. Review image changes visually before syncing
`screenshots`, `images`, or `all`.
