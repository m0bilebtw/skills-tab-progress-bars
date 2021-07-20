# Skills Tab Progress Bars [![Plugin Installs](http://img.shields.io/endpoint?url=https://i.pluginhub.info/shields/installs/plugin/skills-tab-progress-bars)](https://runelite.net/plugin-hub/m0bile%20btw) [![Plugin Rank](http://img.shields.io/endpoint?url=https://i.pluginhub.info/shields/rank/plugin/skills-tab-progress-bars)](https://runelite.net/plugin-hub)
##### A plugin for [RuneLite](https://runelite.net/)
Adds progress bars to the skills tab to show how close the next level ups are

Config options include 
- full colour customisation (Thanks to https://github.com/Matthew-nop)
- option to indent to match skill panel corners
- adjustable bar height
- showing for virtual levels
- showing for goals set using the in-game XP menu

While hovering a skill, all progress bars except the one for that skill are hidden.
This avoids bars covering the tooltips and their text, which otherwise would happen because
there is no layer available between the skills themselves and their tooltips,
so we must draw on top of it all.
