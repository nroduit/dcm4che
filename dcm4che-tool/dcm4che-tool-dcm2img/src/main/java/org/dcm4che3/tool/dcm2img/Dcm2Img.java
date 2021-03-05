package org.dcm4che3.tool.dcm2img;

import org.dcm4che3.data.Implementation;
import org.dcm4che3.img.DicomImageReadParam;
import org.dcm4che3.img.ImageTranscodeParam;
import org.dcm4che3.img.Transcoder;
import org.dcm4che3.img.data.PrDicomObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.awt.*;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * @author Nicolas Roduit
 * @since Jun 2020
 */
@CommandLine.Command(
    name = "dcm2img",
    mixinStandardHelpOptions = true,
    versionProvider = Dcm2Img.ModuleVersionProvider.class,
    descriptionHeading = "%n",
    description = "The dcm2img utility allows converting a DICOM file into a standard image formats (JPEG, PNG, TIF, JP2, PNM, BMP or HDR).",
    parameterListHeading = "%nParameters:%n",
    optionListHeading = "%nOptions:%n",
    showDefaultValues = true,
    footerHeading = "%nExample:%n",
    footer = {"$ dcm2img -f TIF -o \"/tmp/dcm/\" \"home/user/image.dcm\"",
        "Write a tif image into /tmp/dcm/ by extracting the pixel data from image.dcm. Without the '--preserve' " +
            "option, the default Window/Level is applied to obtain a 8 bit/pixel image."}
)
public class Dcm2Img implements Callable<Integer> {

    @CommandLine.Parameters(
        description = "Path of the DICOM input file.",
        arity = "1",
        index = "0")
    Path dcmfile;

    @CommandLine.Option(
        names = {"-o", "--output"},
        required = true,
        description = "Path of the output image. if the path is a directory then the filename is taken from the source path.",
        arity = "1")
    Path outDir;

    @CommandLine.Option(
        names = {"-p", "--input-dcm-pr"},
        description = "Path of the DICOM Presentation State file to apply")
    Path prfile;

    @CommandLine.Option(names = {"-f", "--format"}, description = "Output image format: ${COMPLETION-CANDIDATES}")
    Transcoder.Format format = Transcoder.Format.JPEG;

    @CommandLine.Option(names = "--rgb-lossy", negatable = true, description = "Keep RGB model with JPEG lossy. " +
        "If FALSE the reader force using YBR color model")
    boolean keepRgb = false;

    @CommandLine.Option(names = {"-q", "--quality"}, description = "JPEG compression quality between 1 to 100 (100 is the best lossy quality).")
    Integer jpegCompressionQuality = 80;

    @CommandLine.Option(names = "--overlay-color", description = "The color defined in Hex Code (RRGGBB or AARRGGBB with " +
        "alpha) for displaying overlays. Ex. red color is FF0000")
    Color overlayColor = new Color(36, 36, 148);

    @CommandLine.Option(names = "--no-pixel-padding", description = "Do not exclude the pixel padding values.")
    boolean noPixelPadding;

    @CommandLine.Option(names = "--inverse-lut", description = "Invert the lookup table")
    boolean inverseLut;

    @CommandLine.Option(names = "--win-level", arity = "2", description = "Set the Window/Level.")
    Double[] winLevel = null;

    @CommandLine.Option(names = "--win-level-index", description = "Select the embedded Window/Level where the index defined the position.")
    Integer winIndex = 0;

    @CommandLine.Option(names = "--preserve", negatable = true, description = "It preserves the raw data when the pixel depth is more " +
        "than 8 bit. JPEG and BMP don't support more than 8-bit; PNG, JP2 and PNM support unsigned 16-bit; " +
        "TIF supports unsigned 16-bit, 32-bit float and 64-bit double; HDR supports 64-bit double. " +
        "When FALSE, the default value applies the W/L, the output image will be always a 8-bit per sample image.")
    boolean preservePixelDepth;

    public static void main(String[] args) {
        CommandLine cl = new CommandLine(new Dcm2Img());
        cl.registerConverter(Color.class, Dcm2Img::hexadecimal2Color);
        cl.execute(args);
    }

    @Override
    public Integer call() throws Exception {
        DicomImageReadParam readParam = new DicomImageReadParam();
        readParam.setKeepRgbForLossyJpeg(keepRgb);
        readParam.setInverseLut(inverseLut);
        readParam.setApplyPixelPadding(!noPixelPadding);
        readParam.setOverlayColor(overlayColor);
        readParam.setWindowIndex(winIndex);
        readParam.setFillOutsideLutRange(true);
        if(winLevel != null){
            readParam.setWindowWidth(winLevel[0]);
            readParam.setWindowCenter(winLevel[1]);
        }
        if (prfile != null) {
            readParam.setPresentationState(PrDicomObject.getPresentationState(prfile.toString()));
        }
        ImageTranscodeParam params = new ImageTranscodeParam(readParam, format);
        params.setJpegCompressionQuality(jpegCompressionQuality);
        params.setPreserveRawImage(preservePixelDepth);
        Transcoder.dcm2image(dcmfile, outDir, params);

        System.out.println(String.format(
            "Extract the image(s) of %s to %s.", dcmfile, outDir));
        return 0;
    }


    static class ModuleVersionProvider implements CommandLine.IVersionProvider {
        public String[] getVersion() {
            return new String[]{Implementation.getVersionName()};
        }
    }

    public static Color hexadecimal2Color(String hexColor) {
        int intValue = 0xff000000;
        if (hexColor != null && hexColor.length() > 6) {
            intValue = (int) (Long.parseLong(hexColor, 16));
        } else if (hexColor != null) {
            intValue |= Integer.parseInt(hexColor, 16);
        }
        return new Color(intValue, true);
    }
}
