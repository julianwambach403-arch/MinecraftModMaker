package com.modmaker;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModMaker implements ModInitializer {
	public static final String MOD_ID = "modmaker";
	public static final Logger LOGGER = LoggerFactory.getLogger("ModMaker");

	@Override
	public void onInitialize() {
		LOGGER.info("ModMaker initialising...");
	}
}
