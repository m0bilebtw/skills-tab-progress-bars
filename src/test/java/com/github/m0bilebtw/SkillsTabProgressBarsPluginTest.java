package com.github.m0bilebtw;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SkillsTabProgressBarsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SkillsTabProgressBarsPlugin.class);
		RuneLite.main(args);
	}
}