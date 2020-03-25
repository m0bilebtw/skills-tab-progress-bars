# Skills Tab Progress Bars
##### A plugin for [RuneLite](https://runelite.net/)
Adds progress bars to the skills tab to show how close the next level ups are

Config options include 
- transparency toggle
- draw bar backgrounds toggle
- adjustable bar height

While hovering a skill, all progress bars except the one for that skill are hidden.
This avoids bars covering the tooltips and their text, which otherwise would happen because
there is no layer available between the skills themselves and their tooltips,
so we must draw on top of it all.