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

        this.face.setPixelSizes(0, (int) fontSize);

        this.maxGlyphHeight = (this.face.getSize().getMetrics().getAscender() - this.face.getSize().getMetrics().getDescender());

        this.createFontAtlas();

    }

    private void generateAllMinecraftChars() {

        this.generateChars(0, 55295);
        this.generateChars(63744, 65533);
        this.generateChars(65536, 73727);
        this.generateChars(74650, 74751);
        this.generateChars(74863, 74863);
        this.generateChars(74869, 74879);
        this.generateChars(75076, 77823);
        this.generateChars(78895, 78943);
        this.generateChars(82939, 82943);
        this.generateChars(83527, 92159);
        this.generateChars(92729, 94207);
        this.generateChars(100344, 100351);
        this.generateChars(101120, 101631);
        this.generateChars(101641, 131069);
        this.generateChars(131072, 132927);
        this.generateChars(132943, 132943);
        this.generateChars(132985, 132985);
        this.generateChars(133033, 133033);
        this.generateChars(133037, 133037);
        this.generateChars(133123, 133123);
        this.generateChars(133127, 133127);
        this.generateChars(133149, 133149);
        this.generateChars(133178, 133178);
        this.generateChars(133269, 133269);
        this.generateChars(133305, 133305);
        this.generateChars(133500, 133500);
        this.generateChars(133533, 133533);
        this.generateChars(133594, 133594);
        this.generateChars(133843, 133843);
        this.generateChars(133917, 133917);
        this.generateChars(134047, 134047);
        this.generateChars(134103, 134103);
        this.generateChars(134209, 134209);
        this.generateChars(134227, 134227);
        this.generateChars(134264, 134264);
        this.generateChars(134286, 134286);
        this.generateChars(134294, 134294);
        this.generateChars(134335, 134335);
        this.generateChars(134351, 134352);
        this.generateChars(134356, 134357);
        this.generateChars(134421, 134421);
        this.generateChars(134469, 134469);
        this.generateChars(134493, 134493);
        this.generateChars(134513, 134513);
        this.generateChars(134524, 134524);
        this.generateChars(134527, 134527);
        this.generateChars(134567, 134567);
        this.generateChars(134625, 134625);
        this.generateChars(134657, 134657);
        this.generateChars(134670, 134671);
        this.generateChars(134756, 134756);
        this.generateChars(134765, 134765);
        this.generateChars(134774, 134775);
        this.generateChars(134805, 134805);
        this.generateChars(134808, 134808);
        this.generateChars(134813, 134813);
        this.generateChars(134818, 134818);
        this.generateChars(134871, 134872);
        this.generateChars(134905, 134906);
        this.generateChars(134957, 134958);
        this.generateChars(134971, 134971);
        this.generateChars(134988, 134988);
        this.generateChars(135007, 135007);
        this.generateChars(135038, 135038);
        this.generateChars(135056, 135056);
        this.generateChars(135092, 135092);
        this.generateChars(135100, 135100);
        this.generateChars(135146, 135146);
        this.generateChars(135188, 135188);
        this.generateChars(135260, 135260);
        this.generateChars(135279, 135279);
        this.generateChars(135285, 135286);
        this.generateChars(135291, 135291);
        this.generateChars(135339, 135339);
        this.generateChars(135359, 135359);
        this.generateChars(135361, 135361);
        this.generateChars(135369, 135369);
        this.generateChars(135414, 135414);
        this.generateChars(135493, 135493);
        this.generateChars(135586, 135586);
        this.generateChars(135641, 135641);
        this.generateChars(135681, 135681);
        this.generateChars(135741, 135741);
        this.generateChars(135765, 135765);
        this.generateChars(135781, 135781);
        this.generateChars(135796, 135796);
        this.generateChars(135803, 135803);
        this.generateChars(135895, 135895);
        this.generateChars(135908, 135908);
        this.generateChars(135933, 135933);
        this.generateChars(135963, 135963);
        this.generateChars(135990, 135990);
        this.generateChars(136004, 136004);
        this.generateChars(136012, 136012);
        this.generateChars(136055, 136055);
        this.generateChars(136090, 136090);
        this.generateChars(136132, 136132);
        this.generateChars(136139, 136139);
        this.generateChars(136211, 136211);
        this.generateChars(136269, 136269);
        this.generateChars(136301, 136302);
        this.generateChars(136324, 136324);
        this.generateChars(136663, 136663);
        this.generateChars(136775, 136775);
        this.generateChars(136870, 136870);
        this.generateChars(136884, 136884);
        this.generateChars(136927, 136927);
        this.generateChars(136944, 136944);
        this.generateChars(136966, 136966);
        this.generateChars(137026, 137026);
        this.generateChars(137047, 137047);
        this.generateChars(137069, 137069);
        this.generateChars(137171, 137171);
        this.generateChars(137229, 137229);
        this.generateChars(137347, 137347);
        this.generateChars(137405, 137405);
        this.generateChars(137596, 137596);
        this.generateChars(137667, 137667);
        this.generateChars(138282, 138282);
        this.generateChars(138326, 138326);
        this.generateChars(138402, 138402);
        this.generateChars(138462, 138462);
        this.generateChars(138541, 138541);
        this.generateChars(138565, 138565);
        this.generateChars(138594, 138594);
        this.generateChars(138616, 138616);
        this.generateChars(138642, 138642);
        this.generateChars(138652, 138652);
        this.generateChars(138657, 138657);
        this.generateChars(138662, 138662);
        this.generateChars(138679, 138679);
        this.generateChars(138705, 138705);
        this.generateChars(138720, 138720);
        this.generateChars(138803, 138804);
        this.generateChars(138894, 138894);
        this.generateChars(139023, 139023);
        this.generateChars(139038, 139038);
        this.generateChars(139126, 139126);
        this.generateChars(139258, 139258);
        this.generateChars(139286, 139286);
        this.generateChars(139463, 139463);
        this.generateChars(139643, 139643);
        this.generateChars(139681, 139681);
        this.generateChars(139800, 139800);
        this.generateChars(140062, 140062);
        this.generateChars(140185, 140185);
        this.generateChars(140205, 140205);
        this.generateChars(140425, 140425);
        this.generateChars(140508, 140508);
        this.generateChars(140571, 140571);
        this.generateChars(140880, 140880);
        this.generateChars(141043, 141043);
        this.generateChars(141173, 141173);
        this.generateChars(141237, 141237);
        this.generateChars(141403, 141403);
        this.generateChars(141483, 141483);
        this.generateChars(141711, 141711);
        this.generateChars(142001, 142001);
        this.generateChars(142008, 142008);
        this.generateChars(142031, 142031);
        this.generateChars(142037, 142037);
        this.generateChars(142054, 142054);
        this.generateChars(142060, 142060);
        this.generateChars(142147, 142147);
        this.generateChars(142150, 142150);
        this.generateChars(142159, 142160);
        this.generateChars(142246, 142246);
        this.generateChars(142282, 142282);
        this.generateChars(142317, 142317);
        this.generateChars(142334, 142334);
        this.generateChars(142365, 142365);
        this.generateChars(142372, 142372);
        this.generateChars(142409, 142409);
        this.generateChars(142411, 142411);
        this.generateChars(142417, 142417);
        this.generateChars(142421, 142421);
        this.generateChars(142434, 142434);
        this.generateChars(142436, 142436);
        this.generateChars(142516, 142516);
        this.generateChars(142520, 142520);
        this.generateChars(142530, 142530);
        this.generateChars(142534, 142534);
        this.generateChars(142570, 142570);
        this.generateChars(142600, 142600);
        this.generateChars(142668, 142668);
        this.generateChars(142695, 142695);
        this.generateChars(142720, 142720);
        this.generateChars(142817, 142817);
        this.generateChars(143027, 143027);
        this.generateChars(143116, 143116);
        this.generateChars(143131, 143131);
        this.generateChars(143170, 143170);
        this.generateChars(143230, 143230);
        this.generateChars(143475, 143476);
        this.generateChars(143798, 143798);
        this.generateChars(143811, 143812);
        this.generateChars(143861, 143861);
        this.generateChars(143926, 143926);
        this.generateChars(144208, 144208);
        this.generateChars(144242, 144242);
        this.generateChars(144336, 144336);
        this.generateChars(144338, 144339);
        this.generateChars(144341, 144341);
        this.generateChars(144346, 144346);
        this.generateChars(144351, 144351);
        this.generateChars(144354, 144354);
        this.generateChars(144356, 144356);
        this.generateChars(144447, 144447);
        this.generateChars(144458, 144459);
        this.generateChars(144465, 144465);
        this.generateChars(144485, 144485);
        this.generateChars(144612, 144612);
        this.generateChars(144730, 144730);
        this.generateChars(144788, 144788);
        this.generateChars(144836, 144836);
        this.generateChars(144843, 144843);
        this.generateChars(144952, 144954);
        this.generateChars(144967, 144967);
        this.generateChars(145082, 145082);
        this.generateChars(145134, 145134);
        this.generateChars(145164, 145164);
        this.generateChars(145180, 145180);
        this.generateChars(145215, 145215);
        this.generateChars(145251, 145252);
        this.generateChars(145383, 145383);
        this.generateChars(145407, 145407);
        this.generateChars(145444, 145444);
        this.generateChars(145469, 145469);
        this.generateChars(145765, 145765);
        this.generateChars(145980, 145980);
        this.generateChars(146072, 146072);
        this.generateChars(146178, 146178);
        this.generateChars(146208, 146208);
        this.generateChars(146230, 146230);
        this.generateChars(146290, 146290);
        this.generateChars(146312, 146312);
        this.generateChars(146525, 146525);
        this.generateChars(146556, 146556);
        this.generateChars(146559, 146559);
        this.generateChars(146583, 146584);
        this.generateChars(146601, 146601);
        this.generateChars(146615, 146615);
        this.generateChars(146686, 146686);
        this.generateChars(146688, 146688);
        this.generateChars(146702, 146702);
        this.generateChars(146752, 146752);
        this.generateChars(146790, 146790);
        this.generateChars(146899, 146899);
        this.generateChars(146937, 146938);
        this.generateChars(146979, 146980);
        this.generateChars(147192, 147192);
        this.generateChars(147214, 147214);
        this.generateChars(147326, 147326);
        this.generateChars(147606, 147606);
        this.generateChars(147666, 147666);
        this.generateChars(147715, 147715);
        this.generateChars(147874, 147874);
        this.generateChars(147884, 147884);
        this.generateChars(147893, 147893);
        this.generateChars(147910, 147910);
        this.generateChars(147966, 147966);
        this.generateChars(147982, 147982);
        this.generateChars(148117, 148117);
        this.generateChars(148150, 148150);
        this.generateChars(148237, 148237);
        this.generateChars(148249, 148249);
        this.generateChars(148306, 148306);
        this.generateChars(148324, 148324);
        this.generateChars(148412, 148412);
        this.generateChars(148505, 148505);
        this.generateChars(148528, 148528);
        this.generateChars(148691, 148691);
        this.generateChars(148997, 148997);
        this.generateChars(149033, 149033);
        this.generateChars(149157, 149157);
        this.generateChars(149402, 149402);
        this.generateChars(149412, 149412);
        this.generateChars(149430, 149430);
        this.generateChars(149487, 149487);
        this.generateChars(149489, 149489);
        this.generateChars(149654, 149654);
        this.generateChars(149737, 149737);
        this.generateChars(149979, 149979);
        this.generateChars(150017, 150017);
        this.generateChars(150093, 150093);
        this.generateChars(150141, 150141);
        this.generateChars(150217, 150217);
        this.generateChars(150358, 150358);
        this.generateChars(150370, 150370);
        this.generateChars(150383, 150383);
        this.generateChars(150501, 150501);
        this.generateChars(150550, 150550);
        this.generateChars(150617, 150617);
        this.generateChars(150669, 150669);
        this.generateChars(150804, 150804);
        this.generateChars(150912, 150912);
        this.generateChars(150915, 150915);
        this.generateChars(150968, 150968);
        this.generateChars(151018, 151018);
        this.generateChars(151041, 151041);
        this.generateChars(151054, 151054);
        this.generateChars(151089, 151089);
        this.generateChars(151095, 151095);
        this.generateChars(151146, 151146);
        this.generateChars(151173, 151173);
        this.generateChars(151179, 151179);
        this.generateChars(151207, 151207);
        this.generateChars(151210, 151210);
        this.generateChars(151242, 151242);
        this.generateChars(151626, 151626);
        this.generateChars(151637, 151637);
        this.generateChars(151842, 151842);
        this.generateChars(151848, 151848);
        this.generateChars(151851, 151851);
        this.generateChars(151880, 151880);
        this.generateChars(151934, 151934);
        this.generateChars(151975, 151975);
        this.generateChars(151977, 151977);
        this.generateChars(152013, 152013);
        this.generateChars(152018, 152018);
        this.generateChars(152037, 152037);
        this.generateChars(152094, 152094);
        this.generateChars(152140, 152140);
        this.generateChars(152179, 152179);
        this.generateChars(152346, 152346);
        this.generateChars(152393, 152393);
        this.generateChars(152622, 152622);
        this.generateChars(152629, 152629);
        this.generateChars(152686, 152686);
        this.generateChars(152718, 152718);
        this.generateChars(152793, 152793);
        this.generateChars(152846, 152846);
        this.generateChars(152882, 152882);
        this.generateChars(152909, 152909);
        this.generateChars(152930, 152930);
        this.generateChars(152964, 152964);
        this.generateChars(152999, 153000);
        this.generateChars(153085, 153085);
        this.generateChars(153457, 153457);
        this.generateChars(153513, 153513);
        this.generateChars(153524, 153524);
        this.generateChars(153543, 153543);
        this.generateChars(153975, 153975);
        this.generateChars(154052, 154052);
        this.generateChars(154068, 154068);
        this.generateChars(154327, 154327);
        this.generateChars(154339, 154340);
        this.generateChars(154353, 154353);
        this.generateChars(154546, 154546);
        this.generateChars(154591, 154591);
        this.generateChars(154597, 154597);
        this.generateChars(154600, 154600);
        this.generateChars(154644, 154644);
        this.generateChars(154699, 154699);
        this.generateChars(154724, 154724);
        this.generateChars(154890, 154890);
        this.generateChars(155041, 155041);
        this.generateChars(155182, 155182);
        this.generateChars(155209, 155209);
        this.generateChars(155222, 155222);
        this.generateChars(155234, 155234);
        this.generateChars(155237, 155237);
        this.generateChars(155270, 155270);
        this.generateChars(155330, 155330);
        this.generateChars(155351, 155352);
        this.generateChars(155368, 155368);
        this.generateChars(155381, 155381);
        this.generateChars(155427, 155427);
        this.generateChars(155484, 155484);
        this.generateChars(155604, 155604);
        this.generateChars(155616, 155616);
        this.generateChars(155643, 155643);
        this.generateChars(155660, 155660);
        this.generateChars(155671, 155671);
        this.generateChars(155744, 155744);
        this.generateChars(155885, 155885);
        this.generateChars(156193, 156193);
        this.generateChars(156238, 156238);
        this.generateChars(156248, 156248);
        this.generateChars(156268, 156268);
        this.generateChars(156272, 156272);
        this.generateChars(156294, 156294);
        this.generateChars(156307, 156307);
        this.generateChars(156492, 156492);
        this.generateChars(156563, 156563);
        this.generateChars(156660, 156660);
        this.generateChars(156674, 156674);
        this.generateChars(156813, 156813);
        this.generateChars(157138, 157138);
        this.generateChars(157302, 157302);
        this.generateChars(157310, 157310);
        this.generateChars(157360, 157360);
        this.generateChars(157416, 157416);
        this.generateChars(157446, 157446);
        this.generateChars(157469, 157469);
        this.generateChars(157564, 157564);
        this.generateChars(157644, 157644);
        this.generateChars(157674, 157674);
        this.generateChars(157759, 157759);
        this.generateChars(157834, 157834);
        this.generateChars(157917, 157917);
        this.generateChars(157930, 157930);
        this.generateChars(157966, 157966);
        this.generateChars(158033, 158033);
        this.generateChars(158063, 158063);
        this.generateChars(158173, 158173);
        this.generateChars(158194, 158194);
        this.generateChars(158202, 158202);
        this.generateChars(158238, 158238);
        this.generateChars(158249, 158249);
        this.generateChars(158253, 158253);
        this.generateChars(158296, 158296);
        this.generateChars(158348, 158348);
        this.generateChars(158391, 158391);
        this.generateChars(158397, 158397);
        this.generateChars(158463, 158463);
        this.generateChars(158540, 158540);
        this.generateChars(158556, 158556);
        this.generateChars(158753, 158753);
        this.generateChars(158761, 158761);
        this.generateChars(158835, 158835);
        this.generateChars(158941, 158941);
        this.generateChars(159135, 159135);
        this.generateChars(159237, 159237);
        this.generateChars(159296, 159296);
        this.generateChars(159319, 159319);
        this.generateChars(159333, 159333);
        this.generateChars(159448, 159448);
        this.generateChars(159636, 159636);
        this.generateChars(159734, 159736);
        this.generateChars(159984, 159984);
        this.generateChars(159988, 159988);
        this.generateChars(160013, 160013);
        this.generateChars(160057, 160057);
        this.generateChars(160351, 160351);
        this.generateChars(160389, 160389);
        this.generateChars(160516, 160516);
        this.generateChars(160625, 160625);
        this.generateChars(160730, 160731);
        this.generateChars(160766, 160766);
        this.generateChars(160784, 160784);
        this.generateChars(160841, 160841);
        this.generateChars(160902, 160902);
        this.generateChars(160957, 160957);
        this.generateChars(161300, 161301);
        this.generateChars(161329, 161329);
        this.generateChars(161412, 161412);
        this.generateChars(161427, 161427);
        this.generateChars(161550, 161550);
        this.generateChars(161571, 161571);
        this.generateChars(161601, 161601);
        this.generateChars(161618, 161618);
        this.generateChars(161776, 161776);
        this.generateChars(161970, 161970);
        this.generateChars(162181, 162181);
        this.generateChars(162208, 162208);
        this.generateChars(162215, 162215);
        this.generateChars(162366, 162367);
        this.generateChars(162403, 162403);
        this.generateChars(162436, 162436);
        this.generateChars(162602, 162602);
        this.generateChars(162713, 162713);
        this.generateChars(162730, 162730);
        this.generateChars(162739, 162739);
        this.generateChars(162750, 162750);
        this.generateChars(162759, 162759);
        this.generateChars(163000, 163000);
        this.generateChars(163232, 163232);
        this.generateChars(163344, 163344);
        this.generateChars(163503, 163503);
        this.generateChars(163572, 163572);
        this.generateChars(163767, 163767);
        this.generateChars(163777, 163777);
        this.generateChars(163820, 163820);
        this.generateChars(163827, 163827);
        this.generateChars(163833, 163833);
        this.generateChars(163978, 163978);
        this.generateChars(164027, 164027);
        this.generateChars(164030, 164031);
        this.generateChars(164037, 164037);
        this.generateChars(164073, 164073);
        this.generateChars(164080, 164080);
        this.generateChars(164180, 164180);
        this.generateChars(164189, 164189);
        this.generateChars(164359, 164359);
        this.generateChars(164471, 164471);
        this.generateChars(164482, 164482);
        this.generateChars(164557, 164557);
        this.generateChars(164578, 164578);
        this.generateChars(164595, 164595);
        this.generateChars(164733, 164733);
        this.generateChars(164746, 164746);
        this.generateChars(164813, 164813);
        this.generateChars(164872, 164872);
        this.generateChars(164876, 164876);
        this.generateChars(164949, 164949);
        this.generateChars(164968, 164968);
        this.generateChars(164999, 164999);
        this.generateChars(165227, 165227);
        this.generateChars(165269, 165269);
        this.generateChars(165320, 165321);
        this.generateChars(165496, 165496);
        this.generateChars(165525, 165525);
        this.generateChars(165591, 165591);
        this.generateChars(165626, 165626);
        this.generateChars(165802, 165802);
        this.generateChars(165856, 165856);
        this.generateChars(166033, 166033);
        this.generateChars(166214, 166214);
        this.generateChars(166216, 166217);
        this.generateChars(166222, 166222);
        this.generateChars(166251, 166251);
        this.generateChars(166279, 166280);
        this.generateChars(166305, 166305);
        this.generateChars(166330, 166331);
        this.generateChars(166336, 166336);
        this.generateChars(166415, 166415);
        this.generateChars(166430, 166430);
        this.generateChars(166441, 166441);
        this.generateChars(166467, 166467);
        this.generateChars(166513, 166513);
        this.generateChars(166553, 166553);
        this.generateChars(166605, 166605);
        this.generateChars(166621, 166621);
        this.generateChars(166628, 166628);
        this.generateChars(166726, 166726);
        this.generateChars(166729, 166729);
        this.generateChars(166734, 166734);
        this.generateChars(166849, 166849);
        this.generateChars(166895, 166895);
        this.generateChars(166974, 166974);
        this.generateChars(166983, 166983);
        this.generateChars(166989, 166991);
        this.generateChars(166993, 166993);
        this.generateChars(166995, 166996);
        this.generateChars(167114, 167114);
        this.generateChars(167117, 167117);
        this.generateChars(167122, 167122);
        this.generateChars(167184, 167184);
        this.generateChars(167281, 167281);
        this.generateChars(167321, 167321);
        this.generateChars(167419, 167419);
        this.generateChars(167439, 167439);
        this.generateChars(167455, 167455);
        this.generateChars(167478, 167478);
        this.generateChars(167481, 167481);
        this.generateChars(167561, 167561);
        this.generateChars(167577, 167577);
        this.generateChars(167655, 167655);
        this.generateChars(167659, 167659);
        this.generateChars(167670, 167670);
        this.generateChars(167730, 167730);
        this.generateChars(167928, 167928);
        this.generateChars(168540, 168540);
        this.generateChars(168608, 168608);
        this.generateChars(168625, 168625);
        this.generateChars(169053, 169053);
        this.generateChars(169086, 169086);
        this.generateChars(169104, 169104);
        this.generateChars(169146, 169146);
        this.generateChars(169189, 169189);
        this.generateChars(169423, 169423);
        this.generateChars(169599, 169599);
        this.generateChars(169640, 169640);
        this.generateChars(169644, 169644);
        this.generateChars(169705, 169705);
        this.generateChars(169712, 169712);
        this.generateChars(169732, 169732);
        this.generateChars(169753, 169753);
        this.generateChars(169776, 169776);
        this.generateChars(169808, 169809);
        this.generateChars(169996, 169997);
        this.generateChars(170000, 170000);
        this.generateChars(170182, 170182);
        this.generateChars(170498, 170498);
        this.generateChars(170610, 170610);
        this.generateChars(171377, 171377);
        this.generateChars(171416, 171416);
        this.generateChars(171475, 171475);
        this.generateChars(171477, 171477);
        this.generateChars(171483, 171483);
        this.generateChars(171520, 172287);
        this.generateChars(172432, 172432);
        this.generateChars(172513, 172513);
        this.generateChars(172616, 172616);
        this.generateChars(172938, 172938);
        this.generateChars(172940, 172940);
        this.generateChars(173086, 173086);
        this.generateChars(173111, 173111);
        this.generateChars(173456, 173456);
        this.generateChars(173553, 173553);
        this.generateChars(173570, 173570);
        this.generateChars(173586, 173586);
        this.generateChars(173594, 173594);
        this.generateChars(173642, 173642);
        this.generateChars(173746, 173746);
        this.generateChars(173783, 173791);
        this.generateChars(173878, 173878);
        this.generateChars(173981, 173981);
        this.generateChars(174045, 174045);
        this.generateChars(174136, 174136);
        this.generateChars(174141, 174141);
        this.generateChars(174254, 174254);
        this.generateChars(174301, 174301);
        this.generateChars(174331, 174331);
        this.generateChars(174359, 174359);
        this.generateChars(174448, 174448);
        this.generateChars(174599, 174599);
        this.generateChars(174602, 174602);
        this.generateChars(174640, 174640);
        this.generateChars(174646, 174646);
        this.generateChars(174680, 174680);
        this.generateChars(174946, 174946);
        this.generateChars(175027, 175027);
        this.generateChars(175706, 175706);
        this.generateChars(175824, 175824);
        this.generateChars(176020, 176020);
        this.generateChars(176034, 176034);
        this.generateChars(176086, 176086);
        this.generateChars(176337, 176337);
        this.generateChars(176421, 176421);
        this.generateChars(176423, 176424);
        this.generateChars(176439, 176440);
        this.generateChars(176621, 176621);
        this.generateChars(176705, 176705);
        this.generateChars(176827, 176827);
        this.generateChars(176896, 176896);
        this.generateChars(176936, 176936);
        this.generateChars(176991, 176991);
        this.generateChars(176994, 176995);
        this.generateChars(177007, 177007);
        this.generateChars(177010, 177010);
        this.generateChars(177019, 177019);
        this.generateChars(177021, 177021);
        this.generateChars(177156, 177156);
        this.generateChars(177168, 177168);
        this.generateChars(177171, 177171);
        this.generateChars(177249, 177249);
        this.generateChars(177314, 177314);
        this.generateChars(177383, 177383);
        this.generateChars(177385, 177385);
        this.generateChars(177391, 177391);
        this.generateChars(177398, 177398);
        this.generateChars(177401, 177401);
        this.generateChars(177421, 177422);
        this.generateChars(177462, 177462);
        this.generateChars(177539, 177539);
        this.generateChars(177556, 177556);
        this.generateChars(177560, 177560);
        this.generateChars(177582, 177583);
        this.generateChars(177587, 177587);
        this.generateChars(177589, 177589);
        this.generateChars(177593, 177593);
        this.generateChars(177639, 177639);
        this.generateChars(177643, 177643);
        this.generateChars(177648, 177648);
        this.generateChars(177652, 177652);
        this.generateChars(177692, 177693);
        this.generateChars(177700, 177700);
        this.generateChars(177702, 177704);
        this.generateChars(177706, 177706);
        this.generateChars(177708, 177708);
        this.generateChars(177801, 177801);
        this.generateChars(177810, 177810);
        this.generateChars(177812, 177814);
        this.generateChars(177837, 177837);
        this.generateChars(177882, 177882);
        this.generateChars(177886, 177886);
        this.generateChars(177897, 177897);
        this.generateChars(177901, 177901);
        this.generateChars(177910, 177910);
        this.generateChars(177960, 177960);
        this.generateChars(177973, 177977);
        this.generateChars(177984, 178205);
        this.generateChars(178360, 178360);
        this.generateChars(178374, 178374);
        this.generateChars(178378, 178378);
        this.generateChars(178671, 178671);
        this.generateChars(178779, 178779);
        this.generateChars(178803, 178803);
        this.generateChars(178817, 178817);
        this.generateChars(178840, 178840);
        this.generateChars(178887, 178887);
        this.generateChars(178999, 178999);
        this.generateChars(179039, 179039);
        this.generateChars(179042, 179042);
        this.generateChars(179050, 179050);
        this.generateChars(179068, 179068);
        this.generateChars(179075, 179075);
        this.generateChars(179227, 179227);
        this.generateChars(179233, 179233);
        this.generateChars(179248, 179248);
        this.generateChars(179366, 179366);
        this.generateChars(179544, 179544);
        this.generateChars(179575, 179575);
        this.generateChars(179580, 179580);
        this.generateChars(179591, 179591);
        this.generateChars(179703, 179703);
        this.generateChars(179753, 179753);
        this.generateChars(179997, 179997);
        this.generateChars(180265, 180266);
        this.generateChars(180353, 180353);
        this.generateChars(180393, 180393);
        this.generateChars(180416, 180416);
        this.generateChars(180426, 180426);
        this.generateChars(180693, 180693);
        this.generateChars(180697, 180697);
        this.generateChars(180702, 180702);
        this.generateChars(180729, 180729);
        this.generateChars(180860, 180860);
        this.generateChars(180872, 180872);
        this.generateChars(180892, 180892);
        this.generateChars(180900, 180900);
        this.generateChars(180918, 180918);
        this.generateChars(181015, 181015);
        this.generateChars(181083, 181083);
        this.generateChars(181089, 181089);
        this.generateChars(181092, 181092);
        this.generateChars(181126, 181126);
        this.generateChars(181335, 181335);
        this.generateChars(181384, 181384);
        this.generateChars(181396, 181396);
        this.generateChars(181399, 181399);
        this.generateChars(181570, 181570);
        this.generateChars(181643, 181643);
        this.generateChars(181662, 181662);
        this.generateChars(181779, 181779);
        this.generateChars(181784, 181784);
        this.generateChars(181793, 181794);
        this.generateChars(181801, 181801);
        this.generateChars(181803, 181805);
        this.generateChars(181807, 181807);
        this.generateChars(181813, 181813);
        this.generateChars(181826, 181826);
        this.generateChars(181834, 181835);
        this.generateChars(182060, 182060);
        this.generateChars(182063, 182063);
        this.generateChars(182175, 182175);
        this.generateChars(182209, 182209);
        this.generateChars(182252, 182252);
        this.generateChars(182269, 182269);
        this.generateChars(182294, 182294);
        this.generateChars(182410, 182410);
        this.generateChars(182464, 182464);
        this.generateChars(182489, 182489);
        this.generateChars(182494, 182494);
        this.generateChars(182497, 182497);
        this.generateChars(182515, 182515);
        this.generateChars(182535, 182535);
        this.generateChars(182538, 182538);
        this.generateChars(182557, 182557);
        this.generateChars(182704, 182704);
        this.generateChars(182720, 182720);
        this.generateChars(182786, 182786);
        this.generateChars(182798, 182798);
        this.generateChars(182909, 182909);
        this.generateChars(182925, 182925);
        this.generateChars(182953, 182953);
        this.generateChars(182994, 182994);
        this.generateChars(183081, 183081);
        this.generateChars(183083, 183083);
        this.generateChars(183085, 183086);
        this.generateChars(183089, 183089);
        this.generateChars(183096, 183099);
        this.generateChars(183103, 183103);
        this.generateChars(183105, 183105);
        this.generateChars(183114, 183114);
        this.generateChars(183118, 183118);
        this.generateChars(183130, 183131);
        this.generateChars(183139, 183140);
        this.generateChars(183145, 183145);
        this.generateChars(183148, 183148);
        this.generateChars(183151, 183151);
        this.generateChars(183155, 183155);
        this.generateChars(183158, 183158);
        this.generateChars(183160, 183160);
        this.generateChars(183164, 183164);
        this.generateChars(183204, 183204);
        this.generateChars(183217, 183217);
        this.generateChars(183220, 183220);
        this.generateChars(183231, 183232);
        this.generateChars(183246, 183246);
        this.generateChars(183382, 183382);
        this.generateChars(183391, 183391);
        this.generateChars(183473, 183473);
        this.generateChars(183485, 183485);
        this.generateChars(183495, 183495);
        this.generateChars(183541, 183542);
        this.generateChars(183549, 183549);
        this.generateChars(183551, 183551);
        this.generateChars(183554, 183555);
        this.generateChars(183562, 183562);
        this.generateChars(183568, 183568);
        this.generateChars(183688, 183689);
        this.generateChars(183691, 183691);
        this.generateChars(183693, 183693);
        this.generateChars(183695, 183696);
        this.generateChars(183711, 183712);
        this.generateChars(183720, 183720);
        this.generateChars(183725, 183726);
        this.generateChars(183731, 183731);
        this.generateChars(183765, 183765);
        this.generateChars(183813, 183813);
        this.generateChars(183832, 183832);
        this.generateChars(183834, 183834);
        this.generateChars(183843, 183843);
        this.generateChars(183846, 183846);
        this.generateChars(183850, 183850);
        this.generateChars(183930, 183930);
        this.generateChars(183932, 183932);
        this.generateChars(183944, 183944);
        this.generateChars(183955, 183955);
        this.generateChars(184925, 184925);
        this.generateChars(185218, 185218);
        this.generateChars(185668, 185668);
        this.generateChars(187466, 187466);
        this.generateChars(187658, 187658);
        this.generateChars(188436, 188436);
        this.generateChars(189035, 189036);
        this.generateChars(189039, 189039);
        this.generateChars(189466, 189466);
        this.generateChars(189480, 189480);
        this.generateChars(189698, 189698);
        this.generateChars(189701, 189701);
        this.generateChars(189706, 189706);
        this.generateChars(189873, 189873);
        this.generateChars(190026, 190027);
        this.generateChars(190324, 190325);
        this.generateChars(190329, 190329);
        this.generateChars(190706, 190707);
        this.generateChars(190770, 190770);
        this.generateChars(190965, 190965);
        this.generateChars(191035, 191035);
        this.generateChars(191069, 191070);
        this.generateChars(191141, 191141);
        this.generateChars(191261, 191261);
        this.generateChars(191265, 191265);
        this.generateChars(191334, 191334);
        this.generateChars(191336, 191336);
        this.generateChars(191449, 191449);
        this.generateChars(191472, 192093);
        this.generateChars(194692, 194692);
        this.generateChars(194742, 194742);
        this.generateChars(196608, 196991);
        this.generateChars(197083, 197083);
        this.generateChars(197091, 197091);
        this.generateChars(197312, 197312);
        this.generateChars(197376, 197376);
        this.generateChars(197378, 197378);
        this.generateChars(198535, 198535);
        this.generateChars(198616, 198616);
        this.generateChars(198744, 198744);
        this.generateChars(198907, 198907);
        this.generateChars(198933, 198933);
        this.generateChars(199359, 199359);
        this.generateChars(199676, 199676);
        this.generateChars(199794, 199794);
        this.generateChars(200029, 200030);
        this.generateChars(200039, 200039);
        this.generateChars(200052, 200052);
        this.generateChars(200060, 200060);
        this.generateChars(200074, 200074);
        this.generateChars(200165, 200165);
        this.generateChars(200300, 200300);
        this.generateChars(200413, 200414);
        this.generateChars(200535, 200535);
        this.generateChars(200538, 200538);
        this.generateChars(200564, 200564);
        this.generateChars(200573, 200573);
        this.generateChars(200585, 200585);
        this.generateChars(200608, 200608);
        this.generateChars(200619, 200619);
        this.generateChars(200812, 200812);
        this.generateChars(200848, 200848);
        this.generateChars(200873, 200873);
        this.generateChars(200881, 200881);
        this.generateChars(200938, 200938);
        this.generateChars(200945, 200945);
        this.generateChars(200954, 200954);
        this.generateChars(201035, 201035);
        this.generateChars(201051, 201051);
        this.generateChars(201187, 201187);
        this.generateChars(201201, 201201);
        this.generateChars(201300, 201300);
        this.generateChars(201417, 201417);
        this.generateChars(201518, 201518);
        this.generateChars(201660, 201660);
        this.generateChars(201727, 201727);
        this.generateChars(202126, 202126);
        this.generateChars(202130, 202130);
        this.generateChars(203666, 203666);
        this.generateChars(203676, 203676);
        this.generateChars(203903, 203903);
        this.generateChars(204063, 204063);
        this.generateChars(204081, 204081);
        this.generateChars(204295, 204295);
        this.generateChars(204365, 204365);
        this.generateChars(204803, 204803);
        this.generateChars(204942, 204942);
        this.generateChars(205335, 205335);
        this.generateChars(205540, 205540);
        this.generateChars(205659, 205659);
        this.generateChars(205677, 205677);
        this.generateChars(205695, 205695);
        this.generateChars(917504, 917631);
        this.generateChars(917760, 917999);
        this.generateChars(1048574, 1048575);

    }

    private void createFontAtlas() {

        atlasImage = new BufferedImage(2048, 2048, BufferedImage.TYPE_INT_ARGB);

        this.generateAllMinecraftChars();

        // debug if u need
        /*File outputFile = new File("output.png");

        try {
            ImageIO.write(atlasImage, "png", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }*/

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
        this.renderText(text, x - (float) getStringWidth(String.valueOf(text.charAt(0))) / 2, y - (float) getBaseAscender() / 2, color);
    }

    public void renderVCenteredText(String text, float x, float y, float[] color) {
        this.renderText(text, x, y - (float) this.getBaseAscender() / 2, color);
    }

    public void renderHVCenteredText(String text, float x, float y, float[] color) {
        this.renderText(text, x - (float) getStringWidth(text) / 2, y - (float) getBaseAscender() / 2, color);
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

    public void renderOutlinedText(String text, float x, float y, float[] textColor, float[] outlineColor) {

        if(textColor.length != 4)
            textColor = new float[]{textColor[0], textColor[1], textColor[2], 255};

        if(outlineColor.length != 4)
            outlineColor = new float[]{outlineColor[0], outlineColor[1], outlineColor[2], 255};

        this.renderText(text, x - 1, y, outlineColor);
        this.renderText(text, x + 1, y, outlineColor);
        this.renderText(text, x, y + 1, outlineColor);
        this.renderText(text, x, y - 1, outlineColor);

        this.renderText(text, x + 1, y + 1, outlineColor);
        this.renderText(text, x - 1, y - 1, outlineColor);
        this.renderText(text, x + 1, y - 1, outlineColor);
        this.renderText(text, x - 1, y + 1, outlineColor);

        this.renderText(text, x, y, textColor);

    }

    public void renderTextWithShadow(String text, float x, float y, float[] color) {
        this.renderText(text, x + 1, y + 1, sisicat.main.utilities.Color.c12);
        this.renderText(text, x, y, color);
    }

    @Override
    public void close() {

        if (library != null) library.done();
        if (face != null) face.done();

    }

}