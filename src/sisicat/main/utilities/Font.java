package sisicat.main.utilities;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.blaze3d.platform.NativeImage;
import generaloss.freetype.FTLibrary;
import generaloss.freetype.bitmap.FTBitmap;
import generaloss.freetype.face.FTFace;
import generaloss.freetype.face.FTLoad;
import generaloss.freetype.face.FTLoadFlags;
import generaloss.freetype.glyph.FTGlyphSlot;
import net.minecraft.client.renderer.texture.DynamicTexture;

import org.lwjgl.BufferUtils;

import sisicat.IDefault;
import sisicat.main.gui.elements.Window;

import javax.imageio.ImageIO;


public class Font implements IDefault, AutoCloseable {

    private final FTLibrary library;
    public FTFace face;

    private BufferedImage atlasImage;
    public DynamicTexture loadedTexture;

    private int baseAscender;
    private final int maxGlyphHeight;
    public int glyphRenderOffset;

    public Map<Integer, int[]> charactersMap = new HashMap<>();

    public Font(InputStream fontStream, float fontSize) throws Exception {

        this.library = FTLibrary.init();

        if (this.library == null) throw new RuntimeException("ft failed to init");

        byte[] fontBytes = fontStream.readAllBytes();

        ByteBuffer fontBuffer = BufferUtils.createByteBuffer(fontBytes.length);
        fontBuffer.put(fontBytes).flip();

        this.face = library.newMemoryFace(fontBuffer, 0);

        if (this.face == null) {
            library.done();
            throw new RuntimeException("face failed to init");
        }

        //face.(FTEncoding.UNICODE);
        face.setPixelSizes(0, (int) fontSize);

        this.maxGlyphHeight = (this.face.getSize().getMetrics().getAscender() - this.face.getSize().getMetrics().getDescender());

        createFontAtlas();

    }

    private void generateAllMinecraftChars() {

        generateChars(0, 55295);
        generateChars(63744, 65533);
        generateChars(65536, 73727);
        generateChars(74650, 74751);
        generateChars(74863, 74863);
        generateChars(74869, 74879);
        generateChars(75076, 77823);
        generateChars(78895, 78943);
        generateChars(82939, 82943);
        generateChars(83527, 92159);
        generateChars(92729, 94207);
        generateChars(100344, 100351);
        generateChars(101120, 101631);
        generateChars(101641, 131069);
        generateChars(131072, 132927);
        generateChars(132943, 132943);
        generateChars(132985, 132985);
        generateChars(133033, 133033);
        generateChars(133037, 133037);
        generateChars(133123, 133123);
        generateChars(133127, 133127);
        generateChars(133149, 133149);
        generateChars(133178, 133178);
        generateChars(133269, 133269);
        generateChars(133305, 133305);
        generateChars(133500, 133500);
        generateChars(133533, 133533);
        generateChars(133594, 133594);
        generateChars(133843, 133843);
        generateChars(133917, 133917);
        generateChars(134047, 134047);
        generateChars(134103, 134103);
        generateChars(134209, 134209);
        generateChars(134227, 134227);
        generateChars(134264, 134264);
        generateChars(134286, 134286);
        generateChars(134294, 134294);
        generateChars(134335, 134335);
        generateChars(134351, 134352);
        generateChars(134356, 134357);
        generateChars(134421, 134421);
        generateChars(134469, 134469);
        generateChars(134493, 134493);
        generateChars(134513, 134513);
        generateChars(134524, 134524);
        generateChars(134527, 134527);
        generateChars(134567, 134567);
        generateChars(134625, 134625);
        generateChars(134657, 134657);
        generateChars(134670, 134671);
        generateChars(134756, 134756);
        generateChars(134765, 134765);
        generateChars(134774, 134775);
        generateChars(134805, 134805);
        generateChars(134808, 134808);
        generateChars(134813, 134813);
        generateChars(134818, 134818);
        generateChars(134871, 134872);
        generateChars(134905, 134906);
        generateChars(134957, 134958);
        generateChars(134971, 134971);
        generateChars(134988, 134988);
        generateChars(135007, 135007);
        generateChars(135038, 135038);
        generateChars(135056, 135056);
        generateChars(135092, 135092);
        generateChars(135100, 135100);
        generateChars(135146, 135146);
        generateChars(135188, 135188);
        generateChars(135260, 135260);
        generateChars(135279, 135279);
        generateChars(135285, 135286);
        generateChars(135291, 135291);
        generateChars(135339, 135339);
        generateChars(135359, 135359);
        generateChars(135361, 135361);
        generateChars(135369, 135369);
        generateChars(135414, 135414);
        generateChars(135493, 135493);
        generateChars(135586, 135586);
        generateChars(135641, 135641);
        generateChars(135681, 135681);
        generateChars(135741, 135741);
        generateChars(135765, 135765);
        generateChars(135781, 135781);
        generateChars(135796, 135796);
        generateChars(135803, 135803);
        generateChars(135895, 135895);
        generateChars(135908, 135908);
        generateChars(135933, 135933);
        generateChars(135963, 135963);
        generateChars(135990, 135990);
        generateChars(136004, 136004);
        generateChars(136012, 136012);
        generateChars(136055, 136055);
        generateChars(136090, 136090);
        generateChars(136132, 136132);
        generateChars(136139, 136139);
        generateChars(136211, 136211);
        generateChars(136269, 136269);
        generateChars(136301, 136302);
        generateChars(136324, 136324);
        generateChars(136663, 136663);
        generateChars(136775, 136775);
        generateChars(136870, 136870);
        generateChars(136884, 136884);
        generateChars(136927, 136927);
        generateChars(136944, 136944);
        generateChars(136966, 136966);
        generateChars(137026, 137026);
        generateChars(137047, 137047);
        generateChars(137069, 137069);
        generateChars(137171, 137171);
        generateChars(137229, 137229);
        generateChars(137347, 137347);
        generateChars(137405, 137405);
        generateChars(137596, 137596);
        generateChars(137667, 137667);
        generateChars(138282, 138282);
        generateChars(138326, 138326);
        generateChars(138402, 138402);
        generateChars(138462, 138462);
        generateChars(138541, 138541);
        generateChars(138565, 138565);
        generateChars(138594, 138594);
        generateChars(138616, 138616);
        generateChars(138642, 138642);
        generateChars(138652, 138652);
        generateChars(138657, 138657);
        generateChars(138662, 138662);
        generateChars(138679, 138679);
        generateChars(138705, 138705);
        generateChars(138720, 138720);
        generateChars(138803, 138804);
        generateChars(138894, 138894);
        generateChars(139023, 139023);
        generateChars(139038, 139038);
        generateChars(139126, 139126);
        generateChars(139258, 139258);
        generateChars(139286, 139286);
        generateChars(139463, 139463);
        generateChars(139643, 139643);
        generateChars(139681, 139681);
        generateChars(139800, 139800);
        generateChars(140062, 140062);
        generateChars(140185, 140185);
        generateChars(140205, 140205);
        generateChars(140425, 140425);
        generateChars(140508, 140508);
        generateChars(140571, 140571);
        generateChars(140880, 140880);
        generateChars(141043, 141043);
        generateChars(141173, 141173);
        generateChars(141237, 141237);
        generateChars(141403, 141403);
        generateChars(141483, 141483);
        generateChars(141711, 141711);
        generateChars(142001, 142001);
        generateChars(142008, 142008);
        generateChars(142031, 142031);
        generateChars(142037, 142037);
        generateChars(142054, 142054);
        generateChars(142060, 142060);
        generateChars(142147, 142147);
        generateChars(142150, 142150);
        generateChars(142159, 142160);
        generateChars(142246, 142246);
        generateChars(142282, 142282);
        generateChars(142317, 142317);
        generateChars(142334, 142334);
        generateChars(142365, 142365);
        generateChars(142372, 142372);
        generateChars(142409, 142409);
        generateChars(142411, 142411);
        generateChars(142417, 142417);
        generateChars(142421, 142421);
        generateChars(142434, 142434);
        generateChars(142436, 142436);
        generateChars(142516, 142516);
        generateChars(142520, 142520);
        generateChars(142530, 142530);
        generateChars(142534, 142534);
        generateChars(142570, 142570);
        generateChars(142600, 142600);
        generateChars(142668, 142668);
        generateChars(142695, 142695);
        generateChars(142720, 142720);
        generateChars(142817, 142817);
        generateChars(143027, 143027);
        generateChars(143116, 143116);
        generateChars(143131, 143131);
        generateChars(143170, 143170);
        generateChars(143230, 143230);
        generateChars(143475, 143476);
        generateChars(143798, 143798);
        generateChars(143811, 143812);
        generateChars(143861, 143861);
        generateChars(143926, 143926);
        generateChars(144208, 144208);
        generateChars(144242, 144242);
        generateChars(144336, 144336);
        generateChars(144338, 144339);
        generateChars(144341, 144341);
        generateChars(144346, 144346);
        generateChars(144351, 144351);
        generateChars(144354, 144354);
        generateChars(144356, 144356);
        generateChars(144447, 144447);
        generateChars(144458, 144459);
        generateChars(144465, 144465);
        generateChars(144485, 144485);
        generateChars(144612, 144612);
        generateChars(144730, 144730);
        generateChars(144788, 144788);
        generateChars(144836, 144836);
        generateChars(144843, 144843);
        generateChars(144952, 144954);
        generateChars(144967, 144967);
        generateChars(145082, 145082);
        generateChars(145134, 145134);
        generateChars(145164, 145164);
        generateChars(145180, 145180);
        generateChars(145215, 145215);
        generateChars(145251, 145252);
        generateChars(145383, 145383);
        generateChars(145407, 145407);
        generateChars(145444, 145444);
        generateChars(145469, 145469);
        generateChars(145765, 145765);
        generateChars(145980, 145980);
        generateChars(146072, 146072);
        generateChars(146178, 146178);
        generateChars(146208, 146208);
        generateChars(146230, 146230);
        generateChars(146290, 146290);
        generateChars(146312, 146312);
        generateChars(146525, 146525);
        generateChars(146556, 146556);
        generateChars(146559, 146559);
        generateChars(146583, 146584);
        generateChars(146601, 146601);
        generateChars(146615, 146615);
        generateChars(146686, 146686);
        generateChars(146688, 146688);
        generateChars(146702, 146702);
        generateChars(146752, 146752);
        generateChars(146790, 146790);
        generateChars(146899, 146899);
        generateChars(146937, 146938);
        generateChars(146979, 146980);
        generateChars(147192, 147192);
        generateChars(147214, 147214);
        generateChars(147326, 147326);
        generateChars(147606, 147606);
        generateChars(147666, 147666);
        generateChars(147715, 147715);
        generateChars(147874, 147874);
        generateChars(147884, 147884);
        generateChars(147893, 147893);
        generateChars(147910, 147910);
        generateChars(147966, 147966);
        generateChars(147982, 147982);
        generateChars(148117, 148117);
        generateChars(148150, 148150);
        generateChars(148237, 148237);
        generateChars(148249, 148249);
        generateChars(148306, 148306);
        generateChars(148324, 148324);
        generateChars(148412, 148412);
        generateChars(148505, 148505);
        generateChars(148528, 148528);
        generateChars(148691, 148691);
        generateChars(148997, 148997);
        generateChars(149033, 149033);
        generateChars(149157, 149157);
        generateChars(149402, 149402);
        generateChars(149412, 149412);
        generateChars(149430, 149430);
        generateChars(149487, 149487);
        generateChars(149489, 149489);
        generateChars(149654, 149654);
        generateChars(149737, 149737);
        generateChars(149979, 149979);
        generateChars(150017, 150017);
        generateChars(150093, 150093);
        generateChars(150141, 150141);
        generateChars(150217, 150217);
        generateChars(150358, 150358);
        generateChars(150370, 150370);
        generateChars(150383, 150383);
        generateChars(150501, 150501);
        generateChars(150550, 150550);
        generateChars(150617, 150617);
        generateChars(150669, 150669);
        generateChars(150804, 150804);
        generateChars(150912, 150912);
        generateChars(150915, 150915);
        generateChars(150968, 150968);
        generateChars(151018, 151018);
        generateChars(151041, 151041);
        generateChars(151054, 151054);
        generateChars(151089, 151089);
        generateChars(151095, 151095);
        generateChars(151146, 151146);
        generateChars(151173, 151173);
        generateChars(151179, 151179);
        generateChars(151207, 151207);
        generateChars(151210, 151210);
        generateChars(151242, 151242);
        generateChars(151626, 151626);
        generateChars(151637, 151637);
        generateChars(151842, 151842);
        generateChars(151848, 151848);
        generateChars(151851, 151851);
        generateChars(151880, 151880);
        generateChars(151934, 151934);
        generateChars(151975, 151975);
        generateChars(151977, 151977);
        generateChars(152013, 152013);
        generateChars(152018, 152018);
        generateChars(152037, 152037);
        generateChars(152094, 152094);
        generateChars(152140, 152140);
        generateChars(152179, 152179);
        generateChars(152346, 152346);
        generateChars(152393, 152393);
        generateChars(152622, 152622);
        generateChars(152629, 152629);
        generateChars(152686, 152686);
        generateChars(152718, 152718);
        generateChars(152793, 152793);
        generateChars(152846, 152846);
        generateChars(152882, 152882);
        generateChars(152909, 152909);
        generateChars(152930, 152930);
        generateChars(152964, 152964);
        generateChars(152999, 153000);
        generateChars(153085, 153085);
        generateChars(153457, 153457);
        generateChars(153513, 153513);
        generateChars(153524, 153524);
        generateChars(153543, 153543);
        generateChars(153975, 153975);
        generateChars(154052, 154052);
        generateChars(154068, 154068);
        generateChars(154327, 154327);
        generateChars(154339, 154340);
        generateChars(154353, 154353);
        generateChars(154546, 154546);
        generateChars(154591, 154591);
        generateChars(154597, 154597);
        generateChars(154600, 154600);
        generateChars(154644, 154644);
        generateChars(154699, 154699);
        generateChars(154724, 154724);
        generateChars(154890, 154890);
        generateChars(155041, 155041);
        generateChars(155182, 155182);
        generateChars(155209, 155209);
        generateChars(155222, 155222);
        generateChars(155234, 155234);
        generateChars(155237, 155237);
        generateChars(155270, 155270);
        generateChars(155330, 155330);
        generateChars(155351, 155352);
        generateChars(155368, 155368);
        generateChars(155381, 155381);
        generateChars(155427, 155427);
        generateChars(155484, 155484);
        generateChars(155604, 155604);
        generateChars(155616, 155616);
        generateChars(155643, 155643);
        generateChars(155660, 155660);
        generateChars(155671, 155671);
        generateChars(155744, 155744);
        generateChars(155885, 155885);
        generateChars(156193, 156193);
        generateChars(156238, 156238);
        generateChars(156248, 156248);
        generateChars(156268, 156268);
        generateChars(156272, 156272);
        generateChars(156294, 156294);
        generateChars(156307, 156307);
        generateChars(156492, 156492);
        generateChars(156563, 156563);
        generateChars(156660, 156660);
        generateChars(156674, 156674);
        generateChars(156813, 156813);
        generateChars(157138, 157138);
        generateChars(157302, 157302);
        generateChars(157310, 157310);
        generateChars(157360, 157360);
        generateChars(157416, 157416);
        generateChars(157446, 157446);
        generateChars(157469, 157469);
        generateChars(157564, 157564);
        generateChars(157644, 157644);
        generateChars(157674, 157674);
        generateChars(157759, 157759);
        generateChars(157834, 157834);
        generateChars(157917, 157917);
        generateChars(157930, 157930);
        generateChars(157966, 157966);
        generateChars(158033, 158033);
        generateChars(158063, 158063);
        generateChars(158173, 158173);
        generateChars(158194, 158194);
        generateChars(158202, 158202);
        generateChars(158238, 158238);
        generateChars(158249, 158249);
        generateChars(158253, 158253);
        generateChars(158296, 158296);
        generateChars(158348, 158348);
        generateChars(158391, 158391);
        generateChars(158397, 158397);
        generateChars(158463, 158463);
        generateChars(158540, 158540);
        generateChars(158556, 158556);
        generateChars(158753, 158753);
        generateChars(158761, 158761);
        generateChars(158835, 158835);
        generateChars(158941, 158941);
        generateChars(159135, 159135);
        generateChars(159237, 159237);
        generateChars(159296, 159296);
        generateChars(159319, 159319);
        generateChars(159333, 159333);
        generateChars(159448, 159448);
        generateChars(159636, 159636);
        generateChars(159734, 159736);
        generateChars(159984, 159984);
        generateChars(159988, 159988);
        generateChars(160013, 160013);
        generateChars(160057, 160057);
        generateChars(160351, 160351);
        generateChars(160389, 160389);
        generateChars(160516, 160516);
        generateChars(160625, 160625);
        generateChars(160730, 160731);
        generateChars(160766, 160766);
        generateChars(160784, 160784);
        generateChars(160841, 160841);
        generateChars(160902, 160902);
        generateChars(160957, 160957);
        generateChars(161300, 161301);
        generateChars(161329, 161329);
        generateChars(161412, 161412);
        generateChars(161427, 161427);
        generateChars(161550, 161550);
        generateChars(161571, 161571);
        generateChars(161601, 161601);
        generateChars(161618, 161618);
        generateChars(161776, 161776);
        generateChars(161970, 161970);
        generateChars(162181, 162181);
        generateChars(162208, 162208);
        generateChars(162215, 162215);
        generateChars(162366, 162367);
        generateChars(162403, 162403);
        generateChars(162436, 162436);
        generateChars(162602, 162602);
        generateChars(162713, 162713);
        generateChars(162730, 162730);
        generateChars(162739, 162739);
        generateChars(162750, 162750);
        generateChars(162759, 162759);
        generateChars(163000, 163000);
        generateChars(163232, 163232);
        generateChars(163344, 163344);
        generateChars(163503, 163503);
        generateChars(163572, 163572);
        generateChars(163767, 163767);
        generateChars(163777, 163777);
        generateChars(163820, 163820);
        generateChars(163827, 163827);
        generateChars(163833, 163833);
        generateChars(163978, 163978);
        generateChars(164027, 164027);
        generateChars(164030, 164031);
        generateChars(164037, 164037);
        generateChars(164073, 164073);
        generateChars(164080, 164080);
        generateChars(164180, 164180);
        generateChars(164189, 164189);
        generateChars(164359, 164359);
        generateChars(164471, 164471);
        generateChars(164482, 164482);
        generateChars(164557, 164557);
        generateChars(164578, 164578);
        generateChars(164595, 164595);
        generateChars(164733, 164733);
        generateChars(164746, 164746);
        generateChars(164813, 164813);
        generateChars(164872, 164872);
        generateChars(164876, 164876);
        generateChars(164949, 164949);
        generateChars(164968, 164968);
        generateChars(164999, 164999);
        generateChars(165227, 165227);
        generateChars(165269, 165269);
        generateChars(165320, 165321);
        generateChars(165496, 165496);
        generateChars(165525, 165525);
        generateChars(165591, 165591);
        generateChars(165626, 165626);
        generateChars(165802, 165802);
        generateChars(165856, 165856);
        generateChars(166033, 166033);
        generateChars(166214, 166214);
        generateChars(166216, 166217);
        generateChars(166222, 166222);
        generateChars(166251, 166251);
        generateChars(166279, 166280);
        generateChars(166305, 166305);
        generateChars(166330, 166331);
        generateChars(166336, 166336);
        generateChars(166415, 166415);
        generateChars(166430, 166430);
        generateChars(166441, 166441);
        generateChars(166467, 166467);
        generateChars(166513, 166513);
        generateChars(166553, 166553);
        generateChars(166605, 166605);
        generateChars(166621, 166621);
        generateChars(166628, 166628);
        generateChars(166726, 166726);
        generateChars(166729, 166729);
        generateChars(166734, 166734);
        generateChars(166849, 166849);
        generateChars(166895, 166895);
        generateChars(166974, 166974);
        generateChars(166983, 166983);
        generateChars(166989, 166991);
        generateChars(166993, 166993);
        generateChars(166995, 166996);
        generateChars(167114, 167114);
        generateChars(167117, 167117);
        generateChars(167122, 167122);
        generateChars(167184, 167184);
        generateChars(167281, 167281);
        generateChars(167321, 167321);
        generateChars(167419, 167419);
        generateChars(167439, 167439);
        generateChars(167455, 167455);
        generateChars(167478, 167478);
        generateChars(167481, 167481);
        generateChars(167561, 167561);
        generateChars(167577, 167577);
        generateChars(167655, 167655);
        generateChars(167659, 167659);
        generateChars(167670, 167670);
        generateChars(167730, 167730);
        generateChars(167928, 167928);
        generateChars(168540, 168540);
        generateChars(168608, 168608);
        generateChars(168625, 168625);
        generateChars(169053, 169053);
        generateChars(169086, 169086);
        generateChars(169104, 169104);
        generateChars(169146, 169146);
        generateChars(169189, 169189);
        generateChars(169423, 169423);
        generateChars(169599, 169599);
        generateChars(169640, 169640);
        generateChars(169644, 169644);
        generateChars(169705, 169705);
        generateChars(169712, 169712);
        generateChars(169732, 169732);
        generateChars(169753, 169753);
        generateChars(169776, 169776);
        generateChars(169808, 169809);
        generateChars(169996, 169997);
        generateChars(170000, 170000);
        generateChars(170182, 170182);
        generateChars(170498, 170498);
        generateChars(170610, 170610);
        generateChars(171377, 171377);
        generateChars(171416, 171416);
        generateChars(171475, 171475);
        generateChars(171477, 171477);
        generateChars(171483, 171483);
        generateChars(171520, 172287);
        generateChars(172432, 172432);
        generateChars(172513, 172513);
        generateChars(172616, 172616);
        generateChars(172938, 172938);
        generateChars(172940, 172940);
        generateChars(173086, 173086);
        generateChars(173111, 173111);
        generateChars(173456, 173456);
        generateChars(173553, 173553);
        generateChars(173570, 173570);
        generateChars(173586, 173586);
        generateChars(173594, 173594);
        generateChars(173642, 173642);
        generateChars(173746, 173746);
        generateChars(173783, 173791);
        generateChars(173878, 173878);
        generateChars(173981, 173981);
        generateChars(174045, 174045);
        generateChars(174136, 174136);
        generateChars(174141, 174141);
        generateChars(174254, 174254);
        generateChars(174301, 174301);
        generateChars(174331, 174331);
        generateChars(174359, 174359);
        generateChars(174448, 174448);
        generateChars(174599, 174599);
        generateChars(174602, 174602);
        generateChars(174640, 174640);
        generateChars(174646, 174646);
        generateChars(174680, 174680);
        generateChars(174946, 174946);
        generateChars(175027, 175027);
        generateChars(175706, 175706);
        generateChars(175824, 175824);
        generateChars(176020, 176020);
        generateChars(176034, 176034);
        generateChars(176086, 176086);
        generateChars(176337, 176337);
        generateChars(176421, 176421);
        generateChars(176423, 176424);
        generateChars(176439, 176440);
        generateChars(176621, 176621);
        generateChars(176705, 176705);
        generateChars(176827, 176827);
        generateChars(176896, 176896);
        generateChars(176936, 176936);
        generateChars(176991, 176991);
        generateChars(176994, 176995);
        generateChars(177007, 177007);
        generateChars(177010, 177010);
        generateChars(177019, 177019);
        generateChars(177021, 177021);
        generateChars(177156, 177156);
        generateChars(177168, 177168);
        generateChars(177171, 177171);
        generateChars(177249, 177249);
        generateChars(177314, 177314);
        generateChars(177383, 177383);
        generateChars(177385, 177385);
        generateChars(177391, 177391);
        generateChars(177398, 177398);
        generateChars(177401, 177401);
        generateChars(177421, 177422);
        generateChars(177462, 177462);
        generateChars(177539, 177539);
        generateChars(177556, 177556);
        generateChars(177560, 177560);
        generateChars(177582, 177583);
        generateChars(177587, 177587);
        generateChars(177589, 177589);
        generateChars(177593, 177593);
        generateChars(177639, 177639);
        generateChars(177643, 177643);
        generateChars(177648, 177648);
        generateChars(177652, 177652);
        generateChars(177692, 177693);
        generateChars(177700, 177700);
        generateChars(177702, 177704);
        generateChars(177706, 177706);
        generateChars(177708, 177708);
        generateChars(177801, 177801);
        generateChars(177810, 177810);
        generateChars(177812, 177814);
        generateChars(177837, 177837);
        generateChars(177882, 177882);
        generateChars(177886, 177886);
        generateChars(177897, 177897);
        generateChars(177901, 177901);
        generateChars(177910, 177910);
        generateChars(177960, 177960);
        generateChars(177973, 177977);
        generateChars(177984, 178205);
        generateChars(178360, 178360);
        generateChars(178374, 178374);
        generateChars(178378, 178378);
        generateChars(178671, 178671);
        generateChars(178779, 178779);
        generateChars(178803, 178803);
        generateChars(178817, 178817);
        generateChars(178840, 178840);
        generateChars(178887, 178887);
        generateChars(178999, 178999);
        generateChars(179039, 179039);
        generateChars(179042, 179042);
        generateChars(179050, 179050);
        generateChars(179068, 179068);
        generateChars(179075, 179075);
        generateChars(179227, 179227);
        generateChars(179233, 179233);
        generateChars(179248, 179248);
        generateChars(179366, 179366);
        generateChars(179544, 179544);
        generateChars(179575, 179575);
        generateChars(179580, 179580);
        generateChars(179591, 179591);
        generateChars(179703, 179703);
        generateChars(179753, 179753);
        generateChars(179997, 179997);
        generateChars(180265, 180266);
        generateChars(180353, 180353);
        generateChars(180393, 180393);
        generateChars(180416, 180416);
        generateChars(180426, 180426);
        generateChars(180693, 180693);
        generateChars(180697, 180697);
        generateChars(180702, 180702);
        generateChars(180729, 180729);
        generateChars(180860, 180860);
        generateChars(180872, 180872);
        generateChars(180892, 180892);
        generateChars(180900, 180900);
        generateChars(180918, 180918);
        generateChars(181015, 181015);
        generateChars(181083, 181083);
        generateChars(181089, 181089);
        generateChars(181092, 181092);
        generateChars(181126, 181126);
        generateChars(181335, 181335);
        generateChars(181384, 181384);
        generateChars(181396, 181396);
        generateChars(181399, 181399);
        generateChars(181570, 181570);
        generateChars(181643, 181643);
        generateChars(181662, 181662);
        generateChars(181779, 181779);
        generateChars(181784, 181784);
        generateChars(181793, 181794);
        generateChars(181801, 181801);
        generateChars(181803, 181805);
        generateChars(181807, 181807);
        generateChars(181813, 181813);
        generateChars(181826, 181826);
        generateChars(181834, 181835);
        generateChars(182060, 182060);
        generateChars(182063, 182063);
        generateChars(182175, 182175);
        generateChars(182209, 182209);
        generateChars(182252, 182252);
        generateChars(182269, 182269);
        generateChars(182294, 182294);
        generateChars(182410, 182410);
        generateChars(182464, 182464);
        generateChars(182489, 182489);
        generateChars(182494, 182494);
        generateChars(182497, 182497);
        generateChars(182515, 182515);
        generateChars(182535, 182535);
        generateChars(182538, 182538);
        generateChars(182557, 182557);
        generateChars(182704, 182704);
        generateChars(182720, 182720);
        generateChars(182786, 182786);
        generateChars(182798, 182798);
        generateChars(182909, 182909);
        generateChars(182925, 182925);
        generateChars(182953, 182953);
        generateChars(182994, 182994);
        generateChars(183081, 183081);
        generateChars(183083, 183083);
        generateChars(183085, 183086);
        generateChars(183089, 183089);
        generateChars(183096, 183099);
        generateChars(183103, 183103);
        generateChars(183105, 183105);
        generateChars(183114, 183114);
        generateChars(183118, 183118);
        generateChars(183130, 183131);
        generateChars(183139, 183140);
        generateChars(183145, 183145);
        generateChars(183148, 183148);
        generateChars(183151, 183151);
        generateChars(183155, 183155);
        generateChars(183158, 183158);
        generateChars(183160, 183160);
        generateChars(183164, 183164);
        generateChars(183204, 183204);
        generateChars(183217, 183217);
        generateChars(183220, 183220);
        generateChars(183231, 183232);
        generateChars(183246, 183246);
        generateChars(183382, 183382);
        generateChars(183391, 183391);
        generateChars(183473, 183473);
        generateChars(183485, 183485);
        generateChars(183495, 183495);
        generateChars(183541, 183542);
        generateChars(183549, 183549);
        generateChars(183551, 183551);
        generateChars(183554, 183555);
        generateChars(183562, 183562);
        generateChars(183568, 183568);
        generateChars(183688, 183689);
        generateChars(183691, 183691);
        generateChars(183693, 183693);
        generateChars(183695, 183696);
        generateChars(183711, 183712);
        generateChars(183720, 183720);
        generateChars(183725, 183726);
        generateChars(183731, 183731);
        generateChars(183765, 183765);
        generateChars(183813, 183813);
        generateChars(183832, 183832);
        generateChars(183834, 183834);
        generateChars(183843, 183843);
        generateChars(183846, 183846);
        generateChars(183850, 183850);
        generateChars(183930, 183930);
        generateChars(183932, 183932);
        generateChars(183944, 183944);
        generateChars(183955, 183955);
        generateChars(184925, 184925);
        generateChars(185218, 185218);
        generateChars(185668, 185668);
        generateChars(187466, 187466);
        generateChars(187658, 187658);
        generateChars(188436, 188436);
        generateChars(189035, 189036);
        generateChars(189039, 189039);
        generateChars(189466, 189466);
        generateChars(189480, 189480);
        generateChars(189698, 189698);
        generateChars(189701, 189701);
        generateChars(189706, 189706);
        generateChars(189873, 189873);
        generateChars(190026, 190027);
        generateChars(190324, 190325);
        generateChars(190329, 190329);
        generateChars(190706, 190707);
        generateChars(190770, 190770);
        generateChars(190965, 190965);
        generateChars(191035, 191035);
        generateChars(191069, 191070);
        generateChars(191141, 191141);
        generateChars(191261, 191261);
        generateChars(191265, 191265);
        generateChars(191334, 191334);
        generateChars(191336, 191336);
        generateChars(191449, 191449);
        generateChars(191472, 192093);
        generateChars(194692, 194692);
        generateChars(194742, 194742);
        generateChars(196608, 196991);
        generateChars(197083, 197083);
        generateChars(197091, 197091);
        generateChars(197312, 197312);
        generateChars(197376, 197376);
        generateChars(197378, 197378);
        generateChars(198535, 198535);
        generateChars(198616, 198616);
        generateChars(198744, 198744);
        generateChars(198907, 198907);
        generateChars(198933, 198933);
        generateChars(199359, 199359);
        generateChars(199676, 199676);
        generateChars(199794, 199794);
        generateChars(200029, 200030);
        generateChars(200039, 200039);
        generateChars(200052, 200052);
        generateChars(200060, 200060);
        generateChars(200074, 200074);
        generateChars(200165, 200165);
        generateChars(200300, 200300);
        generateChars(200413, 200414);
        generateChars(200535, 200535);
        generateChars(200538, 200538);
        generateChars(200564, 200564);
        generateChars(200573, 200573);
        generateChars(200585, 200585);
        generateChars(200608, 200608);
        generateChars(200619, 200619);
        generateChars(200812, 200812);
        generateChars(200848, 200848);
        generateChars(200873, 200873);
        generateChars(200881, 200881);
        generateChars(200938, 200938);
        generateChars(200945, 200945);
        generateChars(200954, 200954);
        generateChars(201035, 201035);
        generateChars(201051, 201051);
        generateChars(201187, 201187);
        generateChars(201201, 201201);
        generateChars(201300, 201300);
        generateChars(201417, 201417);
        generateChars(201518, 201518);
        generateChars(201660, 201660);
        generateChars(201727, 201727);
        generateChars(202126, 202126);
        generateChars(202130, 202130);
        generateChars(203666, 203666);
        generateChars(203676, 203676);
        generateChars(203903, 203903);
        generateChars(204063, 204063);
        generateChars(204081, 204081);
        generateChars(204295, 204295);
        generateChars(204365, 204365);
        generateChars(204803, 204803);
        generateChars(204942, 204942);
        generateChars(205335, 205335);
        generateChars(205540, 205540);
        generateChars(205659, 205659);
        generateChars(205677, 205677);
        generateChars(205695, 205695);
        generateChars(917504, 917631);
        generateChars(917760, 917999);
        generateChars(1048574, 1048575);

    }

    private void createFontAtlas() {

        atlasImage = new BufferedImage(2048, 2048, BufferedImage.TYPE_INT_ARGB);

        this.generateAllMinecraftChars();

        File outputFile = new File("output.png");

        try {
            ImageIO.write(atlasImage, "png", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        loadTexture();

        this.glyphRenderOffset = this.maxGlyphHeight - ((this.face.getSize().getMetrics().getAscender()) - this.baseAscender);

    }

    private int
            atlasX = 0,
            atlasY = 0;

    private void generateChars(int from, int to) {

        for (int c = from; c <= to; c++) {

            if(face.getCharIndex(c) == 0)
                continue;

            FTLoadFlags ftLoadFlags = new FTLoadFlags();
            ftLoadFlags.set(FTLoad.RENDER);

            face.loadChar(c, ftLoadFlags);

            FTGlyphSlot glyph = face.getGlyph();
            FTBitmap bitmap = glyph.getBitmap();

            if(c == 'A')
                this.baseAscender = glyph.getBitmap().getRows();

            int charWidth = glyph.getAdvanceX() >> 6;
            int charHeight = this.maxGlyphHeight;

            if (atlasX + charWidth + 4 > atlasImage.getWidth()) {
                atlasX = 0;
                atlasY += charHeight + 4;
            }

            if (atlasY + charHeight + 4 > atlasImage.getHeight()) {
                System.err.println("atlas overflow");
                break;
            }

            drawGlyphToAtlas(bitmap, glyph, atlasX, atlasY);

            charactersMap.put(c, new int[]{atlasX, atlasY, charWidth, charHeight});

            atlasX += charWidth + 4;

        }

    }

    private void drawGlyphToAtlas(FTBitmap bitmap, FTGlyphSlot glyph, int atlasX, int atlasY) {

        ByteBuffer buffer = bitmap.getBuffer();

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getRows();

        int drawY = atlasY + (face.getSize().getMetrics().getAscender()) - glyph.getBitmapTop();
        int drawX = atlasX + glyph.getBitmapLeft();

        for (int j = 0; j < bitmapHeight; j++) {

            for (int i = 0; i < bitmapWidth; i++) {

                int alpha = buffer.get(j * bitmap.getPitch() + i) & 0xFF;

                if (alpha > 0) {

                    int color = (alpha << 24) | 0x00FFFFFF;

                    if (drawX + i >= 0 && drawX + i < atlasImage.getWidth() && drawY + j >= 0 && drawY + j < atlasImage.getHeight())
                        atlasImage.setRGB(drawX + i, drawY + j, color);

                }

            }

        }

    }

    public void loadTexture() {
        try {
            ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
            ImageIO.write(atlasImage, "png", BAOS);
            byte[] bytes = BAOS.toByteArray();
            ByteBuffer data = BufferUtils.createByteBuffer(bytes.length).put(bytes);
            data.flip();
            loadedTexture = new DynamicTexture(NativeImage.read(data));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getStringWidth(String text) {

        AtomicInteger width = new AtomicInteger();

        text = removeParagraphPairs(text);

        text.codePoints().forEach(c -> {
            if(charactersMap.containsKey(c))
                width.addAndGet(charactersMap.get(c)[2]);
            else width.addAndGet(charactersMap.get((int) '?')[2]);
        });

        return width.get();

    }

    public int getBaseAscender() {
        return this.baseAscender;
    }

    public void renderText(String text, float x, float y, float[] color) {

        y = Window.gameWindowHeight - y - this.glyphRenderOffset;

        float currentX = x;

        for (char c : text.toCharArray()) {

            if (c == '\u00A7') continue;

            int[] charData = charactersMap.get((int)c);

            if (charData == null) {
                charData = charactersMap.get((int)'?');
                if (charData == null) continue;
            }

            int charWidth = charData[2];

            Render.drawCharacter(c, currentX, y, color, 255, this);

            currentX += charWidth;

        }

    }

    public void renderHVCenteredCharacter(String text, float x, float y, float[] color) {
        if (text.isEmpty()) return;
        renderText(text, x - (float) getStringWidth(String.valueOf(text.charAt(0))) / 2, y - (float) getBaseAscender() / 2, color);
    }

    public void renderVCenteredText(String text, float x, float y, float[] color) {
        renderText(text, x, y - (float) this.getBaseAscender() / 2, color);
    }

    public void renderHVCenteredText(String text, float x, float y, float[] color) {
        renderText(text, x - (float) getStringWidth(text) / 2, y - (float) getBaseAscender() / 2, color);
    }

    public static String removeParagraphPairs(String input) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '\u00A7' && i + 1 < input.length()) {
                i++;
            } else {
                result.append(input.charAt(i));
            }
        }
        return result.toString();
    }

    public void renderOutlinedText(String text, float x, float y, float[] textColor, float[] outlineColor, float alpha) {
        if(textColor.length == 4) textColor = new float[]{textColor[0], textColor[1], textColor[2], textColor[3] / 255 * (alpha / 255) * 255}; else textColor = new float[]{textColor[0], textColor[1], textColor[2], alpha};
        if(outlineColor.length == 4) outlineColor = new float[]{outlineColor[0], outlineColor[1], outlineColor[2], outlineColor[3] / 255 * (alpha / 255) * 255}; else outlineColor = new float[]{outlineColor[0], outlineColor[1], outlineColor[2], alpha};
        this.renderText(text, x - 1, y, outlineColor); this.renderText(text, x + 1, y, outlineColor); this.renderText(text, x, y + 1, outlineColor); this.renderText(text, x, y - 1, outlineColor);
        this.renderText(text, x + 1, y + 1, outlineColor); this.renderText(text, x - 1, y - 1, outlineColor); this.renderText(text, x + 1, y - 1, outlineColor); this.renderText(text, x - 1, y + 1, outlineColor);
        this.renderText(text, x, y, textColor);
    }

    public void renderTextWithShadow(String text, float x, float y, float[] color) {
        renderText(text, x + 1, y + 1, sisicat.main.utilities.Color.c12);
        renderText(text, x, y, color);
    }

    @Override
    public void close() {

        if (library != null) library.done();
        if (face != null) face.done();

    }

}