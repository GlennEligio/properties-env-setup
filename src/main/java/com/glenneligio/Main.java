package com.glenneligio;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.help.Help;
import com.glenneligio.commands.EnvSetup;
import com.glenneligio.commands.PropertiesSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

@Cli(name = "setup",
        description = "PROPERTIES ENV SETUP CLI",
        defaultCommand = Help.class,
        commands = {PropertiesSetup.class, EnvSetup.class, Help.class})
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        logger.info("Args: {}", Arrays.stream(args).collect(Collectors.toList()));
        com.github.rvesse.airline.Cli<Runnable> cli = new com.github.rvesse.airline.Cli<>(Main.class);
        Runnable cmd = cli.parse(args);
        cmd.run();
    }

}