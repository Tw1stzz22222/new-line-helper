package com.new_line_transcript;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = NewLineTranscript.MODID, name = NewLineTranscript.NAME, version = NewLineTranscript.VERSION)
public class NewLineTranscript
{
    public static final String MODID = "new_line_transcript";
    public static final String NAME = "New Line Transcript";
    public static final String VERSION = "1.0";

    @SidedProxy(clientSide = "com.new_line_transcript.ClientProxy", serverSide = "com.new_line_transcript.CommonProxy")
    public static CommonProxy proxy;

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        proxy.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.init();
        logger.info("{} initialized", NAME);
    }
}
