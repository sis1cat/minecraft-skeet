package net.minecraft.client.data;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.SharedConstants;
import net.minecraft.SuppressForbidden;
import net.minecraft.client.ClientBootstrap;
import net.minecraft.client.data.models.EquipmentAssetProvider;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.server.Bootstrap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Main {
    @DontObfuscate
//    @SuppressForbidden(
//        a = "System.out needed before bootstrap"
//    )
    public static void main(String[] pArgs) throws IOException {
        SharedConstants.tryDetectVersion();
        OptionParser optionparser = new OptionParser();
        OptionSpec<Void> optionspec = optionparser.accepts("help", "Show the help menu").forHelp();
        OptionSpec<Void> optionspec1 = optionparser.accepts("client", "Include client generators");
        OptionSpec<Void> optionspec2 = optionparser.accepts("all", "Include all generators");
        OptionSpec<String> optionspec3 = optionparser.accepts("output", "Output folder").withRequiredArg().defaultsTo("generated");
        OptionSet optionset = optionparser.parse(pArgs);
        if (!optionset.has(optionspec) && optionset.hasOptions()) {
            Path path = Paths.get(optionspec3.value(optionset));
            boolean flag = optionset.has(optionspec2);
            boolean flag1 = flag || optionset.has(optionspec1);
            Bootstrap.bootStrap();
            ClientBootstrap.bootstrap();
            DataGenerator datagenerator = new DataGenerator(path, SharedConstants.getCurrentVersion(), true);
            addClientProviders(datagenerator, flag1);
            datagenerator.run();
        } else {
            optionparser.printHelpOn(System.out);
        }
    }

    public static void addClientProviders(DataGenerator pDataGenerator, boolean pToRun) {
        DataGenerator.PackGenerator datagenerator$packgenerator = pDataGenerator.getVanillaPack(pToRun);
        datagenerator$packgenerator.addProvider(ModelProvider::new);
        datagenerator$packgenerator.addProvider(EquipmentAssetProvider::new);
    }
}